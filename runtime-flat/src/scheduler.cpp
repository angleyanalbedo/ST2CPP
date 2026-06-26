#include "scheduler/scheduler.h"
#include <algorithm>

namespace rt_plc {

int Scheduler::addProgram(const char* name,
               PRG_InitFunc init,
               PRG_CyclicFunc cyclic,
               PRG_PreFunc pre,
               PRG_PostFunc post) {
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

int Scheduler::addCyclicTask(const char* name, int priority, TIME intervalUs) {
    if (!name) name = "unnamed";
    if (priority < MIN_PRIORITY) priority = MIN_PRIORITY;
    if (priority > MAX_PRIORITY) priority = MAX_PRIORITY;
    if (intervalUs <= 0) intervalUs = baseCycleTime;
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

int Scheduler::addFreewheelingTask(const char* name, int priority) {
    if (!name) name = "unnamed";
    if (priority < MIN_PRIORITY) priority = MIN_PRIORITY;
    if (priority > MAX_PRIORITY) priority = MAX_PRIORITY;
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

int Scheduler::addEventTask(const char* name, int priority,
                 EventConditionFunc cond, EventEdge edge) {
    if (!name) name = "unnamed";
    if (priority < MIN_PRIORITY) priority = MIN_PRIORITY;
    if (priority > MAX_PRIORITY) priority = MAX_PRIORITY;
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

bool Scheduler::addPOU(int taskIndex, POUFunc func) {
    if (taskIndex < 0 || taskIndex >= MAX_TASKS) return false;
    return tasks_[taskIndex].addPOU(func);
}

bool Scheduler::addProgramToTask(int taskIndex, int programIndex) {
    if (taskIndex < 0 || taskIndex >= MAX_TASKS) return false;
    return tasks_[taskIndex].addProgram(programIndex);
}

void Scheduler::setTaskWatchdog(int taskIndex, TIME limitUs) {
    if (taskIndex >= 0 && taskIndex < MAX_TASKS) {
        tasks_[taskIndex].watchdogLimit = limitUs;
    }
}

void Scheduler::start(StartupMode mode) {
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
    runStartWall_ = platform::steadyUs();
}

void Scheduler::stop() {
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

void Scheduler::pause() {
    if (systemState == SystemState::RUN) {
        systemState = SystemState::PAUSED;
    }
}

void Scheduler::resume() {
    if (systemState == SystemState::PAUSED) {
        systemState = SystemState::RUN;
    }
}

void Scheduler::error() {
    systemState = SystemState::ERROR;
}

void Scheduler::resetError() {
    if (systemState == SystemState::ERROR) {
        errorMgr.reset();
        watchdog.reset();
        systemState = SystemState::STOP;
    }
}

void Scheduler::run(StartupMode mode) {
    start(mode);

    while (systemState == SystemState::RUN) {
        int64_t loopStart = platform::steadyUs();

        tick();

        int64_t elapsed = platform::steadyUs() - loopStart;
        int64_t sleepUs = (int64_t)baseCycleTime - elapsed;
        if (sleepUs > 0) {
            platform::sleepUs(sleepUs);
        }
    }
}

void Scheduler::tick() {
    if (sortNeeded_) {
        sortTasksByPriority();
        sortNeeded_ = false;
    }

    // 手动 tick 时自动进入 RUN
    if (systemState == SystemState::STOP) {
        start(StartupMode::COLD);
    }

    if (systemState != SystemState::RUN) return;

#ifdef ENABLE_DIAG
    int64_t scanStart = platform::steadyUs();
#endif

    // Phase 1: Read Inputs
    currentPhase = ScanPhase::READ_INPUTS;
    syncInputs();

    // 更新系统时间（仅诊断模式下精确计时）
#ifdef ENABLE_DIAG
    int64_t nowWall = platform::steadyUs();
    systemTime = nowWall - runStartWall_;
#else
    // 非诊断模式：用 tick 计数代替精确时间
    systemTime = totalTicks * baseCycleTime;
#endif

    // 检查事件触发（输入更新后才能检测）
    checkEvents();

    // Phase 2: Logic Solve
    currentPhase = ScanPhase::LOGIC_SOLVE;

    for (int i = 0; i < taskCount_; i++) {
        int idx = taskOrder_[i];
        Task& task = tasks_[idx];

        if (!shouldRun(task)) continue;

#ifdef ENABLE_DIAG
        int64_t execStart = platform::steadyUs();
#endif
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

#ifdef ENABLE_DIAG
        int64_t execEnd = platform::steadyUs();
        task.lastExecTime = execEnd - execStart;
        if (task.lastExecTime > task.maxExecTime) {
            task.maxExecTime = task.lastExecTime;
        }
#endif

        // 看门狗检查（诊断模式下用时间，非诊断模式下用 tick 计数）
        if (watchdog.check(task, idx)) {
            task.state = TaskState::OVERRUN;
            task.overrunCount++;
            diag.totalOverruns++;
#ifdef ENABLE_DIAG
            RT_LOG_ERR("[Watchdog] Task '%s' overrun: %lld us (limit: %lld us)\n",
                    task.name, (long long)task.lastExecTime,
                    (long long)(task.watchdogLimit > 0 ? task.watchdogLimit : watchdog.defaultLimit));
#endif
            // 严重超时 → 进入 ERROR
            if (task.overrunCount >= MAX_CONSECUTIVE_OVERRUNS) {
                errorMgr.report(ErrorCode::WATCHDOG_TIMEOUT, 0, 0,
                                task.name, systemTime);
                systemState = SystemState::ERROR;
                return;
            }
        } else {
            task.state = TaskState::READY;
            task.overrunCount = 0;  // 正则运行时重置计数
        }

        // 检查致命错误
        if (errorMgr.fatalMode) {
            systemState = SystemState::ERROR;
            return;
        }
    }

    // Phase 3: Write Outputs
    currentPhase = ScanPhase::WRITE_OUTPUTS;
    syncOutputs();

    // Phase 4: Housekeeping
    currentPhase = ScanPhase::HOUSEKEEPING;

#ifdef ENABLE_DIAG
    int64_t scanEnd = platform::steadyUs();
    TIME scanTime = scanEnd - scanStart;
    diag.recordScan(scanTime);
#endif

    totalTicks++;
    currentPhase = ScanPhase::IDLE;
}

void Scheduler::printDiag() const {
    RT_LOG_INFO("=== Scheduler Diagnostics ===\n");
    RT_LOG_INFO("System: %s | Ticks: %llu | Time: %lld us\n",
           stateName(systemState),
           (unsigned long long)totalTicks,
           (long long)systemTime);
    RT_LOG_INFO("Watchdog: %s\n", watchdog.tripped ? "TRIPPED" : "OK");
    RT_LOG_INFO("Errors: %d (fatal: %s)\n",
           errorMgr.totalCount, errorMgr.fatalMode ? "YES" : "NO");

    RT_LOG_INFO("\nScan Times: last=%lld  avg=%lld  min=%lld  max=%lld us\n",
           (long long)diag.lastScanTime,
           (long long)diag.avgScanTime(),
           (long long)diag.minScanTime,
           (long long)diag.maxScanTime);
    RT_LOG_INFO("Total Overruns: %llu\n", (unsigned long long)diag.totalOverruns);

    RT_LOG_INFO("\n%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
           "Task", "Trigger", "Pri", "Cycles", "Last(us)", "Max(us)", "Overrun");
    RT_LOG_INFO("%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
           "----", "-------", "---", "------", "--------", "--------", "-------");
    for (int i = 0; i < taskCount_; i++) {
        const Task& t = tasks_[i];
        RT_LOG_INFO("%-20s %-10s %-8d %-10llu %-10lld %-10lld %-8u\n",
               t.name,
               triggerName(t.trigger),
               t.priority,
               (unsigned long long)t.cycleCount,
               (long long)t.lastExecTime,
               (long long)t.maxExecTime,
               t.overrunCount);
    }

    RT_LOG_INFO("\nPrograms:\n");
    for (int i = 0; i < programCount_; i++) {
        const ProgramInstance& p = programs_[i];
        RT_LOG_INFO("  %-20s phase=%-12s cycles=%llu\n",
               p.name, phaseName(p.phase),
               (unsigned long long)p.cycleCount);
    }

    RT_LOG_INFO("\nGVL used: %zu / %zu bytes (retain: [%zu, %zu))\n",
           gvl.usedBytes(), GVL_SIZE,
           gvl.retainStart, gvl.retainEnd);
    RT_LOG_INFO("=============================\n");
}

int Scheduler::findFreeTask() {
    for (int i = 0; i < MAX_TASKS; i++) {
        if (tasks_[i].pouCount == 0 && tasks_[i].programCount == 0
            && tasks_[i].trigger == TaskTrigger::CYCLIC
            && tasks_[i].interval == 0) return i;
    }
    return -1;
}

int Scheduler::findFreeEvent() {
    for (int i = 0; i < MAX_EVENTS; i++) {
        if (events_[i].condition == nullptr) return i;
    }
    return -1;
}

int Scheduler::findFreeProgram() {
    for (int i = 0; i < MAX_PROGRAMS; i++) {
        if (programs_[i].cyclicFunc == nullptr) return i;
    }
    return -1;
}

void Scheduler::sortTasksByPriority() {
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

bool Scheduler::shouldRun(Task& task) const {
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

void Scheduler::checkEvents() {
    for (int i = 0; i < eventCount_; i++) {
        if (events_[i].check(gvl, image)) {
            int taskIdx = events_[i].taskIndex;
            if (taskIdx >= 0 && taskIdx < MAX_TASKS) {
                Task& task = tasks_[taskIdx];
                int64_t execStart = platform::steadyUs();
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
                int64_t execEnd = platform::steadyUs();
                task.lastExecTime = execEnd - execStart;
                if (task.lastExecTime > task.maxExecTime) {
                    task.maxExecTime = task.lastExecTime;
                }
                task.state = TaskState::IDLE;
            }
        }
    }
}

void Scheduler::syncInputs() {
    if (tci) tci->syncInputs(image);
}

void Scheduler::syncOutputs() {
    if (tci) tci->syncOutputs(image);
}

const char* Scheduler::stateName(SystemState s) {
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

const char* Scheduler::triggerName(TaskTrigger t) {
    switch (t) {
        case TaskTrigger::CYCLIC:       return "Cyclic";
        case TaskTrigger::EVENT:        return "Event";
        case TaskTrigger::FREEWHEELING: return "FreeWheel";
    }
    return "?";
}

const char* Scheduler::phaseName(ProgramPhase p) {
    switch (p) {
        case ProgramPhase::UNINIT:     return "UNINIT";
        case ProgramPhase::INIT:       return "INIT";
        case ProgramPhase::FIRST_SCAN: return "FIRST_SCAN";
        case ProgramPhase::CYCLIC:     return "CYCLIC";
        case ProgramPhase::STOPPED:    return "STOPPED";
    }
    return "?";
}

} // namespace rt_plc
