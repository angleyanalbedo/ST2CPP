/**
 * gpio_tci.h — RK3588 GPIO TCI（/dev/mem 直读版）
 *
 * 原理：RT 任务在 syncInputs() 中通过 mmap 的 /dev/mem 指针直接读取
 * GPIO EXT_PORT 寄存器。没有 ioctl，没有域切换，没有扫描线程。
 *
 * RT-safe 验证：
 *   init() 中的 mmap 在 RT 启动前完成
 *   syncInputs() 仅包含一条 load 指令 + 位运算
 *   syncOutputs() 为 no-op（pinctrl 阻止 /dev/mem 写）
 *
 * MatriBox 默认：
 *   gpio140 (GPIO4_C4, bit 12) = %IX0.0  (plc_run 按钮, ACTIVE LOW)
 *   gpio141 (GPIO4_C5, bit 13) = %IX0.1  (plc_stop 按钮, ACTIVE LOW)
 */

#ifndef RK3588_GPIO_TCI_H
#define RK3588_GPIO_TCI_H

#include "rt_plc.h"
#include <cstdint>

#define RK_GPIO_TCI_MAX_PINS 32
#define RK3588_GPIO4_PHYS    0xFEC50000u
#define RK3588_EXT_PORT_OFF  0x08u

class Rk3588GpioTCI : public rt_plc::TCI {
public:
    struct PinMapping {
        int globalPin;      // 物理引脚号（如 140）
        int bitOffset;      // EXT_PORT 中的位偏移（gpio140=bit 12）
        int byteOffset;     // ProcessImage 字节偏移
        int bitIndex;       // ProcessImage 位索引
        bool activeLow;
        bool isInput;
    };

    Rk3588GpioTCI() = default;
    ~Rk3588GpioTCI() override;

    void addInputMapping(int globalPin, int byteOffset, int bitIndex, bool activeLow = false);
    void addOutputMapping(int globalPin, int byteOffset, int bitIndex, bool activeLow = false);
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
    int gpioToBitOffset(int globalPin);

    PinMapping m_mappings[RK_GPIO_TCI_MAX_PINS];
    int m_mappingCount = 0;
    int m_inputCount = 0;
    int m_outputCount = 0;
    bool m_initialized = false;

    /* /dev/mem 映射 */
    int m_memFd = -1;
    volatile uint32_t* m_gpioRegs = nullptr;
};

#endif /* RK3588_GPIO_TCI_H */
