/**
 * runtime_rpi.cpp — Raspberry Pi Linux PLC 运行时
 *
 * 启动方式：
 *   sudo ./plc_runtime_rpi                         # 默认 GPIO, 1ms 周期
 *   sudo ./plc_runtime_rpi --cycle-us 500           # 500us 周期
 */
#include "rt_runtime.h"
#include "rt_plc.h"
#include "core/platform.h"

#include <csignal>
#include <unistd.h>
#include <cstring>

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

// ═══ PLC ═──
extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

static void plc_init() {
    configureRuntime(sched, compositeTCI);
    if (cycleUs != sched.baseCycleTime)
        sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;
    
    sched.setTaskWatchdog(0, T_us(cycleUs * 5));
    sched.start(StartupMode::COLD);
    initialized = true;

    platform::logInfo("PLC Runtime: cycle=%lldus\n", (long long)cycleUs);
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
            printf("Usage: %s [options]\n", argv[0]);
            printf("  --cycle-us <us>  PLC cycle (us, default 1000)\n");
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

    struct timespec next;
    clock_gettime(CLOCK_MONOTONIC, &next);

    int64_t lastDiagUs = 0;
    while (running) {
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, nullptr);

        next.tv_nsec += cycleUs * 1000;
        while (next.tv_nsec >= 1000000000L) {
            next.tv_sec++;
            next.tv_nsec -= 1000000000L;
        }

        plc_runtime_tick();
        plc_rpi_jitter_sample();

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
