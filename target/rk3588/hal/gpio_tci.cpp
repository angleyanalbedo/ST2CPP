/**
 * gpio_tci.cpp — RK3588 GPIO TCI 实现（扫描线程后端）
 */

#include "gpio_tci.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cerrno>

#define TAG "[RK3588_GPIO]"

Rk3588GpioTCI::~Rk3588GpioTCI() {
    /* GpioScanThread 析构时自动 stop */
}

void Rk3588GpioTCI::addInputMapping(int globalPin, int byteOffset, int bitOffset, bool activeLow) {
    if (m_mappingCount >= RK_GPIO_TCI_MAX_PINS) return;
    m_mappings[m_mappingCount].globalPin  = globalPin;
    m_mappings[m_mappingCount].byteOffset = byteOffset;
    m_mappings[m_mappingCount].bitOffset  = bitOffset;
    m_mappings[m_mappingCount].isInput    = true;
    m_mappings[m_mappingCount].activeLow  = activeLow;
    m_mappings[m_mappingCount].halIndex   = -1;
    m_mappingCount++;
    m_inputCount++;
}

void Rk3588GpioTCI::addOutputMapping(int globalPin, int byteOffset, int bitOffset, bool activeLow) {
    if (m_mappingCount >= RK_GPIO_TCI_MAX_PINS) return;
    m_mappings[m_mappingCount].globalPin  = globalPin;
    m_mappings[m_mappingCount].byteOffset = byteOffset;
    m_mappings[m_mappingCount].bitOffset  = bitOffset;
    m_mappings[m_mappingCount].isInput    = false;
    m_mappings[m_mappingCount].activeLow  = activeLow;
    m_mappings[m_mappingCount].halIndex   = -1;
    m_mappingCount++;
    m_outputCount++;
}

void Rk3588GpioTCI::clearMappings() {
    m_mappingCount = 0;
    m_inputCount = 0;
    m_outputCount = 0;
    m_halCount = 0;
}

void Rk3588GpioTCI::loadDefaultMapping() {
    addInputMapping(140, 0, 0, true);   // %IX0.0 = plc_run (ACTIVE LOW)
    addInputMapping(141, 0, 1, true);   // %IX0.1 = plc_stop (ACTIVE LOW)
    addOutputMapping(139, 0, 0);        // %QX0.0 = GPIO4_B3 (LED)
}

int Rk3588GpioTCI::rebuildHalConfig() {
    m_halCount = 0;
    for (int i = 0; i < m_mappingCount; i++) {
        m_halPins[m_halCount].globalPin = m_mappings[i].globalPin;
        m_halPins[m_halCount].direction = m_mappings[i].isInput ?
            RK_GPIO_DIR_IN : RK_GPIO_DIR_OUT;
        m_halPins[m_halCount].activeLow = m_mappings[i].activeLow ? 1 : 0;
        m_mappings[i].halIndex = m_halCount;
        m_halCount++;
    }
    return m_halCount;
}

int Rk3588GpioTCI::init() {
    if (m_initialized) return 0;

    if (m_mappingCount == 0)
        loadDefaultMapping();

    rebuildHalConfig();

    /* 启动非实时 GPIO 扫描线程（SCHED_FIFO 1，远低于 PLC 任务的 90） */
    int ret = m_scanThread.start(m_halPins, m_halCount);
    if (ret != 0) {
        fprintf(stderr, TAG " GpioScanThread::start() failed\n");
        return -1;
    }

    m_initialized = true;
    fprintf(stdout, TAG " init: %d mappings (%d in, %d out) via scan thread\n",
            m_mappingCount, m_inputCount, m_outputCount);
    return 0;
}

void Rk3588GpioTCI::syncInputs(rt_plc::ProcessImage& img) {
    if (!m_initialized) return;

    /* 纯共享内存读取 — 零域切换 */
    for (int i = 0; i < m_mappingCount; i++) {
        if (!m_mappings[i].isInput) continue;
        int hi = m_mappings[i].halIndex;
        if (hi < 0) continue;

        int raw = m_scanThread.inputValues[hi];
        int val = m_mappings[i].activeLow ? (!raw) : raw;

        img.writeInputBit(m_mappings[i].byteOffset,
                          m_mappings[i].bitOffset,
                          val != 0);
    }
}

