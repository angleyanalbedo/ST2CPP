# EtherCAT 使用指南

## 概述

本项目通过 SOEM（Simple Open EtherCAT Master）将 EtherCAT 从站设备接入 PLC 运行时。
你只需在 ST 代码中用 `AT %IW`/`AT %QW` 地址声明变量，运行时自动完成 EtherCAT 数据交换。

## 快速开始

### 1. 安装依赖（首次）

```bash
# 在 Pi 4 上
sudo apt install libpcap-dev build-essential

# 克隆 SOEM
cd ~/st2c-runtime/target/rpi
git clone https://github.com/OpenEtherCATsociety/soem.git soem
```

### 2. 编译

```bash
cd ~/st2c-runtime/target/rpi
make ethercat
```

### 3. 运行

```bash
# 优化实时性能
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
sudo systemctl stop irqbalance

# 运行（eth0 为板载网口）
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci ethercat --ecat-if eth0
```

## 完整工作流程

```
┌─────────────────────────────────────────────────────────────────────┐
│  Step 1: 查看从站 PDO 结构                                          │
│  Step 2: 修改 pdo_config.h 映射表                                   │
│  Step 3: 写 ST 代码（AT %IW/%QW 地址）                              │
│  Step 4: 编译 ST → C++（Java 编译器）                                │
│  Step 5: 编译 C++ → 可执行文件（g++）                                │
│  Step 6: 运行                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Step 1：查看从站 PDO 结构

接好 EtherCAT 从站（伺服驱动器、IO 模块等），用网线连接 Pi 4 的 eth0。

用 SOEM 自带的示例程序查看从站信息：

```bash
cd ~/st2c-runtime/target/rpi/soem/samples/simple_test
make
sudo ./simple_test eth0
```

输出示例：
```
Found 1 slaves
Slaves mapped: 1
slave 1: name=AX58100 Output size: 8bytes Input size: 8bytes
  SM0: len=8, type=0  (outputs)
  SM1: len=8, type=0  (inputs)
  RXPDO: 0x1600 ControlWord(2B), TargetVelocity(2B), TargetPosition(4B)
  TXPDO: 0x1A00 StatusWord(2B), ActualVelocity(2B), ActualPosition(4B)
```

记录下每个 PDO 条目的：
- **方向**：TXPDO = 从站→主站（PLC 输入），RXPDO = 主站→从站（PLC 输出）
- **字节大小**：2B = INT，4B = DINT/REAL
- **PDO 内字节偏移**：从 0 开始的偏移量

## Step 2：修改 pdo_config.h

编辑 `target/rpi/pdo_config.h`：

```cpp
struct PdoMapping {
    int slaveIdx;       // 从站编号（1-based，第一个从站 = 1）
    int pdoByteOff;     // PDO 内字节偏移（从 Step 1 获取）
    int plcByteOff;     // ProcessImage 字节偏移（对应 ST 的 AT 地址）
    int plcBitOff;      // 位偏移（0-7，-1 表示多字节变量）
    int sizeBytes;      // 数据大小（字节）
};

// ═══ 示例：单个伺服驱动器 ═══
// TXPDO（从站→主站，PLC 读取）：
//   StatusWord     2B → %IW0 (字节偏移 0)
//   ActualVelocity 2B → %IW1 (字节偏移 2)
//   ActualPosition 4B → %ID2 (字节偏移 4)
//
// RXPDO（主站→从站，PLC 写入）：
//   ControlWord    2B → %QW0 (字节偏移 0)
//   TargetVelocity 2B → %QW1 (字节偏移 2)
//   TargetPosition 4B → %QD2 (字节偏移 4)

static const PdoMapping ECAT_INPUT_MAP[] = {
    // { 从站号, PDO字节偏移, PLC字节偏移, PLC位偏移(-1=多字节), 大小 }
    { 1, 0,  0, -1, 2 },   // StatusWord     → %IW0
    { 1, 2,  2, -1, 2 },   // ActualVelocity → %IW1
    { 1, 4,  4, -1, 4 },   // ActualPosition → %ID2
};

