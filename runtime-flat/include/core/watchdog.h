#pragma once

#include "types.h"
#include "task.h"

namespace rt_plc {

struct Watchdog {
    TIME    defaultLimit = 0;
    bool    tripped      = false;
    int     trippedTask  = -1;

    void setDefault(TIME limit);
    void reset();
    bool check(Task& task, int taskIndex);
};

} // namespace rt_plc
