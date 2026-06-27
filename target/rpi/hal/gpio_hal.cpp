/**
 * gpio_hal.cpp — Raspberry Pi GPIO HAL 实现
 *
 * 直接寄存器访问（/dev/gpiomem），支持 Pi 1-5。
 */

#include "gpio_hal.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <cerrno>

// ═══ BCM2835/2836/2837/2711 GPIO 寄存器偏移 ═══
// 所有型号的 GPIO 寄存器基地址相对偏移一致

#define GPIO_BASE_OFFSET   0x200000   // BCM2835/2836/2837
#define GPIO_BASE_OFFSET_4 0x200000   // BCM2711 (Pi 4) 也用 0x200000
#define GPIO_MEM_SIZE      0x1000     // 映射 4KB

#define GPFSEL0   0   // 功能选择 0-5（每引脚 3 bit）
#define GPFSEL1   1
#define GPFSEL2   2
#define GPFSEL3   3
#define GPFSEL4   4
#define GPFSEL5   5
#define GPSET0    7   // 输出置 1
#define GPSET1    8
#define GPCLR0    10  // 输出置 0
#define GPCLR1    11
#define GPLEV0    13  // 读引脚电平
#define GPLEV1    14
#define GPPUD     37  // 上拉/下拉（BCM2835/2836/2837）
#define GPPUDCLK0 38
#define GPPUDCLK1 39

// Pi 4 (BCM2711) 使用不同的上拉控制寄存器
#define GPPUPPDN0 57
#define GPPUPPDN1 58


static volatile uint32_t* g_gpio = nullptr;
static rpi_model_t g_model = RPI_MODEL_UNKNOWN;
static int g_pin_count = 0;

// BCM 号 → 型号对应的最大 GPIO 编号
// BCM2835/2836/2837: GPIO 0-27 (28 个)
// BCM2711 (Pi 4):    GPIO 0-27 (28 个)
// BCM2712 (Pi 5):    GPIO 0-27 (28 个) + 扩展
#define BCM_MAX_PIN 27


// ═══ 型号检测 ═══

static rpi_model_t detect_model(void) {
    FILE* f = fopen("/proc/device-tree/model", "r");
    if (!f) {
        f = fopen("/proc/cpuinfo", "r");
        if (!f) return RPI_MODEL_UNKNOWN;

        char line[256];
        rpi_model_t model = RPI_MODEL_UNKNOWN;
        while (fgets(line, sizeof(line), f)) {
            if (strstr(line, "BCM2708")) { model = RPI_MODEL_1; break; }
            if (strstr(line, "BCM2709")) { model = RPI_MODEL_2; break; }
            if (strstr(line, "BCM2835")) { model = RPI_MODEL_1; break; }
            if (strstr(line, "BCM2836")) { model = RPI_MODEL_2; break; }
            if (strstr(line, "BCM2837")) { model = RPI_MODEL_3; break; }
        }
        fclose(f);
        return model;
    }

    char model_str[256] = {};
    fread(model_str, 1, sizeof(model_str) - 1, f);
    fclose(f);

    if (strstr(model_str, "Raspberry Pi 5"))       return RPI_MODEL_5;
    if (strstr(model_str, "Raspberry Pi 4"))       return RPI_MODEL_4;
    if (strstr(model_str, "Raspberry Pi 3"))       return RPI_MODEL_3;
    if (strstr(model_str, "Raspberry Pi 2"))       return RPI_MODEL_2;
    if (strstr(model_str, "Raspberry Pi Zero 2"))  return RPI_MODEL_ZERO2;
    if (strstr(model_str, "Raspberry Pi Zero"))    return RPI_MODEL_ZERO;
    if (strstr(model_str, "Raspberry Pi"))          return RPI_MODEL_1;

    return RPI_MODEL_UNKNOWN;
}


// ═══ BCM 编号合法性检查 ═══

static inline bool valid_bcm(int pin) {
    return pin >= 0 && pin <= BCM_MAX_PIN;
}


// ═══ 引脚模式操作 ═══

static void set_gpio_function(int pin, uint32_t func) {
    int reg   = pin / 10;
    int shift = (pin % 10) * 3;
    uint32_t val = g_gpio[reg];
    val &= ~(7u << shift);
    val |= ((func & 7u) << shift);
    g_gpio[reg] = val;
}

static uint32_t get_gpio_function(int pin) {
    int reg   = pin / 10;
    int shift = (pin % 10) * 3;
    return (g_gpio[reg] >> shift) & 7u;
}


// ═══ 上拉/下拉（BCM2835/2836/2837） ═══