static const PdoMapping ECAT_OUTPUT_MAP[] = {
    { 1, 0,  0, -1, 2 },   // ControlWord    → %QW0
    { 1, 2,  2, -1, 2 },   // TargetVelocity → %QW1
    { 1, 4,  4, -1, 4 },   // TargetPosition → %QD2
};
```

### PLC 字节偏移规则

| ST 地址 | PLC 字节偏移 | 说明 |
|---------|-------------|------|
| `%IW0` | 0 | 字地址 0 → 字节 0 |
| `%IW1` | 2 | 字地址 1 → 字节 2 |
| `%ID0` | 0 | 双字地址 0 → 字节 0 |
| `%ID1` | 4 | 双字地址 1 → 字节 4 |
| `%IB0` | 0 | 字节地址 0 → 字节 0 |
| `%IX0.3` | 0, bit 3 | 字节 0 第 3 位 |

**公式**：字地址 × 2 = 字节偏移，双字地址 × 4 = 字节偏移。

## Step 3：写 ST 代码

```st
PROGRAM servo_control
VAR
    // ═══ 输入变量（从 EtherCAT 从站读取）═══
    StatusWord      AT %IW0  : INT;
    ActualVelocity  AT %IW1  : INT;
    ActualPosition  AT %ID2  : DINT;

    // ═══ 输出变量（写入 EtherCAT 从站）═══
    ControlWord     AT %QW0  : INT;
    TargetVelocity  AT %QW1  : INT;
    TargetPosition  AT %QD2  : DINT;

    // ═══ 内部逻辑变量 ═══
    target_speed    : INT := 1500;
    run_flag        : BOOL := FALSE;
END_VAR

// 伺服使能
IF run_flag THEN
    ControlWord := 16#000F;    // CiA 402 状态机：Operation Enabled
    TargetVelocity := target_speed;
ELSE
    ControlWord := 16#0000;    // 关闭伺服
    TargetVelocity := 0;
END_IF;

// 读取实际状态
// ActualVelocity 和 ActualPosition 由 EtherCAT 自动更新
```

### 常见 AT 地址格式

| 格式 | 类型 | 大小 | 示例 |
|------|------|------|------|
| `%IX0.0` | BOOL | 1 bit | 传感器开关 |
| `%IW0` | INT | 2 bytes | 速度、力矩 |
| `%ID0` | DINT | 4 bytes | 位置 |
| `%QX0.0` | BOOL | 1 bit | 继电器、LED |
| `%QW0` | INT | 2 bytes | 目标速度 |
| `%QD0` | DINT | 4 bytes | 目标位置 |

## Step 4-5：编译

```bash
# PC 上：ST → C++
cd java && mvn package -DskipTests
java -jar target/st2c-jar-with-dependencies.jar \
  --input ../examples/servo.st \
  --output-dir ../output/flat/build

# Pi 上：C++ → 可执行文件
cd ~/st2c-runtime/target/rpi
make ethercat
```

## Step 6：运行

```bash
taskset -c 3 sudo ./plc_runtime_ecat \
  --cycle-us 1000 \
  --tci ethercat \
  --ecat-if eth0
```

预期输出：
```
[EtherCAT] Initializing on eth0...
[EtherCAT] Network interface opened
[EtherCAT] Found 1 slave(s)
  Slave 1: AX58100
[EtherCAT] PDO mapping: WKC=3
[EtherCAT] All slaves OPERATIONAL
[GPIO] Model: Raspberry Pi 4 (BCM2711) | Pins: 40
TCI: 1 backend(s) active
PLC Runtime: 1 POU(s), cycle=1000us
Timer started: 1000us period
```

## 命令行参数

```bash
./plc_runtime_ecat [选项]

