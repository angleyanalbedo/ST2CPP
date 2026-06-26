#pragma once

// ═══════════════════════════════════════════════════════
// Platform Abstraction Layer
//
// 三种平台：
//   1. 默认（Windows/macOS）：使用 <chrono>, <thread>, fprintf
//   2. RT_PLATFORM_LINUX：使用 clock_gettime (Linux 用户态，精度更高)
//   3. RT_PLATFORM_BARE_METAL：用户提供实现（STM32 等裸机）
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

#define RT_THREAD_LOCAL static

#elif defined(RT_PLATFORM_LINUX)

// ─── Linux：clock_gettime 纳秒精度 ───

#include <time.h>
#include <unistd.h>
#include <cstdio>
#include <cstdarg>

namespace rt_plc { namespace platform {

inline int64_t nowUs() {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000000LL + ts.tv_nsec / 1000;
}

inline int64_t steadyUs() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
    return (int64_t)ts.tv_sec * 1000000LL + ts.tv_nsec / 1000;
}

inline void sleepUs(int64_t us) {
    struct timespec ts;
    ts.tv_sec  = us / 1000000;
    ts.tv_nsec = (us % 1000000) * 1000;
    nanosleep(&ts, nullptr);
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

#else

// ─── 桌面端（Windows/macOS）：使用标准库 ───

#include <chrono>
#include <cstdio>
#include <cstdarg>
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

#endif // RT_PLATFORM_BARE_METAL / RT_PLATFORM_LINUX

// ─── 统一日志宏 ───

#define RT_LOG_ERR(...)  rt_plc::platform::logErr(__VA_ARGS__)
#define RT_LOG_INFO(...) rt_plc::platform::logInfo(__VA_ARGS__)
