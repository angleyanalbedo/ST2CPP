#include "rt_runtime.h"
#include "rt_plc.h"

void __attribute__((weak)) registerAllPOUs(rt_plc::POURegistry& reg) {
    (void)reg;
}
