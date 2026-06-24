#pragma once

#include "gvl.h"
#include "types.h"
#include "rt_plc.h"

namespace rt_plc {

// PROGRAM 生命周期函数（与 rt_plc.h 中的 ProgramCallbacks 一致）
// 这里重定义避免循环依赖
typedef void (*PRG_InitFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_PreFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_CyclicFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
typedef void (*PRG_PostFunc)(GVL& gvl, ProcessImage& io);

struct ProgramInstance {
    char            name[32] = {0};
    ProgramPhase    phase    = ProgramPhase::UNINIT;

    PRG_InitFunc    initFunc   = nullptr;
    PRG_PreFunc     preFunc    = nullptr;   // 可选
    PRG_CyclicFunc  cyclicFunc = nullptr;   // 必需
    PRG_PostFunc    postFunc   = nullptr;   // 可选

    uint64_t        cycleCount = 0;

    // 执行当前阶段的逻辑
    void doInit(GVL& gvl, ProcessImage& io);
    void doPre(GVL& gvl, ProcessImage& io);
    void doCyclic(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
    void doPost(GVL& gvl, ProcessImage& io);
};

} // namespace rt_plc
