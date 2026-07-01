#include "core/runtime_validator.h"
#include "scheduler/scheduler.h"

#include <cstdarg>
#include <cstdio>
#include <cstring>

namespace rt_plc {

const char* RuntimeValidationResult::firstFatalMessage() const {
    for (int i = 0; i < issueCount; i++) {
        if (issues[i].severity == RuntimeValidationSeverity::FATAL) {
            return issues[i].message;
        }
    }
    return "runtime validation failed";
}

void RuntimeValidationResult::add(RuntimeValidationSeverity severity, const char* message) {
    if (severity == RuntimeValidationSeverity::FATAL) fatalCount++;
    if (severity == RuntimeValidationSeverity::WARNING) warningCount++;

    if (issueCount >= MAX_ISSUES) return;

    RuntimeValidationIssue& issue = issues[issueCount++];
    issue.severity = severity;
    if (message) {
        strncpy(issue.message, message, sizeof(issue.message) - 1);
    }
}

void RuntimeValidationResult::addf(RuntimeValidationSeverity severity, const char* fmt, ...) {
    char buffer[128] = {};
    if (fmt) {
        va_list args;
        va_start(args, fmt);
        vsnprintf(buffer, sizeof(buffer), fmt, args);
        va_end(args);
    }
    add(severity, buffer);
}

RuntimeValidationResult RuntimeValidator::validate(const Scheduler& sched) {
    RuntimeValidationResult result;

    if (sched.baseCycleTime <= 0) {
        result.addf(RuntimeValidationSeverity::FATAL,
                    "base cycle must be > 0, got %lld",
                    (long long)sched.baseCycleTime);
    }

    if (sched.taskCount_ < 0 || sched.taskCount_ > MAX_TASKS) {
        result.addf(RuntimeValidationSeverity::FATAL,
                    "task count out of range: %d",
                    sched.taskCount_);
    }

    if (sched.programCount_ < 0 || sched.programCount_ > MAX_PROGRAMS) {
        result.addf(RuntimeValidationSeverity::FATAL,
                    "program count out of range: %d",
                    sched.programCount_);
    }

    if (sched.eventCount_ < 0 || sched.eventCount_ > MAX_EVENTS) {
        result.addf(RuntimeValidationSeverity::FATAL,
                    "event count out of range: %d",
                    sched.eventCount_);
    }

    if (sched.retain.start() > sched.retain.end() || sched.retain.end() > GVL_SIZE) {
        result.addf(RuntimeValidationSeverity::FATAL,
                    "retain region out of range: [%zu, %zu)",
                    sched.retain.start(), sched.retain.end());
    }

    for (int i = 0; i < sched.taskCount_ && i < MAX_TASKS; i++) {
        const Task& task = sched.tasks_[i];
        if (task.name[0] == '\0') {
            result.addf(RuntimeValidationSeverity::WARNING,
                        "task %d has empty name", i);
        }

        if (task.priority < MIN_PRIORITY || task.priority > MAX_PRIORITY) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "task %d priority out of range: %d",
                        i, task.priority);
        }

        if (task.trigger == TaskTrigger::CYCLIC && task.interval <= 0) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "cyclic task %d interval must be > 0", i);
        }

        if (task.watchdogLimit < 0) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "task %d watchdog limit must be >= 0", i);
        }

        if (task.trigger == TaskTrigger::CYCLIC &&
            task.watchdogLimit > 0 &&
            task.interval > 0 &&
            task.watchdogLimit < task.interval) {
            result.addf(RuntimeValidationSeverity::WARNING,
                        "task %d watchdog %lldus is below interval %lldus",
                        i, (long long)task.watchdogLimit,
                        (long long)task.interval);
        }

        if (task.pouCount < 0 || task.pouCount > MAX_POUS_PER_TASK ||
            task.programCount < 0 || task.programCount > MAX_POUS_PER_TASK) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "task %d POU/program count out of range", i);
            continue;
        }

        if (task.pouCount == 0 && task.programCount == 0) {
            result.addf(RuntimeValidationSeverity::WARNING,
                        "task %d has no POU or PROGRAM", i);
        }

        for (int j = 0; j < task.pouCount; j++) {
            if (task.pous[j] == nullptr) {
                result.addf(RuntimeValidationSeverity::FATAL,
                            "task %d POU slot %d is null", i, j);
            }
        }

        for (int j = 0; j < task.programCount; j++) {
            int pIdx = task.programIndices[j];
            if (pIdx < 0 || pIdx >= sched.programCount_) {
                result.addf(RuntimeValidationSeverity::FATAL,
                            "task %d PROGRAM index %d out of range", i, pIdx);
            } else if (sched.programs_[pIdx].cyclicFunc == nullptr) {
                result.addf(RuntimeValidationSeverity::FATAL,
                            "task %d PROGRAM %d has no cyclic callback", i, pIdx);
            }
        }
    }

    for (int i = 0; i < sched.eventCount_ && i < MAX_EVENTS; i++) {
        const Event& event = sched.events_[i];
        if (event.condition == nullptr) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "event %d has null condition", i);
        }
        if (event.taskIndex < 0 || event.taskIndex >= sched.taskCount_) {
            result.addf(RuntimeValidationSeverity::FATAL,
                        "event %d task index out of range: %d",
                        i, event.taskIndex);
        }
    }

    return result;
}

} // namespace rt_plc
