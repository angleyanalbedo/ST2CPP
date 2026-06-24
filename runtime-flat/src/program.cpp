#include "core/program.h"

namespace rt_plc {

void ProgramInstance::doInit(GVL& gvl, ProcessImage& io) {
    if (initFunc) initFunc(gvl, io);
    phase = ProgramPhase::INIT;
}

void ProgramInstance::doPre(GVL& gvl, ProcessImage& io) {
    if (preFunc) preFunc(gvl, io);
    phase = ProgramPhase::FIRST_SCAN;
}

void ProgramInstance::doCyclic(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    if (cyclicFunc) cyclicFunc(gvl, io, cycleTimeUs);
    if (phase == ProgramPhase::FIRST_SCAN) {
        phase = ProgramPhase::CYCLIC;  // 首次扫描后进入正常循环
    }
    cycleCount++;
}

void ProgramInstance::doPost(GVL& gvl, ProcessImage& io) {
    if (postFunc) postFunc(gvl, io);
    phase = ProgramPhase::STOPPED;
}

} // namespace rt_plc