选项：
  --cycle-us N       PLC 扫描周期（微秒），默认 1000
  --tci MODE         I/O 模式：
                       gpio      - 仅 GPIO（默认）
                       ethercat  - 仅 EtherCAT
                       both      - GPIO + EtherCAT 同时
  --ecat-if NAME     EtherCAT 网口名，默认 eth0
  --help             显示帮助
```

## 多从站配置

如果有多个从站（比如 2 个伺服 + 1 个 IO 模块）：

```cpp
static const PdoMapping ECAT_INPUT_MAP[] = {
    // 从站 1：伺服
    { 1, 0,  0, -1, 2 },   // Slave1 StatusWord    → %IW0
    { 1, 2,  2, -1, 2 },   // Slave1 ActualVel     → %IW1
    { 1, 4,  4, -1, 4 },   // Slave1 ActualPos     → %ID2

    // 从站 2：伺服
    { 2, 0,  8, -1, 2 },   // Slave2 StatusWord    → %IW4
    { 2, 2, 10, -1, 2 },   // Slave2 ActualVel     → %IW5
    { 2, 4, 12, -1, 4 },   // Slave2 ActualPos     → %ID6

    // 从站 3：IO 模块（8 路数字输入）
    { 3, 0, 16, -1, 1 },   // Slave3 DigitalIn     → %IB8
};

static const PdoMapping ECAT_OUTPUT_MAP[] = {
    // 从站 1：伺服
    { 1, 0,  0, -1, 2 },   // Slave1 ControlWord   → %QW0
    { 1, 2,  2, -1, 2 },   // Slave1 TargetVel     → %QW1
    { 1, 4,  4, -1, 4 },   // Slave1 TargetPos     → %QD2

    // 从站 2：伺服
    { 2, 0,  8, -1, 2 },   // Slave2 ControlWord   → %QW4
    { 2, 2, 10, -1, 2 },   // Slave2 TargetVel     → %QW5
    { 2, 4, 12, -1, 4 },   // Slave2 TargetPos     → %QD6

    // 从站 3：IO 模块（8 路数字输出）
    { 3, 0, 17, -1, 1 },   // Slave3 DigitalOut    → %QB8
};
```

## GPIO + EtherCAT 同时使用

```bash
taskset -c 3 sudo ./plc_runtime_ecat --tci both --ecat-if eth0
```

典型场景：
- **EtherCAT**：连接远端伺服驱动器（`%IW`/`%QW`）
- **GPIO**：本地紧急停止按钮、安全继电器（`%IX`/`%QX`）

ST 代码中两组地址同时有效：
```st
VAR
    // EtherCAT 从站
    ServoStatus  AT %IW0  : INT;     // EtherCAT 伺服状态
    ServoTarget  AT %QW1  : INT;     // EtherCAT 伺服目标速度

    // 本地 GPIO
    EStop        AT %IX0.0 : BOOL;   // 急停按钮（GPIO Pin 29）
    MotorRelay   AT %QX0.0 : BOOL;   // 电机继电器（GPIO Pin 11）
END_VAR
```

## 故障排查

### "No slaves found"

- 检查网线是否连接到 eth0（`ip link show eth0`）
- 确认从站已上电
- 尝试 `sudo ip link set eth0 up` 启用网口

### "ecx_init failed"

- 需要 root 权限：`sudo ./plc_runtime_ecat ...`
- 确认 libpcap 已安装：`dpkg -l | grep libpcap`

### "WKC=0, comm lost"

- 从站断开连接或不在 OP 状态
- 检查 EtherCAT 电缆和从站供电
- 用 `simple_test` 确认从站能正常进入 OP

### 编译报错 "soem/soem.h: No such file"

- SOEM 未克隆：`cd target/rpi && git clone https://github.com/OpenEtherCATsociety/soem.git soem`

### 运行时抖动过大

```bash
# 确认优化已生效
cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor  # 应显示 performance
cat /sys/bus/workqueue/cpumask  # 检查 IRQ 亲和性
```
