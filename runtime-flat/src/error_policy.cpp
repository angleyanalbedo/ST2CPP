#include "core/error_policy.h"

namespace rt_plc {

FaultPolicyDecision ErrorPolicy::decide(ErrorCode code) const {
    switch (code) {
        case ErrorCode::NONE:
            return {false, false, false, false};

        case ErrorCode::USER_ERROR:
            return {true, false, false, false};

        case ErrorCode::CONFIG_ERROR:
            return {true, true, false, true};

        case ErrorCode::WATCHDOG_TIMEOUT:
        case ErrorCode::TASK_OVERRUN:
        case ErrorCode::IO_ERROR:
        case ErrorCode::RETAIN_CORRUPTED:
        case ErrorCode::SAFETY_VIOLATION:
            return {true, true, true, true};

        case ErrorCode::DIV_BY_ZERO:
        case ErrorCode::ARRAY_OUT_OF_BOUNDS:
        case ErrorCode::INT_OVERFLOW:
        case ErrorCode::FLOAT_OVERFLOW:
        case ErrorCode::NULL_POINTER:
        case ErrorCode::STRING_OVERFLOW:
        case ErrorCode::STACK_OVERFLOW:
            return {true, true, true, true};

        case ErrorCode::MISC_ERROR:
            return {true, false, false, false};
    }

    return {true, true, true, true};
}

} // namespace rt_plc
