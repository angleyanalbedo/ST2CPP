#pragma once

#include <cstddef>

namespace rt_plc {

constexpr int    MAX_TASKS         = 16;
constexpr int    MAX_EVENTS        = 16;
constexpr int    MAX_PROGRAMS      = 32;    // 最大 PROGRAM 实例数
constexpr int    MAX_POUS_PER_TASK = 8;
constexpr size_t GVL_SIZE          = 65536; // 64 KB 全局变量区
constexpr size_t GVL_RETAIN_SIZE   = 8192;  // 8 KB RETAIN 区域（暖启动保留）
constexpr int    MAX_PRIORITY      = 31;
constexpr int    MIN_PRIORITY      = 0;

} // namespace rt_plc
