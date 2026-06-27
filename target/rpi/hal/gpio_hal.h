/**
 * gpio_hal.h — Raspberry Pi GPIO 硬件抽象层
 *
 * 支持 Pi 1/2/3/4/5 的 26 针和 40 针排针布局。
 * 通过 /dev/gpiomem 直接寄存器访问，无需外部库。
 *
 * 用法：
 *   gpio_hal_init();
 *   gpio_set_mode(17, GPIO_MODE_OUTPUT);
 *   gpio_set_mode(27, GPIO_MODE_INPUT);
 *   gpio_write(17, 1);
 *   int val = gpio_read(27);
 *   gpio_shutdown();
 */

#ifndef GPIO_HAL_H
#define GPIO_HAL_H

#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

// ═══ 引脚编号：BCM 芯片编号（直接对应寄存器 bit） ═══

// Raspberry Pi 排针物理引脚 → BCM GPIO 映射（40 针排针）
// 物理引脚编号从 1 开始，奇数在左列，偶数在右列
//
//  排针布局（40 针）：
//  Pin  1: 3.3V          Pin  2: 5V
//  Pin  3: GPIO2 (SDA1)  Pin  4: 5V
//  Pin  5: GPIO3 (SCL1)  Pin  6: GND
//  Pin  7: GPIO4          Pin  8: GPIO14 (TXD)
//  Pin  9: GND            Pin 10: GPIO15 (RXD)
//  Pin 11: GPIO17         Pin 12: GPIO18
//  Pin 13: GPIO27         Pin 14: GND
//  Pin 15: GPIO22         Pin 16: GPIO23
//  Pin 17: 3.3V           Pin 18: GPIO24
//  Pin 19: GPIO10 (MOSI) Pin 20: GND
//  Pin 21: GPIO9 (MISO)  Pin 22: GPIO25
//  Pin 23: GPIO11 (SCLK) Pin 24: GPIO8 (CE0)
//  Pin 25: GND            Pin 26: GPIO7 (CE1)
//  Pin 27: GPIO0 (ID_SD) Pin 28: GPIO1 (ID_SC)
//  Pin 29: GPIO5          Pin 30: GND
//  Pin 31: GPIO6          Pin 32: GPIO12
//  Pin 33: GPIO13         Pin 34: GND
//  Pin 35: GPIO19         Pin 36: GPIO16
//  Pin 37: GPIO26         Pin 38: GPIO20
//  Pin 39: GND            Pin 40: GPIO21
//
// 排针物理引脚 → BCM 转换表（40 针型号，index = 物理引脚号 - 1）
static const int PIN_MAP_40[] = {
    0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  // Pin  1-10
   10, 11, 12, 13, 14, 15, 16, 17, 18, 19,  // Pin 11-20
   20, 21, 22, 23, 24, 25, 26, 27, 28, 29,  // Pin 21-30
   30, 31, 32, 33, 34, 35, 36, 37, 38, 39,  // Pin 31-40
   -1  // 40 针排针外的物理引脚
};

// 26 针排针 → BCM 转换表（Pi 1 首发版 / Pi Zero）
// Pin  1: 3.3V       Pin  2: 5V
// Pin  3: GPIO2      Pin  4: 5V
// Pin  5: GPIO3      Pin  6: GND
// Pin  7: GPIO4      Pin  8: GPIO14
// Pin  9: GND        Pin 10: GPIO15
// Pin 11: GPIO17     Pin 12: GPIO18
// Pin 13: GPIO27     Pin 14: GND
// Pin 15: GPIO22     Pin 16: GPIO23
// Pin 17: 3.3V       Pin 18: GPIO24
// Pin 19: GPIO10     Pin 20: GND
// Pin 21: GPIO9      Pin 22: GPIO25
// Pin 23: GPIO11     Pin 24: GPIO8
// Pin 25: GND        Pin 26: GPIO7
static const int PIN_MAP_26[] = {
    0,  1,  2,  3,  4,  5,  6,  7,  8,  9,
   10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
   20, 21, 22, 23, 24, 25, -1
};


// ═══ GPIO 模式 ═══
enum gpio_mode_t {
    GPIO_MODE_INPUT  = 0,
    GPIO_MODE_OUTPUT = 1,
    GPIO_MODE_ALT0   = 4,
    GPIO_MODE_ALT1   = 5,
    GPIO_MODE_ALT2   = 6,
    GPIO_MODE_ALT3   = 7,
    GPIO_MODE_ALT4   = 3,
    GPIO_MODE_ALT5   = 2
};

// ═══ 型号枚举 ═══
enum rpi_model_t {
    RPI_MODEL_UNKNOWN = 0,
    RPI_MODEL_1,      // BCM2835, 26 针
    RPI_MODEL_2,      // BCM2836/7
    RPI_MODEL_3,      // BCM2837
    RPI_MODEL_4,      // BCM2711, 40 针
    RPI_MODEL_5,      // BCM2712
    RPI_MODEL_ZERO,
    RPI_MODEL_ZERO2
};


// ═══ 核心 API ═══

/**
 * 初始化 GPIO HAL。
 * 1. 检测树莓派型号
 * 2. 映射寄存器
 * @return 0 成功, -1 失败
 */
int gpio_hal_init(void);

/** 释放映射 */
void gpio_hal_shutdown(void);

/** 设置引脚模式（input/output/alt） */
int gpio_set_mode(int bcm_pin, gpio_mode_t mode);

/** 读取引脚值（0 或 1） */
int gpio_read(int bcm_pin);

/** 写入引脚值（0 或 1） */
void gpio_write(int bcm_pin, int value);

/** 切换引脚值 */
void gpio_toggle(int bcm_pin);


// ═══ 物理引脚号 → BCM 转换 ═══

/**
 * 将物理排针引脚号转为 BCM GPIO 编号。
 * @param physical_pin  物理排针引脚号（1-40 或 1-26）
 * @return BCM GPIO 编号，无效引脚返回 -1
 */
int gpio_physical_to_bcm(int physical_pin);

/** 获取检测到的型号 */
rpi_model_t gpio_get_model(void);

/** 获取型号名称字符串 */
const char* gpio_get_model_name(void);

/** 获取当前型号支持的物理排针引脚总数 */
int gpio_get_pin_count(void);


#ifdef __cplusplus
}
#endif

#endif // GPIO_HAL_H
