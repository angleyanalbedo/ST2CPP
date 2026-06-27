/**
 * runtime_rpi.cpp — Raspberry Pi Linux PLC 运行时入口
 *
 * 启动方式：
 *   sudo ./plc_runtime_rpi                    # 默认 1ms 周期
 *   sudo ./plc_runtime_rpi --cycle-us 500     # 500us 周期
 *   sudo chrt -f 99 ./plc_runtime_rpi         # SCHED_FIFO 最高优先级
 *
 * 编译：
 *   g++ -O2 -DRT_PLATFORM_LINUX -I include/ src/*.cpp platform_rpi.cpp runtime_rpi.cpp \
 *       -lpthread -o plc_runtime_rpi
 */
#include "rt_runtime.h"
#include "rt_plc.h"
#include "hal/gpio_tci.h"

#include <csignal>
#include <unistd.h>
#include <cstring>

using namespace rt_plc;

extern void registerAllPOUs(POURegistry& reg);
extern "C" int  plc_rpi_create_timer(int64_t intervalUs);
extern "C" void plc_rpi_destroy_timer();
extern "C" int  plc_rpi_set_rt_priority(int priority);
extern "C" uint32_t plc_rpi_get_tick_count();
extern "C" int64_t  plc_rpi_get_uptime_us();
extern "C" void plc_rpi_print_jitter_stats();

static Scheduler sched;
static RpiGpioTCI gpioTCI;
static bool initialized = false;
static int64_t cycleUs = 1000;

static void plc_init() {
    POURegistry reg;
    registerAllPOUs(reg);

    sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;

    // 注册 GPIO I/O 同步
    if (gpioTCI.init() == 0) {
        sched.setTCI(&gpioTCI);
    } else {
        fprintf(stderr, "[WARN] GPIO TCI init failed, running without hardware I/O\n");
    }

    int mainTask = sched.addCyclicTask("Main", 5, T_us(cycleUs));
    if (mainTask < 0) {
        platform::logErr("FATAL: Failed to create main task\n");
        return;
    }

    for (int i = 0; i < reg.count(); i++) {
        const auto& e = reg.entries()[i];
        if (e.cbs.cyclic) {
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic,
                                           e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) {
                sched.addProgramToTask(mainTask, progIdx);
            }
        }
    }

    sched.setTaskWatchdog(mainTask, T_us(cycleUs * 5));
    sched.start(StartupMode::COLD);
    initialized = true;

    platform::logInfo("PLC Runtime: %d POU(s), cycle=%lldus\n",
                      reg.count(), (long long)cycleUs);
}

extern "C" void plc_runtime_tick() {
    if (initialized) {
        sched.tick();
    }
}

static volatile bool running = true;
static void signal_handler(int) { running = false; }

int main(int argc, char* argv[]) {
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [--cycle-us <microseconds>]\n", argv[0]);
            return 0;
        }
    }

    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    plc_rpi_set_rt_priority(90);
    plc_init();

    if (plc_rpi_create_timer(cycleUs) != 0) {
        platform::logErr("Timer failed, falling back to busy loop\n");
        while (running) {
            plc_runtime_tick();
            platform::sleepUs(cycleUs);
        }
    } else {
        platform::logInfo("Timer started: %lldus period\n", (long long)cycleUs);
        platform::logInfo("Press Ctrl+C to stop\n");

        int64_t lastDiagUs = 0;
        while (running) {
            usleep(100000);
            int64_t now = platform::steadyUs();
            if (now - lastDiagUs > 5000000) {
                lastDiagUs = now;
                platform::logInfo("[DIAG] ticks=%u uptime=%lldms\n",
                                 plc_rpi_get_tick_count(),
                                 plc_rpi_get_uptime_us() / 1000);
            }
        }
    }

    plc_rpi_destroy_timer();
    sched.stop();
    plc_rpi_print_jitter_stats();
    platform::logInfo("Stopped. Total ticks: %u\n", plc_rpi_get_tick_count());
    return 0;
}
