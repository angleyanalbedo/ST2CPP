#include "ws2812.h"
#include "stm32f1xx_hal.h"

#define WS_PORT  GPIOE
#define WS_PIN   GPIO_PIN_5
#define WS_HIGH  (WS_PORT->BSRR = WS_PIN)
#define WS_LOW   (WS_PORT->BRR  = WS_PIN)

// WS2812 时序 @72MHz:
//   官方 Keil+SPL 代码: GPIO_SetBits(~10cyc) + NOPs + GPIO_ResetBits(~10cyc) + NOPs
//   GCC 直写 BSRR(~2cyc) + NOPs + BRR(~2cyc) + NOPs
//   需要比官方多 ~16 NOP 来补偿函数调用开销差
//
//   T0H = 220-380ns → 目标 ~22 周期 (BSRR 2 + 20 NOP)
//   T0L = 580-1000ns → 目标 ~50 周期 (BRR 2 + 48 NOP)
//   T1H = 580-1000ns → 目标 ~50 周期 (BSRR 2 + 48 NOP)
//   T1L = 220-380ns → 目标 ~22 周期 (BRR 2 + 20 NOP)

static void ws_bit0() {
    WS_HIGH;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    WS_LOW;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
}

static void ws_bit1() {
    WS_HIGH;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    WS_LOW;
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
    __NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();__NOP();
}

static void ws_reset() {
    WS_LOW;
    for (volatile int i = 0; i < 6000; i++) {}
}

static void ws_send_byte(uint8_t v) {
    for (int i = 0; i < 8; i++) {
        if (v & 0x80) ws_bit1(); else ws_bit0();
        v <<= 1;
    }
}

static void ws_send_pixel(uint8_t r, uint8_t g, uint8_t b) {
    ws_send_byte(g);
    ws_send_byte(r);
    ws_send_byte(b);
}

void rgb_init() {
    __HAL_RCC_GPIOE_CLK_ENABLE();
    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = WS_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(WS_PORT, &gpio);
    WS_LOW;
}

void rgb_clear() {
    __disable_irq();
    for (int i = 0; i < 25; i++)
        ws_send_pixel(0, 0, 0);
    ws_reset();
    __enable_irq();
}

void rgb_set_all(uint8_t r, uint8_t g, uint8_t b) {
    __disable_irq();
    for (int i = 0; i < 25; i++)
        ws_send_pixel(r, g, b);
    ws_reset();
    __enable_irq();
}
