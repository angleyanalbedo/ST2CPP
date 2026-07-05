# 普中玄武 F103 综合实验板 — 硬件手册

板载 MCU: **STM32F103ZET6** (Cortex-M3, 72MHz, 512KB Flash, 64KB RAM)
来源分析: 官方示例程序 `0--玄武F103综合实验程序.zip`

## 外设概览

| 类型 | 外设 | 接口 |
|------|------|------|
| **板载芯片** | 外部 SRAM (IS62WV51216) | FSMC NE3 |
| | SPI Flash (W25Q64/128) | SPI2 + PB12 |
| | AT24C02 EEPROM | 软件 I2C PB6/PB7 |
| | MPU6050 六轴传感器 | 软件 I2C PB6/PB7 |
| | ILI9488 TFT 彩屏 + 电阻触摸 | FSMC NE4 + 软件 SPI |
| **板载座子** | SD 卡槽 | SDIO (PC8~12, PD2) |
| | RS232 座 (DB9) | USART2 (PA2/PA3) |
| | RS485 座 | USART2 (PA2/PA3) + PD7 方向 |
| | CAN 接口 | CAN1 (PA11/PA12, 需外接收发器) |
| | USB 设备口 | USB (PA11/PA12) |
| **排针引出 (需外接模块)** | NRF24L01 无线 | SPI2 + PG6/PG7/PG8 |
| | 摄像头 OV7670/OV7725 | 并行 8bit + SCCB |
| | VS10xx MP3 模块 | SPI1 + PC13/PE6/PF6/PF7 |
| | 红外遥控接收头 | PB9 EXTI9 |
| | DS18B20 温度传感器 | PG11 单总线 |
| | WS2812 彩灯 | (需确认引脚) |
| | 光敏电阻 | PF8 ADC3_IN6 |
| | 电位器 / ADC | PA1 ADC1_IN1 |
| | DAC 输出 | PA4 |

---

## 1. 系统时钟树

| 参数 | 值 |
|------|------|
| HSE 晶振 | **8 MHz** |
| PLL 源 | HSE |
| PLL 倍频 | x9 (8M x9 = 72M) |
| SYSCLK | **72 MHz** |
| HCLK (AHB) | 72 MHz (不分频) |
| PCLK2 (APB2) | 72 MHz (不分频) |
| PCLK1 (APB1) | 36 MHz (/2) |
| APB1 定时器时钟 | 72 MHz (APB1 预分频≠1, 自动 x2) |
| ADC 时钟 | 12 MHz (PCLK2/6) |
| USB 时钟 | 48 MHz (PLLCLK/1.5) |
| SysTick | 9 MHz (HCLK/8) |
| Flash 等待 | 2 周期 (预取缓冲启用) |

---

## 2. 存储器映射

| 区域 | 起始地址 | 大小 | 说明 |
|------|---------|------|------|
| 内部 Flash | 0x08000000 | 512KB | 程序存储 |
| 内部 SRAM | 0x20000000 | 64KB | 运行时数据 |
| FSMC NE3 (SRAM) | 0x68000000 | 1MB | 外部 SRAM (IS62WV51216) |
| FSMC NE4 (LCD) | 0x6C000000 + 0x7FE | reg | ILI9488 TFT LCD |

---

## 3. 完整引脚映射

### 3.1 板上基本外设

| 外设 | 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|------|
| **LED1** | 指示灯 | **PB5** | PP 输出 | 低电平亮 |
| **LED2** | 指示灯 | **PE5** | PP 输出 | 低电平亮 |
| **蜂鸣器** | BEEP | **PB8** | PP 输出 | 高电平响 |
| **KEY_UP** | 唤醒/上 | **PA0** | IP 下拉 | EXTI0 上升沿 |
| **KEY0** | 按键 0 | **PE4** | IP 上拉 | EXTI4 下降沿 |
| **KEY1** | 按键 1 | **PE3** | IP 上拉 | EXTI3 下降沿 |
| **KEY2** | 按键 2 | **PE2** | IP 上拉 | 无 EXTI |

