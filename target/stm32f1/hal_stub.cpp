// hal_stub.cpp — 当 STM32CubeF1 HAL 不存在时提供最小占位
// 仅用于语法验证编译，不可用于实际硬件运行
#include <stdint.h>
#include <string.h>
#include <stdio.h>

// CMSIS 核心类型
typedef uint32_t IRQn_Type;

// HAL 状态类型
typedef enum { HAL_OK = 0, HAL_ERROR = 1, HAL_BUSY = 2, HAL_TIMEOUT = 3 } HAL_StatusTypeDef;

// 外设基地址（占位）
#define PERIPH_BASE          0x40000000UL
#define APB1PERIPH_BASE      PERIPH_BASE
#define APB2PERIPH_BASE      0x40010000UL
#define TIM2_BASE            (APB1PERIPH_BASE + 0x00000000UL)
#define TIM3_BASE            (APB1PERIPH_BASE + 0x00000400UL)
#define USART1_BASE          (APB2PERIPH_BASE + 0x00003800UL)
#define GPIOC_BASE           (APB2PERIPH_BASE + 0x00001000UL)

// 寄存器结构体（精确模拟 STM32F1 寄存器布局）
typedef struct {
    volatile uint32_t CR1, CR2, SMCR, DIER, SR, EGR, CCMR1, CCMR2,
        CCER, CNT, PSC, ARR, RCR, CCR1, CCR2, CCR3, CCR4, BDTR, DCR, DMAR;
} TIM_TypeDef;

typedef struct {
    volatile uint32_t SR, DR, BRR, CR1, CR2, CR3, CR4, CR5, CR6, CR7, CR8, CR9, CR10;
} USART_TypeDef;

typedef struct {
    volatile uint32_t CRL, CRH, IDR, ODR, BSRR, BRR, LCKR;
} GPIO_TypeDef;

#define TIM2                ((TIM_TypeDef*)TIM2_BASE)
#define TIM3                ((TIM_TypeDef*)TIM3_BASE)
#define USART1              ((USART_TypeDef*)USART1_BASE)
#define GPIOC               ((GPIO_TypeDef*)GPIOC_BASE)

// HAL 函数占位实现
HAL_StatusTypeDef HAL_Init() { memset((void*)TIM2, 0, sizeof(TIM_TypeDef)); memset((void*)TIM3, 0, sizeof(TIM_TypeDef)); return HAL_OK; }
HAL_StatusTypeDef HAL_InitTick(uint32_t) { return HAL_OK; }
uint32_t HAL_GetTick() { static uint32_t tick = 0; return tick++; }

void HAL_NVIC_SetPriority(IRQn_Type, uint32_t, uint32_t) {}
void HAL_NVIC_EnableIRQ(IRQn_Type) {}

typedef struct { uint16_t Pin; uint32_t Mode, Pull, Speed; } GPIO_InitTypeDef;
#define GPIO_MODE_OUTPUT_PP  0x01
#define GPIO_NOPULL          0x00
#define GPIO_PIN_SET         1
#define GPIO_PIN_13          ((uint16_t)0x2000)
#define GPIO_SPEED_FREQ_LOW  0x00

void __HAL_RCC_GPIOC_CLK_ENABLE() { GPIOC->CRH = 0; }
void __HAL_RCC_TIM2_CLK_ENABLE() { TIM2->CR1 = 0; }
void __HAL_RCC_TIM3_CLK_ENABLE() { TIM3->CR1 = 0; }

void HAL_GPIO_Init(GPIO_TypeDef*, GPIO_InitTypeDef*) {}
void HAL_GPIO_WritePin(GPIO_TypeDef*, uint16_t, uint32_t) { GPIOC->BSRR = 0; }

// 定时器 SR/DIER 标志位
#define TIM_SR_UIF     0x01
#define TIM_DIER_UIE   0x01
#define TIM_CR1_ARPE   0x80
#define TIM_CR1_CEN    0x01
#define TIM_EGR_UG     0x01

// UART SR 标志位
#define USART_SR_TXE   0x80
