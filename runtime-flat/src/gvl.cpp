#include "core/gvl.h"
#include <cstring>

namespace rt_plc {

void GVL::clear() {
    memset(memory, 0, GVL_SIZE);
    highWaterMark = 0;
}

void GVL::clearNonRetain() {
    if (retainStart > 0) {
        memset(memory, 0, retainStart);
    }
    if (retainEnd < GVL_SIZE) {
        memset(memory + retainEnd, 0, GVL_SIZE - retainEnd);
    }
    // 重算 highWaterMark（保守：保留 RETAIN 区域的大小）
    highWaterMark = retainEnd;
}

void GVL::setRetainRegion(size_t start, size_t end) {
    retainStart = start;
    retainEnd   = end;
}

} // namespace rt_plc
