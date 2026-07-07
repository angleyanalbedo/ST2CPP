/**
 * gpio_scan.h — RK3588 GPIO 非实时扫描守护线程
 *
 * 本模块解决 Xenomai 实时任务中无法直接调用 GPIO cdev ioctl 的问题。
 * 原理：将 GPIO I/O 放到一个 Linux 域（非实时）线程中做 cycle-by-cycle 轮询，
 * 通过 std::atomic 共享内存与 Xenomai 实时任务交换数据。
 *
 * ┌──────────────────────┐      std::atomic      ┌──────────────────────┐
 * │ Linux 域扫描 Daemon   │◀────────────────────▶│ Xenomai RT 任务       │
 * │ ioctl(gpiochip)       │    共享内存 (relaxed) │ syncInputs/syncOutputs│
 * │ 任意域切换，无所谓      │                      │ 纯 atomic load/store  │
 * └──────────────────────┘                      └──────────────────────┘
 */

#ifndef RK3588_GPIO_SCAN_H
#define RK3588_GPIO_SCAN_H

#include "gpio_hal.h"
#include <atomic>
#include <pthread.h>
#include <cstdint>

#define RK_GPIO_SCAN_MAX_PINS 32

class GpioScanDaemon {
public:
    GpioScanDaemon();
    ~GpioScanDaemon();

    int start(const rk_gpio_pin_t* pins, int count, int scanPeriodUs = 100);
    void stop();

    int pinCount() const { return m_pinCount; }

    /* RT 安全读取输入（从共享快照，无 syscall） */
    int readInput(int halIndex) const {
        if (halIndex < 0 || halIndex >= m_pinCount) return 0;
        return m_inputValues[halIndex].load(std::memory_order_relaxed);
    }

    /* RT 安全写入输出（到共享快照，无 syscall） */
    void writeOutput(int halIndex, int value) {
        if (halIndex < 0 || halIndex >= m_pinCount) return;
        m_outputValues[halIndex].store(value, std::memory_order_relaxed);
    }

private:
    bool m_halInited = false;
    const rk_gpio_pin_t* m_pins = nullptr;
    int m_pinCount = 0;
    int m_scanPeriodUs = 100;
    pthread_t m_thread = 0;
    std::atomic<bool> m_running{false};

    /* 共享内存（RT 任务 readInput/writeOutput 访问，Daemon 线程读写） */
    std::atomic<int> m_inputValues[RK_GPIO_SCAN_MAX_PINS]{};
    std::atomic<int> m_outputValues[RK_GPIO_SCAN_MAX_PINS]{};

    /* Daemon 线程本地缓冲区（避免 ioctl 直接读写原子变量） */
    int m_localInput[RK_GPIO_SCAN_MAX_PINS] = {};
    int m_localOutput[RK_GPIO_SCAN_MAX_PINS] = {};

    static void* scanLoop(void* arg);
    void loop();

    /* 用 clock_nanosleep(ABSTIME) 等待下一个扫描周期 */
    void sleepUntilNextScan(struct timespec& next);
};

#endif /* RK3588_GPIO_SCAN_H */
