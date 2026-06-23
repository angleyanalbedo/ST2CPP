/**
 * rt_runtime.h — PLC 运行时调度框架
 *
 * 多任务调度器，支持：
 *   - Cyclic 任务（固定周期）
 *   - Event 任务（事件触发，上升沿/下降沿）
 *   - Freewheeling 任务（连续执行）
 *   - 全局变量表 GVL（跨 POU 变量访问，支持 RETAIN 区域）
 *   - PROGRAM 实例生命周期（Init → FirstScan → Cyclic → Post）
 *   - 分阶段扫描周期（ReadInputs → LogicSolve → WriteOutputs → Housekeeping）
 *   - 看门狗（任务级 + 全局）
 *   - 冷启动 / 暖启动
 *   - 运行时错误管理
 *   - 系统状态机（STOP → RUN → ERROR → PAUSED）
 *
 * 依赖: rt_plc.h（类型定义 + 标准功能块 + 错误处理 + 互斥锁）
 */
#ifndef RT_RUNTIME_H
#define RT_RUNTIME_H

#include "rt_plc.h"
#include <cstdint>
#include <cstring>
#include <cstdio>

namespace rt_plc {

// ═══════════════════════════════════════════════════════
// 常量
// ═══════════════════════════════════════════════════════

constexpr int    MAX_TASKS         = 16;
constexpr int    MAX_EVENTS        = 16;
constexpr int    MAX_PROGRAMS      = 32;    // 最大 PROGRAM 实例数
constexpr int    MAX_POUS_PER_TASK = 8;
constexpr size_t GVL_SIZE          = 65536; // 64 KB 全局变量区
constexpr size_t GVL_RETAIN_SIZE   = 8192;  // 8 KB RETAIN 区域（暖启动保留）
constexpr int    MAX_PRIORITY      = 31;
constexpr int    MIN_PRIORITY      = 0;


// ═══════════════════════════════════════════════════════
// 枚举
// ═══════════════════════════════════════════════════════

enum class TaskTrigger : uint8_t {
    CYCLIC,
    EVENT,
    FREEWHEELING
};

enum class SystemState : uint8_t {
    STOP,
    STARTING,   // 正在启动（Init/Pre 阶段）
    RUN,
    STOPPING,   // 正在停止（Post 阶段）
    ERROR,
    PAUSED
};

enum class TaskState : uint8_t {
    IDLE,
    READY,
    RUNNING,
    OVERRUN,
    ERROR
};

enum class EventEdge : uint8_t {
    RISING,
    FALLING,
    BOTH
};

enum class StartupMode : uint8_t {
    COLD,   // 冷启动：全部清零
    WARM    // 暖启动：保留 RETAIN 区域
};

// 扫描周期阶段
enum class ScanPhase : uint8_t {
    IDLE,
    READ_INPUTS,
    LOGIC_SOLVE,
    WRITE_OUTPUTS,
    HOUSEKEEPING
};


// ═══════════════════════════════════════════════════════
// GVL: 全局变量表（跨 POU 共享）
// ═══════════════════════════════════════════════════════

struct GVL {
    uint8_t memory[GVL_SIZE];

    // RETAIN 区域标记：偏移 [retainStart, retainEnd) 在暖启动时保留
    size_t retainStart = 0;
    size_t retainEnd   = 0;

    void clear() { memset(memory, 0, GVL_SIZE); }

    // 暖启动清零：只清零非 RETAIN 区域
    void clearNonRetain() {
        if (retainStart > 0) {
            memset(memory, 0, retainStart);
        }
        if (retainEnd < GVL_SIZE) {
            memset(memory + retainEnd, 0, GVL_SIZE - retainEnd);
        }
    }

    // 设置 RETAIN 区域范围（编译器在 Memory Layout Pass 中计算）
    void setRetainRegion(size_t start, size_t end) {
        retainStart = start;
        retainEnd   = end;
    }

    template<typename T>
    T read(size_t offset) const {
        T val;
        memcpy(&val, memory + offset, sizeof(T));
        return val;
    }

    template<typename T>
    void write(size_t offset, T val) {
        memcpy(memory + offset, &val, sizeof(T));
    }

    template<typename T>
    T* ptr(size_t offset) {
        return reinterpret_cast<T*>(memory + offset);
    }

    template<typename T>
    const T* ptr(size_t offset) const {
        return reinterpret_cast<const T*>(memory + offset);
    }

