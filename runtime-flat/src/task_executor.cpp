#include "core/task_executor.h"

namespace rt_plc {

bool TaskExecutor::execute(Task& task, int taskIndex, TaskExecutorContext& ctx) {
    int64_t execStart = platform::steadyUs();

    task.state = TaskState::RUNNING;

    task.executePOUs(ctx.gvl, ctx.image, ctx.baseCycleTime);

    for (int j = 0; j < task.programCount; j++) {
        int pIdx = task.programIndices[j];
        if (pIdx >= 0 && pIdx < ctx.programCapacity) {
            ctx.programs[pIdx].doCyclic(ctx.gvl, ctx.image, ctx.baseCycleTime);
        }
    }

    task.cycleCount++;
    task.lastRunTime = ctx.systemTime;

    int64_t execEnd = platform::steadyUs();
    task.lastExecTime = execEnd - execStart;
    if (task.lastExecTime > task.maxExecTime) {
        task.maxExecTime = task.lastExecTime;
    }

    if (ctx.watchdog.check(task, taskIndex)) {
        task.state = TaskState::OVERRUN;
        task.overrunCount++;
        ctx.diag.totalOverruns++;
#ifdef ENABLE_DIAG
        RT_LOG_ERR("[Watchdog] Task '%s' overrun: %lld us (limit: %lld us)\n",
                task.name, (long long)task.lastExecTime,
                (long long)(task.watchdogLimit > 0 ? task.watchdogLimit : ctx.watchdog.defaultLimit));
#endif
        if (task.overrunCount >= (uint32_t)ctx.maxConsecutiveOverruns) {
            ctx.errorMgr.report(ErrorCode::WATCHDOG_TIMEOUT, 0, 0,
                                task.name, ctx.systemTime);
            return false;
        }
    } else {
        task.state = (task.trigger == TaskTrigger::EVENT) ? TaskState::IDLE : TaskState::READY;
        task.overrunCount = 0;
    }

    return !ctx.errorMgr.fatalMode;
}

} // namespace rt_plc