### 3.2 USART / RS232 / RS485

| 外设 | 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|------|
| **USART1** (调试/RS232) | TX | **PA9** | AF PP | 115200 8N1 |
| | RX | **PA10** | 浮空输入 | |
| **USART2** (RS232) | TX | **PA2** | AF PP | |
| | RX | **PA3** | 浮空输入 | |
| **RS485** (USART2) | TX | PA2 | AF PP | 与 RS232 共用 USART2 |
| | RX | PA3 | 浮空输入 | |
| | **方向控制** | **PD7** | PP 输出 | 0=RX, 1=TX |

> `PD7` 与 SD 卡检测共用，RS485 和 SD 卡互斥。

### 3.3 CAN / USB

| 外设 | 信号 | GPIO | 备注 |
|------|------|------|------|
| **CAN1** (座子) | RX | **PA11** | 需外接 CAN 收发器 (如 TJA1050) |
| | TX | **PA12** | 与 USB_DM 共用引脚 |
| **USB** (座子) | D- | PA11 | USB 全速设备 |
| | D+ | PA12 | USB 全速设备 |

> CAN 和 USB 互斥，不能同时使用。

### 3.4 SPI1 — VS10xx MP3 接口 (排针引出, 需外接模块)

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **SPI1_SCK** | **PA5** | AF PP | |
| **SPI1_MISO** | **PA6** | AF PP | |
| **SPI1_MOSI** | **PA7** | AF PP | |
| VS_DREQ | **PC13** | IP 上拉 | VS10xx 数据请求 |
| VS_RST | **PE6** | PP 输出 | VS10xx 复位 |
| VS_XCS | **PF7** | PP 输出 | 命令片选 |
| VS_XDCS | **PF6** | PP 输出 | 数据片选 |

### 3.5 SPI2 — SPI Flash + NRF24L01 接口 (共享)

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **SPI2_SCK** | **PB13** | AF PP | SPI2 时钟 |
| **SPI2_MISO** | **PB14** | AF PP | SPI2 MISO |
| **SPI2_MOSI** | **PB15** | AF PP | SPI2 MOSI |
| **FLASH_CS** | **PB12** | PP 输出 | **板载** SPI Flash (W25Q64/128) 片选 |
| NRF24L01_CE | **PG8** | PP 输出 | **排针**, NRF24L01 模块使能 |
| NRF24L01_CSN | **PG7** | PP 输出 | **排针**, NRF24L01 模块片选 |
| NRF24L01_IRQ | **PG6** | IP 下拉 | **排针**, NRF24L01 中断 |

### 3.6 软件 I2C — EEPROM + MPU6050

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **IIC_SCL** | **PB6** | PP 输出 | 软件 I2C 时钟 |
| **IIC_SDA** | **PB7** | PP 输出 / IP 上拉 | 软件 I2C 数据 (双向) |

从设备:
- AT24C02 EEPROM: 地址 0xA0
- MPU6050 六轴: 地址 0x68

### 3.7 触摸屏

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| T_PEN (PENIRQ) | **PF10** | IP 上拉 | 触摸中断 |
| T_MISO (DOUT) | **PB2** | IP 上拉 | 软件 SPI MISO |
| T_MOSI (TDIN) | **PF9** | PP 输出 | 软件 SPI MOSI |
| T_SCK (TCLK) | **PB1** | PP 输出 | 软件 SPI 时钟 |
| T_CS (TCS) | **PF11** | PP 输出 | 触摸片选 |

> 触摸使用软件 SPI (bit-bang), 不是硬件 SPI。

### 3.8 ADC

| 通道 | GPIO | 备注 |
|------|------|------|
| **ADC1_IN1** | **PA1** | 排针引出, 可接电位器/传感器 |
| **ADC3_IN6** | **PF8** | 排针引出, 可接光敏电阻模块 |
| ADC1_IN16 | 内部 | 芯片内部温度传感器 |