static void set_pud_old(int pin, int pud) {
    // pud: 0=off, 1=down, 2=up
    g_gpio[GPPUD] = pud;
    usleep(5);
    g_gpio[GPPUDCLK0] = (1u << pin);
    usleep(5);
    g_gpio[GPPUD] = 0;
    g_gpio[GPPUDCLK0] = 0;
}

// BCM2711 (Pi 4) 上拉控制
static void set_pud_new(int pin, int pud) {
    int reg   = pin / 16;
    int shift = (pin % 16) * 2;
    uint32_t val = g_gpio[GPPUPPDN0 + reg];
    val &= ~(3u << shift);
    val |= ((pud & 3u) << shift);
    g_gpio[GPPUPPDN0 + reg] = val;
}


// ═══ 公开 API ═══

int gpio_hal_init(void) {
    if (g_gpio) return 0;  // 已初始化

    g_model = detect_model();

    const char* dev = "/dev/gpiomem";
    int fd = open(dev, O_RDWR | O_SYNC);
    if (fd < 0) {
        fprintf(stderr, "[GPIO] Cannot open %s: %s\n", dev, strerror(errno));
        return -1;
    }

    g_gpio = (volatile uint32_t*)mmap(
        nullptr, GPIO_MEM_SIZE, PROT_READ | PROT_WRITE,
        MAP_SHARED, fd, GPIO_BASE_OFFSET);
    close(fd);

    if (g_gpio == MAP_FAILED) {
        fprintf(stderr, "[GPIO] mmap failed: %s\n", strerror(errno));
        g_gpio = nullptr;
        return -1;
    }

    // 确定排针引脚数
    switch (g_model) {
        case RPI_MODEL_1:
            g_pin_count = 26;   // 早期 Pi 1 是 26 针
            break;
        case RPI_MODEL_ZERO:
            g_pin_count = 26;   // Pi Zero 也是 26 针
            break;
        default:
            g_pin_count = 40;   // Pi 2/3/4/5/Zero2 都是 40 针
            break;
    }

    fprintf(stderr, "[GPIO] Model: %s | Pins: %d | BCM max: %d\n",
            gpio_get_model_name(), g_pin_count, BCM_MAX_PIN);
    return 0;
}


void gpio_hal_shutdown(void) {
    if (g_gpio) {
        munmap((void*)g_gpio, GPIO_MEM_SIZE);
        g_gpio = nullptr;
    }
    g_model = RPI_MODEL_UNKNOWN;
    g_pin_count = 0;
}


int gpio_set_mode(int bcm_pin, gpio_mode_t mode) {
    if (!g_gpio) return -1;
    if (!valid_bcm(bcm_pin)) {
        fprintf(stderr, "[GPIO] Invalid BCM pin: %d (max %d)\n", bcm_pin, BCM_MAX_PIN);
        return -1;
    }
    set_gpio_function(bcm_pin, (uint32_t)mode);
    return 0;
}


int gpio_read(int bcm_pin) {
    if (!g_gpio) return 0;
    if (!valid_bcm(bcm_pin)) return 0;
    return (g_gpio[GPLEV0] >> bcm_pin) & 1u;
}


void gpio_write(int bcm_pin, int value) {
    if (!g_gpio) return;
    if (!valid_bcm(bcm_pin)) return;

    if (value)
        g_gpio[GPSET0] = (1u << bcm_pin);
    else
        g_gpio[GPCLR0] = (1u << bcm_pin);
}


void gpio_toggle(int bcm_pin) {
    gpio_write(bcm_pin, !gpio_read(bcm_pin));
}


int gpio_physical_to_bcm(int physical_pin) {
    if (physical_pin < 1 || physical_pin > g_pin_count) return -1;

    if (g_pin_count == 26) {
        return PIN_MAP_26[physical_pin - 1];
    } else {
        return PIN_MAP_40[physical_pin - 1];
    }
}


rpi_model_t gpio_get_model(void) {
    return g_model;
}


const char* gpio_get_model_name(void) {
    switch (g_model) {
        case RPI_MODEL_1:     return "Raspberry Pi 1 (BCM2835)";
        case RPI_MODEL_2:     return "Raspberry Pi 2 (BCM2836/7)";
        case RPI_MODEL_3:     return "Raspberry Pi 3 (BCM2837)";
        case RPI_MODEL_4:     return "Raspberry Pi 4 (BCM2711)";
        case RPI_MODEL_5:     return "Raspberry Pi 5 (BCM2712)";
        case RPI_MODEL_ZERO:  return "Raspberry Pi Zero";
        case RPI_MODEL_ZERO2: return "Raspberry Pi Zero 2";
        default:              return "Unknown";
    }
}


int gpio_get_pin_count(void) {
    return g_pin_count;
}
