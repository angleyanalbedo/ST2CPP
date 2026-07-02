/**
 * gpio_hal.cpp — RK3588 GPIO 硬件抽象层实现
 *
 * 基于 Linux GPIO cdev 接口（/dev/gpiochipN），使用 GPIOHANDLE 批量 I/O。
 * 无需 libgpiod，直接调用 ioctl。
 *
 * 每个 cycle 仅 2 次 ioctl 系统调用（一次读、一次写），
 * 在 Xenomai Alchemy 下产生 2 次 domain switch，对 1ms cycle 可忽略。
 */

#include "gpio_hal.h"
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/gpio.h>

#define TAG "[GPIO_HAL]"

/* 每 GPIO chip 的引脚数 */
#define PINS_PER_BANK 32

/* 预分配的最大引脚数 */
#define MAX_PINS 32

/* 每个 bank 对应的 /dev/gpiochipN */
#define GPIO_CHIP_DEV "/dev/gpiochip"

struct GpioContext {
    const rk_gpio_pin_t* pins;
    int count;
    int chipFds[6];          /* gpiochip0-5，-1 = 未打开 */
    struct gpiohandle_data chipInputData[6];
    struct gpiohandle_data chipOutputData[6];
    int chipInputCount[6];   /* 每 bank 输入引脚数 */
    int chipOutputCount[6];  /* 每 bank 输出引脚数 */
    int chipInputOffsets[6][MAX_PINS];   /* 每 bank 输入引脚在 chip 内的偏移 */
    int chipOutputOffsets[6][MAX_PINS];  /* 每 bank 输出引脚偏移 */
    int chipInputFds[6];     /* 每 bank input handle fd */
    int chipOutputFds[6];    /* 每 bank output handle fd */
    bool initialized;
};

static GpioContext s_ctx = {};

static int bank_of(int pin) {
    return pin / PINS_PER_BANK;
}

static int offset_in_bank(int pin) {
    return pin % PINS_PER_BANK;
}

int rk_gpio_hal_init(const rk_gpio_pin_t* pins, int count) {
    if (s_ctx.initialized)
        return 0;

    if (!pins || count <= 0 || count > MAX_PINS * 6) {
        fprintf(stderr, TAG " invalid params: pins=%p count=%d\n", (void*)pins, count);
        return -1;
    }

    s_ctx.pins = pins;
    s_ctx.count = count;
    for (int i = 0; i < 6; i++) {
        s_ctx.chipFds[i] = -1;
        s_ctx.chipInputFds[i] = -1;
        s_ctx.chipOutputFds[i] = -1;
        s_ctx.chipInputCount[i] = 0;
        s_ctx.chipOutputCount[i] = 0;
    }

    /* 按 bank 分类引脚 */
    for (int i = 0; i < count; i++) {
        int pin = pins[i].globalPin;
        int bk = bank_of(pin);
        int off = offset_in_bank(pin);

        if (bk < 0 || bk > 5) {
            fprintf(stderr, TAG " invalid bank %d for pin %d\n", bk, pin);
            continue;
        }

        if (pins[i].direction == RK_GPIO_DIR_IN) {
            int idx = s_ctx.chipInputCount[bk];
            if (idx < MAX_PINS) {
                s_ctx.chipInputOffsets[bk][idx] = off;
                s_ctx.chipInputCount[bk]++;
            }
        } else {
            int idx = s_ctx.chipOutputCount[bk];
            if (idx < MAX_PINS) {
                s_ctx.chipOutputOffsets[bk][idx] = off;
                s_ctx.chipOutputCount[bk]++;
            }
        }
    }

    /* 打开每个有引脚的 bank */
    for (int bk = 0; bk < 6; bk++) {
        if (s_ctx.chipInputCount[bk] == 0 && s_ctx.chipOutputCount[bk] == 0)
            continue;

        char devPath[32];
        snprintf(devPath, sizeof(devPath), GPIO_CHIP_DEV "%d", bk);
        int fd = open(devPath, O_RDWR);
        if (fd < 0) {
            fprintf(stderr, TAG " open %s failed: %s\n", devPath, strerror(errno));
            continue;
        }
        s_ctx.chipFds[bk] = fd;

        /* 请求输入 handle */
        if (s_ctx.chipInputCount[bk] > 0) {
            struct gpiohandle_request req = {};
            req.flags = GPIOHANDLE_REQUEST_INPUT;
            req.lines = s_ctx.chipInputCount[bk];
            for (int i = 0; i < req.lines; i++)
                req.lineoffsets[i] = s_ctx.chipInputOffsets[bk][i];
            snprintf(req.consumer_label, sizeof(req.consumer_label), "st2c-plc-in-%d", bk);

            if (ioctl(fd, GPIO_GET_LINEHANDLE_IOCTL, &req) < 0) {
                fprintf(stderr, TAG " GPIO_GET_LINEHANDLE_IOCTL(in) bank%d failed: %s\n",
                        bk, strerror(errno));
                continue;
            }
            s_ctx.chipInputFds[bk] = req.fd;
        }

        /* 请求输出 handle */
        if (s_ctx.chipOutputCount[bk] > 0) {
            struct gpiohandle_request req = {};
            req.flags = GPIOHANDLE_REQUEST_OUTPUT;
            req.lines = s_ctx.chipOutputCount[bk];
            for (int i = 0; i < req.lines; i++)
                req.lineoffsets[i] = s_ctx.chipOutputOffsets[bk][i];
            snprintf(req.consumer_label, sizeof(req.consumer_label), "st2c-plc-out-%d", bk);

            if (ioctl(fd, GPIO_GET_LINEHANDLE_IOCTL, &req) < 0) {
                fprintf(stderr, TAG " GPIO_GET_LINEHANDLE_IOCTL(out) bank%d failed: %s\n",
                        bk, strerror(errno));
                continue;
            }
            s_ctx.chipOutputFds[bk] = req.fd;
        }
    }

    s_ctx.initialized = true;

    int inTotal = 0, outTotal = 0;
    for (int bk = 0; bk < 6; bk++) {
        inTotal += s_ctx.chipInputCount[bk];
        outTotal += s_ctx.chipOutputCount[bk];
    }
    fprintf(stdout, TAG " init: %d pins (%d in, %d out)\n", count, inTotal, outTotal);
    return 0;
}

