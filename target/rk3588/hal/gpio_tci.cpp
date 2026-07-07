/**
 * gpio_tci.cpp — RK3588 GPIO TCI 实现（/dev/mem 直读版）
 */

#include "gpio_tci.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>

#define TAG "[RK3588_GPIO]"

/* 将全局引脚号转换为 EXT_PORT 位偏移，-1 表示不支持 */
int Rk3588GpioTCI::gpioToBitOffset(int globalPin) {
    if (globalPin < 128 || globalPin > 159) {
        fprintf(stderr, TAG " pin %d: only gpio4 (128-159) supported\n", globalPin);
        return -1;
    }
    return globalPin - 128;  /* gpio139=11, gpio140=12, gpio141=13 */
}

Rk3588GpioTCI::~Rk3588GpioTCI() {
    if (m_gpioRegs) {
        munmap((void*)m_gpioRegs, 0x1000);
        m_gpioRegs = nullptr;
    }
    if (m_memFd >= 0) { close(m_memFd); m_memFd = -1; }
}

void Rk3588GpioTCI::addInputMapping(int globalPin, int byteOffset, int bitIndex, bool activeLow) {
    if (m_mappingCount >= RK_GPIO_TCI_MAX_PINS) return;
    m_mappings[m_mappingCount].globalPin  = globalPin;
    m_mappings[m_mappingCount].byteOffset = byteOffset;
    m_mappings[m_mappingCount].bitIndex   = bitIndex;
    m_mappings[m_mappingCount].isInput    = true;
    m_mappings[m_mappingCount].activeLow  = activeLow;
    m_mappings[m_mappingCount].bitOffset  = gpioToBitOffset(globalPin);
    m_mappingCount++;
    m_inputCount++;
}

void Rk3588GpioTCI::addOutputMapping(int globalPin, int byteOffset, int bitIndex, bool activeLow) {
    if (m_mappingCount >= RK_GPIO_TCI_MAX_PINS) return;
    m_mappings[m_mappingCount].globalPin  = globalPin;
    m_mappings[m_mappingCount].byteOffset = byteOffset;
    m_mappings[m_mappingCount].bitIndex   = bitIndex;
    m_mappings[m_mappingCount].isInput    = false;
    m_mappings[m_mappingCount].activeLow  = activeLow;
    m_mappings[m_mappingCount].bitOffset  = gpioToBitOffset(globalPin);
    m_mappingCount++;
    m_outputCount++;
}

void Rk3588GpioTCI::clearMappings() {
    m_mappingCount = 0; m_inputCount = 0; m_outputCount = 0;
}

void Rk3588GpioTCI::loadDefaultMapping() {
    addInputMapping(140, 0, 0, true);   // %IX0.0 = plc_run
    addInputMapping(141, 0, 1, true);   // %IX0.1 = plc_stop
    addOutputMapping(139, 0, 0);        // %QX0.0 = LED (write no-op for now)
}

int Rk3588GpioTCI::init() {
    if (m_initialized) return 0;

    if (m_mappingCount == 0)
        loadDefaultMapping();

    /* 验证所有映射的位偏移有效 */
    for (int i = 0; i < m_mappingCount; i++) {
        if (m_mappings[i].bitOffset < 0)
            return -1;
    }

    /* 映射 GPIO4 寄存器到用户空间 */
    m_memFd = open("/dev/mem", O_RDWR | O_SYNC);
    if (m_memFd < 0) {
        fprintf(stderr, TAG " open /dev/mem failed: %s\n", strerror(errno));
        return -1;
    }

    m_gpioRegs = (volatile uint32_t*)
        mmap(NULL, 0x1000, PROT_READ | PROT_WRITE,
             MAP_SHARED, m_memFd, RK3588_GPIO4_PHYS);
    if (m_gpioRegs == MAP_FAILED) {
        fprintf(stderr, TAG " mmap gpio4 @ 0x%08X failed: %s\n",
                (unsigned)RK3588_GPIO4_PHYS, strerror(errno));
        close(m_memFd); m_memFd = -1;
        return -1;
    }

    m_initialized = true;
    fprintf(stdout, TAG " init: %d mappings (%d in, %d out) via /dev/mem [0x%08X]\n",
            m_mappingCount, m_inputCount, m_outputCount,
            (unsigned)RK3588_GPIO4_PHYS);
    return 0;
}

