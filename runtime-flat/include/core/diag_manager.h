#pragma once

#include "diag.h"
#include "error_manager.h"
#include "gvl.h"
#include "program.h"
#include "task.h"
#include "watchdog.h"
#include "rt_plc.h"

namespace rt_plc {

class DiagManager {
public:
    explicit DiagManager(DiagStats& stats);

    DiagStats& stats();
    const DiagStats& stats() const;

    void reset();
    void recordScan(TIME scanTime);
    void recordTaskOverrun();

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
    static const char* triggerName(TaskTrigger t);
    static const char* phaseName(ProgramPhase p);
};

} // namespace rt_plc
