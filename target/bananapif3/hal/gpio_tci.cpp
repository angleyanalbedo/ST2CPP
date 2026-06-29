#include "gpio_tci.h"
#include "gpio_hal.h"

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <cstdlib>

using namespace rt_plc;

BpiGpioTCI::BpiGpioTCI()
    : m_inputCount(0), m_outputCount(0)
{
    memset(m_inputs, 0xff, sizeof(m_inputs));
    memset(m_outputs, 0xff, sizeof(m_outputs));
}

BpiGpioTCI::~BpiGpioTCI() {
    gpio_hal_shutdown();
}

int BpiGpioTCI::init() {
    int ret = gpio_hal_init();
    if (ret != 0) {
        fprintf(stderr, "[GPIO-TCI] GPIO HAL init failed\n");
        return -1;
    }

    // 如果外部（gen_config.py）已添加映射，跳过默认映射
    if (m_inputCount == 0 && m_outputCount == 0) {
        loadDefaultMapping();
    }

    dumpMapping();
    return 0;
}

void BpiGpioTCI::loadDefaultMapping() {
    // 默认仅使用 40-pin 排针上安全的用户 GPIO
    // 输入（排针左侧）: Pin 11, 15, 16, 18, 19, 23, 24, 26
    addInputMapping(109, 0, 0);  // Pin 11
    addInputMapping(111, 0, 1);  // Pin 15
    addInputMapping(112, 0, 2);  // Pin 16
    addInputMapping(113, 0, 3);  // Pin 18
    addInputMapping(114, 0, 4);  // Pin 19
    addInputMapping(117, 0, 5);  // Pin 23
    addInputMapping(118, 0, 6);  // Pin 24
    addInputMapping(119, 0, 7);  // Pin 26

    // 输出（排针右侧）: Pin 29, 31, 12, 33, 40, 38
    addOutputMapping(63,  0, 0);  // Pin 29
    addOutputMapping(67,  0, 1);  // Pin 31
    addOutputMapping(77,  0, 2);  // Pin 12
    addOutputMapping(80,  0, 3);  // Pin 33
    addOutputMapping(126, 0, 4);  // Pin 40
    addOutputMapping(127, 0, 5);  // Pin 38

    for (int i = 0; i < m_inputCount; i++)
        gpio_set_mode(m_inputs[i].gpioLine, 0);
    for (int i = 0; i < m_outputCount; i++)
        gpio_set_mode(m_outputs[i].gpioLine, 1);
}

void BpiGpioTCI::addInputMapping(int gpioLine, int byteOffset, int bitOffset) {
    if (m_inputCount >= GPIO_TCI_MAX_PINS) {
        fprintf(stderr, "[GPIO-TCI] Input mapping full, cannot add GPIO %d\n", gpioLine);
        return;
    }
    GpioPinMapping& m = m_inputs[m_inputCount++];
    m.gpioLine   = gpioLine;
    m.byteOffset = byteOffset;
    m.bitOffset  = bitOffset;
    m.isInput    = true;
}

void BpiGpioTCI::addOutputMapping(int gpioLine, int byteOffset, int bitOffset) {
    if (m_outputCount >= GPIO_TCI_MAX_PINS) {
        fprintf(stderr, "[GPIO-TCI] Output mapping full, cannot add GPIO %d\n", gpioLine);
        return;
    }
    GpioPinMapping& m = m_outputs[m_outputCount++];
    m.gpioLine   = gpioLine;
    m.byteOffset = byteOffset;
    m.bitOffset  = bitOffset;
    m.isInput    = false;
}

void BpiGpioTCI::clearMappings() {
    m_inputCount = 0;
    m_outputCount = 0;
}

void BpiGpioTCI::syncInputs(rt_plc::ProcessImage& img) {
    for (int i = 0; i < m_inputCount; i++) {
        const GpioPinMapping& m = m_inputs[i];
        int val = gpio_read(m.gpioLine);
        img.writeInputBit(m.byteOffset, m.bitOffset, val ? TRUE : FALSE);
    }
}

void BpiGpioTCI::syncOutputs(rt_plc::ProcessImage& img) {
    for (int i = 0; i < m_outputCount; i++) {
        const GpioPinMapping& m = m_outputs[i];
        BOOL val = img.readOutputBit(m.byteOffset, m.bitOffset);
        gpio_write(m.gpioLine, val ? 1 : 0);
    }
}

void BpiGpioTCI::dumpMapping() const {
    fprintf(stderr, "\n=== GPIO-TCI Pin Mapping (BPI-F3) ===\n");
    fprintf(stderr, "Chip: %s\n", gpio_get_chip_label());

    fprintf(stderr, "\nInputs:\n");
    for (int i = 0; i < m_inputCount; i++) {
        const GpioPinMapping& m = m_inputs[i];
        fprintf(stderr, "  GPIO%3d → %%IX%d.%d\n", m.gpioLine, m.byteOffset, m.bitOffset);
    }

    fprintf(stderr, "\nOutputs:\n");
    for (int i = 0; i < m_outputCount; i++) {
        const GpioPinMapping& m = m_outputs[i];
        fprintf(stderr, "  %%QX%d.%d → GPIO%3d\n", m.byteOffset, m.bitOffset, m.gpioLine);
    }
    fprintf(stderr, "===========================\n\n");
}
