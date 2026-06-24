#include "core/diag.h"

namespace rt_plc {

void DiagStats::recordScan(TIME scanTime) {
    totalScanCount++;
    totalScanTime += scanTime;
    lastScanTime = scanTime;
    if (scanTime < minScanTime) minScanTime = scanTime;
    if (scanTime > maxScanTime) maxScanTime = scanTime;
}

TIME DiagStats::avgScanTime() const {
    return totalScanCount > 0 ? totalScanTime / (TIME)totalScanCount : 0;
}

void DiagStats::reset() {
    totalScanCount = 0;
    minScanTime    = INT64_MAX;
    maxScanTime    = 0;
    totalScanTime  = 0;
    lastScanTime   = 0;
    totalOverruns  = 0;
}

} // namespace rt_plc
