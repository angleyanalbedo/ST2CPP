# 普中玄武 F103（STM32F103ZET6）PLC 运行时

## 开发板信息

| 项目 | 说明 |
|------|------|
| 型号 | 普中玄武 F103 综合实验板 |
| MCU  | STM32F103ZET6（Cortex-M3, 72MHz） |
| Flash | 512KB |
| RAM  | 64KB |
| 调试接口 | 20-pin JTAG（ST-Link 仅支持 SWD） |

## 外设引脚

| 外设 | 引脚 | 说明 |
|------|------|------|
| **LED** | **PB5** | 低电平点亮 |
| **蜂鸣器** | **PB8** | 高电平响 |
| KEY_UP | PA0 | 高电平有效（内部下拉） |
| KEY0 | PE4 | 低电平有效（内部上拉） |
| KEY1 | PE3 | 同上 |
| KEY2 | PE2 | 同上 |
| **USART1 TX** | **PA9** | 115200 8N1 |
| **USART1 RX** | **PA10** | 115200 8N1 |
| I2C SCL (24C02) | PB6 | 软件 I2C |
| I2C SDA (24C02) | PB7 | 软件 I2C |
| SPI Flash CS | PB12 | EN25Q128 |
| SPI2 SCK/MISO/MOSI | PB13/PB14/PB15 | SPI Flash |
| LCD | FSMC 16-bit | ILI9488 |

## 前置准备

编译需要 ARM 交叉编译器 + OpenOCD，按以下步骤准备：

```powershell
powershell -ExecutionPolicy Bypass -File tools_download.ps1
```

详见 `tools/README.md`。

## 编译

```powershell
cd target/puzhong_zet6
make all
```

编译产物在 `build/` 目录：
- `plc_puzhong.elf` — ELF 文件
- `plc_puzhong.bin` — 裸二进制（烧录用）
- `plc_puzhong.hex` — Intel HEX

## 烧录

通过 ST-Link 的 20-pin 排线连接到开发板 JTAG 接口（SWD 模式）：

```powershell
make flash
```

成功后 ST-Link 会自动复位芯片。

## 查看串口输出

USB-UART 转换器接：

| STM32 | USB-UART |
|-------|----------|
| PA9 (TX) | RXD |
| GND | GND |

串口参数：**115200, 8N1**

启动后应看到：

```
PLC Puzhong ZET6: 72MHz
PLC ZET6 Runtime initialized
PLC ZET6 Ready.
```

LED（PB5）每 100ms 翻转一次。

## 清理

```powershell
make clean
```
