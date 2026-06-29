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
    loadDefaultMapping();
    dumpMapping();
    return 0;
}

void BpiGpioTCI::loadDefaultMapping() {
    addInputMapping(63,  0, 0);
    addInputMapping(67,  0, 1);
    addInputMapping(80,  0, 2);
    addInputMapping(96,  0, 3);
    addInputMapping(97,  0, 4);
    addInputMapping(110, 0, 5);
    addInputMapping(115, 0, 6);
    addInputMapping(116, 0, 7);

    addOutputMapping(123, 0, 0);
    addOutputMapping(124, 0, 1);
    addOutputMapping(127, 0, 2);

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
