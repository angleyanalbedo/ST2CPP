#include "rt_runtime.h"
#include "rt_plc.h"
#include "ws2812.h"

#if defined(RT_PLATFORM_BARE_METAL)

#include "stm32f1xx_hal.h"
#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>

#ifndef PLC_SYSTEM_CORE_CLOCK
#define PLC_SYSTEM_CORE_CLOCK  72000000UL
#endif

#ifndef PLC_TICK_TIM
#define PLC_TICK_TIM           TIM2
#endif

#ifndef PLC_TICK_IRQ
#define PLC_TICK_IRQ           TIM2_IRQn
#endif

#ifndef PLC_US_TIM
#define PLC_US_TIM             TIM3
#endif

#ifndef PLC_LED_PORT
#define PLC_LED_PORT           GPIOB
#endif

#ifndef PLC_LED_PIN
#define PLC_LED_PIN            GPIO_PIN_5
#endif

#ifndef PLC_BEEP_PORT
#define PLC_BEEP_PORT          GPIOB
#endif

#ifndef PLC_BEEP_PIN
#define PLC_BEEP_PIN           GPIO_PIN_8
#endif

#ifndef PLC_UART
#define PLC_UART               USART1
#endif

#ifndef PLC_UART_BAUD
#define PLC_UART_BAUD           115200
#endif

#ifndef PLC_UART_TX_PIN
#define PLC_UART_TX_PIN         GPIO_PIN_9
#endif

#ifndef PLC_UART_RX_PIN
#define PLC_UART_RX_PIN         GPIO_PIN_10
#endif

#ifndef PLC_UART_GPIO_PORT
#define PLC_UART_GPIO_PORT      GPIOA
#endif

// ═══ TIM3 微秒计数器 ═══

static volatile uint32_t us_timer_overflow = 0;

extern "C" void TIM3_IRQHandler(void) {
    if (PLC_US_TIM->SR & TIM_SR_UIF) {
        PLC_US_TIM->SR &= ~TIM_SR_UIF;
        us_timer_overflow++;
    }
}

static int64_t us_timer_read64() {
    uint32_t ovf, cnt;
    do {
        ovf = us_timer_overflow;
        cnt = PLC_US_TIM->CNT;
    } while (ovf != us_timer_overflow);
    return ((int64_t)ovf << 16) | cnt;
}

static void us_timer_init() {
    __HAL_RCC_TIM3_CLK_ENABLE();
    PLC_US_TIM->PSC  = (SystemCoreClock / 1000000) - 1;
    PLC_US_TIM->ARR  = 0xFFFF;
    PLC_US_TIM->DIER |= TIM_DIER_UIE;
    PLC_US_TIM->CR1  |= TIM_CR1_ARPE;
    PLC_US_TIM->EGR  |= TIM_EGR_UG;
    PLC_US_TIM->SR   &= ~TIM_SR_UIF;
    HAL_NVIC_SetPriority(TIM3_IRQn, 1, 0);
    HAL_NVIC_EnableIRQ(TIM3_IRQn);
    PLC_US_TIM->CR1 |= TIM_CR1_CEN;
}

// ═══ platform 命名空间 ═══

namespace rt_plc { namespace platform {

int64_t steadyUs() {
    return us_timer_read64();
}

int64_t nowUs() {
    return (int64_t)HAL_GetTick() * 1000LL;
}

void sleepUs(int64_t us) {
    if (us <= 0) return;
    int64_t start = us_timer_read64();
    while ((us_timer_read64() - start) < us) {}
}

void logErr(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
}

void logInfo(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stdout, fmt, args);
    va_end(args);
}

}} // namespace rt_plc::platform

// ═══ UART 重定向 ═══

#ifdef __GNUC__
int _write(int fd, char* ptr, int len) {
    (void)fd;
    for (int i = 0; i < len; i++) {
        while (!(PLC_UART->SR & USART_SR_TXE)) {}
        PLC_UART->DR = (uint8_t)ptr[i];
    }
    return len;
}
#endif

// ═══ TIM2 中断：PLC tick ═══

extern "C" void plc_runtime_tick();

