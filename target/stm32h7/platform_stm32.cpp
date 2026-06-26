/**
 * platform_stm32.cpp — STM32 裸机 platform 实现
 *
 * 提供高精度微秒级计时 + UART 重定向，用于驱动 PLC 运行时。
 *
 * 硬件要求：
 *   - STM32F4/H7/G4 系列（需 DWT cycle counter）
 *   - 一个通用定时器（TIM2/TIM3/TIM5 用于周期 tick）
 *   - 一个 UART（用于 printf 输出）
 *   - 一个 GPIO LED（用于心跳指示）
 *
 * 精度指标：
 *   - nowUs() / steadyUs()：基于 DWT CYCCNT，精度 = 1/SystemCoreClock 秒
 *     例：168MHz → 5.95ns 精度，远超 1us 要求
 *   - tick 中断：TIM2 自动重装，抖动 < 1 tick（取决于中断优先级）
 *
 * 编译方式：
 *   与 runtime-flat 的头文件和源文件一起编译，
 *   定义 -DRT_PLATFORM_BARE_METAL -DSTM32F4xx（或对应芯片宏）
 *
 * 用法：
 *   1. SystemClock_Config() 之后调用 plc_platform_init()
 *   2. plc_platform_init() 会配置 DWT + TIM2 + UART
 *   3. 主循环中不需要任何代码（所有逻辑在中断中执行）
 *   4. plc_runtime_init() 在 plc_platform_init() 之后调用
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#if defined(RT_PLATFORM_BARE_METAL)

// ═══════════════════════════════════════════════════════
// 用户需根据实际硬件修改以下宏
// ═══════════════════════════════════════════════════════

#ifndef PLC_SYSTEM_CORE_CLOCK
#define PLC_SYSTEM_CORE_CLOCK  168000000UL  // 168 MHz (STM32F407)
#endif

#ifndef PLC_TICK_TIM
#define PLC_TICK_TIM           TIM2         // 用于 PLC 周期 tick 的定时器
#endif

#ifndef PLC_TICK_IRQ
#define PLC_TICK_IRQ           TIM2_IRQn
#endif

#ifndef PLC_LED_PORT
#define PLC_LED_PORT           GPIOD
#endif

#ifndef PLC_LED_PIN
#define PLC_LED_PIN            GPIO_PIN_12  // 板载 LED（绿灯）
#endif

#ifndef PLC_UART
#define PLC_UART               USART2       // printf 重定向目标
#endif


// ═══════════════════════════════════════════════════════
// DWT Cycle Counter（微秒级计时核心）
// ═══════════════════════════════════════════════════════

static inline void dwt_init() {
    CoreDebug->DEMCR |= CoreDebug_DEMCR_TRCENA_Msk;
    DWT->CYCCNT = 0;
    DWT->CTRL |= DWT_CTRL_CYCCNTENA_Msk;
}

static inline uint32_t dwt_cyccnt() {
    return DWT->CYCCNT;
}

// ═══════════════════════════════════════════════════════
// platform 命名空间实现
// ═══════════════════════════════════════════════════════

namespace rt_plc { namespace platform {

// 单调时钟微秒（DWT CYCCNT 转换）
// 溢出周期：CYCCNT 是 32 位，168MHz 下约 25.4 秒溢出一次
// 对于 PLC 扫描周期（ms 级）完全够用
int64_t steadyUs() {
    return (int64_t)dwt_cyccnt() / (PLC_SYSTEM_CORE_CLOCK / 1000000UL);
}

// 系统时钟微秒（基于 HAL tick，精度 1ms）
// 用于 NOW() 等不需要纳秒精度的场景
int64_t nowUs() {
    return (int64_t)HAL_GetTick() * 1000LL;
}

// 延时微秒（忙等，用于极短延时）
// 对于 PLC 调度场景通常不需要，调度器由中断驱动
void sleepUs(int64_t us) {
    if (us <= 0) return;
    uint32_t start = dwt_cyccnt();
    uint32_t cycles = (uint32_t)(us * (PLC_SYSTEM_CORE_CLOCK / 1000000UL));
    while ((dwt_cyccnt() - start) < cycles) {
        // busy wait
    }
}

// 错误日志（重定向到 UART）
void logErr(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    vfprintf(stderr, fmt, args);
    va_end(args);
}

// 信息日志（重定向到 UART）
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
static volatile int64_t  plc_tick_start_us = 0;

extern "C" void TIM2_IRQHandler(void) {
    if (PLC_TICK_TIM->SR & TIM_SR_UIF) {
        PLC_TICK_TIM->SR &= ~TIM_SR_UIF;  // 清中断标志

        plc_tick_count++;

        // 执行 PLC tick（核心调度逻辑）
        plc_runtime_tick();

        // LED 心跳：每 100 tick 翻转一次（1ms tick → 100ms 闪烁）
        if ((plc_tick_count % 100) == 0) {
            PLC_LED_PORT->ODR ^= PLC_LED_PIN;
        }
    }
}


// ═══════════════════════════════════════════════════════
// 初始化函数
// ═══════════════════════════════════════════════════════

/**
 * 初始化 PLC 硬件抽象层
 * 必须在 SystemClock_Config() 之后、plc_runtime_init() 之前调用
 */
extern "C" void plc_platform_init() {
    // 1. 初始化 DWT cycle counter（微秒精度计时）
    dwt_init();

    // 2. 配置 LED GPIO（PD12 绿灯）
    __HAL_RCC_GPIOD_CLK_ENABLE();
    GPIO_InitTypeDef gpio = {};
    gpio.Pin   = PLC_LED_PIN;
    gpio.Mode  = GPIO_MODE_OUTPUT_PP;
    gpio.Pull  = GPIO_NOPULL;
    gpio.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(PLC_LED_PORT, &gpio);
    HAL_GPIO_WritePin(PLC_LED_PORT, PLC_LED_PIN, GPIO_PIN_SET);  // LED 亮

    // 3. 配置 TIM2 为 1ms 周期中断
    __HAL_RCC_TIM2_CLK_ENABLE();
    PLC_TICK_TIM->PSC  = (PLC_SYSTEM_CORE_CLOCK / 1000000) - 1;  // 1MHz (1us)
    PLC_TICK_TIM->ARR  = 1000 - 1;  // 1000us = 1ms
    PLC_TICK_TIM->DIER |= TIM_DIER_UIE;  // 使能更新中断
    PLC_TICK_TIM->CR1  |= TIM_CR1_ARPE;  // 自动重装预装载
    PLC_TICK_TIM->EGR  |= TIM_EGR_UG;    // 产生更新事件加载影子寄存器
    PLC_TICK_TIM->SR   &= ~TIM_SR_UIF;   // 清中断标志

    HAL_NVIC_SetPriority(PLC_TICK_IRQ, 0, 0);  // 最高优先级（0,0）
    HAL_NVIC_EnableIRQ(PLC_TICK_IRQ);

    PLC_TICK_TIM->CR1 |= TIM_CR1_CEN;  // 启动定时器

    plc_tick_start_us = platform::steadyUs();

    // 4. 输出启动信息
    platform::logInfo("PLC Platform: DWT=%luMHz TIM2=1ms IRQ=PRI0\n",
                      PLC_SYSTEM_CORE_CLOCK / 1000000);
}


/**
 * 获取 tick 统计信息（用于诊断）
 */
extern "C" uint32_t plc_platform_get_tick_count() {
    return plc_tick_count;
}

extern "C" int64_t plc_platform_get_uptime_us() {
    return platform::steadyUs() - plc_tick_start_us;
}

#endif // RT_PLATFORM_BARE_METAL
