#pragma once

#include <cstdint>

namespace rt_plc {

enum class TaskTrigger : uint8_t {
    CYCLIC,
    EVENT,
    FREEWHEELING
};

enum class SystemState : uint8_t {
    STOP,
    STARTING,   // 正在启动（Init/Pre 阶段）
    RUN,
    STOPPING,   // 正在停止（Post 阶段）
    ERROR,
    PAUSED
};

enum class TaskState : uint8_t {
    IDLE,
    READY,
    RUNNING,
    OVERRUN,
    ERROR
};

enum class EventEdge : uint8_t {
    RISING,
    FALLING,
    BOTH
};

enum class StartupMode : uint8_t {
    COLD,   // 冷启动：全部清零
    WARM    // 暖启动：保留 RETAIN 区域
};

// 扫描周期阶段
enum class ScanPhase : uint8_t {
    IDLE,
    READ_INPUTS,
    LOGIC_SOLVE,
    WRITE_OUTPUTS,
    HOUSEKEEPING
};

} // namespace rt_plc