void Rk3588GpioTCI::syncInputs(rt_plc::ProcessImage& img) {
    if (!m_initialized || !m_gpioRegs) return;

    /* load 指令读取 EXT_PORT — 零域切换、零 syscall */
    uint32_t ext = m_gpioRegs[RK3588_EXT_PORT_OFF / 4];

    for (int i = 0; i < m_mappingCount; i++) {
        if (!m_mappings[i].isInput) continue;
        int raw = (ext >> m_mappings[i].bitOffset) & 1;
        int val = m_mappings[i].activeLow ? (!raw) : raw;
        img.writeInputBit(m_mappings[i].byteOffset,
                          m_mappings[i].bitIndex,
                          val != 0);
    }
}

void Rk3588GpioTCI::syncOutputs(rt_plc::ProcessImage& img) {
    /* /dev/mem 写被 pinctrl 拦截 — 输出功能暂不可用 */
    (void)img;
}

void Rk3588GpioTCI::dumpMapping() {
    printf(TAG " Mappings (%d):\n", m_mappingCount);
    for (int i = 0; i < m_mappingCount; i++) {
        const char* dir = m_mappings[i].isInput ? "->" : "<-";
        printf("  [%d] GPIO%d(bit%d) %s %%I%c%d.%d%s\n",
               i, m_mappings[i].globalPin, m_mappings[i].bitOffset, dir,
               m_mappings[i].isInput ? 'X' : 'Q',
               m_mappings[i].byteOffset, m_mappings[i].bitIndex,
               m_mappings[i].activeLow ? " ACTIVE_LOW" : "");
    }
}

/* ---- JSON 配置解析（开发辅助） ---- */

static int skipWs(FILE* f) {
    int c;
    do { c = fgetc(f); } while (c != EOF && (c == ' ' || c == '\t' || c == '\n' || c == '\r'));
    return c;
}

static int expect(FILE* f, int e) {
    int c = skipWs(f);
    if (c != e) { fprintf(stderr, TAG " JSON: expected '%c' got '%c'\n", (char)e, (char)c); return -1; }
    return 0;
}

int Rk3588GpioTCI::loadMapping(const char* jsonPath) {
    FILE* f = fopen(jsonPath, "r");
    if (!f) { fprintf(stderr, TAG " open '%s' failed: %s\n", jsonPath, strerror(errno)); return -1; }
    clearMappings();
    int c;
    if (expect(f, '{') != 0) { fclose(f); return -1; }
    while ((c = skipWs(f)) != EOF && c != '}') {
        ungetc(c, f);
        char sec[32];
        if (fscanf(f, "\"%31[^\"]\"", sec) != 1) break;
        if (expect(f, ':') != 0) break;
        c = skipWs(f);
        if (c == '[') {
            while ((c = skipWs(f)) != EOF && c != ']') {
                ungetc(c, f);
                if (expect(f, '{') != 0) break;
                int pin=-1, bo=0, bi=0, al=0;
                while ((c = skipWs(f)) != EOF && c != '}') {
                    ungetc(c, f); char key[32];
                    if (fscanf(f, "\"%31[^\"]\"", key) != 1) break;
                    if (expect(f, ':') != 0) break;
                    if (strcmp(key,"pin")==0) (void)fscanf(f,"%d",&pin);
                    else if (strcmp(key,"byte")==0) (void)fscanf(f,"%d",&bo);
                    else if (strcmp(key,"bit")==0) (void)fscanf(f,"%d",&bi);
                    else if (strcmp(key,"activeLow")==0||strcmp(key,"active-low")==0) (void)fscanf(f,"%d",&al);
                    else { int d; (void)fscanf(f,"%d",&d); }
                    c = skipWs(f);
                    if (c=='}') { ungetc(c,f); break; }
                    if (c!=',') break;
                }
                if (expect(f,'}')!=0) break;
                if (pin>=0) {
                    if (strcmp(sec,"inputs")==0) addInputMapping(pin,bo,bi,al!=0);
                    else if (strcmp(sec,"outputs")==0) addOutputMapping(pin,bo,bi,al!=0);
                }
                c=skipWs(f);
                if (c==']') break;
                if (c!=',') break;
            }
        }
        c=skipWs(f);
        if (c=='}') break;
        if (c!=',') break;
    }
    fclose(f);
    fprintf(stdout, TAG " loaded '%s': %d mappings\n", jsonPath, m_mappingCount);
    return 0;
}