### 3.9 DAC

| 通道 | GPIO | 备注 |
|------|------|------|
| **DAC1_OUT** | **PA4** | DAC 模拟输出 |

### 3.10 定时器 / PWM / 输入捕获

| 功能 | 定时器 | 通道 | GPIO | 备注 |
|------|--------|------|------|------|
| **PWM** | TIM3 | CH2 | **PB5** | 与 LED1 共用引脚 |
| **输入捕获** | TIM5 | CH1 | **PA0** | 与 KEY_UP 共用引脚 |
| **触摸按键** | TIM5 | CH2 | **PA1** | 与 ADC1_IN1 共用引脚 |

### 3.11 红外遥控接收头 (排针引出)

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **IR_IN** | **PB9** | IP 上拉 | EXTI9 下降沿, 需外接红外接收头 |

### 3.12 DS18B20 温度传感器 (排针引出, 无板载模块)

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **DS18B20_DQ** | **PG11** | PP 输出 / IP 上拉 | 仅引出排针, 需自备 DS18B20 传感器 |

### 3.13 SD 卡 (SDIO)

| 信号 | GPIO | 模式 |
|------|------|------|
| **SDIO_D0** | **PC8** | AF PP |
| **SDIO_D1** | **PC9** | AF PP |
| **SDIO_D2** | **PC10** | AF PP |
| **SDIO_D3** | **PC11** | AF PP |
| **SDIO_CK** | **PC12** | AF PP |
| **SDIO_CMD** | **PD2** | AF PP |
| **SD_DETECT** | **PD7** | IP 上拉 (与 RS485_TX_EN 共用) |

> SDIO 使用 DMA2 传输数据。

### 3.14 摄像头 OV7670 / OV7725 (排针引出, 需外接模块)

| 信号 | GPIO | 模式 | 备注 |
|------|------|------|------|
| **VSYNC** | **PA8** | 输入 (EXTI8) | 帧同步, 上升沿触发 |
| FIFO_WRST | **PD6** | PP 输出 | FIFO 写复位 |
| FIFO_WREN | **PB3** | PP 输出 | FIFO 写使能 |
| FIFO_RCK | **PB4** | PP 输出 | FIFO 读时钟 |
| FIFO_RRST | **PG14** | PP 输出 | FIFO 读复位 |
| FIFO_OE | **PG15** | PP 输出 | FIFO 输出使能 |
| **CAM_D0~D7** | **PC0~PC7** | 输入 | 8 位并行数据 |

SCCB (I2C 类): PD3=SCL, PG13=SDA

### 3.15 FSMC — LCD + 外部 SRAM

两条 FSMC 总线共用大部分数据/控制引脚, 通过不同片选选择设备:

#### 信号线

| FSMC 信号 | GPIO | LCD (NE4) | SRAM (NE3) |
|-----------|------|-----------|------------|
| **D0** | PD14 | ✔ | ✔ |
| **D1** | PD15 | ✔ | ✔ |
| **D2** | PD0 | ✔ | ✔ |
| **D3** | PD1 | ✔ | ✔ |
| **D4** | PD8 | ✔ | ✔ |
| **D5** | PD9 | ✔ | ✔ |
| **D6** | PD10 | ✔ | ✔ |
| **D7** | PE7 | ✔ | ✔ |
| **D8** | PE8 | ✔ | ✔ |
| **D9** | PE9 | ✔ | ✔ |
| **D10** | PE10 | ✔ | ✔ |
| **D11** | PE11 | ✔ | ✔ |
| **D12** | PE12 | ✔ | ✔ |
| **D13** | PE13 | ✔ | ✘ (LCD only) |
| **D14** | PE14 | ✔ | ✘ (LCD only) |
| **D15** | PE15 | ✔ | ✔ (SRAM 也用) |
| **NOE (RD)** | PD4 | ✔ | ✔ |
| **NWE (WR)** | PD5 | ✔ | ✔ |
| **NE4 (LCD_CS)** | **PG12** | ✔ | ✘ |
| **NE3 (SRAM_CS)** | **PG10** | ✘ | ✔ |
| **A10 (LCD_RS)** | **PG0** | ✔ | A16 (SRAM 地址) |
| **A0~A5** | PF0~5 | ✘ | SRAM 地址线 |
| **A12~A21** | PF12~PG5 | ✘ | SRAM 地址线 |
| **NBL0** | PE0 | ✘ | SRAM 低字节掩码 |
| **NBL1** | PE1 | ✘ | SRAM 高字节掩码 |
| **LCD_LED** | PB0 | 背光 | PP 输出 |

