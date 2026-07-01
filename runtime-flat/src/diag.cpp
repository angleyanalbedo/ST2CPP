#include "core/diag.h"

namespace rt_plc {

void PhaseDiagStats::record(TIME elapsedUs) {
    count++;
    totalTime += elapsedUs;
    lastTime = elapsedUs;
    if (elapsedUs < minTime) minTime = elapsedUs;
    if (elapsedUs > maxTime) maxTime = elapsedUs;
}

TIME PhaseDiagStats::avgTime() const {
    return count > 0 ? totalTime / (TIME)count : 0;
}

void PhaseDiagStats::reset() {
    count = 0;
    minTime = INT64_MAX;
    maxTime = 0;
    totalTime = 0;
    lastTime = 0;
}

void DiagStats::recordScan(TIME scanTime) {
    totalScanCount++;
    totalScanTime += scanTime;
    lastScanTime = scanTime;
    if (scanTime < minScanTime) minScanTime = scanTime;
    if (scanTime > maxScanTime) maxScanTime = scanTime;
}

void DiagStats::recordPhase(ScanPhase phase, TIME elapsedUs) {
    int idx = static_cast<int>(phase);
    if (idx < 0 || idx >= SCAN_PHASE_COUNT) return;
    phases[idx].record(elapsedUs);
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
    for (int i = 0; i < SCAN_PHASE_COUNT; i++) {
        phases[i].reset();
    }
}

} // namespace rt_plc
