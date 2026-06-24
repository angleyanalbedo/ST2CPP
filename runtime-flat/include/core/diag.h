#pragma once

#include "rt_plc.h"
#include <cstdint>
#include <climits>

namespace rt_plc {

struct DiagStats {
    uint64_t totalScanCount     = 0;
    TIME     minScanTime        = INT64_MAX;
    TIME     maxScanTime        = 0;
    TIME     totalScanTime      = 0;  // 用于算平均
    TIME     lastScanTime       = 0;
    uint64_t totalOverruns      = 0;

    void recordScan(TIME scanTime);
    TIME avgScanTime() const;
    void reset();
};

} // namespace rt_plc
