#pragma once

// ═══════════════════════════════════════════════════════
// Platform Abstraction Layer
//
// 桌面端（默认）：使用 <chrono>, <thread>, <atomic>, fprintf
// 裸机端（STM32 等）：定义 RT_PLATFORM_BARE_METAL，提供自己的实现
//
// STM32 裸机用户需要实现以下函数：
//   int64_t  rt_plc::platform::nowUs()       — 系统时钟微秒（如 HAL_GetTick * 1000）
//   int64_t  rt_plc::platform::steadyUs()    — 单调时钟微秒（如 DWT cycle counter）
//   void     rt_plc::platform::sleepUs(int64_t) — 延时微秒（或 no-op）
//   void     rt_plc::platform::logErr(const char* fmt, ...)  — 错误输出
//   void     rt_plc::platform::logInfo(const char* fmt, ...) — 信息输出
// ═══════════════════════════════════════════════════════

#if defined(RT_PLATFORM_BARE_METAL)

// ─── 裸机：用户提供实现 ───

#include <cstdarg>
#include <cstdint>

namespace rt_plc { namespace platform {

int64_t nowUs();
int64_t steadyUs();
void sleepUs(int64_t us);

void logErr(const char* fmt, ...);
void logInfo(const char* fmt, ...);

}} // namespace rt_plc::platform

// 裸机上 thread_local 不可用，用 static 替代
#define RT_THREAD_LOCAL static

#else

// ─── 桌面端：使用标准库 ───

#include <chrono>
#include <cstdio>
#include <thread>

namespace rt_plc { namespace platform {

inline int64_t nowUs() {
    auto now = std::chrono::system_clock::now();
    return std::chrono::duration_cast<std::chrono::microseconds>(
        now.time_since_epoch()).count();
}

inline int64_t steadyUs() {
    auto now = std::chrono::steady_clock::now();
    return std::chrono::duration_cast<std::chrono::microseconds>(
        now.time_since_epoch()).count();
}

inline void sleepUs(int64_t us) {
    std::this_thread::sleep_for(std::chrono::microseconds(us));
}

inline void logErr(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
}

inline void logInfo(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stdout, fmt, args);
    va_end(args);
}

}} // namespace rt_plc::platform

#define RT_THREAD_LOCAL thread_local

#endif // RT_PLATFORM_BARE_METAL

// ─── 统一日志宏 ───

#define RT_LOG_ERR(...)  rt_plc::platform::logErr(__VA_ARGS__)
#define RT_LOG_INFO(...) rt_plc::platform::logInfo(__VA_ARGS__)