void Rk3588GpioTCI::syncOutputs(rt_plc::ProcessImage& img) {
    if (!m_initialized) return;

    /* 纯共享内存写入 — 零域切换 */
    for (int i = 0; i < m_mappingCount; i++) {
        if (m_mappings[i].isInput) continue;
        int hi = m_mappings[i].halIndex;
        if (hi < 0) continue;

        bool val = img.readOutputBit(m_mappings[i].byteOffset,
                                     m_mappings[i].bitOffset);
        int raw = m_mappings[i].activeLow ? (!val) : (val ? 1 : 0);
        m_scanThread.outputValues[hi] = raw;
    }
}

void Rk3588GpioTCI::dumpMapping() {
    printf(TAG " Mappings (%d):\n", m_mappingCount);
    for (int i = 0; i < m_mappingCount; i++) {
        const char* dir = m_mappings[i].isInput ? "->" : "<-";
        printf("  [%d] GPIO%d %s %%I%c%d.%d (hal_idx=%d)%s\n",
               i, m_mappings[i].globalPin, dir,
               m_mappings[i].isInput ? 'X' : 'Q',
               m_mappings[i].byteOffset,
               m_mappings[i].bitOffset,
               m_mappings[i].halIndex,
               m_mappings[i].activeLow ? " ACTIVE_LOW" : "");
    }
}

/* ---- JSON 配置解析 ---- */

static int skipWhitespace(FILE* f) {
    int c;
    while ((c = fgetc(f)) != EOF) {
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
            return c;
    }
    return EOF;
}

static int expectChar(FILE* f, int expected) {
    int c = skipWhitespace(f);
    if (c != expected) {
        fprintf(stderr, TAG " JSON: expected '%c' got '%c'\n", (char)expected, (char)c);
        return -1;
    }
    return 0;
}

int Rk3588GpioTCI::loadMapping(const char* jsonPath) {
    FILE* f = fopen(jsonPath, "r");
    if (!f) {
        fprintf(stderr, TAG " open '%s' failed: %s\n", jsonPath, strerror(errno));
        return -1;
    }

    clearMappings();

    int c;
    if (expectChar(f, '{') != 0) { fclose(f); return -1; }

    while ((c = skipWhitespace(f)) != EOF && c != '}') {
        ungetc(c, f);

        char section[32];
        if (fscanf(f, "\"%31[^\"]\"", section) != 1) break;
        if (expectChar(f, ':') != 0) break;

        c = skipWhitespace(f);
        if (c == '[') {
            while ((c = skipWhitespace(f)) != EOF && c != ']') {
                ungetc(c, f);
                if (expectChar(f, '{') != 0) break;

                int pin = -1, byteOff = 0, bitOff = 0, activeLow = 0;
                while ((c = skipWhitespace(f)) != EOF && c != '}') {
                    ungetc(c, f);
                    char key[32];
                    if (fscanf(f, "\"%31[^\"]\"", key) != 1) break;
                    if (expectChar(f, ':') != 0) break;

                    if (strcmp(key, "pin") == 0) fscanf(f, "%d", &pin);
                    else if (strcmp(key, "byte") == 0) fscanf(f, "%d", &byteOff);
                    else if (strcmp(key, "bit") == 0) fscanf(f, "%d", &bitOff);
                    else if (strcmp(key, "activeLow") == 0 || strcmp(key, "active-low") == 0)
                        fscanf(f, "%d", &activeLow);
                    else { int dummy; fscanf(f, "%d", &dummy); }

                    c = skipWhitespace(f);
                    if (c == '}') { ungetc(c, f); break; }
                    if (c != ',') break;
                }
                if (expectChar(f, '}') != 0) break;

                if (pin >= 0) {
                    if (strcmp(section, "inputs") == 0)
                        addInputMapping(pin, byteOff, bitOff, activeLow != 0);
                    else if (strcmp(section, "outputs") == 0)
                        addOutputMapping(pin, byteOff, bitOff, activeLow != 0);
                }

                c = skipWhitespace(f);
                if (c == ']') break;
                if (c != ',') break;
            }
        }

        c = skipWhitespace(f);
        if (c == '}') break;
        if (c != ',') break;
    }

    fclose(f);
    fprintf(stdout, TAG " loaded '%s': %d mappings\n", jsonPath, m_mappingCount);
    return 0;
}
