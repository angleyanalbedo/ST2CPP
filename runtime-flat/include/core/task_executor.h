#pragma once

#include "constants.h"
#include "diag_manager.h"
#include "error_manager.h"
#include "gvl.h"
#include "program.h"
#include "task.h"
#include "watchdog.h"
#include "rt_plc.h"

namespace rt_plc {

struct TaskExecutorContext {
    GVL&             gvl;
    ProcessImage&    image;
    ProgramInstance* programs;
    int              programCapacity;
    Watchdog&        watchdog;
    ErrorManager&    errorMgr;
    DiagManager&     diag;
    TIME             baseCycleTime;
    TIME             systemTime;
    int              maxConsecutiveOverruns;
};

class TaskExecutor {
public:
    bool execute(Task& task, int taskIndex, TaskExecutorContext& ctx);
};

} // namespace rt_plc
