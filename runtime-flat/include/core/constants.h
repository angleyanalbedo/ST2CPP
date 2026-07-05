#pragma once

#include <cstddef>

namespace rt_plc {

#ifndef MAX_TASKS
  constexpr int MAX_TASKS = 16;
#endif
constexpr int    MAX_EVENTS        = 16;
#ifndef MAX_PROGRAMS
  constexpr int MAX_PROGRAMS = 32;    // 最大 PROGRAM 实例数
#endif
#ifndef MAX_POUS_PER_TASK
  constexpr int MAX_POUS_PER_TASK = 8;
#endif

// GVL 大小可通过编译宏覆盖（STM32 等小 RAM 平台）
#ifndef GVL_SIZE_OVERRIDE
  constexpr size_t GVL_SIZE        = 65536; // 64 KB 全局变量区
#else
  constexpr size_t GVL_SIZE        = GVL_SIZE_OVERRIDE;
#endif

// RETAIN 区域大小（暖启动保留）
#ifndef GVL_RETAIN_SIZE_OVERRIDE
  constexpr size_t GVL_RETAIN_SIZE = 8192;  // 8 KB
#else
  constexpr size_t GVL_RETAIN_SIZE = GVL_RETAIN_SIZE_OVERRIDE;
#endif

// ProcessImage 大小也可覆盖
#ifndef PROCESS_IMAGE_SIZE_OVERRIDE
  constexpr size_t PROCESS_IMAGE_SIZE_CFG = 65536;
#else
  constexpr size_t PROCESS_IMAGE_SIZE_CFG = PROCESS_IMAGE_SIZE_OVERRIDE;
#endif

constexpr int    MAX_PRIORITY      = 31;
constexpr int    MIN_PRIORITY      = 0;

// Debug engine
#ifndef DEBUG_MAX_WATCH_VARS
constexpr int    DEBUG_MAX_WATCH_VARS = 64;
#endif

#ifndef DEBUG_MAX_FORCES
constexpr int    DEBUG_MAX_FORCES     = 32;
#endif

#ifndef DEBUG_MAX_PENDING_COMMANDS
constexpr int    DEBUG_MAX_PENDING_COMMANDS = 16;
#endif

} // namespace rt_plc
