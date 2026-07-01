#pragma once

#include "rt_plc.h"
#include <cstdint>
#include <climits>

namespace rt_plc {

constexpr int SCAN_PHASE_COUNT = 5;

struct PhaseDiagStats {
    uint64_t count = 0;
    TIME     minTime = INT64_MAX;
    TIME     maxTime = 0;
    TIME     totalTime = 0;
    TIME     lastTime = 0;

    void record(TIME elapsedUs);
    TIME avgTime() const;
    void reset();
};

struct DiagStats {
    uint64_t totalScanCount     = 0;
    TIME     minScanTime        = INT64_MAX;
    TIME     maxScanTime        = 0;
    TIME     totalScanTime      = 0;  // 用于算平均
    TIME     lastScanTime       = 0;
    uint64_t totalOverruns      = 0;
    PhaseDiagStats phases[SCAN_PHASE_COUNT] = {};

    void recordScan(TIME scanTime);
    void recordPhase(ScanPhase phase, TIME elapsedUs);
    TIME avgScanTime() const;
    void reset();
};

} // namespace rt_plc
