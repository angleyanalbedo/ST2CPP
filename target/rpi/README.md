# target/rpi — Raspberry Pi PLC Runtime

## 抖动测试实验数据

### 测试环境

| 项目 | 值 |
|------|-----|
| 硬件 | Raspberry Pi 4 (BCM2711) |
| 内核 | 6.18.34+rpt-rpi-v8 (标准内核，非 PREEMPT_RT) |
| OS | Debian 13 (trixie), aarch64 |
| 编译器 | g++ 12.2, -O2 |
| RT 配置 | SCHED_FIFO/99, taskset -c 3, performance governor |

### 实验一：定时器驱动方式对比（1000us 周期）

| 指标 | SIGEV_THREAD (旧) | clock_nanosleep (新) | 改善倍数 |
|------|-------------------|---------------------|---------|
| Min jitter | 38 us | 0 us | ∞ |
| Max jitter | 6,898 us | 431 us | 16x |
| Avg jitter | 1,001 us | 1 us | 1000x |
| 样本数 | 2,907 | 7,975 | — |

> SIGEV_THREAD：POSIX timer_create + SIGEV_THREAD 回调，独立线程驱动
> clock_nanosleep：主线程 CLOCK_MONOTONIC + TIMER_ABSTIME 绝对时间等待

### 实验二：clock_nanosleep 不同周期对比

| 周期 | Samples | Min jitter | Max jitter | Avg jitter |
|------|---------|------------|------------|------------|
| 1000 us | 7,975 | 0 us | 431 us | 1 us |
| 500 us | 15,950 | 0 us | 182 us | 0 us |
| 250 us | 31,900 | 0 us | 200 us | 0 us |

> 250us 周期 = 4kHz 扫描率，稳态抖动接近 0

### 实验三：系统优化措施效果

| 措施 | 效果 |
|------|------|
| taskset -c 3 (绑核) | 消除 CPU 迁移抖动 |
| performance governor | 消除 CPU 频率切换抖动 |
| stop irqbalance | 减少 IRQ 中断干扰 |
| SCHED_FIFO/99 | 最高实时优先级 |
| mlockall | 锁定内存，避免 page fault |

### 关键发现

1. **SIGEV_THREAD 是抖动元凶**：POSIX timer 的 SIGEV_THREAD 回调在独立线程中执行，内核调度延迟不可控
2. **clock_nanosleep 绝对时间模式**：基于 CLOCK_MONOTONIC 的绝对时间等待，避免累积漂移
3. **标准内核也能做到微秒级**：不需要 PREEMPT_RT，正确的定时器驱动方式 + RT 优先级即可
4. **Max jitter 431us 来自启动阶段**：稳态抖动接近 0us

## 完整流程

### Step 1: 编译 ST → C++（PC 上）

```bash
cd java && mvn package -DskipTests
java -jar target/st2c-jar-with-dependencies.jar \
  --input ../../examples/test.st \
  --output-dir ../../output/flat/build \
  --file-id test
```

生成 `output/flat/build/test.cpp`（含 `registerPOU_test()` 函数）。

### Step 2: 生成 POU 注册桥接（PC 上）

```bash
python target/rpi/gen_registry.py
```

### Step 3: 部署到 Pi（PC 上）

```python
import paramiko, tarfile, tempfile, os

RPI_IP, RPI_USER, RPI_PASS = "192.168.5.128", "pi", "123456"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(RPI_IP, 22, RPI_USER, RPI_PASS, timeout=10)

tar_path = os.path.join(tempfile.gettempdir(), "st2c.tar.gz")
with tarfile.open(tar_path, "w:gz") as tar:
    tar.add("runtime-flat/include", arcname="runtime-flat/include")
    tar.add("runtime-flat/src", arcname="runtime-flat/src")
    tar.add("target/rpi/platform_rpi.cpp", arcname="target/rpi/platform_rpi.cpp")
    tar.add("target/rpi/runtime_rpi.cpp", arcname="target/rpi/runtime_rpi.cpp")
    tar.add("target/rpi/hal", arcname="target/rpi/hal")
    tar.add("target/rpi/pou_stub.cpp", arcname="target/rpi/pou_stub.cpp")
    for f in os.listdir("output/flat/build"):
        if f.endswith(".cpp"):
            tar.add(f"output/flat/build/{f}", arcname=f"output/flat/build/{f}")

remote_dir = client.exec_command("pwd")[1].read().decode().strip() + "/st2c-runtime"
sftp = client.open_sftp()
sftp.put(tar_path, f"{remote_dir}/st2c.tar.gz")
sftp.close()
```

### Step 4: Pi 上编译 + 运行

**含 POU 模式：**
```bash
cd ~/st2c-runtime && tar xzf st2c.tar.gz
g++ -O2 -std=c++17 -DRT_PLATFORM_LINUX -D_GNU_SOURCE \
    -I runtime-flat/include \
    runtime-flat/src/*.cpp \
    target/rpi/platform_rpi.cpp target/rpi/runtime_rpi.cpp \
    target/rpi/hal/gpio_hal.cpp target/rpi/hal/gpio_tci.cpp \
    output/flat/build/*.cpp \
    -lpthread -o plc_runtime_rpi

# 运行优化
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
sudo systemctl stop irqbalance
taskset -c 3 sudo ./plc_runtime_rpi --cycle-us 1000
```

**无 POU 模式（纯运行时 + GPIO 测试）：**
```bash
g++ -O2 -std=c++17 -DRT_PLATFORM_LINUX -D_GNU_SOURCE \
    -I runtime-flat/include \
    runtime-flat/src/*.cpp \
    target/rpi/platform_rpi.cpp target/rpi/runtime_rpi.cpp \
    target/rpi/hal/gpio_hal.cpp target/rpi/hal/gpio_tci.cpp \
    target/rpi/pou_stub.cpp \
    -lpthread -o plc_runtime_stub

taskset -c 3 sudo ./plc_runtime_stub --cycle-us 1000
```

## 文件结构

```
target/rpi/
├── Makefile              # 编译（含 stub 模式）
├── gen_registry.py       # 生成 registerAllPOUs 桥接
├── platform_rpi.cpp      # 平台 HAL（定时器/GPIO/RT优先级/抖动统计）
├── runtime_rpi.cpp       # PLC 运行时入口（clock_nanosleep 驱动）
├── pou_stub.cpp          # 最小 POU 注册桩（绕过编译器输出错误）
├── jitter_test.cpp       # 独立抖动测试工具
├── config/tasks.json     # 任务配置
└── hal/                  # GPIO HAL
    ├── gpio_hal.h        # 多型号 Pi GPIO 寄存器访问
    ├── gpio_hal.cpp      # BCM 编号/物理引脚映射
    ├── gpio_tci.h        # GPIO ↔ PLC ProcessImage 同步
    └── gpio_tci.cpp      # TCI 实现（%IX/%QX ↔ BCM GPIO）
```

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

## 配置

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `CYCLE_US` | 1000 | PLC 周期（微秒） |
