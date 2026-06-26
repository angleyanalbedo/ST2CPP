/**
 * platform_rpi.cpp — Raspberry Pi Linux 平台扩展
 *
 * 提供 platform.h 之上的额外功能：
 *   - POSIX timer（高精度 PLC 周期 tick）
 *   - 实时调度策略（SCHED_FIFO）
 *   - GPIO 直接寄存器访问（LED 心跳 + 示波器抖动测量）
 *   - 抖动统计
 *
 * platform.h 已提供 nowUs/steadyUs/sleepUs/logErr/logInfo（通过 RT_PLATFORM_LINUX）
 * 本文件不需要重复定义。
 */

#include "rt_runtime.h"
#include "rt_plc.h"

#include <time.h>
#include <signal.h>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <pthread.h>
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <cerrno>

using namespace rt_plc::platform;


// ═══════════════════════════════════════════════════════
// POSIX Timer：高精度 PLC 周期 tick
// ═══════════════════════════════════════════════════════

extern "C" void plc_runtime_tick();

static timer_t posix_timer_id = 0;
static volatile int64_t tick_start_us = 0;
static volatile uint32_t tick_count = 0;

// 抖动统计
static volatile int64_t jitter_last_us = 0;
static volatile int64_t jitter_sum = 0;
static volatile int64_t jitter_min = 999999;
static volatile int64_t jitter_max = 0;
static volatile uint64_t jitter_count = 0;

static void timer_handler(sigval_t /*sv*/) {
    int64_t now = steadyUs();
    int64_t delta = now - jitter_last_us;
    jitter_last_us = now;

    if (tick_count > 0 && delta > 0) {
        jitter_sum += delta;
        if (delta < jitter_min) jitter_min = delta;
        if (delta > jitter_max) jitter_max = delta;
        jitter_count++;
    }

    tick_count++;
    plc_runtime_tick();
}


extern "C" int plc_rpi_create_timer(int64_t intervalUs) {
    struct sigevent sev = {};
    sev.sigev_notify          = SIGEV_THREAD;
    sev.sigev_notify_function = timer_handler;
    sev.sigev_value.sival_ptr = nullptr;

    if (timer_create(CLOCK_MONOTONIC, &sev, &posix_timer_id) != 0) {
        logErr("timer_create failed: %s\n", strerror(errno));
        return -1;
    }

    struct itimerspec its = {};
    its.it_interval.tv_sec  = intervalUs / 1000000;
    its.it_interval.tv_nsec = (intervalUs % 1000000) * 1000;
    its.it_value = its.it_interval;

    if (timer_settime(posix_timer_id, 0, &its, nullptr) != 0) {
        logErr("timer_settime failed: %s\n", strerror(errno));
        timer_delete(posix_timer_id);
        posix_timer_id = 0;
        return -1;
    }

    tick_start_us = steadyUs();
    jitter_last_us = tick_start_us;
    return 0;
}


extern "C" void plc_rpi_destroy_timer() {
    if (posix_timer_id) {
        timer_delete(posix_timer_id);
        posix_timer_id = 0;
    }
}


// ═══════════════════════════════════════════════════════
// 实时调度策略（需要 root）
// ═══════════════════════════════════════════════════════

extern "C" int plc_rpi_set_rt_priority(int priority) {
    struct sched_param param = {};
    param.sched_priority = priority;

    if (sched_setscheduler(0, SCHED_FIFO, &param) != 0) {
        logErr("sched_setscheduler failed: %s (need root?)\n", strerror(errno));
        return -1;
    }

    mlockall(MCL_CURRENT | MCL_FUTURE);
    logInfo("RT priority set: SCHED_FIFO/%d\n", priority);
    return 0;
}


// ═══════════════════════════════════════════════════════
// GPIO 直接寄存器访问
// ═══════════════════════════════════════════════════════

static volatile uint32_t* gpio_base = nullptr;

extern "C" int plc_rpi_gpio_init() {
    int fd = open("/dev/gpiomem", O_RDWR | O_SYNC);
    if (fd < 0) {
        logErr("Cannot open /dev/gpiomem: %s\n", strerror(errno));
        return -1;
    }

    gpio_base = (volatile uint32_t*)mmap(
        nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);

    if (gpio_base == MAP_FAILED) {
        logErr("mmap gpio failed: %s\n", strerror(errno));
        gpio_base = nullptr;
        return -1;
    }

    logInfo("GPIO initialized\n");
    return 0;
}

extern "C" void plc_rpi_gpio_set(int pin, int value) {
    if (!gpio_base) return;
    if (value) gpio_base[7] = (1 << pin);
    else       gpio_base[10] = (1 << pin);
}

extern "C" int plc_rpi_gpio_read(int pin) {
    if (!gpio_base) return 0;
    return (gpio_base[13] >> pin) & 1;
}


// ═══════════════════════════════════════════════════════
// 统计接口
// ═══════════════════════════════════════════════════════

extern "C" uint32_t plc_rpi_get_tick_count() {
    return tick_count;
}

extern "C" int64_t plc_rpi_get_uptime_us() {
    return steadyUs() - tick_start_us;
}

extern "C" void plc_rpi_print_jitter_stats() {
    logInfo("\n=== Jitter Statistics ===\n");
    logInfo("  Samples    : %llu\n", (unsigned long long)jitter_count);
    if (jitter_count > 0) {
        logInfo("  Min jitter : %lld us\n", (long long)jitter_min);
        logInfo("  Max jitter : %lld us\n", (long long)jitter_max);
        logInfo("  Avg jitter : %lld us\n", (long long)(jitter_sum / jitter_count));
    }
    logInfo("==========================\n");
}
