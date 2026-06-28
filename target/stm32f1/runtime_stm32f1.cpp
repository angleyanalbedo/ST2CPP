#include "rt_runtime.h"
#include "rt_plc.h"
#include "stm32f1xx_hal.h"

#if defined(RT_PLATFORM_BARE_METAL)

using namespace rt_plc;

extern void registerAllPOUs(POURegistry& reg);
extern "C" void plc_platform_init();

static Scheduler sched;
static bool initialized = false;

static constexpr TIME PLC_BASE_CYCLE_US = 1000;
static constexpr TIME PLC_TASK_INTERVAL = 1000;
static constexpr int  PLC_TASK_PRIORITY = 5;
static constexpr TIME PLC_WATCHDOG_US   = 5000;


extern "C" void plc_runtime_init() {
    POURegistry reg;
    registerAllPOUs(reg);

    sched.setBaseCycle(T_us(PLC_BASE_CYCLE_US));
    sched.gvl.errorMgr = &sched.errorMgr;

    int mainTask = sched.addCyclicTask("Main", PLC_TASK_PRIORITY,
                                       T_us(PLC_TASK_INTERVAL));
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

    sched.setTaskWatchdog(mainTask, T_us(PLC_WATCHDOG_US));
    sched.start(StartupMode::COLD);
    initialized = true;

    platform::logInfo("PLC F1 Runtime: %d POU(s), cycle=%lldus\n",
                      reg.count(), (long long)PLC_BASE_CYCLE_US);
}


extern "C" void plc_runtime_tick() {
    if (initialized) {
        sched.tick();
    }
}


extern "C" Scheduler* plc_runtime_get_scheduler() {
    return initialized ? &sched : nullptr;
}


int main() {
    plc_platform_init();
    plc_runtime_init();

    platform::logInfo("PLC F1 Ready. Tick=1ms LED=100ms\n");

    while (1) {
        __WFI();
    }
}

#else

int main() { return 0; }

#endif // RT_PLATFORM_BARE_METAL
