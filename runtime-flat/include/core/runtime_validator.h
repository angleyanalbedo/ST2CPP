#pragma once

#include "constants.h"
#include "types.h"
#include "rt_plc.h"

#include <cstdint>

namespace rt_plc {

class Scheduler;

enum class RuntimeValidationSeverity : uint8_t {
    INFO,
    WARNING,
    FATAL
};

struct RuntimeValidationIssue {
    RuntimeValidationSeverity severity = RuntimeValidationSeverity::INFO;
    char message[128] = {};
};

struct RuntimeValidationResult {
    static constexpr int MAX_ISSUES = 16;

    RuntimeValidationIssue issues[MAX_ISSUES] = {};
    int issueCount = 0;
    int fatalCount = 0;
    int warningCount = 0;

    bool ok() const { return fatalCount == 0; }
    const char* firstFatalMessage() const;

    void add(RuntimeValidationSeverity severity, const char* message);
    void addf(RuntimeValidationSeverity severity, const char* fmt, ...);
};

class RuntimeValidator {
public:
    static RuntimeValidationResult validate(const Scheduler& sched);
};

} // namespace rt_plc
