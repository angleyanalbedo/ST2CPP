#ifndef GPIO_HAL_H
#define GPIO_HAL_H

#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

/* BPI-F3 40-pin header layout (Spacemit K1X):
 *
 *  Pin  1: 3.3V       Pin  2: 5V
 *  Pin  3: GPIO.120   Pin  4: 5V
 *  Pin  5: GPIO.121   Pin  6: GND
 *  Pin  7: GPIO.122   Pin  8: GPIO.107 (UART0 TX)
 *  Pin  9: GND        Pin 10: GPIO.108 (UART0 RX)
 *  Pin 11: GPIO.109   Pin 12: GPIO.77
 *  Pin 13: GPIO.110   Pin 14: GND
 *  Pin 15: GPIO.111   Pin 16: GPIO.112
 *  Pin 17: 3.3V       Pin 18: GPIO.113
 *  Pin 19: GPIO.114   Pin 20: GND
 *  Pin 21: GPIO.115   Pin 22: GPIO.116
 *  Pin 23: GPIO.117   Pin 24: GPIO.118
 *  Pin 25: GND        Pin 26: GPIO.119
 *  Pin 27: I2C3_SDA   Pin 28: I2C3_SCL
 *  Pin 29: GPIO.63    Pin 30: GND
 *  Pin 31: GPIO.67    Pin 32: GPIO.123
 *  Pin 33: GPIO.80    Pin 34: GND
 *  Pin 35: GPIO.96    Pin 36: GPIO.124
 *  Pin 37: GPIO.97    Pin 38: GPIO.127
 *  Pin 39: GND        Pin 40: GPIO.126
 */

#define BPI_GPIO_MAX_LINE 127

int gpio_hal_init(void);
void gpio_hal_shutdown(void);

int gpio_set_mode(int line, int output);
int gpio_read(int line);
void gpio_write(int line, int value);
void gpio_toggle(int line);

const char* gpio_get_chip_label(void);

#ifdef __cplusplus
}
#endif

#endif
