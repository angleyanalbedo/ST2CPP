#pragma once
#include <stdint.h>

// 完全参照板商官方代码 APP/ws2812/ws2812.h
// 引脚: PE5 (与 LED2 共用)

void rgb_init(void);
void rgb_clear(void);
void rgb_set_all(uint8_t r, uint8_t g, uint8_t b);
void rgb_show_all_red(void);
void rgb_show_all_green(void);
void rgb_show_all_blue(void);
