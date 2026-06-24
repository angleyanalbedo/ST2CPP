#pragma once

#include "constants.h"
#include "types.h"
#include "gvl.h"
#include "program.h"
#include "rt_plc.h"

namespace rt_plc {

// POU 函数签名：接收 GVL、ProcessImage、当前周期时间
typedef void (*POUFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);

struct Task {
    char        name[32] = {0};
    int         priority = MAX_PRIORITY;
    TaskTrigger trigger  = TaskTrigger::CYCLIC;

    // Cyclic 参数
    TIME  interval    = 0;
    TIME  lastRunTime = 0;

    // 挂载的 POU 列表
    POUFunc pous[MAX_POUS_PER_TASK] = {};
    int     pouCount = 0;

    // 挂载的 PROGRAM 实例列表（通过索引引用 Scheduler::programs_）
    int     programIndices[MAX_POUS_PER_TASK] = {};
    int     programCount = 0;

    // 运行时状态
    TaskState state         = TaskState::IDLE;
    uint64_t  cycleCount    = 0;
    TIME      lastExecTime  = 0;
    TIME      maxExecTime   = 0;
    uint32_t  overrunCount  = 0;
    TIME      watchdogLimit = 0;

    bool addPOU(POUFunc func);
    bool addProgram(int progIndex);
    void executePOUs(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
    bool isDue(TIME currentTime) const;
};

} // namespace rt_plc
