# 目标平台部署指南

## 平台总览

| 平台 | 目录 | 定时器 | 精度 | 典型场景 |
|------|------|--------|------|----------|
| Linux 通用 | `target/linux/` | timerfd + SCHED_FIFO | <20us (PREEMPT_RT) | 工控机、服务器 |
| Windows | `target/windows/` | QPC + WaitableTimer | 100-500us | 开发测试 |
| Desktop（跨平台） | `target/desktop/` | chrono::steady_clock | ~1ms | 功能验证 |
| Raspberry Pi | `target/rpi/` | clock_nanosleep + GPIO | ~1us (稳态) | 边缘控制、原型 |
| STM32H7 | `target/stm32h7/` | DWT CYCCNT + TIM2 IRQ | 5.95ns | 硬实时裸机 |
| STM32F1 | `target/stm32f1/` | — | — | 低资源裸机 |
| RK3588 | `target/rk3588/` | timerfd + CPU affinity | <20us | 高性能 ARM |
| Banana Pi BPI-F3 | `target/bananapif3/` | RVV 1.0 向量扩展 | — | RISC-V 实验 |

---

## 1. 桌面 Desktop（开发测试）

### 特点
- 跨平台（Windows / macOS / Linux）
- 依赖 `std::chrono`，精度低但零配置
- 适合功能验证和 CI

### 构建

```bash
cd target/desktop

# 安装依赖（首次）
# Windows: 已包含 MinGW
# Linux:  sudo apt install g++ cmake make
# macOS:  brew install gcc cmake make

# 编译（需先执行 Java 编译器生成 POU .cpp）
make config        # 仅生成运行时配置
make               # 完整编译（含 POU）
make stub          # 最小编译（无 POU，纯运行时）
```

### 运行

```bash
# 完整运行
./plc_runtime_desktop.exe

# 指定周期（微秒）
./plc_runtime_desktop.exe --cycle-us 500

# 纯抖动测试（不加载 PLC 逻辑）
./plc_runtime_desktop.exe --jitter-only

# 诊断间隔（秒）
./plc_runtime_desktop.exe --diag-interval 2

# 帮助
./plc_runtime_desktop.exe --help
```

### 命令行参数

| 参数 | 说明 | 默认 |
|------|------|------|
| `--cycle-us N` | PLC 周期（微秒） | 1000 |
| `--diag-interval N` | 诊断输出间隔（秒） | 5 |
| `--jitter-only` | 仅测定时器抖动 | false |

---

## 2. Windows（QPC + WaitableTimer）

### 特点
- 使用 `QueryPerformanceCounter` + `WaitableTimer`
- `timeBeginPeriod(1)` 最小化调度精度
- 非实时 OS，1ms 周期抖动约 100-500us

### 构建

```bash
# 需要 MinGW（MSYS2）
pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-cmake make

cd target/windows
make            # 完整编译
make stub       # 最小编译
```

### 运行

```bash
# 管理员权限可提高优先级
plc_runtime_windows.exe --cycle-us 1000
```

### EtherCAT（需 Npcap）

```bash
# 下载 Npcap SDK，解压到 target/windows/lib/
make ethercat
plc_runtime_windows_ecat.exe --cycle-us 1000 --tci ethercat --ecat-if eth0
```

---

## 3. Linux 通用（timerfd + SCHED_FIFO）

### 特点
- `timerfd` 周期定时器 + `SCHED_FIFO` 实时优先级
- **需要 root** 设置调度策略
- PREEMPT_RT 内核下抖动 <20us，标准内核 <500us
- 不依赖 GPIO，纯用户态

### 构建

```bash
cd target/linux
make            # 完整编译
make stub       # 最小编译（无 POU）
```

### 运行

```bash
# 标准运行（需 root）
sudo ./plc_runtime_linux --cycle-us 1000

# 指定实时优先级（1-99，默认 90）
sudo ./plc_runtime_linux --cycle-us 500 --rt-prio 95

# 纯抖动测试
sudo ./plc_runtime_linux --jitter-only
```

### 命令行参数

| 参数 | 说明 | 默认 |
|------|------|------|
| `--cycle-us N` | PLC 周期（微秒） | 1000 |
| `--rt-prio N` | SCHED_FIFO 优先级 1-99 | 90 |
| `--diag-interval N` | 诊断间隔（秒） | 5 |
| `--jitter-only` | 仅测定时器抖动 | false |

