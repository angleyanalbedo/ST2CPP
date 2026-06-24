#include "core/task.h"

namespace rt_plc {

bool Task::addPOU(POUFunc func) {
    if (pouCount >= MAX_POUS_PER_TASK) return false;
    pous[pouCount++] = func;
    return true;
}

bool Task::addProgram(int progIndex) {
    if (programCount >= MAX_POUS_PER_TASK) return false;
    programIndices[programCount++] = progIndex;
    return true;
}

void Task::executePOUs(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    for (int i = 0; i < pouCount; i++) {
        pous[i](gvl, io, cycleTimeUs);
    }
}

bool Task::isDue(TIME currentTime) const {
    return (currentTime - lastRunTime) >= interval;
}

} // namespace rt_plc
