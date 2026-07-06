#include "ws2812.h"
#include "stm32f1xx_hal.h"
#include <cmsis_gcc.h>

// 完全参照板商官方代码 APP/ws2812/ws2812.c
// 引脚: PE5 (与 LED2 共用)

#define RGB_PORT  GPIOE
#define RGB_PIN   GPIO_PIN_5
#define RGB_LED_HIGH  (RGB_PORT->BSRR = RGB_PIN)
#define RGB_LED_LOW   (RGB_PORT->BRR  = RGB_PIN)

static void RGB_LED_Write0(void) {
    RGB_LED_HIGH;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();
    RGB_LED_LOW;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();
}

static void RGB_LED_Write1(void) {
    RGB_LED_HIGH;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();
    RGB_LED_LOW;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();
}

static void RGB_LED_Reset(void) {
    RGB_LED_LOW;
    for (volatile int i = 0; i < 8000; i++) {}
}

static void RGB_LED_Write_Byte(uint8_t byte) {
    for (int i = 0; i < 8; i++) {
        if (byte & 0x80) RGB_LED_Write1();
        else             RGB_LED_Write0();
        byte <<= 1;
    }
}

static void RGB_LED_Write_24Bits(uint8_t green, uint8_t red, uint8_t blue) {
    RGB_LED_Write_Byte(green);
    RGB_LED_Write_Byte(red);
    RGB_LED_Write_Byte(blue);
}

void rgb_init(void) {
    __HAL_RCC_GPIOE_CLK_ENABLE();
    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = RGB_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(RGB_PORT, &gpio);
    RGB_LED_HIGH;

    rgb_clear();
}

void rgb_clear(void) {
    __disable_irq();
    for (int i = 0; i < 25; i++)
        RGB_LED_Write_24Bits(0, 0, 0);
    RGB_LED_Reset();
    __enable_irq();
}

void rgb_set_all(uint8_t r, uint8_t g, uint8_t b) {
    __disable_irq();
    for (int i = 0; i < 25; i++)
        RGB_LED_Write_24Bits(g, r, b);
    RGB_LED_Reset();
    __enable_irq();
}

void rgb_show_all_red(void) {
    rgb_set_all(0xFF, 0, 0);
}

void rgb_show_all_green(void) {
    rgb_set_all(0, 0xFF, 0);
}

void rgb_show_all_blue(void) {
    rgb_set_all(0, 0, 0xFF);
}
