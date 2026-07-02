/**
 * gpio_hal.h — RK3588 GPIO 硬件抽象层
 *
 * 通过 Linux GPIO cdev 接口访问引脚（/dev/gpiochipN），
 * 支持批量 I/O，每个 cycle 仅 2 次 ioctl（一次读、一次写）。
 */

#ifndef RK3588_GPIO_HAL_H
#define RK3588_GPIO_HAL_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* 最大引脚数 */
#define RK_GPIO_MAX_PINS 32

/* 方向 */
#define RK_GPIO_DIR_IN   0
#define RK_GPIO_DIR_OUT  1

/* 电平 */
#define RK_GPIO_LOW      0
#define RK_GPIO_HIGH     1

/* 便捷宏：构建 bank+offset */
#define RK_GPIO_PIN(bank, offset)  ((bank) * 32 + (offset))

/* GPIO 引脚描述 */
typedef struct {
    int globalPin;    /* 全局引脚号 0-159 */
    int direction;    /* RK_GPIO_DIR_IN / RK_GPIO_DIR_OUT */
    int activeLow;    /* 1 = 低电平有效 */
} rk_gpio_pin_t;

/* ============================================================
 * API
 * ============================================================ */

/**
 * 初始化 GPIO HAL：打开并请求所有配置的引脚。
 * pins: 引脚配置数组
 * count: 引脚数量
 * 返回 0 成功，-1 失败。
 */
int rk_gpio_hal_init(const rk_gpio_pin_t* pins, int count);

/**
 * 关闭 GPIO HAL：释放引脚和关闭文件描述符。
 */
void rk_gpio_hal_shutdown(void);

/**
 * 批量读取所有请求的引脚电平。
 * 按 pins 数组顺序填充 values。
 * 返回 0 成功。
 */
int rk_gpio_read_all(int* values);

/**
 * 批量写入所有请求的引脚电平。
 * values 数组与 pins 数组顺序对应。
 * 返回 0 成功。
 */
int rk_gpio_write_all(const int* values);

/**
 * 获取引脚总数。
 */
int rk_gpio_pin_count(void);

#ifdef __cplusplus
}
#endif

#endif /* RK3588_GPIO_HAL_H */
