#pragma once
#include <stdint.h>

// WS2812 5x5 RGB LED Matrix
// Data pin: PE5 (shared with LED2 — LED2 unavailable when WS2812 active)

#define RGB_PANEL_W  5
#define RGB_PANEL_H  5
#define RGB_NUM_LEDS (RGB_PANEL_W * RGB_PANEL_H)

static inline uint32_t RGB(uint8_t r, uint8_t g, uint8_t b) {
    return ((uint32_t)r << 16) | ((uint32_t)g << 8) | b;
}

#define RGB_RED    0xFF0000
#define RGB_GREEN  0x00FF00
#define RGB_BLUE   0x0000FF
#define RGB_WHITE  0xFFFFFF
#define RGB_YELLOW 0xFFFF00
#define RGB_CYAN   0x00FFFF
#define RGB_MAGENTA 0xFF00FF
#define RGB_ORANGE 0xFFA500
#define RGB_BLACK  0x000000

void rgb_init(void);
void rgb_clear(void);
void rgb_show(void);

void rgb_set_pixel(int x, int y, uint32_t color);
void rgb_set_all(uint32_t color);
void rgb_set_buf(const uint32_t colors[RGB_NUM_LEDS]);

// Drawing primitives (operate on framebuffer, call rgb_show() to update)
void rgb_draw_rect(int x1, int y1, int x2, int y2, uint32_t color);
void rgb_draw_line(int x1, int y1, int x2, int y2, uint32_t color);
void rgb_fill_rect(int x1, int y1, int x2, int y2, uint32_t color);
void rgb_show_digit(int x, int y, uint8_t num, uint32_t color);