static volatile uint32_t plc_tick_count = 0;
static volatile uint32_t plc_heartbeat = 0;

extern "C" void TIM2_IRQHandler(void) {
    if (PLC_TICK_TIM->SR & TIM_SR_UIF) {
        PLC_TICK_TIM->SR &= ~TIM_SR_UIF;
        plc_tick_count++;

        plc_runtime_tick();

        // LED1 (PB5) 心跳: 每 500ms 翻转一次
        if ((plc_tick_count % 500) == 0) {
            PLC_LED_PORT->ODR ^= PLC_LED_PIN;
        }
    }
}

// ═══ 系统时钟 ═══

static void system_clock_init() {
    int timeout = 1000000;
    RCC->CR |= RCC_CR_HSEON;
    while (!(RCC->CR & RCC_CR_HSERDY)) {
        if (--timeout == 0) { RCC->CR &= ~RCC_CR_HSEON; break; }
    }

    if (RCC->CR & RCC_CR_HSERDY) {
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLMULL) | RCC_CFGR_PLLMULL9;
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLSRC) | RCC_CFGR_PLLSRC;
        RCC->CR |= RCC_CR_PLLON;
        while (!(RCC->CR & RCC_CR_PLLRDY)) {}

        FLASH->ACR = (FLASH->ACR & ~FLASH_ACR_LATENCY) | FLASH_ACR_LATENCY_2;
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_SW) | RCC_CFGR_SW_PLL;
        while ((RCC->CFGR & RCC_CFGR_SWS) != RCC_CFGR_SWS_PLL) {}
        SystemCoreClock = 72000000;
    } else {
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_SW) | RCC_CFGR_SW_HSI;
        while ((RCC->CFGR & RCC_CFGR_SWS) != RCC_CFGR_SWS_HSI) {}
        SystemCoreClock = 8000000;
    }

    HAL_SYSTICK_Config(SystemCoreClock / 1000);
    HAL_SYSTICK_CLKSourceConfig(SYSTICK_CLKSOURCE_HCLK);
}

// ═══ USART1 初始化 ═══

static void uart_init() {
    __HAL_RCC_USART1_CLK_ENABLE();
    __HAL_RCC_GPIOA_CLK_ENABLE();

    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = PLC_UART_TX_PIN;
    gpio.Mode  = GPIO_MODE_AF_PP;
    gpio.Speed = GPIO_SPEED_FREQ_HIGH;
    HAL_GPIO_Init(PLC_UART_GPIO_PORT, &gpio);

    gpio.Pin   = PLC_UART_RX_PIN;
    gpio.Mode  = GPIO_MODE_INPUT;
    gpio.Pull  = GPIO_PULLUP;
    HAL_GPIO_Init(PLC_UART_GPIO_PORT, &gpio);

    uint32_t usartDiv = SystemCoreClock / PLC_UART_BAUD;
    PLC_UART->BRR = usartDiv;
    PLC_UART->CR1 = USART_CR1_UE | USART_CR1_TE | USART_CR1_RE;
}

// ═══ 蜂鸣器 ═══

static void beep_on()  { PLC_BEEP_PORT->BSRR = PLC_BEEP_PIN; }
static void beep_off() { PLC_BEEP_PORT->BRR  = PLC_BEEP_PIN; }

static void beep_beep(int ms) {
    beep_on();
    for (volatile int i = 0; i < ms * 1000; i++) { __asm__("nop"); }
    beep_off();
}

// ═══ GPIO 初始化 ═══

static void gpio_init() {
    __HAL_RCC_GPIOB_CLK_ENABLE();
    __HAL_RCC_GPIOE_CLK_ENABLE();

    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = PLC_LED_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Pull  = GPIO_NOPULL;
    gpio.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(PLC_LED_PORT, &gpio);
    HAL_GPIO_WritePin(PLC_LED_PORT, PLC_LED_PIN, GPIO_PIN_SET);

    gpio.Pin   = PLC_BEEP_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    HAL_GPIO_Init(PLC_BEEP_PORT, &gpio);
    HAL_GPIO_WritePin(PLC_BEEP_PORT, PLC_BEEP_PIN, GPIO_PIN_RESET);
}

