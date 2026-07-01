#pragma once

#include "types.h"

namespace rt_plc {

struct FaultPolicyDecision {
    bool recordError = true;
    bool enterError = false;
    bool applySafeOutputs = false;
    bool latch = false;
};

class ErrorPolicy {
public:
    FaultPolicyDecision decide(ErrorCode code) const;
};

} // namespace rt_plc
