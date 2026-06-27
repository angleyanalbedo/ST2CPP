/**
 * gpio_tci.cpp — GPIO ↔ PLC ProcessImage 同步实现
 */

#include "gpio_tci.h"
#include "gpio_hal.h"

#include <cstdio>
#include <cstring>
#include <cerrno>
#include <cstdlib>

using namespace rt_plc;


RpiGpioTCI::RpiGpioTCI()
    : m_inputCount(0), m_outputCount(0)
{
    memset(m_inputs, 0xff, sizeof(m_inputs));   // -1 = 未映射
    memset(m_outputs, 0xff, sizeof(m_outputs));
}

RpiGpioTCI::~RpiGpioTCI() {
    gpio_hal_shutdown();
}


int RpiGpioTCI::init() {
    int ret = gpio_hal_init();
    if (ret != 0) {
        fprintf(stderr, "[GPIO-TCI] GPIO HAL init failed\n");
        return -1;
    }

    loadDefaultMapping();
    dumpMapping();
    return 0;
}


// ═══ 默认映射 ═══

void RpiGpioTCI::loadDefaultMapping() {
    // 输入：Pi 排针右列 GPIO（便于接按钮/传感器）
    // 物理引脚: 29,31,33,35,37,40,38,36
    addInputMapping( 5, 0, 0);  // %IX0.0  Pin 29
    addInputMapping( 6, 0, 1);  // %IX0.1  Pin 31
    addInputMapping(13, 0, 2);  // %IX0.2  Pin 33
    addInputMapping(19, 0, 3);  // %IX0.3  Pin 35
    addInputMapping(26, 0, 4);  // %IX0.4  Pin 37
    addInputMapping(21, 0, 5);  // %IX0.5  Pin 40
    addInputMapping(20, 0, 6);  // %IX0.6  Pin 38
    addInputMapping(16, 0, 7);  // %IX0.7  Pin 36

    // 输出：Pi 排针左列 GPIO（便于接 LED/继电器）
    // 物理引脚: 11,13,15,16,18,22,32,12
    addOutputMapping(17, 0, 0);  // %QX0.0  Pin 11
    addOutputMapping(27, 0, 1);  // %QX0.1  Pin 13
    addOutputMapping(22, 0, 2);  // %QX0.2  Pin 15
    addOutputMapping(23, 0, 3);  // %QX0.3  Pin 16
    addOutputMapping(24, 0, 4);  // %QX0.4  Pin 18
    addOutputMapping(25, 0, 5);  // %QX0.5  Pin 22
    addOutputMapping(12, 0, 6);  // %QX0.6  Pin 32
    addOutputMapping(18, 0, 7);  // %QX0.7  Pin 12

    // 设置所有映射引脚为 GPIO 模式
    for (int i = 0; i < m_inputCount; i++) {
        gpio_set_mode(m_inputs[i].bcmPin, GPIO_MODE_INPUT);
    }
    for (int i = 0; i < m_outputCount; i++) {
        gpio_set_mode(m_outputs[i].bcmPin, GPIO_MODE_OUTPUT);
    }
}


// ═══ 手动映射 ═══

void RpiGpioTCI::addInputMapping(int bcmPin, int byteOffset, int bitOffset) {
    if (m_inputCount >= GPIO_TCI_MAX_PINS) {
        fprintf(stderr, "[GPIO-TCI] Input mapping full, cannot add BCM %d\n", bcmPin);
        return;
    }
    GpioPinMapping& m = m_inputs[m_inputCount++];
    m.bcmPin     = bcmPin;
    m.byteOffset = byteOffset;
    m.bitOffset  = bitOffset;
    m.isInput    = true;
}

void RpiGpioTCI::addOutputMapping(int bcmPin, int byteOffset, int bitOffset) {
    if (m_outputCount >= GPIO_TCI_MAX_PINS) {
        fprintf(stderr, "[GPIO-TCI] Output mapping full, cannot add BCM %d\n", bcmPin);
        return;
    }
    GpioPinMapping& m = m_outputs[m_outputCount++];
    m.bcmPin     = bcmPin;
    m.byteOffset = byteOffset;
    m.bitOffset  = bitOffset;
    m.isInput    = false;
}

