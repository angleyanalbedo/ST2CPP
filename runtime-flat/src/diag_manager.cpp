#include "core/diag_manager.h"

namespace rt_plc {

DiagManager::DiagManager(DiagStats& stats)
    : stats_(stats) {
}

DiagStats& DiagManager::stats() {
    return stats_;
}

const DiagStats& DiagManager::stats() const {
    return stats_;
}

void DiagManager::reset() {
    stats_.reset();
}

void DiagManager::recordScan(TIME scanTime) {
    stats_.recordScan(scanTime);
}

void DiagManager::recordTaskOverrun() {
    stats_.totalOverruns++;
}

void DiagManager::printSchedulerSnapshot(SystemState systemState,
                                         uint64_t totalTicks,
                                         TIME systemTime,
                                         const Watchdog& watchdog,
                                         const ErrorManager& errorMgr,
                                         const Task* tasks,
                                         int taskCount,
                                         const ProgramInstance* programs,
                                         int programCount,
                                         const GVL& gvl) const {
    RT_LOG_INFO("=== Scheduler Diagnostics ===\n");
    RT_LOG_INFO("System: %s | Ticks: %llu | Time: %lld us\n",
           stateName(systemState),
           (unsigned long long)totalTicks,
           (long long)systemTime);
    RT_LOG_INFO("Watchdog: %s\n", watchdog.tripped ? "TRIPPED" : "OK");
    RT_LOG_INFO("Errors: %d (fatal: %s)\n",
           errorMgr.totalCount, errorMgr.fatalMode ? "YES" : "NO");

    RT_LOG_INFO("\nScan Times: last=%lld  avg=%lld  min=%lld  max=%lld us\n",
           (long long)stats_.lastScanTime,
           (long long)stats_.avgScanTime(),
           (long long)stats_.minScanTime,
           (long long)stats_.maxScanTime);
    RT_LOG_INFO("Total Overruns: %llu\n", (unsigned long long)stats_.totalOverruns);

    RT_LOG_INFO("\n%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
           "Task", "Trigger", "Pri", "Cycles", "Last(us)", "Max(us)", "Overrun");
    RT_LOG_INFO("%-20s %-10s %-8s %-10s %-10s %-10s %-8s\n",
           "----", "-------", "---", "------", "--------", "--------", "-------");
    for (int i = 0; i < taskCount; i++) {
        const Task& t = tasks[i];
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
    for (int i = 0; i < programCount; i++) {
        const ProgramInstance& p = programs[i];
        RT_LOG_INFO("  %-20s phase=%-12s cycles=%llu\n",
               p.name, phaseName(p.phase),
               (unsigned long long)p.cycleCount);
    }

    RT_LOG_INFO("\nGVL used: %zu / %zu bytes (retain: [%zu, %zu))\n",
           gvl.usedBytes(), GVL_SIZE,
           gvl.retainStart, gvl.retainEnd);
    RT_LOG_INFO("=============================\n");
}

const char* DiagManager::stateName(SystemState s) {
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

const char* DiagManager::triggerName(TaskTrigger t) {
    switch (t) {
        case TaskTrigger::CYCLIC:       return "Cyclic";
        case TaskTrigger::EVENT:        return "Event";
        case TaskTrigger::FREEWHEELING: return "FreeWheel";
    }
    return "?";
}

const char* DiagManager::phaseName(ProgramPhase p) {
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
