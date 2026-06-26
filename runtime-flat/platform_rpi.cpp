/**
 * platform_rpi.cpp — Raspberry Pi Linux 平台实现
 *
 * 在 Linux 用户态实现硬实时 PLC 调度。
 * 精度依赖内核：PREEMPT_RT 内核 < 50us 抖动，标准内核 < 500us。
 *
 * 硬件要求：
 *   - Raspberry Pi 3/4/5（ARM Cortex-A）
 *   - 建议刷 PREEMPT_RT 内核（Ubuntu PREEMPT_RT 或 RT-PREEMPT patch）
 *
 * 编译方式：
 *   g++ -O2 -DRT_PLATFORM_LINUX -I include/ -c platform_rpi.cpp -o platform_rpi.o
 *   （不定义 RT_PLATFORM_BARE_METAL，使用桌面版 platform.h 中的 chrono）
 *   但本文件提供更高精度的实现，替代默认的 chrono 版本。
 *
 * 精度指标（PREEMPT_RT 内核）：
 *   - clock_gettime(CLOCK_MONOTONIC_RAW)：纳秒精度
 *   - POSIX timer + SIGEV_THREAD：抖动 < 30us
 *   - 普通 Linux 内核：抖动 < 500us（可接受用于 10ms+ 周期）
 *
 * 启动方式：
 *   sudo ./plc_runtime  # 需要 root 来设置实时调度策略
 *
 * 实时性优化（用户可选）：
 *   sudo chrt -f 99 ./plc_runtime   # SCHED_FIFO 最高优先级
 *   或在代码中调用 plc_rpi_set_rt_priority()
 */

#include "rt_runtime.h"
#include "rt_plc.h"

// 本文件在桌面 Linux 上也可编译（用于开发测试）
// 不需要 RT_PLATFORM_BARE_METAL

#include <time.h>
#include <signal.h>
#include <unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <cerrno>

namespace rt_plc { namespace platform {

// ═══════════════════════════════════════════════════════
// 高精度计时（clock_gettime，纳秒级）
// ═══════════════════════════════════════════════════════

int64_t steadyUs() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
    return (int64_t)ts.tv_sec * 1000000LL + ts.tv_nsec / 1000;
}

int64_t nowUs() {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000000LL + ts.tv_nsec / 1000;
}

void sleepUs(int64_t us) {
    struct timespec ts;
    ts.tv_sec  = us / 1000000;
    ts.tv_nsec = (us % 1000000) * 1000;
    nanosleep(&ts, nullptr);
}

void logErr(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
}

void logInfo(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stdout, fmt, args);
    va_end(args);
}

}} // namespace rt_plc::platform


// ═══════════════════════════════════════════════════════
// POSIX Timer：高精度 PLC 周期 tick
// ═══════════════════════════════════════════════════════

extern "C" void plc_runtime_tick();

static timer_t posix_timer_id = 0;
static volatile int64_t tick_start_us = 0;
static volatile uint32_t tick_count = 0;

static void timer_handler(sigval_t /*sv*/) {
    tick_count++;
    plc_runtime_tick();
}


/**
 * 创建 POSIX 定时器
 * @param intervalUs 周期（微秒）
 * @return 0 成功，-1 失败
 */
extern "C" int plc_rpi_create_timer(int64_t intervalUs) {
    struct sigevent sev = {};
    sev.sigev_notify          = SIGEV_THREAD;
    sev.sigev_notify_function = timer_handler;
    sev.sigev_value.sival_ptr = nullptr;

    if (timer_create(CLOCK_MONOTONIC, &sev, &posix_timer_id) != 0) {
        platform::logErr("timer_create failed: %s\n", strerror(errno));
        return -1;
    }

    struct itimerspec its = {};
    its.it_interval.tv_sec  = intervalUs / 1000000;
    its.it_interval.tv_nsec = (intervalUs % 1000000) * 1000;
    its.it_value = its.it_interval;  // 首次触发也在 interval 后

    if (timer_settime(posix_timer_id, 0, &its, nullptr) != 0) {
        platform::logErr("timer_settime failed: %s\n", strerror(errno));
        timer_delete(posix_timer_id);
        posix_timer_id = 0;
        return -1;
    }

    tick_start_us = platform::steadyUs();
    return 0;
}


/**
 * 销毁 POSIX 定时器
 */
extern "C" void plc_rpi_destroy_timer() {
    if (posix_timer_id) {
        timer_delete(posix_timer_id);
        posix_timer_id = 0;
    }
}


// ═══════════════════════════════════════════════════════
// 实时调度策略（可选，需要 root）
// ═══════════════════════════════════════════════════════

/**
 * 设置当前线程为 SCHED_FIFO 实时调度
 * @param priority 优先级（1-99，99 最高）
 * @return 0 成功，-1 失败
 */
extern "C" int plc_rpi_set_rt_priority(int priority) {
    struct sched_param param = {};
    param.sched_priority = priority;

    if (sched_setscheduler(0, SCHED_FIFO, &param) != 0) {
        platform::logErr("sched_setscheduler failed: %s (need root?)\n",
                        strerror(errno));
        return -1;
    }

    // 锁定内存，防止被换出到 swap
    mlockall(MCL_CURRENT | MCL_FUTURE);

    platform::logInfo("RT priority set: SCHED_FIFO/%d\n", priority);
    return 0;
}


/**
 * 获取 tick 统计信息
 */
extern "C" uint32_t plc_rpi_get_tick_count() {
    return tick_count;
}

extern "C" int64_t plc_rpi_get_uptime_us() {
    return platform::steadyUs() - tick_start_us;
}


// ═══════════════════════════════════════════════════════
// GPIO 操作（直接寄存器访问，用于 LED/按键）
// ═══════════════════════════════════════════════════════

// Raspberry Pi GPIO 基地址（BCM2835/BCM2711）
// Pi 3: 0x3F200000, Pi 4: 0xFE200000
// 更好的做法是通过 /dev/gpiomem mmap

static volatile uint32_t* gpio_base = nullptr;

extern "C" int plc_rpi_gpio_init() {
    int fd = open("/dev/gpiomem", O_RDWR | O_SYNC);
    if (fd < 0) {
        platform::logErr("Cannot open /dev/gpiomem: %s\n", strerror(errno));
        return -1;
    }

    gpio_base = (volatile uint32_t*)mmap(
        nullptr, 0x1000, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);

    if (gpio_base == MAP_FAILED) {
        platform::logErr("mmap gpio failed: %s\n", strerror(errno));
        gpio_base = nullptr;
        return -1;
    }

    return 0;
}

extern "C" void plc_rpi_gpio_set(int pin, int value) {
    if (!gpio_base) return;
    if (value)
        gpio_base[7] = (1 << pin);   // GPSET0
    else
        gpio_base[10] = (1 << pin);  // GPCLR0
}

extern "C" int plc_rpi_gpio_read(int pin) {
    if (!gpio_base) return 0;
    return (gpio_base[13] >> pin) & 1;  // GPLEV0
}
