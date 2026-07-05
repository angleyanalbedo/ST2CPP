#include "rt_runtime.h"
#include "rt_plc.h"

#if defined(RT_PLATFORM_BARE_METAL)

#include "stm32f1xx_hal.h"
#include <stdint.h>
#include <stdio.h>
#include <stdarg.h>

// ═══════════════════════════════════════════════════════
// 用户需根据实际硬件修改以下宏
// ═══════════════════════════════════════════════════════

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
#define PLC_US_TIM             TIM3         // 自由运行的 1MHz 计数器
#endif

#ifndef PLC_LED_PORT
#define PLC_LED_PORT           GPIOC
#endif

#ifndef PLC_LED_PIN
#define PLC_LED_PIN            GPIO_PIN_13
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


// ═══════════════════════════════════════════════════════
// TIM3 自由运行计数器（替代 DWT CYCCNT）
// STM32F1 无 DWT，用 TIM3 做微秒级计时
// TIM3 在 72MHz / 72 = 1MHz 下每 65.5ms 溢出，需软件扩展
// ═══════════════════════════════════════════════════════

static volatile uint32_t us_timer_overflow = 0;

extern "C" void TIM3_IRQHandler(void) {
    if (PLC_US_TIM->SR & TIM_SR_UIF) {
        PLC_US_TIM->SR &= ~TIM_SR_UIF;
        us_timer_overflow++;
    }
}

static inline uint32_t us_timer_read() {
    return PLC_US_TIM->CNT;
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
    PLC_US_TIM->PSC  = (PLC_SYSTEM_CORE_CLOCK / 1000000) - 1;
    PLC_US_TIM->ARR  = 0xFFFF;
    PLC_US_TIM->DIER |= TIM_DIER_UIE;
    PLC_US_TIM->CR1  |= TIM_CR1_ARPE;
    PLC_US_TIM->EGR  |= TIM_EGR_UG;
    PLC_US_TIM->SR   &= ~TIM_SR_UIF;
    HAL_NVIC_SetPriority(TIM3_IRQn, 1, 0);
    HAL_NVIC_EnableIRQ(TIM3_IRQn);
    PLC_US_TIM->CR1 |= TIM_CR1_CEN;
}

// ═══════════════════════════════════════════════════════
// platform 命名空间实现
// ═══════════════════════════════════════════════════════

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


// ═══════════════════════════════════════════════════════
// UART 重定向（printf → UART）
// ═══════════════════════════════════════════════════════

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


// ═══════════════════════════════════════════════════════
// TIM2 中断：PLC 周期 tick 驱动
// ═══════════════════════════════════════════════════════

extern "C" void plc_runtime_tick();

static volatile uint32_t plc_tick_count = 0;

extern "C" void TIM2_IRQHandler(void) {
    if (PLC_TICK_TIM->SR & TIM_SR_UIF) {
        PLC_TICK_TIM->SR &= ~TIM_SR_UIF;
        plc_tick_count++;

        plc_runtime_tick();

        if ((plc_tick_count % 100) == 0) {
            PLC_LED_PORT->ODR ^= PLC_LED_PIN;
        }
    }
}


// ═══════════════════════════════════════════════════════
// 系统时钟配置：8MHz HSE → PLL → 72MHz
// ═══════════════════════════════════════════════════════

static void system_clock_init() {
    RCC->CR |= RCC_CR_HSEON;
    while (!(RCC->CR & RCC_CR_HSERDY)) {}

    RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLMULL) | RCC_CFGR_PLLMULL9;
    RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLSRC) | RCC_CFGR_PLLSRC;
    RCC->CR |= RCC_CR_PLLON;
    while (!(RCC->CR & RCC_CR_PLLRDY)) {}

    FLASH->ACR = (FLASH->ACR & ~FLASH_ACR_LATENCY) | FLASH_ACR_LATENCY_2;
    RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_SW) | RCC_CFGR_SW_PLL;
    while ((RCC->CFGR & RCC_CFGR_SWS) != RCC_CFGR_SWS_PLL) {}

    SystemCoreClock = 72000000;
    HAL_SYSTICK_Config(SystemCoreClock / 1000);
    HAL_SYSTICK_CLKSourceConfig(SYSTICK_CLKSOURCE_HCLK);
}

// ═══════════════════════════════════════════════════════
// USART1 初始化（115200 8N1）
// ═══════════════════════════════════════════════════════

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

    uint32_t usartDiv = PLC_SYSTEM_CORE_CLOCK / PLC_UART_BAUD;
    PLC_UART->BRR = usartDiv;
    PLC_UART->CR1 = USART_CR1_UE | USART_CR1_TE | USART_CR1_RE;
}

// ═══════════════════════════════════════════════════════
// 初始化函数
// ═══════════════════════════════════════════════════════

extern "C" void plc_platform_init() {
    HAL_Init();
    system_clock_init();
    HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq() / 1000);
    uart_init();
    us_timer_init();

    __HAL_RCC_GPIOC_CLK_ENABLE();
    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = PLC_LED_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Pull  = GPIO_NOPULL;
    gpio.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(PLC_LED_PORT, &gpio);
    HAL_GPIO_WritePin(PLC_LED_PORT, PLC_LED_PIN, GPIO_PIN_SET);

    __HAL_RCC_TIM2_CLK_ENABLE();
    PLC_TICK_TIM->PSC  = (PLC_SYSTEM_CORE_CLOCK / 1000000) - 1;
    PLC_TICK_TIM->ARR  = 1000 - 1;
    PLC_TICK_TIM->DIER |= TIM_DIER_UIE;
    PLC_TICK_TIM->CR1  |= TIM_CR1_ARPE;
    PLC_TICK_TIM->EGR  |= TIM_EGR_UG;
    PLC_TICK_TIM->SR   &= ~TIM_SR_UIF;

    HAL_NVIC_SetPriority(PLC_TICK_IRQ, 0, 0);
    HAL_NVIC_EnableIRQ(PLC_TICK_IRQ);

    PLC_TICK_TIM->CR1 |= TIM_CR1_CEN;

    rt_plc::platform::logInfo("PLC F1 Platform: %luMHz TIM2=1ms TIM3=us\n",
                               PLC_SYSTEM_CORE_CLOCK / 1000000);
}

extern "C" uint32_t plc_platform_get_tick_count() {
    return plc_tick_count;
}

extern "C" int64_t plc_platform_get_uptime_us() {
    return rt_plc::platform::steadyUs();
}

#endif // RT_PLATFORM_BARE_METAL