// ═══ WS2812 演示 ═══

static uint32_t demo_step = 0;

// 边框跑马灯: 5x5 矩阵边框共 16 个位置
static const int8_t border[16][2] = {
    {0,0},{1,0},{2,0},{3,0},{4,0},
    {4,1},{4,2},{4,3},{4,4},
    {3,4},{2,4},{1,4},{0,4},
    {0,3},{0,2},{0,1},
};

static void demo_startup() {
    // 逐个点亮 (流水灯)
    for (int i = 0; i < RGB_NUM_LEDS; i++) {
        uint32_t hue = (i * 60) % 360;
        // 简化的 HSV 转 RGB (取色环 6 段)
        uint32_t color;
        if (hue < 60)       color = RGB(0xFF, hue * 4, 0);
        else if (hue < 120) color = RGB((120 - hue) * 4, 0xFF, 0);
        else if (hue < 180) color = RGB(0, 0xFF, (hue - 120) * 4);
        else if (hue < 240) color = RGB(0, (240 - hue) * 4, 0xFF);
        else if (hue < 300) color = RGB((hue - 240) * 4, 0, 0xFF);
        else                color = RGB(0xFF, 0, (360 - hue) * 4);
        rgb_set_pixel(i % 5, i / 5, color);
        rgb_show();
        for (volatile int w = 0; w < 30000; w++) { __asm__("nop"); }
    }
    for (volatile int w = 0; w < 60000; w++) { __asm__("nop"); }
}

extern "C" void plc_platform_demo_tick() {
    int ct = plc_tick_count;

    // 每 200ms 更新一次 WS2812
    if ((ct % 200) != 0) return;

    // 边框跑马灯
    int pos = (ct / 200) % 16;
    rgb_clear();
    for (int i = 0; i < 4; i++) {
        int p = (pos + i) % 16;
        uint8_t bright = (4 - i) * 60;
        rgb_set_pixel(border[p][0], border[p][1], RGB(bright, bright / 2, 0));
    }
    // 中心显示运行状态: 每 50 步显示不同颜色
    uint32_t center = (ct / 1000) % 6;
    uint32_t ccolor = RGB_WHITE;
    switch (center) {
        case 0: ccolor = RGB_RED;    break;
        case 1: ccolor = RGB_GREEN;  break;
        case 2: ccolor = RGB_BLUE;   break;
        case 3: ccolor = RGB_YELLOW; break;
        case 4: ccolor = RGB_CYAN;   break;
        case 5: ccolor = RGB_MAGENTA; break;
    }
    rgb_set_pixel(2, 2, ccolor);
    rgb_show();

    demo_step = ct;
}

// ═══ 初始化入口 ═══

extern "C" void plc_platform_init() {
    HAL_Init();
    system_clock_init();
    HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq() / 1000);

    uart_init();
    gpio_init();
    us_timer_init();
    rgb_init();

    __HAL_RCC_TIM2_CLK_ENABLE();
    PLC_TICK_TIM->PSC  = (SystemCoreClock / 1000000) - 1;
    PLC_TICK_TIM->ARR  = 1000 - 1;
    PLC_TICK_TIM->DIER |= TIM_DIER_UIE;
    PLC_TICK_TIM->CR1  |= TIM_CR1_ARPE;
    PLC_TICK_TIM->EGR  |= TIM_EGR_UG;
    PLC_TICK_TIM->SR   &= ~TIM_SR_UIF;

    HAL_NVIC_SetPriority(PLC_TICK_IRQ, 0, 0);
    HAL_NVIC_EnableIRQ(PLC_TICK_IRQ);

    PLC_TICK_TIM->CR1 |= TIM_CR1_CEN;

    // 启动演示
    beep_beep(80);
    demo_startup();

    rt_plc::platform::logInfo("PLC Puzhong ZET6: %luMHz\n",
                               SystemCoreClock / 1000000);
}

extern "C" uint32_t plc_platform_get_tick_count() {
    return plc_tick_count;
}

extern "C" int64_t plc_platform_get_uptime_us() {
    return rt_plc::platform::steadyUs();
}

#endif // RT_PLATFORM_BARE_METAL