### EtherCAT

```bash
sudo apt install libpcap-dev
make ethercat
sudo ./plc_runtime_linux_ecat --cycle-us 1000 --tci ethercat --ecat-if eth0
```

---

## 4. Raspberry Pi（clock_nanosleep + GPIO）

### 特点
- `clock_nanosleep` 绝对时间模式 + `SCHED_FIFO/99`
- GPIO：BCM 寄存器直接访问（`/dev/gpiomem`）
- 标准内核即可达到 ~1us 稳态抖动
- 实测 250us（4kHz）周期稳态抖动接近 0

### 硬件要求

| 项目 | 说明 |
|------|------|
| 硬件 | Raspberry Pi 4 (BCM2711) |
| 内核 | 6.18.34+rpt-rpi-v8（标准内核即可） |
| OS | Debian 13 (trixie), aarch64 |

### 构建

#### 本地编译（在树莓派上）

```bash
sudo apt install build-essential cmake python3

cd target/rpi
make stub                # 无 POU（纯运行时 + GPIO）
make                     # 含 POU
```

#### 交叉编译（在 PC 上）

```bash
# 安装交叉工具链
# Ubuntu: sudo apt install g++-aarch64-linux-gnu
# Windows: MSYS2 → pacman -S mingw-w64-ucrt-x86_64-aarch64-linux-gnu-gcc

cd target/rpi
make CROSS=aarch64-linux-gnu-         # 交叉编译
# 产出的 plc_runtime_rpi 需 scp 到树莓派运行
```

### 运行

```bash
# 系统优化
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
sudo systemctl stop irqbalance

# GPIO 模式（默认）
taskset -c 3 sudo ./plc_runtime_stub --cycle-us 1000

# EtherCAT 模式（需 SOEM + libpcap）
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci ethercat --ecat-if eth0

# GPIO + EtherCAT 同时
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci both --ecat-if eth0
```

### 命令行参数

| 参数 | 说明 | 默认 |
|------|------|------|
| `--cycle-us N` | PLC 周期（微秒） | 1000 |
| `--tci MODE` | I/O 模式：`gpio`/`ethercat`/`both` | gpio |
| `--ecat-if NAME` | EtherCAT 网口名 | eth0 |

### GPIO 引脚映射

**输入（BCM → PLC）**

| BCM GPIO | PLC 地址 | 物理引脚 |
|----------|----------|----------|
| 5 | %IX0.0 | Pin 29 |
| 6 | %IX0.1 | Pin 31 |
| 13 | %IX0.2 | Pin 33 |
| 19 | %IX0.3 | Pin 35 |
| 26 | %IX0.4 | Pin 37 |
| 21 | %IX0.5 | Pin 40 |
| 20 | %IX0.6 | Pin 38 |
| 16 | %IX0.7 | Pin 36 |

**输出（PLC → BCM）**

| PLC 地址 | BCM GPIO | 物理引脚 |
|----------|----------|----------|
| %QX0.0 | 17 | Pin 11 |
| %QX0.1 | 27 | Pin 13 |
| %QX0.2 | 22 | Pin 15 |
| %QX0.3 | 23 | Pin 16 |
| %QX0.4 | 24 | Pin 18 |
| %QX0.5 | 25 | Pin 22 |
| %QX0.6 | 12 | Pin 32 |
| %QX0.7 | 18 | Pin 12 |

### 全自动部署

```bash
python target/rpi/deploy_rpi.py --ip 192.168.1.100 --cycle-us 1000
```

### 抖动测试结果

| 周期 | 采样数 | Min | Max | Avg |
|------|--------|-----|-----|-----|
| 1000us | 7,975 | 0us | 431us | 1us |
| 500us | 15,950 | 0us | 182us | 0us |
| 250us | 31,900 | 0us | 200us | 0us |

---

## 5. STM32H7 裸机（DWT + TIM2 中断）

### 特点
- 纯裸机，无 OS
- DWT CYCCNT 精度 5.95ns（168MHz 下）
- TIM2 硬件中断驱动 PLC tick，抖动 <1 tick
- 所有 PLC 逻辑在中断中执行，主循环 `__WFI()` 省电

### 硬件要求

