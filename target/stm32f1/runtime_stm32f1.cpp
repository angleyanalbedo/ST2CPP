#include "rt_runtime.h"
#include "rt_plc.h"
#include "stm32f1xx_hal.h"

#if defined(RT_PLATFORM_BARE_METAL)

using namespace rt_plc;

extern "C" void plc_platform_init();

static Scheduler sched;
static CompositeTCI compositeTCI;
static bool initialized = false;

extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

extern "C" void plc_runtime_init() {
    configureRuntime(sched, compositeTCI);
    sched.start(StartupMode::COLD);
    initialized = true;

    platform::logInfo("PLC F1 Runtime initialized\n");
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

    platform::logInfo("PLC F1 Ready.\n");

    while (1) {
        __WFI();
    }
}

#else

int main() { return 0; }

#endif
