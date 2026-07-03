#pragma once

#include "debug/debug_if.h"

namespace rt_plc {

bool debugTcpServerStart(DebugEngine& engine, int port);
void debugTcpServerStop();
bool debugTcpServerRunning();

} // namespace rt_plc
