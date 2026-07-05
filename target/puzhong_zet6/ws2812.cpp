#include "ws2812.h"
#include "stm32f1xx_hal.h"

// WS2812 数据引脚: PE5
#define WS_PORT  GPIOE
#define WS_PIN   GPIO_PIN_5

static uint32_t fb[RGB_NUM_LEDS];
static bool     fb_dirty = false;

// ─── 时序操作 (72MHz) ───

static inline void ws_write0() {
    WS_PORT->BSRR = WS_PIN;                 // HIGH
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop");
    WS_PORT->BRR = WS_PIN;                  // LOW
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop");
}

static inline void ws_write1() {
    WS_PORT->BSRR = WS_PIN;                 // HIGH
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop");
    WS_PORT->BRR = WS_PIN;                  // LOW
    __asm__("nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop\n nop");
    __asm__("nop\n nop");
}

static void ws_reset() {
    WS_PORT->BRR = WS_PIN;
    for (volatile int i = 0; i < 500; i++) { __asm__("nop"); }
}

static void ws_send_byte(uint8_t v) {
    for (int i = 0; i < 8; i++) {
        if (v & 0x80) ws_write1(); else ws_write0();
        v <<= 1;
    }
}

static void ws_send_pixel(uint32_t color) {
    ws_send_byte((color >> 8) & 0xFF);  // G
    ws_send_byte((color >> 16) & 0xFF); // R
    ws_send_byte(color & 0xFF);         // B
}

// ─── 公开 API ───

void rgb_init() {
    __HAL_RCC_GPIOE_CLK_ENABLE();
    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = WS_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(WS_PORT, &gpio);
    WS_PORT->BRR = WS_PIN;

    for (int i = 0; i < RGB_NUM_LEDS; i++) fb[i] = RGB_BLACK;
    fb_dirty = true;
}

void rgb_clear() {
    for (int i = 0; i < RGB_NUM_LEDS; i++) fb[i] = RGB_BLACK;
    fb_dirty = true;
}

void rgb_show() {
    if (!fb_dirty) return;
    for (int i = 0; i < RGB_NUM_LEDS; i++) {
        ws_send_pixel(fb[i]);
    }
    ws_reset();
    fb_dirty = false;
}

void rgb_set_pixel(int x, int y, uint32_t color) {
    if (x < 0 || x >= RGB_PANEL_W || y < 0 || y >= RGB_PANEL_H) return;
    int idx = y * RGB_PANEL_W + x;
    if (fb[idx] != color) {
        fb[idx] = color;
        fb_dirty = true;
    }
}

void rgb_set_all(uint32_t color) {
    for (int i = 0; i < RGB_NUM_LEDS; i++) fb[i] = color;
    fb_dirty = true;
}

void rgb_set_buf(const uint32_t colors[RGB_NUM_LEDS]) {
    for (int i = 0; i < RGB_NUM_LEDS; i++) {
        if (fb[i] != colors[i]) {
            fb[i] = colors[i];
            fb_dirty = true;
        }
    }
}

// ─── 绘图 ───

void rgb_draw_rect(int x1, int y1, int x2, int y2, uint32_t color) {
    rgb_draw_line(x1, y1, x2, y1, color);
    rgb_draw_line(x1, y1, x1, y2, color);
    rgb_draw_line(x1, y2, x2, y2, color);
    rgb_draw_line(x2, y1, x2, y2, color);
}

void rgb_draw_line(int x1, int y1, int x2, int y2, uint32_t color) {
    int dx = (x2 > x1) ? (x2 - x1) : (x1 - x2);
    int dy = (y2 > y1) ? (y2 - y1) : (y1 - y2);
    int sx = (x1 < x2) ? 1 : -1;
    int sy = (y1 < y2) ? 1 : -1;
    int err = dx - dy;

    while (1) {
        rgb_set_pixel(x1, y1, color);
        if (x1 == x2 && y1 == y2) break;
        int e2 = 2 * err;
        if (e2 > -dy) { err -= dy; x1 += sx; }
        if (e2 < dx)  { err += dx; y1 += sy; }
    }
}

void rgb_fill_rect(int x1, int y1, int x2, int y2, uint32_t color) {
    for (int y = y1; y <= y2; y++)
        for (int x = x1; x <= x2; x++)
            rgb_set_pixel(x, y, color);
}

// 5x5 数字字模 (0-9, A-F)
static const uint8_t digit_font[16][5] = {
    {0x70,0x88,0x88,0x88,0x70},  // 0
    {0x00,0x48,0xF8,0x08,0x00},  // 1
    {0x48,0x98,0xA8,0x48,0x00},  // 2
    {0x00,0x88,0xA8,0x50,0x00},  // 3
    {0x20,0x50,0x90,0x38,0x10},  // 4
    {0x00,0xE8,0xA8,0xB8,0x00},  // 5
    {0x00,0x70,0xA8,0xA8,0x30},  // 6
    {0x80,0x98,0xA0,0xC0,0x00},  // 7
    {0x50,0xA8,0xA8,0xA8,0x50},  // 8
    {0x40,0xA8,0xA8,0xA8,0x70},  // 9
    {0x38,0x50,0x90,0x50,0x38},  // A
    {0xF8,0xA8,0xA8,0x50,0x00},  // B
    {0x70,0x88,0x88,0x88,0x00},  // C
    {0xF8,0x88,0x88,0x50,0x20},  // D
    {0xF8,0xA8,0xA8,0xA8,0x00},  // E
    {0x00,0xF8,0xA0,0xA0,0x00},  // F
};

void rgb_show_digit(int x, int y, uint8_t num, uint32_t color) {
    if (num > 15) return;
    for (int j = 0; j < 5; j++) {
        uint8_t col = digit_font[num][j];
        for (int i = 0; i < 5; i++) {
            uint32_t c = (col & 0x80) ? color : RGB_BLACK;
            rgb_set_pixel(x + j, y + i, c);
            col <<= 1;
        }
    }
}
