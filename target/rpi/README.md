# target/rpi — Raspberry Pi PLC Runtime

## 概述

树莓派 4 上的硬实时 PLC 运行时，支持 GPIO 本地 I/O 和 EtherCAT 远程 I/O。

```
ST 源码 → Java 编译器 → C++ 代码 → 本地编译 → 裸机/RTOS → GPIO / EtherCAT → 真实设备
```

## 目录结构

```
target/rpi/
├── Makefile              # 编译（含 stub / ethercat 模式）
├── runtime_rpi.cpp       # PLC 运行时入口（clock_nanosleep 驱动）
├── platform_rpi.cpp      # 平台 HAL（定时器/GPIO/RT优先级/抖动统计）
├── ethercat_tci.h        # EtherCAT ↔ PLC 同步（SOEM context-based API）
├── ethercat_tci.cpp      # EtherCAT TCI 实现
├── pdo_config.h          # PDO ↔ ProcessImage 映射表
├── hal/                  # GPIO HAL
│   ├── gpio_hal.h        # 多型号 Pi GPIO 寄存器访问
│   ├── gpio_hal.cpp      # BCM 编号/物理引脚映射
│   ├── gpio_tci.h        # GPIO ↔ PLC ProcessImage 同步
│   └── gpio_tci.cpp      # TCI 实现（%IX/%QX ↔ BCM GPIO）
├── soem/                 # SOEM 源码（git clone，第三方 EtherCAT 协议栈）
├── pou_stub.cpp          # 最小 POU 注册桩
├── jitter_test.cpp       # 独立抖动测试工具
├── gen_registry.py       # POU 注册桥接生成
└── config/tasks.json     # 任务配置
```

## 构建

```bash
# 首次：克隆 SOEM
cd target/rpi && git clone https://github.com/OpenEtherCATsociety/soem.git soem

# 安装依赖
sudo apt install libpcap-dev

# 编译
make stub                # 无 POU（纯运行时 + GPIO）
make ethercat            # 含 SOEM（EtherCAT 模式）
make                     # 含 POU
```

## 运行

```bash
# 运行前优化
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
sudo systemctl stop irqbalance

# GPIO 模式
taskset -c 3 sudo ./plc_runtime_stub --cycle-us 1000

# EtherCAT 模式
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci ethercat --ecat-if eth0

# GPIO + EtherCAT 同时
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci both --ecat-if eth0
```

## 命令行参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `--cycle-us N` | PLC 周期（微秒） | 1000 |
| `--tci MODE` | I/O 模式：`gpio` / `ethercat` / `both` | gpio |
| `--ecat-if NAME` | EtherCAT 网口名 | eth0 |

## I/O 架构

```
                    ┌─────────────────────────────┐
                    │     CompositeTCI             │
                    │  (可同时挂多个 TCI 后端)      │
                    └──────┬──────────┬────────────┘
                           │          │
              ┌────────────┴──┐  ┌────┴────────────┐
              │ GPIO TCI      │  │ EtherCAT TCI    │
              │ %IX0.0 → BCM5 │  │ %IW0 → StatusWd │
              │ %QX0.0 → BCM17│  │ %QW0 → ControlWd│
              └───────┬───────┘  └────────┬────────┘
                      │                   │
               物理 GPIO 引脚        eth0 → EtherCAT 电缆 → 从站
```

### TCI 接口

```cpp
struct TCI {
    virtual void syncInputs(ProcessImage& img) = 0;   // 硬件 → ProcessImage
    virtual void syncOutputs(ProcessImage& img) = 0;   // ProcessImage → 硬件
};
```

任何新 I/O 后端（PROFINET、CANopen、Modbus）只需实现这两个方法。

## GPIO 引脚映射

### 输入（BCM → PLC）

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

### 输出（PLC → BCM）

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

## EtherCAT PDO 映射

编辑 `pdo_config.h` 修改映射表。示例（单个伺服驱动器）：

```cpp
// TXPDO（从站→主站）：StatusWord → %IW0, ActualVelocity → %IW1, ActualPosition → %ID2
// RXPDO（主站→从站）：ControlWord → %QW0, TargetVelocity → %QW1, TargetPosition → %QD2
```

ST 代码直接用地址读写：
```st
VAR
    StatusWord   AT %IW0  : INT;    // 读取从站状态
    TargetSpeed  AT %QW1  : INT;    // 写入目标速度
END_VAR
```

## 抖动测试数据

### 测试环境

| 项目 | 值 |
|------|-----|
| 硬件 | Raspberry Pi 4 (BCM2711) |
| 内核 | 6.18.34+rpt-rpi-v8 (标准内核，非 PREEMPT_RT) |
| OS | Debian 13 (trixie), aarch64 |
| RT 配置 | SCHED_FIFO/99, taskset -c 3, performance governor |

### 定时器驱动方式对比（1000us 周期）

| 指标 | SIGEV_THREAD | clock_nanosleep | 改善 |
|------|-------------|-----------------|------|
| Min jitter | 38 us | 0 us | ∞ |
| Max jitter | 6,898 us | 431 us | 16x |
| Avg jitter | 1,001 us | 1 us | 1000x |

### clock_nanosleep 不同周期

| 周期 | Samples | Min | Max | Avg |
|------|---------|-----|-----|-----|
| 1000 us | 7,975 | 0 us | 431 us | 1 us |
| 500 us | 15,950 | 0 us | 182 us | 0 us |
| 250 us | 31,900 | 0 us | 200 us | 0 us |

> 250us = 4kHz 扫描率，稳态抖动接近 0

### 关键发现

1. **SIGEV_THREAD 是抖动元凶**：独立线程回调，内核调度延迟不可控
2. **clock_nanosleep 绝对时间模式**：CLOCK_MONOTONIC + TIMER_ABSTIME，避免累积漂移
3. **标准内核也能微秒级**：不需要 PREEMPT_RT
4. **Max jitter 431us 来自启动阶段**：稳态接近 0us
