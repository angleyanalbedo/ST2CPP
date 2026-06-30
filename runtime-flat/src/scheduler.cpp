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
    diagManager.reset();
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
    syncInputs();

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
    enterErrorState();
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

    // Phase 2: Logic Solve
    currentPhase = ScanPhase::LOGIC_SOLVE;

    // 检查事件触发（输入更新后才能检测）
    checkEvents();
    if (systemState != SystemState::RUN) return;

    for (int i = 0; i < taskCount_; i++) {
        int idx = taskOrder_[i];
        Task& task = tasks_[idx];

        if (!shouldRun(task)) continue;

        if (!executeTask(idx)) return;
    }

    // Phase 3: Write Outputs
    currentPhase = ScanPhase::WRITE_OUTPUTS;
    syncOutputs();

    // Phase 4: Housekeeping
    currentPhase = ScanPhase::HOUSEKEEPING;

#ifdef ENABLE_DIAG
    int64_t scanEnd = platform::steadyUs();
    TIME scanTime = scanEnd - scanStart;
    diagManager.recordScan(scanTime);
#endif

    totalTicks++;
    currentPhase = ScanPhase::IDLE;
}

void Scheduler::printDiag() const {
    diagManager.printSchedulerSnapshot(systemState,
                                       totalTicks,
                                       systemTime,
                                       watchdog,
                                       errorMgr,
                                       tasks_,
                                       taskCount_,
                                       programs_,
                                       programCount_,
                                       gvl);
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

bool Scheduler::executeTask(int taskIndex) {
    if (taskIndex < 0 || taskIndex >= MAX_TASKS) return true;

    Task& task = tasks_[taskIndex];
    TaskExecutorContext ctx {
        gvl,
        image,
        programs_,
        MAX_PROGRAMS,
        watchdog,
        errorMgr,
        diagManager,
        baseCycleTime,
        systemTime,
        MAX_CONSECUTIVE_OVERRUNS
    };

    if (!taskExecutor_.execute(task, taskIndex, ctx)) {
        enterErrorState();
        return false;
    }

    return true;
}

void Scheduler::checkEvents() {
    for (int i = 0; i < eventCount_; i++) {
        if (events_[i].check(gvl, image)) {
            int taskIdx = events_[i].taskIndex;
            if (taskIdx >= 0 && taskIdx < MAX_TASKS) {
                if (!executeTask(taskIdx)) return;
            }
        }
    }
}

void Scheduler::enterErrorState() {
    systemState = SystemState::ERROR;

    if (io.safeOutputsEnabled()) {
        currentPhase = ScanPhase::WRITE_OUTPUTS;
        io.applySafeOutputs(image);
        syncOutputs();
    }

    currentPhase = ScanPhase::IDLE;
}

void Scheduler::syncTCIBinding() {
    if (tci && io.tci() != tci) {
        io.setTCI(tci);
    }
}

void Scheduler::syncInputs() {
    syncTCIBinding();
    io.syncInputs(image);
}

void Scheduler::syncOutputs() {
    syncTCIBinding();
    io.syncOutputs(image);
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
