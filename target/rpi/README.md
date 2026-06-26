# target/rpi — Raspberry Pi PLC Runtime

## 路线

Linux 用户态 + PREEMPT_RT 内核。非裸机。

## 精度指标

| 内核 | 周期 | 抖动 |
|------|------|------|
| PREEMPT_RT (Ubuntu/Raspbian) | 1ms | <50us |
| 标准内核 | 10ms+ | <500us |
| SCHED_FIFO + mlockall | 1ms | <30us |

## 快速开始

### 1. 交叉编译（在 PC 上）

```bash
# 安装交叉编译器
sudo apt install gcc-arm-linux-gnueabihf g++-arm-linux-gnueabihf

# 编译
cd target/rpi
make                    # RPi 4
make RPI_VER=5          # RPi 5
make jitter-test        # 抖动测试版
```

### 2. 部署

```bash
./deploy.sh              # 编译 + 部署
./deploy.sh jitter       # 抖动测试版
./deploy.sh run          # 编译 + 部署 + 启动
```

或手动：

```bash
make deploy RPI_IP=192.168.5.50
```

### 3. 树莓派上运行

```bash
# 普通运行
sudo ./plc_runtime_rpi --cycle-us 1000

# 最高实时优先级
sudo chrt -f 99 ./plc_runtime_rpi --cycle-us 1000

# 安装为 systemd 服务
sudo systemctl enable plc-runtime
sudo systemctl start plc-runtime
```

## 示波器测抖动

1. 连接 GPIO18 (pin 12) 到示波器探头
2. 运行时每个 tick 翻转 GPIO18
3. 示波器测量上升沿间的周期抖动

GPIO 默认引脚：

| 功能 | GPIO | 物理引脚 |
|------|------|----------|
| Jitter 测试（tick 翻转） | 18 | 12 |
| LED 心跳 | 17 | 11 |
| 错误指示 | 27 | 13 |

## 编译 Java 编译器 + 完整流程

```bash
# 编译 ST → C++
cd java && mvn package -DskipTests
java -jar target/st2c-jar-with-dependencies.jar \
  --input ../../examples/test.st \
  --output-dir ../../output/flat/build

# 交叉编译运行时
cd ../target/rpi
make
```