> LCD 数据总线: D0~D15 (16 位), 仅 D13~D15 为 LCD 独占。
> SRAM 数据总线: D0~D15, 外加 A0~A5, A12~A21 地址线。

#### FSMC 时序

| 参数 | LCD 读 | LCD 写 | SRAM |
|------|--------|--------|------|
| AddressSetupTime | 2 HCLK | 22 HCLK | 1 HCLK |
| DataSetupTime | 16 HCLK | 6 HCLK | 9 HCLK |
| 总线宽度 | 16bit | 16bit | 16bit |
| 芯片选择 | NE4 | NE4 | NE3 |
| 基地址 | 0x6C0007FE | 0x6C0007FE | 0x68000000 |

---

## 4. 中断线分配

| EXTI 线 | GPIO | 触发 | 优先级 | 用途 |
|---------|------|------|--------|------|
| EXTI0 | PA0 | 上升沿 | 2,3 | KEY_UP |
| EXTI3 | PE3 | 下降沿 | 2,1 | KEY1 |
| EXTI4 | PE4 | 下降沿 | 2,0 | KEY0 |
| EXTI8 | PA8 | 上升沿 | 0,0 | 摄像头 VSYNC |
| EXTI9 | PB9 | 下降沿 | 0,1 | 红外遥控 |
| EXTI18 | 内部 | 上升沿 | 0,- | USB 唤醒 |

| NVIC 中断 | 抢占,子 | 用途 |
|-----------|--------|------|
| TIM4_IRQn | 0,1 | 20ms 系统心跳 |
| TIM2_IRQn | 3,1 | 100ms 定时器 |
| USART1_IRQn | 3,3 | 串口调试 |
| USART2_IRQn | 2,2 (RS232) / 3,3 (RS485) | 串口 2 |
| SDIO_IRQn | 0,0 (DMA) | SD 卡 |
| DMA2_Channel4_5_IRQn | 0,0 | SDIO DMA |

---

## 5. 引脚冲突汇总

| 引脚 | 冲突双方 |
|------|---------|
| **PA0** | KEY_UP vs TIM5_CH1 输入捕获 |
| **PA1** | ADC1_IN1 vs TIM5_CH2 触摸按键 |
| **PA11** | CAN1_RX vs USB_DM |
| **PA12** | CAN1_TX vs USB_DP |
| **PB1** | 触摸 T_SCK vs 电容触摸 I2C SCL |
| **PB5** | LED1 vs TIM3_CH2 PWM |
| **PD7** | SD_DETECT vs RS485_TX_EN |
| **PG0** | LCD_RS (FSMC_A10) vs SRAM_A16 |

---

## 6. 已使用的 GPIO 占用率

| GPIO 端口 | 16 位中已用 | 剩余可用 |
|-----------|-----------|---------|
| **PA** | 0-12 (13) | 13-15 |
| **PB** | 0-9, 12-15 (14) | 10, 11 |
| **PC** | 0-13 (14) | 14, 15 |
| **PD** | 0-15 (16) | **无** |
| **PE** | 0-15 (16) | **无** |
| **PF** | 0-15 (16) | **无** |
| **PG** | 0-15 (16) | **无** |
