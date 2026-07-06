# 普中玄武 F103 (STM32F103ZET6) — Agent 备忘录

## 开发板

- **型号**: 普中玄武 F103 综合实验板
- **MCU**: STM32F103ZET6 (Cortex-M3 r1p1, 72MHz, 512KB Flash, 64KB RAM)
- **调试接口**: 20-pin JTAG 座 (实际用 SWD 模式)

## 仿真器

用的是 **CMSIS-DAP 仿真器** (ARM 仿真器, 不是 ST-Link)。  
20-pin 排线连接仿真器到开发板 JTAG 座。  
仿真器 USB 接电脑，开发板需要单独供电 (USB 口)。

**注意**: OpenOCD 配置用 `adapter driver cmsis-dap`，不是 `interface/stlink.cfg`。

## 工具链

项目根目录 `tools_download.ps1` 自动下载 ARM GCC + OpenOCD 到 `.env/`。

```powershell
powershell -ExecutionPolicy Bypass -File tools_download.ps1
```

## 编译

```powershell
cd target/puzhong_zet6
make all
```

产物: `build/plc_puzhong.bin`

## 烧录

```powershell
cd target/puzhong_zet6
make flash
```

OpenOCD 输出示例 (成功):
```
adapter speed: 1000 kHz
Info : CMSIS-DAP: FW Version = 2.0.0
Info : SWD DPIDR 0x1ba01477
Info : [stm32f1x.cpu] Cortex-M3 r1p1 processor detected
** Programming Finished **
** Resetting Target **
```

## 关键引脚

| 外设 | 引脚 |
|------|------|
| LED1 | PB5 (低电平亮) |
| LED2 / WS2812 DIN | PE5 |
| BEEP | PB8 (高电平响) |
| USART1 TX | PA9 (115200) |
| USART1 RX | PA10 |

## WS2812 5x5 RGB 矩阵

- 数据线: PE5 (与 LED2 共用引脚)
- 每像素 24bit: G(8) + R(8) + B(8)
- 24 位顺序: GRB (WS2812 协议)
- 时序要求严格, 用 NOP 计数, GCC 编译
- NOP 数量已经补偿了 GCC 直写 BSRR(2cyc) 和 Keil SPL 函数调用(10cyc)的差异

## 当前问题

烧录 OpenOCD 显示成功, 但 LED1 不闪、WS2812 不亮。  
怀疑: 芯片复位后没有执行到用户代码。  
需要排查: 启动文件(startup)、链接脚本或硬件供电/复位。

## 清理

```powershell
make clean
```
