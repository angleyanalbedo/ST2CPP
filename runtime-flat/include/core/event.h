#pragma once

#include "types.h"
#include "gvl.h"
#include "rt_plc.h"

namespace rt_plc {

// 事件条件函数
typedef BOOL (*EventConditionFunc)(GVL& gvl, ProcessImage& io);

struct Event {
    char               name[32] = {0};
    EventConditionFunc  condition = nullptr;
    EventEdge          edge      = EventEdge::RISING;
    int                taskIndex = -1;
    BOOL               lastState = FALSE;

    bool check(GVL& gvl, ProcessImage& io);
};

} // namespace rt_plc
