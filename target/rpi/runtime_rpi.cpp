/**
 * runtime_rpi.cpp — Raspberry Pi Linux PLC 运行时入口
 *
 * 启动方式：
 *   sudo ./plc_runtime_rpi                         # 默认 GPIO, 1ms 周期
 *   sudo ./plc_runtime_rpi --cycle-us 500           # 500us 周期
 *   sudo ./plc_runtime_rpi --tci ethercat           # EtherCAT 模式
 *   sudo ./plc_runtime_rpi --tci both               # GPIO + EtherCAT 同时
 *   sudo ./plc_runtime_rpi --tci both --ecat-if eth0
 */
#include "rt_runtime.h"
#include "rt_plc.h"
#include "hal/gpio_tci.h"

#if defined(RT_ETHERCAT_ENABLED)
#include "../soem/ethercat_tci.h"
#endif

#include <csignal>
#include <unistd.h>
#include <cstring>
#include <time.h>

using namespace rt_plc;

extern void registerAllPOUs(POURegistry& reg);
extern "C" int  plc_rpi_set_rt_priority(int priority);
extern "C" uint32_t plc_rpi_get_tick_count();
extern "C" int64_t  plc_rpi_get_uptime_us();
extern "C" void plc_rpi_print_jitter_stats();
extern "C" void plc_rpi_jitter_init();
extern "C" void plc_rpi_jitter_sample();
extern "C" void plc_rpi_jitter_set_target(int64_t targetUs);

static Scheduler sched;
static CompositeTCI compositeTCI;
static bool initialized = false;
static int64_t cycleUs = 1000;

// TCI 配置
enum class TciMode { GPIO, ETHERCAT, BOTH };
static TciMode tciMode = TciMode::GPIO;
static char ecatIfname[32] = "eth0";

static void plc_init() {
    POURegistry reg;
    registerAllPOUs(reg);

    sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;

    // ─── 初始化 TCI ───
    if (tciMode == TciMode::GPIO || tciMode == TciMode::BOTH) {
        static RpiGpioTCI gpioTCI;
        if (gpioTCI.init() == 0) {
            compositeTCI.add(&gpioTCI);
            platform::logInfo("GPIO TCI registered\n");
        } else {
            platform::logErr("GPIO TCI init failed\n");
        }
    }

#if defined(RT_ETHERCAT_ENABLED)
    if (tciMode == TciMode::ETHERCAT || tciMode == TciMode::BOTH) {
        static EthercatTCI ecatTCI;
        if (ecatTCI.init(ecatIfname) == 0) {
            compositeTCI.add(&ecatTCI);
            platform::logInfo("EtherCAT TCI registered on %s\n", ecatIfname);
        } else {
            platform::logErr("EtherCAT TCI init failed on %s\n", ecatIfname);
        }
    }
#else
    if (tciMode == TciMode::ETHERCAT || tciMode == TciMode::BOTH) {
        platform::logErr("EtherCAT not enabled at compile time. Rebuild with -DRT_ETHERCAT_ENABLED\n");
    }
#endif

    if (compositeTCI.count() > 0) {
        sched.setTCI(&compositeTCI);
        platform::logInfo("TCI: %d backend(s) active\n", compositeTCI.count());
    } else {
        platform::logErr("No TCI backends active, running without hardware I/O\n");
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
        } else if (strcmp(argv[i], "--tci") == 0 && i + 1 < argc) {
            i++;
            if (strcmp(argv[i], "gpio") == 0) tciMode = TciMode::GPIO;
            else if (strcmp(argv[i], "ethercat") == 0) tciMode = TciMode::ETHERCAT;
            else if (strcmp(argv[i], "both") == 0) tciMode = TciMode::BOTH;
            else fprintf(stderr, "Unknown TCI mode: %s (use gpio/ethercat/both)\n", argv[i]);
        } else if (strcmp(argv[i], "--ecat-if") == 0 && i + 1 < argc) {
            strncpy(ecatIfname, argv[i + 1], sizeof(ecatIfname) - 1);
            i++;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [options]\n", argv[0]);
            printf("  --cycle-us <us>     PLC cycle time (default: 1000)\n");
            printf("  --tci <mode>        I/O mode: gpio, ethercat, both (default: gpio)\n");
            printf("  --ecat-if <name>    EtherCAT interface (default: eth0)\n");
            return 0;
        }
    }

    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    plc_rpi_set_rt_priority(90);
    plc_init();

    platform::logInfo("Timer started: %lldus period\n", (long long)cycleUs);
    platform::logInfo("Press Ctrl+C to stop\n");

    plc_rpi_jitter_init();
    plc_rpi_jitter_set_target(cycleUs);

    // 单线程 clock_nanosleep 驱动，避免 SIGEV_THREAD 调度抖动
    struct timespec next;
    clock_gettime(CLOCK_MONOTONIC, &next);

    int64_t lastDiagUs = 0;
    while (running) {
        // 精确等待到下一个周期
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, nullptr);

        // 推进下一个周期时间点（不依赖当前时间，避免累积漂移）
        next.tv_nsec += cycleUs * 1000;
        while (next.tv_nsec >= 1000000000L) {
            next.tv_sec++;
            next.tv_nsec -= 1000000000L;
        }

        // 执行 PLC tick
        plc_runtime_tick();
        plc_rpi_jitter_sample();

        // 每 5 秒输出一次诊断
        int64_t now = platform::steadyUs();
        if (now - lastDiagUs > 5000000) {
            lastDiagUs = now;
            platform::logInfo("[DIAG] ticks=%u uptime=%lldms\n",
                             plc_rpi_get_tick_count(),
                             plc_rpi_get_uptime_us() / 1000);
        }
    }

    sched.stop();
    plc_rpi_print_jitter_stats();
    platform::logInfo("Stopped. Total ticks: %u\n", plc_rpi_get_tick_count());
    return 0;
}
