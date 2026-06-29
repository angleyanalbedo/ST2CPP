#include "gpio_hal.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <cerrno>

#define GPIO_SYSFS "/sys/class/gpio"

static bool g_initialized = false;
static char g_chip_label[64] = {};

static void gpio_sysfs_write(const char* path, const char* value) {
    if (!path || !value) return;
    int fd = open(path, O_WRONLY);
    if (fd < 0) return;
    write(fd, value, strlen(value));
    close(fd);
}

static int gpio_sysfs_read(const char* path) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) return 0;
    char buf[16] = {};
    read(fd, buf, sizeof(buf) - 1);
    close(fd);
    return atoi(buf);
}

static int gpio_export(int line) {
    char path[64];
    snprintf(path, sizeof(path), GPIO_SYSFS "/gpio%d", line);
    struct stat st;
    if (stat(path, &st) == 0) return 0;

    int fd = open(GPIO_SYSFS "/export", O_WRONLY);
    if (fd < 0) return -1;
    char buf[8];
    int n = snprintf(buf, sizeof(buf), "%d", line);
    write(fd, buf, n);
    close(fd);

    usleep(10000);
    return 0;
}

int gpio_hal_init(void) {
    if (g_initialized) return 0;
    g_initialized = true;

    int fd = open(GPIO_SYSFS "/gpiochip0/label", O_RDONLY);
    if (fd >= 0) {
        read(fd, g_chip_label, sizeof(g_chip_label) - 1);
        close(fd);
        char* nl = strchr(g_chip_label, '\n');
        if (nl) *nl = '\0';
    } else {
        snprintf(g_chip_label, sizeof(g_chip_label), "k1x-gpio");
    }

    fprintf(stderr, "[GPIO] HAL initialized via sysfs: %s\n", g_chip_label);
    return 0;
}

void gpio_hal_shutdown(void) {
    g_initialized = false;
}

int gpio_set_mode(int line, int output) {
    if (line < 0 || line > BPI_GPIO_MAX_LINE) return -1;
    if (gpio_export(line) != 0) return -1;

    char path[64];
    snprintf(path, sizeof(path), GPIO_SYSFS "/gpio%d/direction", line);
    gpio_sysfs_write(path, output ? "out" : "in");
    return 0;
}

int gpio_read(int line) {
    if (line < 0 || line > BPI_GPIO_MAX_LINE) return 0;
    char path[64];
    snprintf(path, sizeof(path), GPIO_SYSFS "/gpio%d/value", line);
    return gpio_sysfs_read(path);
}

void gpio_write(int line, int value) {
    if (line < 0 || line > BPI_GPIO_MAX_LINE) return;
    char path[64];
    snprintf(path, sizeof(path), GPIO_SYSFS "/gpio%d/value", line);
    gpio_sysfs_write(path, value ? "1" : "0");
}

void gpio_toggle(int line) {
    gpio_write(line, !gpio_read(line));
}

const char* gpio_get_chip_label(void) {
    return g_chip_label;
}