void RpiGpioTCI::clearMappings() {
    m_inputCount = 0;
    m_outputCount = 0;
}


// ═══ JSON 加载 ═══

int RpiGpioTCI::loadMapping(const char* jsonPath) {
    FILE* f = fopen(jsonPath, "r");
    if (!f) {
        fprintf(stderr, "[GPIO-TCI] Cannot open %s: %s\n", jsonPath, strerror(errno));
        return -1;
    }

    clearMappings();

    // 简易 JSON 解析（不依赖外部库）
    char line[256];
    bool inInputs = false;
    bool inOutputs = false;
    int curBcm = -1, curByte = -1, curBit = -1;

    while (fgets(line, sizeof(line), f)) {
        // 去除空格
        char* p = line;
        while (*p == ' ' || *p == '\t') p++;

        if (strstr(p, "\"inputs\""))  { inInputs = true; inOutputs = false; continue; }
        if (strstr(p, "\"outputs\"")) { inInputs = false; inOutputs = true; continue; }
        if (*p == '}') { inInputs = false; inOutputs = false; continue; }

        // 解析 "bcm": 5
        char* bcmStr = strstr(p, "\"bcm\"");
        char* byteStr = strstr(p, "\"byte\"");
        char* bitStr = strstr(p, "\"bit\"");

        if (bcmStr) curBcm = atoi(bcmStr + 6);
        if (byteStr) curByte = atoi(byteStr + 7);
        if (bitStr)  curBit = atoi(bitStr + 6);

        // 行尾有 } 或 , 表示一条映射结束
        if (curBcm >= 0 && curByte >= 0 && curBit >= 0) {
            if (inInputs)
                addInputMapping(curBcm, curByte, curBit);
            else if (inOutputs)
                addOutputMapping(curBcm, curByte, curBit);
            curBcm = curByte = curBit = -1;
        }
    }

    fclose(f);

    // 重新设置引脚模式
    for (int i = 0; i < m_inputCount; i++)
        gpio_set_mode(m_inputs[i].bcmPin, GPIO_MODE_INPUT);
    for (int i = 0; i < m_outputCount; i++)
        gpio_set_mode(m_outputs[i].bcmPin, GPIO_MODE_OUTPUT);

    fprintf(stderr, "[GPIO-TCI] Loaded %d input + %d output mappings from %s\n",
            m_inputCount, m_outputCount, jsonPath);
    dumpMapping();
    return 0;
}


// ═══ TCI 同步 ═══

void RpiGpioTCI::syncInputs(rt_plc::ProcessImage& img) {
    for (int i = 0; i < m_inputCount; i++) {
        const GpioPinMapping& m = m_inputs[i];
        int val = gpio_read(m.bcmPin);
        img.writeInputBit(m.byteOffset, m.bitOffset, val ? TRUE : FALSE);
    }
}

void RpiGpioTCI::syncOutputs(rt_plc::ProcessImage& img) {
    for (int i = 0; i < m_outputCount; i++) {
        const GpioPinMapping& m = m_outputs[i];
        BOOL val = img.readOutputBit(m.byteOffset, m.bitOffset);
        gpio_write(m.bcmPin, val ? 1 : 0);
    }
}


// ═══ 诊断 ═══

void RpiGpioTCI::dumpMapping() const {
    fprintf(stderr, "\n=== GPIO-TCI Pin Mapping ===\n");
    fprintf(stderr, "Model: %s | Pins: %d\n",
            gpio_get_model_name(), gpio_get_pin_count());

    fprintf(stderr, "\nInputs (physical → BCM → PLC):\n");
    for (int i = 0; i < m_inputCount; i++) {
        const GpioPinMapping& m = m_inputs[i];
        fprintf(stderr, "  BCM%2d → %%IX%d.%d\n", m.bcmPin, m.byteOffset, m.bitOffset);
    }

    fprintf(stderr, "\nOutputs (PLC → BCM → physical):\n");
    for (int i = 0; i < m_outputCount; i++) {
        const GpioPinMapping& m = m_outputs[i];
        fprintf(stderr, "  %%QX%d.%d → BCM%2d\n", m.byteOffset, m.bitOffset, m.bcmPin);
    }
    fprintf(stderr, "===========================\n\n");
}
