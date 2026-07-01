#pragma once

#include "diag.h"
#include "error_manager.h"
#include "gvl.h"
#include "constants.h"
#include "program.h"
#include "task.h"
#include "watchdog.h"
#include "rt_plc.h"

namespace rt_plc {

struct TaskDiagSnapshot {
    char        name[32] = {};
    TaskTrigger trigger = TaskTrigger::CYCLIC;
    TaskState   state = TaskState::IDLE;
    int         priority = 0;
    TIME        interval = 0;
    uint64_t    cycleCount = 0;
    TIME        lastExecTime = 0;
    TIME        maxExecTime = 0;
    uint32_t    overrunCount = 0;
};

struct ProgramDiagSnapshot {
    char         name[32] = {};
    ProgramPhase phase = ProgramPhase::UNINIT;
    uint64_t     cycleCount = 0;
};

struct PhaseDiagSnapshot {
    ScanPhase phase = ScanPhase::IDLE;
    uint64_t  count = 0;
    TIME      lastTime = 0;
    TIME      minTime = 0;
    TIME      maxTime = 0;
    TIME      avgTime = 0;
};

struct DiagSnapshot {
    SystemState systemState = SystemState::STOP;
    ScanPhase   currentPhase = ScanPhase::IDLE;

    uint64_t totalTicks = 0;
    TIME     systemTime = 0;
    TIME     baseCycleTime = 0;

    uint64_t totalScanCount = 0;
    TIME     minScanTime = 0;
    TIME     maxScanTime = 0;
    TIME     avgScanTime = 0;
    TIME     lastScanTime = 0;
    uint64_t totalOverruns = 0;
    PhaseDiagSnapshot phases[SCAN_PHASE_COUNT] = {};

    bool      watchdogTripped = false;
    TIME      watchdogDefaultLimit = 0;
    int       errorCount = 0;
    ErrorCode lastError = ErrorCode::NONE;
    bool      fatalMode = false;

    size_t gvlUsedBytes = 0;
    size_t gvlCapacityBytes = GVL_SIZE;
    size_t retainStart = 0;
    size_t retainEnd = 0;

    bool tciBound = false;
    bool safeOutputsEnabled = false;

    int taskCount = 0;
    TaskDiagSnapshot tasks[MAX_TASKS] = {};

    int programCount = 0;
    ProgramDiagSnapshot programs[MAX_PROGRAMS] = {};
};

class DiagManager {
public:
    explicit DiagManager(DiagStats& stats);

    DiagStats& stats();
    const DiagStats& stats() const;

    void reset();
    void recordScan(TIME scanTime);
    void recordPhase(ScanPhase phase, TIME elapsedUs);
    void recordTaskOverrun();

    DiagSnapshot makeSchedulerSnapshot(SystemState systemState,
                                       ScanPhase currentPhase,
                                       uint64_t totalTicks,
                                       TIME systemTime,
                                       TIME baseCycleTime,
                                       const Watchdog& watchdog,
                                       const ErrorManager& errorMgr,
                                       const Task* tasks,
                                       int taskCount,
                                       const ProgramInstance* programs,
                                       int programCount,
                                       const GVL& gvl,
                                       bool tciBound,
                                       bool safeOutputsEnabled) const;

    void printSchedulerSnapshot(SystemState systemState,
                                uint64_t totalTicks,
                                TIME systemTime,
                                const Watchdog& watchdog,
                                const ErrorManager& errorMgr,
                                const Task* tasks,
                                int taskCount,
                                const ProgramInstance* programs,
                                int programCount,
                                const GVL& gvl) const;

private:
    DiagStats& stats_;

    static const char* stateName(SystemState s);
    static const char* scanPhaseName(ScanPhase p);
    static const char* triggerName(TaskTrigger t);
    static const char* phaseName(ProgramPhase p);
};

} // namespace rt_plc
