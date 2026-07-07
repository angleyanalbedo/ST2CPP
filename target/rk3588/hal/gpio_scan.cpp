/**
 * gpio_scan.cpp — RK3588 GPIO 非实时扫描守护线程实现
 */

#include "gpio_scan.h"
#include <cstdio>
#include <cstring>
#include <cerrno>
#include <time.h>

#define TAG "[GPIO_SCAN]"

GpioScanDaemon::GpioScanDaemon() {
    for (int i = 0; i < RK_GPIO_SCAN_MAX_PINS; i++) {
        m_inputValues[i].store(0, std::memory_order_relaxed);
        m_outputValues[i].store(-1, std::memory_order_relaxed);
    }
}

GpioScanDaemon::~GpioScanDaemon() {
    stop();
}

int GpioScanDaemon::start(const rk_gpio_pin_t* pins, int count, int scanPeriodUs) {
    if (m_running.load(std::memory_order_relaxed)) return 0;
    if (!pins || count <= 0 || count > RK_GPIO_SCAN_MAX_PINS)
        return -1;

    m_pins = pins;
    m_pinCount = count;
    m_scanPeriodUs = scanPeriodUs > 0 ? scanPeriodUs : 100;

    /* 初始化 GPIO HAL（打开 cdev handle） */
    if (rk_gpio_hal_init(pins, count) != 0) {
        fprintf(stderr, TAG " rk_gpio_hal_init() failed\n");
        return -1;
    }
    m_halInited = true;

    /* 清零本地缓冲 */
    memset(m_localInput, 0, sizeof(m_localInput));
    for (int i = 0; i < RK_GPIO_SCAN_MAX_PINS; i++)
        m_localOutput[i] = -1;
    for (int i = 0; i < count; i++) {
        if (pins[i].direction == RK_GPIO_DIR_OUT)
            m_localOutput[i] = 0;
    }

    /* 初始同步 */
    rk_gpio_write_all(m_localOutput);
    rk_gpio_read_all(m_localInput);
    for (int i = 0; i < count; i++)
        m_inputValues[i].store(m_localInput[i], std::memory_order_relaxed);

    /* 启动守护线程 */
    m_running.store(true, std::memory_order_relaxed);
    if (pthread_create(&m_thread, nullptr, scanLoop, this) != 0) {
        fprintf(stderr, TAG " pthread_create failed: %s\n", strerror(errno));
        m_running.store(false, std::memory_order_relaxed);
        return -1;
    }

    fprintf(stdout, TAG " started: %d pins, period=%dus\n", count, m_scanPeriodUs);
    return 0;
}

void GpioScanDaemon::stop() {
    if (!m_running.load(std::memory_order_relaxed)) return;
    m_running.store(false, std::memory_order_relaxed);
    if (m_thread) {
        pthread_join(m_thread, nullptr);
        m_thread = 0;
    }
    if (m_halInited) {
        rk_gpio_hal_shutdown();
        m_halInited = false;
    }
    fprintf(stdout, TAG " stopped\n");
}

void* GpioScanDaemon::scanLoop(void* arg) {
    GpioScanDaemon* self = static_cast<GpioScanDaemon*>(arg);
    self->loop();
    return nullptr;
}

void GpioScanDaemon::sleepUntilNextScan(struct timespec& next) {
    clock_gettime(CLOCK_MONOTONIC, &next);
    next.tv_nsec += m_scanPeriodUs * 1000;
    while (next.tv_nsec >= 1000000000) {
        next.tv_nsec -= 1000000000;
        next.tv_sec++;
    }
    clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, nullptr);
}

void GpioScanDaemon::loop() {
    pthread_setname_np(pthread_self(), "gpio-scan");

    struct timespec next;
    clock_gettime(CLOCK_MONOTONIC, &next);
    next.tv_nsec += m_scanPeriodUs * 1000;
    while (next.tv_nsec >= 1000000000) {
        next.tv_nsec -= 1000000000;
        next.tv_sec++;
    }

    while (m_running.load(std::memory_order_relaxed)) {
        /* 读 GPIO → 写入共享输入缓冲区 */
        if (rk_gpio_read_all(m_localInput) == 0) {
            for (int i = 0; i < m_pinCount; i++)
                m_inputValues[i].store(m_localInput[i], std::memory_order_relaxed);
        }

        /* 读共享输出缓冲区 → 写 GPIO */
        for (int i = 0; i < m_pinCount; i++)
            m_localOutput[i] = m_outputValues[i].load(std::memory_order_relaxed);
        rk_gpio_write_all(m_localOutput);

        sleepUntilNextScan(next);
    }
}
