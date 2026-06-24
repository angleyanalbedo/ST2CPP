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

// 时间戳（微秒）
typedef int64_t TIME;

// 错误码
enum class ErrorCode : uint16_t {
    NONE = 0,
    DIV_BY_ZERO,
    ARRAY_OUT_OF_BOUNDS,
    INT_OVERFLOW,
    FLOAT_OVERFLOW,
    NULL_POINTER,
    STRING_OVERFLOW,
    STACK_OVERFLOW,
    WATCHDOG_TIMEOUT,
    IO_ERROR,
    TASK_OVERRUN,
    CONFIG_ERROR,
    SAFETY_VIOLATION,
    RETAIN_CORRUPTED,
    MISC_ERROR,
    USER_ERROR        // 用户自定义（ASSERT 等）
};

} // namespace rt_plc
