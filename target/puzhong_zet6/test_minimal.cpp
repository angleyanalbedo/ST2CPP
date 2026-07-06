// 最简测试: 仅 LED1 (PB5) 闪烁, 无任何其他功能
// 如果这个都不亮, 说明硬件或烧录有问题

#include "stm32f1xx_hal.h"

extern "C" void _exit(int) { while(1); }

static void clock_init() {
    int tout = 1000000;
    RCC->CR |= RCC_CR_HSEON;
    while (!(RCC->CR & RCC_CR_HSERDY)) { if (--tout == 0) break; }
    if (RCC->CR & RCC_CR_HSERDY) {
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLMULL) | RCC_CFGR_PLLMULL9;
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_PLLSRC) | RCC_CFGR_PLLSRC;
        RCC->CR |= RCC_CR_PLLON;
        while (!(RCC->CR & RCC_CR_PLLRDY)) {}
        FLASH->ACR = (FLASH->ACR & ~FLASH_ACR_LATENCY) | FLASH_ACR_LATENCY_2;
        RCC->CFGR = (RCC->CFGR & ~RCC_CFGR_SW) | RCC_CFGR_SW_PLL;
        while ((RCC->CFGR & RCC_CFGR_SWS) != RCC_CFGR_SWS_PLL) {}
    }
}

int main() {
    clock_init();
    __HAL_RCC_GPIOB_CLK_ENABLE();
    GPIOB->BRR = GPIO_PIN_5;  // PB5 LOW → LED on
    while (1) {
        for (volatile int i = 0; i < 2000000; i++) { __NOP(); }
        GPIOB->ODR ^= GPIO_PIN_5;
    }
}
