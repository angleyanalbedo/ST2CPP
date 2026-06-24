#include "core/watchdog.h"

namespace rt_plc {

void Watchdog::setDefault(TIME limit) {
    defaultLimit = limit;
}

void Watchdog::reset() {
    tripped = false;
    trippedTask = -1;
}

bool Watchdog::check(Task& task, int taskIndex) {
    TIME limit = task.watchdogLimit > 0 ? task.watchdogLimit : defaultLimit;
    if (limit > 0 && task.lastExecTime > limit) {
        tripped = true;
        trippedTask = taskIndex;
        return true;
    }
    return false;
}

} // namespace rt_plc