    // 使用量统计
    size_t usedBytes() const {
        // 简单启发：从后往前找第一个非零字节
        for (size_t i = GVL_SIZE; i > 0; i--) {
            if (memory[i - 1] != 0) return i;
        }
        return 0;
    }
};


// ═══════════════════════════════════════════════════════
// 函数类型
// ═══════════════════════════════════════════════════════

// POU 函数签名：接收 GVL、ProcessImage、当前周期时间
typedef void (*POUFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);

// 事件条件函数
typedef BOOL (*EventConditionFunc)(GVL& gvl, ProcessImage& io);

// PROGRAM 生命周期函数（与 rt_plc.h 中的 ProgramCallbacks 一致）
// 这里重定义避免循环依赖
typedef void (*PRG_InitFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_PreFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_CyclicFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
typedef void (*PRG_PostFunc)(GVL& gvl, ProcessImage& io);


// ═══════════════════════════════════════════════════════
// PROGRAM 实例
// ═══════════════════════════════════════════════════════

struct ProgramInstance {
    char            name[32] = {0};
    ProgramPhase    phase    = ProgramPhase::UNINIT;

    PRG_InitFunc    initFunc   = nullptr;
    PRG_PreFunc     preFunc    = nullptr;   // 可选
    PRG_CyclicFunc  cyclicFunc = nullptr;   // 必需
    PRG_PostFunc    postFunc   = nullptr;   // 可选

    uint64_t        cycleCount = 0;

    // 执行当前阶段的逻辑
    void doInit(GVL& gvl, ProcessImage& io) {
        if (initFunc) initFunc(gvl, io);
        phase = ProgramPhase::INIT;
    }

    void doPre(GVL& gvl, ProcessImage& io) {
        if (preFunc) preFunc(gvl, io);
        phase = ProgramPhase::FIRST_SCAN;
    }

    void doCyclic(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
        if (cyclicFunc) cyclicFunc(gvl, io, cycleTimeUs);
        if (phase == ProgramPhase::FIRST_SCAN) {
            phase = ProgramPhase::CYCLIC;  // 首次扫描后进入正常循环
        }
        cycleCount++;
    }

    void doPost(GVL& gvl, ProcessImage& io) {
        if (postFunc) postFunc(gvl, io);
        phase = ProgramPhase::STOPPED;
    }
};


// ═══════════════════════════════════════════════════════
// Task: 任务定义
// ═══════════════════════════════════════════════════════

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

    bool addPOU(POUFunc func) {
        if (pouCount >= MAX_POUS_PER_TASK) return false;
        pous[pouCount++] = func;
        return true;
    }

    bool addProgram(int progIndex) {
        if (programCount >= MAX_POUS_PER_TASK) return false;
        programIndices[programCount++] = progIndex;
        return true;
    }

    void executePOUs(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
        for (int i = 0; i < pouCount; i++) {
            pous[i](gvl, io, cycleTimeUs);
        }
    }

    bool isDue(TIME currentTime) const {
        return (currentTime - lastRunTime) >= interval;
    }
};


// ═══════════════════════════════════════════════════════
// Event: 事件触发器
// ═══════════════════════════════════════════════════════

struct Event {
    char               name[32] = {0};
    EventConditionFunc  condition = nullptr;
    EventEdge          edge      = EventEdge::RISING;
    int                taskIndex = -1;
    BOOL               lastState = FALSE;

    bool check(GVL& gvl, ProcessImage& io) {
        BOOL currentState = condition ? condition(gvl, io) : FALSE;
        bool triggered = false;
        switch (edge) {
            case EventEdge::RISING:  triggered = (currentState && !lastState); break;
            case EventEdge::FALLING: triggered = (!currentState && lastState); break;
            case EventEdge::BOTH:    triggered = (currentState != lastState);  break;
        }
        lastState = currentState;
        return triggered;
    }
};


// ═══════════════════════════════════════════════════════
// Watchdog: 看门狗
// ═══════════════════════════════════════════════════════

struct Watchdog {
    TIME    defaultLimit = 0;
    bool    tripped      = false;
    int     trippedTask  = -1;

    void setDefault(TIME limit) { defaultLimit = limit; }

    void reset() {
        tripped = false;
        trippedTask = -1;
    }

