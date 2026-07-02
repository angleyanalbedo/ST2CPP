/**
 * gpio_scan.h — RK3588 GPIO 非实时扫描线程
 *
 * 本模块解决 Xenomai 实时任务中无法直接调用 GPIO cdev ioctl 的问题。
 * 原理：将 GPIO I/O 放到一个 Linux 域（非实时）线程中做 cycle-by-cycle 轮询，
 * 通过 volatile 共享内存与 Xenomai 实时任务交换数据。
 *
 * ┌─────────────────────┐          ┌──────────────────────┐
 * │ Linux 域扫描线程      │ volatile │ Xenomai RT 任务       │
 * │ ioctl(gpiochip)      │◀────────▶│ syncInputs/syncOutputs│
 * │ 任意域切换，无所谓     │  共享内存 │ 纯 memcpy，零域切换    │
 * └─────────────────────┘          └──────────────────────┘
 */

#ifndef RK3588_GPIO_SCAN_H
#define RK3588_GPIO_SCAN_H

#include "gpio_hal.h"
#include <pthread.h>
#include <cstdint>

#define RK_GPIO_SCAN_MAX_PINS 32

class GpioScanThread {
public:
    GpioScanThread();
    ~GpioScanThread();

    int start(const rk_gpio_pin_t* pins, int count);
    void stop();

    int pinCount() const { return m_pinCount; }

    /* 共享输入缓冲区 — 扫描线程写，RT 任务读 */
    volatile int inputValues[RK_GPIO_SCAN_MAX_PINS];

    /* 共享输出缓冲区 — RT 任务写，扫描线程读 */
    volatile int outputValues[RK_GPIO_SCAN_MAX_PINS];

private:
    bool m_halInited = false;
    const rk_gpio_pin_t* m_pins = nullptr;
    int m_pinCount = 0;
    pthread_t m_thread = 0;
    volatile bool m_running = false;

    /* 扫描线程本地缓冲区（避免读共享内存时被撕裂） */
    int m_localInput[RK_GPIO_SCAN_MAX_PINS];
    int m_localOutput[RK_GPIO_SCAN_MAX_PINS];

    static void* scanLoop(void* arg);
    void loop();
};

#endif /* RK3588_GPIO_SCAN_H */
