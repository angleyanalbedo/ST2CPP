#pragma once

#include <cstddef>

namespace rt_plc {

constexpr int    MAX_TASKS         = 16;
constexpr int    MAX_EVENTS        = 16;
constexpr int    MAX_PROGRAMS      = 32;    // 最大 PROGRAM 实例数
constexpr int    MAX_POUS_PER_TASK = 8;

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

} // namespace rt_plc
