/**
 * gpio_tci.h — RK3588 GPIO TCI（非实时扫描版）
 *
 * TCI 后端使用 GpioScanThread 将 GPIO I/O 委托给 Linux 域线程，
 * syncInputs/syncOutputs 仅为共享内存拷贝，不在 Xenomai 实时上下文中
 * 触发任何 ioctl 系统调用。
 *
 * MatriBox 默认：
 *   gpio140 (GPIO4_C4) = %IX0.0  (plc_run 按钮, ACTIVE LOW)
 *   gpio141 (GPIO4_C5) = %IX0.1  (plc_stop 按钮, ACTIVE LOW)
 */

#ifndef RK3588_GPIO_TCI_H
#define RK3588_GPIO_TCI_H

#include "gpio_hal.h"
#include "gpio_scan.h"
#include "rt_plc.h"
#include <cstdint>

#define RK_GPIO_TCI_MAX_PINS 32

class Rk3588GpioTCI : public rt_plc::TCI {
public:
    struct PinMapping {
        int globalPin;
        int byteOffset;
        int bitOffset;
        bool isInput;
        bool activeLow;
        int halIndex;       /* 在 HAL pin 数组中的索引 */
    };

    Rk3588GpioTCI() = default;
    ~Rk3588GpioTCI() override;

    void addInputMapping(int globalPin, int byteOffset, int bitOffset, bool activeLow = false);
    void addOutputMapping(int globalPin, int byteOffset, int bitOffset, bool activeLow = false);
    void clearMappings();

    int loadMapping(const char* jsonPath);

    int init();
    void syncInputs(rt_plc::ProcessImage& img) override;
    void syncOutputs(rt_plc::ProcessImage& img) override;

    void dumpMapping();

    int inputCount() const { return m_inputCount; }
    int outputCount() const { return m_outputCount; }

private:
    void loadDefaultMapping();
    int rebuildHalConfig();

    PinMapping m_mappings[RK_GPIO_TCI_MAX_PINS];
    int m_mappingCount = 0;
    int m_inputCount = 0;
    int m_outputCount = 0;
    int m_halCount = 0;
    bool m_initialized = false;

    rk_gpio_pin_t m_halPins[RK_GPIO_TCI_MAX_PINS];

    /* 非实时 GPIO 扫描线程 */
    GpioScanThread m_scanThread;
};

#endif /* RK3588_GPIO_TCI_H */