    bool check(Task& task, int taskIndex) {
        TIME limit = task.watchdogLimit > 0 ? task.watchdogLimit : defaultLimit;
        if (limit > 0 && task.lastExecTime > limit) {
            tripped = true;
            trippedTask = taskIndex;
            return true;
        }
        return false;
    }
};


// ═══════════════════════════════════════════════════════
// 诊断统计
// ═══════════════════════════════════════════════════════

struct DiagStats {
    uint64_t totalScanCount     = 0;
    TIME     minScanTime        = INT64_MAX;
    TIME     maxScanTime        = 0;
    TIME     totalScanTime      = 0;  // 用于算平均
    TIME     lastScanTime       = 0;
    uint64_t totalOverruns      = 0;

    void recordScan(TIME scanTime) {
        totalScanCount++;
        totalScanTime += scanTime;
        lastScanTime = scanTime;
        if (scanTime < minScanTime) minScanTime = scanTime;
        if (scanTime > maxScanTime) maxScanTime = scanTime;
    }

    TIME avgScanTime() const {
        return totalScanCount > 0 ? totalScanTime / (TIME)totalScanCount : 0;
    }

    void reset() {
        totalScanCount = 0;
        minScanTime    = INT64_MAX;
        maxScanTime    = 0;
        totalScanTime  = 0;
        lastScanTime   = 0;
        totalOverruns  = 0;
    }
};


// ═══════════════════════════════════════════════════════
// Scheduler: 调度器
// ═══════════════════════════════════════════════════════

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
                   PRG_PostFunc post = nullptr) {
        int idx = findFreeProgram();
        if (idx < 0) return -1;
        ProgramInstance& p = programs_[idx];
        strncpy(p.name, name, sizeof(p.name) - 1);
        p.initFunc   = init;
        p.cyclicFunc = cyclic;
        p.preFunc    = pre;
        p.postFunc   = post;
        programCount_++;
        return idx;
    }

    // 添加 Cyclic 任务
    int addCyclicTask(const char* name, int priority, TIME intervalUs) {
        int idx = findFreeTask();
        if (idx < 0) return -1;
        Task& t = tasks_[idx];
        strncpy(t.name, name, sizeof(t.name) - 1);
        t.priority = priority;
        t.trigger  = TaskTrigger::CYCLIC;
        t.interval = intervalUs;
        t.state    = TaskState::READY;
        taskCount_++;
        sortNeeded_ = true;
        return idx;
    }

    // 添加 Freewheeling 任务
    int addFreewheelingTask(const char* name, int priority) {
        int idx = findFreeTask();
        if (idx < 0) return -1;
        Task& t = tasks_[idx];
        strncpy(t.name, name, sizeof(t.name) - 1);
        t.priority = priority;
        t.trigger  = TaskTrigger::FREEWHEELING;
        t.state    = TaskState::READY;
        taskCount_++;
        sortNeeded_ = true;
        return idx;
    }

    // 添加 Event 任务
    int addEventTask(const char* name, int priority,
                     EventConditionFunc cond, EventEdge edge = EventEdge::RISING) {
        int taskIdx = findFreeTask();
        int evtIdx  = findFreeEvent();
        if (taskIdx < 0 || evtIdx < 0) return -1;
        Task& t = tasks_[taskIdx];
        strncpy(t.name, name, sizeof(t.name) - 1);
        t.priority = priority;
        t.trigger  = TaskTrigger::EVENT;
        t.state    = TaskState::IDLE;
        Event& e = events_[evtIdx];
        strncpy(e.name, name, sizeof(e.name) - 1);
        e.condition = cond;
        e.edge      = edge;
        e.taskIndex = taskIdx;
        e.lastState = FALSE;
        taskCount_++;
        eventCount_++;
        sortNeeded_ = true;
        return taskIdx;
    }

    // 给任务添加 POU
    bool addPOU(int taskIndex, POUFunc func) {
        if (taskIndex < 0 || taskIndex >= MAX_TASKS) return false;
        return tasks_[taskIndex].addPOU(func);
    }

    // 给任务添加 PROGRAM 实例
    bool addProgramToTask(int taskIndex, int programIndex) {
        if (taskIndex < 0 || taskIndex >= MAX_TASKS) return false;
        return tasks_[taskIndex].addProgram(programIndex);
    }

    // 设置任务级看门狗
    void setTaskWatchdog(int taskIndex, TIME limitUs) {
        if (taskIndex >= 0 && taskIndex < MAX_TASKS) {
            tasks_[taskIndex].watchdogLimit = limitUs;
        }
    }


    // ─── 启动 / 停止 ───

    // 启动系统
    void start(StartupMode mode = StartupMode::COLD) {
        if (sortNeeded_) {
            sortTasksByPriority();
            sortNeeded_ = false;
        }

        // 1. 清零 / 保留 RETAIN
        if (mode == StartupMode::COLD) {
            gvl.clear();
            image.clearInputs();
            image.clearOutputs();
        } else {
            gvl.clearNonRetain();
            image.clearInputs();
            image.clearOutputs();
        }

        errorMgr.reset();
        diag.reset();
        watchdog.reset();

        // 2. 进入 STARTING 状态
        systemState = SystemState::STARTING;

        // 3. 初始化所有 PROGRAM 实例
        for (int i = 0; i < programCount_; i++) {
            programs_[i].doInit(gvl, image);
        }

        // 4. 执行 Pre（首扫描前回调）
        for (int i = 0; i < programCount_; i++) {
            programs_[i].doPre(gvl, image);
        }

        // 5. 同步首次输入
        if (tci) tci->syncInputs(image);

        // 6. 进入 RUN
        systemState = SystemState::RUN;
        runStartWall_ = std::chrono::steady_clock::now();
    }

    // 停止系统
    void stop() {
        if (systemState != SystemState::RUN &&
            systemState != SystemState::ERROR) return;

        systemState = SystemState::STOPPING;

        // 执行所有 PROGRAM 的 Post 回调
        for (int i = 0; i < programCount_; i++) {
            if (programs_[i].phase == ProgramPhase::CYCLIC ||
                programs_[i].phase == ProgramPhase::FIRST_SCAN) {
                programs_[i].doPost(gvl, image);
            }
        }

        systemState = SystemState::STOP;
    }

    void pause() {
        if (systemState == SystemState::RUN) {
            systemState = SystemState::PAUSED;
        }
    }

    void resume() {
        if (systemState == SystemState::PAUSED) {
            systemState = SystemState::RUN;
        }
    }

    void error() {
        systemState = SystemState::ERROR;
    }

    // 从 ERROR 恢复（需要手动确认后调用）
    void resetError() {
        if (systemState == SystemState::ERROR) {
            errorMgr.reset();
            watchdog.reset();
            systemState = SystemState::STOP;
        }
    }


    // ─── 运行循环 ───

    void run(StartupMode mode = StartupMode::COLD) {
        start(mode);

        while (systemState == SystemState::RUN) {
            auto loopStart = std::chrono::steady_clock::now();

            tick();

            auto elapsed = std::chrono::steady_clock::now() - loopStart;
            auto sleepUs = std::chrono::microseconds(baseCycleTime) -
                           std::chrono::duration_cast<std::chrono::microseconds>(elapsed);
            if (sleepUs.count() > 0) {
                std::this_thread::sleep_for(sleepUs);
            }
        }
    }


    // ─── 单次 Tick（分阶段扫描） ───

    void tick() {
        if (sortNeeded_) {
            sortTasksByPriority();
            sortNeeded_ = false;
        }

        // 手动 tick 时自动进入 RUN
        if (systemState == SystemState::STOP) {
            start(StartupMode::COLD);
        }

        if (systemState != SystemState::RUN) return;

        auto scanStart = std::chrono::steady_clock::now();

        // ═══ Phase 1: Read Inputs ═══
        currentPhase = ScanPhase::READ_INPUTS;
        syncInputs();

        // 更新系统时间
        auto nowWall = std::chrono::steady_clock::now();
        systemTime = std::chrono::duration_cast<std::chrono::microseconds>(
            nowWall - runStartWall_).count();

        // 检查事件触发（输入更新后才能检测）
        checkEvents();

        // ═══ Phase 2: Logic Solve ═══
        currentPhase = ScanPhase::LOGIC_SOLVE;

        for (int i = 0; i < taskCount_; i++) {
            int idx = taskOrder_[i];
            Task& task = tasks_[idx];

            if (!shouldRun(task)) continue;

            auto execStart = std::chrono::steady_clock::now();
            task.state = TaskState::RUNNING;

            // 执行挂载的 POU 函数
            task.executePOUs(gvl, image, baseCycleTime);

            // 执行挂载的 PROGRAM 实例
            for (int j = 0; j < task.programCount; j++) {
                int pIdx = task.programIndices[j];
                if (pIdx >= 0 && pIdx < MAX_PROGRAMS) {
                    programs_[pIdx].doCyclic(gvl, image, baseCycleTime);
                }
            }

            task.cycleCount++;
            task.lastRunTime = systemTime;

            auto execEnd = std::chrono::steady_clock::now();
            task.lastExecTime = std::chrono::duration_cast<std::chrono::microseconds>(
                execEnd - execStart).count();
            if (task.lastExecTime > task.maxExecTime) {
                task.maxExecTime = task.lastExecTime;
            }

            // 看门狗检查
            if (watchdog.check(task, idx)) {
                task.state = TaskState::OVERRUN;
                task.overrunCount++;
                diag.totalOverruns++;
                fprintf(stderr, "[Watchdog] Task '%s' overrun: %lld us (limit: %lld us)\n",
                        task.name, (long long)task.lastExecTime,
                        (long long)(task.watchdogLimit > 0 ? task.watchdogLimit : watchdog.defaultLimit));

                // 严重超时 → 进入 ERROR
                if (task.overrunCount >= MAX_CONSECUTIVE_OVERRUNS) {
                    errorMgr.report(ErrorCode::WATCHDOG_TIMEOUT, 0, 0,
                                    task.name, systemTime);
                    systemState = SystemState::ERROR;
                    return;
                }
            } else {
                task.state = TaskState::READY;
            }

            // 检查致命错误
            if (errorMgr.fatalMode) {
                systemState = SystemState::ERROR;
                return;
            }
        }

        // ═══ Phase 3: Write Outputs ═══
        currentPhase = ScanPhase::WRITE_OUTPUTS;
        syncOutputs();

        // ═══ Phase 4: Housekeeping ═══
        currentPhase = ScanPhase::HOUSEKEEPING;

        auto scanEnd = std::chrono::steady_clock::now();
        TIME scanTime = std::chrono::duration_cast<std::chrono::microseconds>(
            scanEnd - scanStart).count();
        diag.recordScan(scanTime);

        totalTicks++;
        currentPhase = ScanPhase::IDLE;
    }


    // ─── 查询 API ───

    int              taskCount()     const { return taskCount_; }
    int              eventCount()    const { return eventCount_; }
    int              programCount()  const { return programCount_; }
    const Task&      task(int idx)   const { return tasks_[idx]; }
    Task&            task(int idx)         { return tasks_[idx]; }
    const ProgramInstance& program(int idx) const { return programs_[idx]; }

    // 诊断输出
    void printDiag() const {
        printf("=== Scheduler Diagnostics ===\n");
        printf("System: %s | Ticks: %llu | Time: %lld us\n",
               stateName(systemState),
               (unsigned long long)totalTicks,
               (long long)systemTime);
        printf("Watchdog: %s\n", watchdog.tripped ? "TRIPPED" : "OK");
        printf("Errors: %d (fatal: %s)\n",
               errorMgr.totalCount, errorMgr.fatalMode ? "YES" : "NO");

        printf("\nScan Times: last=%lld  avg=%lld  min=%lld  max=%lld us\n",
               (long long)diag.lastScanTime,
               (long long)diag.avgScanTime(),
               (long long)diag.minScanTime,
               (long long)diag.maxScanTime);
        printf("Total Overruns: %llu\n", (unsigned long long)diag.totalOverruns);

        printf("\n%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
               "Task", "Trigger", "Pri", "Cycles", "Last(us)", "Max(us)", "Overrun");
        printf("%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
               "----", "-------", "---", "------", "--------", "--------", "-------");
        for (int i = 0; i < taskCount_; i++) {
            const Task& t = tasks_[i];
            printf("%-20s %-10s %-8d %-10llu %-10lld %-10lld %-8u\n",
                   t.name,
                   triggerName(t.trigger),
                   t.priority,
                   (unsigned long long)t.cycleCount,
                   (long long)t.lastExecTime,
                   (long long)t.maxExecTime,
                   t.overrunCount);
        }

        printf("\nPrograms:\n");
        for (int i = 0; i < programCount_; i++) {
            const ProgramInstance& p = programs_[i];
            printf("  %-20s phase=%-12s cycles=%llu\n",
                   p.name, phaseName(p.phase),
                   (unsigned long long)p.cycleCount);
        }

        printf("\nGVL used: %zu / %zu bytes (retain: [%zu, %zu))\n",
               gvl.usedBytes(), GVL_SIZE,
               gvl.retainStart, gvl.retainEnd);
        printf("=============================\n");
    }


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
    int findFreeTask() {
        for (int i = 0; i < MAX_TASKS; i++) {
            if (tasks_[i].pouCount == 0 && tasks_[i].programCount == 0
                && tasks_[i].trigger == TaskTrigger::CYCLIC
                && tasks_[i].interval == 0) return i;
        }
        return -1;
    }

    int findFreeEvent() {
        for (int i = 0; i < MAX_EVENTS; i++) {
            if (events_[i].condition == nullptr) return i;
        }
        return -1;
    }

    int findFreeProgram() {
        for (int i = 0; i < MAX_PROGRAMS; i++) {
            if (programs_[i].cyclicFunc == nullptr) return i;
        }
        return -1;
    }

    void sortTasksByPriority() {
        for (int i = 0; i < taskCount_; i++) taskOrder_[i] = i;
        for (int i = 0; i < taskCount_ - 1; i++) {
            for (int j = i + 1; j < taskCount_; j++) {
                if (tasks_[taskOrder_[j]].priority < tasks_[taskOrder_[i]].priority) {
                    int tmp = taskOrder_[i];
                    taskOrder_[i] = taskOrder_[j];
                    taskOrder_[j] = tmp;
                }
            }
        }
    }

    bool shouldRun(Task& task) const {
        if (systemState != SystemState::RUN) return false;
        switch (task.trigger) {
            case TaskTrigger::CYCLIC:
                return task.isDue(systemTime);
            case TaskTrigger::FREEWHEELING:
                return true;
            case TaskTrigger::EVENT:
                // Event 任务由 checkEvents() 直接执行
                return false;
            default:
                return false;
        }
    }

    void checkEvents() {
        for (int i = 0; i < eventCount_; i++) {
            if (events_[i].check(gvl, image)) {
                int taskIdx = events_[i].taskIndex;
                if (taskIdx >= 0 && taskIdx < MAX_TASKS) {
                    Task& task = tasks_[taskIdx];
                    auto execStart = std::chrono::steady_clock::now();
                    task.state = TaskState::RUNNING;
                    task.executePOUs(gvl, image, baseCycleTime);
                    // 事件任务也执行挂载的 PROGRAM
                    for (int j = 0; j < task.programCount; j++) {
                        int pIdx = task.programIndices[j];
                        if (pIdx >= 0 && pIdx < MAX_PROGRAMS) {
                            programs_[pIdx].doCyclic(gvl, image, baseCycleTime);
                        }
                    }
                    task.cycleCount++;
                    task.lastRunTime = systemTime;
                    auto execEnd = std::chrono::steady_clock::now();
                    task.lastExecTime = std::chrono::duration_cast<std::chrono::microseconds>(
                        execEnd - execStart).count();
                    if (task.lastExecTime > task.maxExecTime) {
                        task.maxExecTime = task.lastExecTime;
                    }
                    task.state = TaskState::IDLE;
                }
            }
        }
    }

    void syncInputs() {
        if (tci) tci->syncInputs(image);
    }

    void syncOutputs() {
        if (tci) tci->syncOutputs(image);
    }

    static const char* stateName(SystemState s) {
        switch (s) {
            case SystemState::STOP:     return "STOP";
            case SystemState::STARTING: return "STARTING";
            case SystemState::RUN:      return "RUN";
            case SystemState::STOPPING: return "STOPPING";
            case SystemState::ERROR:    return "ERROR";
            case SystemState::PAUSED:   return "PAUSED";
        }
        return "?";
    }

    static const char* triggerName(TaskTrigger t) {
        switch (t) {
            case TaskTrigger::CYCLIC:       return "Cyclic";
            case TaskTrigger::EVENT:        return "Event";
            case TaskTrigger::FREEWHEELING: return "FreeWheel";
        }
        return "?";
    }

    static const char* phaseName(ProgramPhase p) {
        switch (p) {
            case ProgramPhase::UNINIT:     return "UNINIT";
            case ProgramPhase::INIT:       return "INIT";
            case ProgramPhase::FIRST_SCAN: return "FIRST_SCAN";
            case ProgramPhase::CYCLIC:     return "CYCLIC";
            case ProgramPhase::STOPPED:    return "STOPPED";
        }
        return "?";
    }
};


} // namespace rt_plc

#endif // RT_RUNTIME_H
