# target/rpi — Raspberry Pi PLC Runtime

## 路线

Linux 用户态 + PREEMPT_RT 内核。非裸机。

## 快速开始

### 1. 编译 ST → C++（在 PC 上）

```bash
cd java && mvn package -DskipTests
java -jar target/st2c-jar-with-dependencies.jar \
  --input ../../examples/test.st \
  --output-dir ../../output/flat/build
```

### 2. 编译运行时

```bash
cd target/rpi

# 本地编译（在 Pi 上）
make

# 交叉编译（在 PC 上）
make CROSS=aarch64-linux-gnu-
```

Makefile 自动完成：
- 扫描 `output/flat/build/*.cpp`（编译器生成的 POU 代码）
- 自动生成 `pou_registry.gen.cpp`（registerAllPOUs 桥接）
- 编译所有源文件

### 3. 部署到 Pi

```bash
# 一键编译+上传
make deploy

# 上传+运行
make run

# 指定 Pi 的 IP
RPI_IP=192.168.1.100 make deploy
```

### 4. 在 Pi 上运行

```bash
sudo ./plc_runtime_rpi --cycle-us 1000
```

## 配置

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `RPI_IP` | 192.168.5.128 | Pi 的 IP 地址 |
| `RPI_USER` | pi | SSH 用户名 |
| `RPI_DIR` | ~/st2c-runtime | 远端部署目录 |
| `CYCLE_US` | 1000 | PLC 周期（微秒） |
| `CROSS` | (空) | 交叉编译前缀 |

## 示波器测抖动

1. 连接 GPIO18 (pin 12) 到示波器探头
2. 运行时每个 tick 翻转 GPIO18
3. 示波器测量上升沿间的周期抖动

| 功能 | GPIO | 物理引脚 |
|------|------|----------|
| Jitter 测试 | 18 | 12 |
| LED 心跳 | 17 | 11 |
| 错误指示 | 27 | 13 |