void rk_gpio_hal_shutdown(void) {
    for (int i = 0; i < 6; i++) {
        if (s_ctx.chipInputFds[i] >= 0) {
            close(s_ctx.chipInputFds[i]);
            s_ctx.chipInputFds[i] = -1;
        }
        if (s_ctx.chipOutputFds[i] >= 0) {
            close(s_ctx.chipOutputFds[i]);
            s_ctx.chipOutputFds[i] = -1;
        }
        if (s_ctx.chipFds[i] >= 0) {
            close(s_ctx.chipFds[i]);
            s_ctx.chipFds[i] = -1;
        }
    }
    s_ctx.initialized = false;
}

int rk_gpio_read_all(int* values) {
    if (!s_ctx.initialized || !values)
        return -1;

    /* 对每个有输入的 bank 执行 bulk read */
    for (int bk = 0; bk < 6; bk++) {
        if (s_ctx.chipInputCount[bk] == 0)
            continue;
        if (s_ctx.chipInputFds[bk] < 0)
            continue;

        if (ioctl(s_ctx.chipInputFds[bk], GPIOHANDLE_GET_LINE_VALUES_IOCTL,
                  &s_ctx.chipInputData[bk]) < 0) {
            fprintf(stderr, TAG " GPIOHANDLE_GET_LINE_VALUES_IOCTL bank%d failed: %s\n",
                    bk, strerror(errno));
            return -1;
        }
    }

    /* 按原引脚顺序填充输出 */
    for (int i = 0; i < s_ctx.count; i++) {
        int pin = s_ctx.pins[i].globalPin;
        int bk = bank_of(pin);
        int off = offset_in_bank(pin);

        if (s_ctx.pins[i].direction == RK_GPIO_DIR_IN) {
            /* 查找这个偏移在 bank 输入中的位置 */
            int idx = -1;
            for (int j = 0; j < s_ctx.chipInputCount[bk]; j++) {
                if (s_ctx.chipInputOffsets[bk][j] == off) {
                    idx = j;
                    break;
                }
            }
            if (idx >= 0) {
                values[i] = s_ctx.chipInputData[bk].values[idx];
                if (s_ctx.pins[i].activeLow)
                    values[i] = !values[i];
            } else {
                values[i] = 0;
            }
        } else {
            values[i] = -1;  /* 输出引脚在 read_all 中标记为不可读 */
        }
    }

    return 0;
}

int rk_gpio_write_all(const int* values) {
    if (!s_ctx.initialized || !values)
        return -1;

    /* 收集每个 bank 的输出值 */
    for (int bk = 0; bk < 6; bk++) {
        if (s_ctx.chipOutputCount[bk] == 0)
            continue;
        if (s_ctx.chipOutputFds[bk] < 0)
            continue;

        /* 清零输出数据 */
        for (int j = 0; j < s_ctx.chipOutputCount[bk]; j++)
            s_ctx.chipOutputData[bk].values[j] = 0;
    }

    /* 按原引脚顺序填充输出值 */
    for (int i = 0; i < s_ctx.count; i++) {
        if (s_ctx.pins[i].direction != RK_GPIO_DIR_OUT)
            continue;

        int pin = s_ctx.pins[i].globalPin;
        int bk = bank_of(pin);
        int off = offset_in_bank(pin);

        int idx = -1;
        for (int j = 0; j < s_ctx.chipOutputCount[bk]; j++) {
            if (s_ctx.chipOutputOffsets[bk][j] == off) {
                idx = j;
                break;
            }
        }
        if (idx >= 0) {
            int val = values[i];
            if (s_ctx.pins[i].activeLow)
                val = !val;
            s_ctx.chipOutputData[bk].values[idx] = val ? 1 : 0;
        }
    }

    /* 对每个有输出的 bank 执行 bulk write */
    for (int bk = 0; bk < 6; bk++) {
        if (s_ctx.chipOutputCount[bk] == 0)
            continue;
        if (s_ctx.chipOutputFds[bk] < 0)
            continue;

        if (ioctl(s_ctx.chipOutputFds[bk], GPIOHANDLE_SET_LINE_VALUES_IOCTL,
                  &s_ctx.chipOutputData[bk]) < 0) {
            fprintf(stderr, TAG " GPIOHANDLE_SET_LINE_VALUES_IOCTL bank%d failed: %s\n",
                    bk, strerror(errno));
            return -1;
        }
    }

    return 0;
}

int rk_gpio_pin_count(void) {
    return s_ctx.count;
}
