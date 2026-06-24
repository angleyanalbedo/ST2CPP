#pragma once

#include "core/constants.h"
#include "core/types.h"
#include "core/gvl.h"
#include "core/program.h"
#include "core/task.h"
#include "core/event.h"
#include "core/watchdog.h"
#include "core/diag.h"
#include "rt_plc.h"

#include <cstdint>
#include <cstring>
#include <cstdio>
#include <chrono>
#include <thread>

namespace rt_plc {

class Scheduler {
public:
    ProcessImage  image;
    GVL           gvl;
    TCI*          tci          = nullptr;
    SystemState   systemState  = SystemState::STOP;
    Watchdog      watchdog;
    ErrorManager  errorMgr;
    DiagStats     diag;
    plc_lock      gvlLock;     // GVL 访问互斥锁

    // 时间
    uint64_t totalTicks    = 0;
    TIME     systemTime    = 0;
    TIME     baseCycleTime = 1000;  // 基础节拍（微秒），默认 1ms

    // 当前扫描阶段（可查询）
    ScanPhase currentPhase = ScanPhase::IDLE;

    // ─── 配置 API ───

    void setTCI(TCI* t) { tci = t; }
    void setBaseCycle(TIME us) { baseCycleTime = us; }
    void setErrorHandler(ErrorHandler h) { errorMgr.setHandler(h); }

    // 设置 GVL RETAIN 区域
    void setRetainRegion(size_t start, size_t end) {
        gvl.setRetainRegion(start, end);
    }

    // 添加 PROGRAM 实例
    int addProgram(const char* name,
                   PRG_InitFunc init,
                   PRG_CyclicFunc cyclic,
                   PRG_PreFunc pre = nullptr,
                   PRG_PostFunc post = nullptr);

    // 添加 Cyclic 任务
    int addCyclicTask(const char* name, int priority, TIME intervalUs);

    // 添加 Freewheeling 任务
    int addFreewheelingTask(const char* name, int priority);

    // 添加 Event 任务
    int addEventTask(const char* name, int priority,
                     EventConditionFunc cond, EventEdge edge = EventEdge::RISING);

    // 给任务添加 POU
    bool addPOU(int taskIndex, POUFunc func);

    // 给任务添加 PROGRAM 实例
    bool addProgramToTask(int taskIndex, int programIndex);

    // 设置任务级看门狗
    void setTaskWatchdog(int taskIndex, TIME limitUs);


    // ─── 启动 / 停止 ───

    // 启动系统
    void start(StartupMode mode = StartupMode::COLD);

    // 停止系统
    void stop();

    void pause();
    void resume();
    void error();

    // 从 ERROR 恢复（需要手动确认后调用）
    void resetError();


    // ─── 运行循环 ───

    void run(StartupMode mode = StartupMode::COLD);


    // ─── 单次 Tick（分阶段扫描） ───

    void tick();


    // ─── 查询 API ───

    int              taskCount()     const { return taskCount_; }
    int              eventCount()    const { return eventCount_; }
    int              programCount()  const { return programCount_; }
    const Task&      task(int idx)   const { return tasks_[idx]; }
    Task&            task(int idx)         { return tasks_[idx]; }
    const ProgramInstance& program(int idx) const { return programs_[idx]; }

    // 诊断输出
    void printDiag() const;


private:
    static constexpr int MAX_CONSECUTIVE_OVERRUNS = 10;

    Task            tasks_[MAX_TASKS];
    Event           events_[MAX_EVENTS];
    ProgramInstance programs_[MAX_PROGRAMS];
    int     taskCount_    = 0;
    int     eventCount_   = 0;
    int     programCount_ = 0;
    int     taskOrder_[MAX_TASKS];
    bool    sortNeeded_   = false;
    std::chrono::steady_clock::time_point runStartWall_;

    // 查找空闲槽位
    int findFreeTask();
    int findFreeEvent();
    int findFreeProgram();

    void sortTasksByPriority();

    bool shouldRun(Task& task) const;

    void checkEvents();

    void syncInputs();
    void syncOutputs();

    static const char* stateName(SystemState s);
    static const char* triggerName(TaskTrigger t);
    static const char* phaseName(ProgramPhase p);
};

} // namespace rt_plc