| 项目 | 说明 |
|------|------|
| MCU | STM32H7xx / STM32F4xx / STM32G4xx |
| 计时 | DWT Cycle Counter（必需） |
| 定时器 | TIM2（或 TIM3/TIM5）做周期 tick |
| UART | USART2（printf 重定向） |
| LED | PD12（心跳指示，可按需修改） |
| 系统时钟 | 168MHz（STM32F407）或 400MHz+（STM32H7） |

### 文件结构

```
target/stm32h7/
├── platform_stm32.cpp    # 硬件抽象层（DWT + TIM2 + UART + LED）
└── runtime_stm32.cpp     # PLC 运行时入口（注册 + 配置 + 主循环）
```

### 编译

STM32 裸机需要配合 HAL 库编译：

```bash
# 将以下文件添加到 STM32CubeMX 生成的工程：
#   runtime-flat/include/   (头文件路径)
#   runtime-flat/src/*.cpp  (运行时源文件)
#   target/stm32h7/platform_stm32.cpp
#   target/stm32h7/runtime_stm32.cpp

# 编译标志（通过 -D 覆盖）
# -DRT_PLATFORM_BARE_METAL
# -DPLC_SYSTEM_CORE_CLOCK=168000000
# -DPLC_TICK_TIM=TIM2
# -DPLC_LED_PORT=GPIOD
# -DPLC_LED_PIN=GPIO_PIN_12
```

### 启动流程

```
Reset → SystemClock_Config() → plc_platform_init() → plc_runtime_init() → while(1) { __WFI(); }
```

### 自定义配置

```cpp
static constexpr TIME PLC_BASE_CYCLE_US = 1000;   // 基础周期
static constexpr TIME PLC_TASK_INTERVAL = 1000;    // 任务执行间隔
static constexpr int  PLC_TASK_PRIORITY = 5;
static constexpr TIME PLC_WATCHDOG_US   = 5000;    // 看门狗超时
```

### 注意事项

- **优先级**：TIM2 中断设为最高（0,0），避免被其他中断抢占
- **非对齐访问**：ARM Cortex-M 触发 hard fault，编译器 GVL 偏移分配必须对齐
- **堆栈**：ISR 中执行 PLC 逻辑，确保堆栈足够（建议 4KB+）
- **printf**：重定向到 UART，调试时串口助手查看

---

## 6. RK3588（高性能 ARM）

```bash
cd target/rk3588
make                    # 含 POU
make stub               # 最小编译

sudo ./plc_runtime_rk3588 --cycle-us 1000 --cpu 4
```

---

## 7. Banana Pi BPI-F3（RISC-V）

```bash
cd target/bananapif3
make
```

---

## 8. STM32F1（低资源裸机）

```bash
cd target/stm32f1
make
```

---

## NetworkTCI — TCP 网络 I/O

NetworkTCI 将耗时 socket I/O 放到后台线程，PLC 周期中只做 `memcpy`：

```
PLC 周期（微秒级）
  ├── syncInputs()  ←  memcpy(rxBuf → ProcessImage)
  ├── 用户逻辑      ←  读写 ProcessImage
  └── syncOutputs() →  memcpy(ProcessImage → txBuf)
          ↕
后台线程（非实时）
  ├── select() + recv() → 写入 rxBuf
  └── select() + send() ← 读取 txBuf
```

```cpp
#include "network_tci.h"

rt_plc::NetworkTCI net;
net.begin("192.168.1.100", 5000);       // 客户端模式
// 或 net.begin(502);                    // 服务端模式，绑定本地端口
net.mapInput(0, 0, 2);    // %IW0 ← 网络 byte 0-1
net.mapOutput(0, 4, 2);   // %QW0 → 网络 byte 4-5

rt_plc::CompositeTCI composite;
composite.add(&net);
```

```bash
cd target/linux && make stub
cd target/windows && make stub
cd target/desktop && make stub
cd target/rpi && make stub
cd target/desktop && make network-test
```

---

## Makefile 通用用法

| 命令 | 说明 |
|------|------|
| `make` | 完整编译（含 POU） |
| `make stub` | 最小编译（无 POU，无配置） |
| `make ethercat` | EtherCAT 编译（需 SOEM） |
| `make run` | 编译 + 运行 |
| `make clean` | 清理 |
| `make config` | 仅生成运行时配置代码 |

环境变量覆盖：

```bash
CROSS=aarch64-linux-gnu- make    # 交叉编译
CYCLE_US=500 make run            # 500us 周期运行
```
