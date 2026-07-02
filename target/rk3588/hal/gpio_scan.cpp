/**
 * gpio_scan.cpp — RK3588 GPIO 非实时扫描线程实现
 */

#include "gpio_scan.h"
#include <cstdio>
#include <cstring>
#include <cerrno>
#include <unistd.h>

#define TAG "[GPIO_SCAN]"

GpioScanThread::GpioScanThread() {
    for (int i = 0; i < RK_GPIO_SCAN_MAX_PINS; i++) {
        inputValues[i] = 0;
        outputValues[i] = -1;
    }
}

GpioScanThread::~GpioScanThread() {
    stop();
}

int GpioScanThread::start(const rk_gpio_pin_t* pins, int count) {
    if (m_running) return 0;
    if (!pins || count <= 0 || count > RK_GPIO_SCAN_MAX_PINS)
        return -1;

    m_pins = pins;
    m_pinCount = count;

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
            m_localOutput[i] = 0;  /* 输出默认低电平 */
    }

    /* 先做一次初始同步，让输出生效 */
    rk_gpio_write_all(m_localOutput);
    rk_gpio_read_all(m_localInput);
    for (int i = 0; i < count; i++)
        inputValues[i] = m_localInput[i];

    /* 启动扫描线程 */
    m_running = true;
    if (pthread_create(&m_thread, nullptr, scanLoop, this) != 0) {
        fprintf(stderr, TAG " pthread_create failed: %s\n", strerror(errno));
        m_running = false;
        return -1;
    }

    fprintf(stdout, TAG " started: %d pins, interval=%dus\n", count, intervalUs);
    return 0;
}

void GpioScanThread::stop() {
    if (!m_running) return;
    m_running = false;
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

void* GpioScanThread::scanLoop(void* arg) {
    GpioScanThread* self = static_cast<GpioScanThread*>(arg);
    self->loop();
    return nullptr;
}

void GpioScanThread::loop() {
    /* 线程名称：方便调试 */
    pthread_setname_np(pthread_self(), "gpio-scan");

    /* 设置极低实时优先级（低于 PLC 任务），保证被 RT 任务随时抢占 */
    struct sched_param sp = {};
    sp.sched_priority = 1;
    pthread_setschedparam(pthread_self(), SCHED_FIFO, &sp);

    /* 不用 usleep — 让 RT 任务自然抢占。RT 任务阻塞时 CPU 自动让给我们 */
    while (m_running) {
        /* ── 1. 读 GPIO → 写入共享输入缓冲区 ── */
        if (rk_gpio_read_all(m_localInput) == 0) {
            for (int i = 0; i < m_pinCount; i++)
                inputValues[i] = m_localInput[i];
        }

        /* ── 2. 读共享输出缓冲区 → 写 GPIO ── */
        for (int i = 0; i < m_pinCount; i++) {
            int val = (int)outputValues[i];
            if (val >= 0)
                m_localOutput[i] = val;
        }
        rk_gpio_write_all(m_localOutput);
    }
}
