# target/rpi — Raspberry Pi PLC Runtime

## 精度指标

| 内核 | 周期 | 抖动 |
|------|------|------|
| 标准内核 | 1ms | Avg ~1000us, Min 48us, Max ~7ms |
| PREEMPT_RT | 1ms | <50us |

> 实测数据（标准内核 6.18.34，SCHED_FIFO/90）：2964 samples, Avg jitter 1001us

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

自动扫描 `output/flat/build/*.cpp`，提取所有 `registerPOU_xxx` 函数，生成 `pou_registry.gen.cpp`。

### Step 3: 部署到 Pi（PC 上）

**方式 A：paramiko Python 脚本（推荐，跨平台）**

```python
import paramiko, tarfile, tempfile, os

RPI_IP, RPI_USER, RPI_PASS = "192.168.5.128", "pi", "123456"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(RPI_IP, 22, RPI_USER, RPI_PASS, timeout=10)

# 打包：runtime-flat + platform + 编译器输出
tar_path = os.path.join(tempfile.gettempdir(), "st2c.tar.gz")
with tarfile.open(tar_path, "w:gz") as tar:
    tar.add("runtime-flat/include", arcname="runtime-flat/include")
    tar.add("runtime-flat/src", arcname="runtime-flat/src")
    tar.add("target/rpi/platform_rpi.cpp", arcname="target/rpi/platform_rpi.cpp")
    tar.add("target/rpi/runtime_rpi.cpp", arcname="target/rpi/runtime_rpi.cpp")
    for f in os.listdir("output/flat/build"):
        if f.endswith(".cpp"):
            tar.add(f"output/flat/build/{f}", arcname=f"output/flat/build/{f}")

remote_dir = client.exec_command("pwd")[1].read().decode().strip() + "/st2c-runtime"
client.exec_command(f"mkdir -p {remote_dir}")
sftp = client.open_sftp()
sftp.put(tar_path, f"{remote_dir}/st2c.tar.gz")
sftp.close()
```

**方式 B：SSH 到 Pi 上 make**

```bash
ssh pi@192.168.5.128 "cd ~/st2c-runtime && make deploy"
```

### Step 4: Pi 上编译 + 运行

```bash
ssh pi@192.168.5.128

cd ~/st2c-runtime
tar xzf st2c.tar.gz

g++ -O2 -std=c++17 -DRT_PLATFORM_LINUX \
    -I runtime-flat/include \
    runtime-flat/src/*.cpp \
    target/rpi/platform_rpi.cpp target/rpi/runtime_rpi.cpp \
    output/flat/build/*.cpp \
    -lpthread -o plc_runtime_rpi

sudo ./plc_runtime_rpi --cycle-us 1000
```

预期输出：
```
PLC Runtime: 5 POU(s), cycle=1000us
Timer started: 1000us period

=== Jitter Statistics ===
  Samples    : 2964
  Min jitter : 48 us
  Avg jitter : 1001 us
```

## 文件结构

```
target/rpi/
├── Makefile              # 编译+部署（Pi 上用）
├── gen_registry.py       # 生成 registerAllPOUs 桥接（PC 上用）
├── platform_rpi.cpp      # 平台 HAL（定时器/GPIO/RT优先级）
├── runtime_rpi.cpp       # PLC 运行时入口
├── config/tasks.json     # 任务配置
└── hal/                  # HAL 扩展
```

## 配置

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `RPI_IP` | 192.168.5.128 | Pi 的 IP 地址 |
| `RPI_USER` | pi | SSH 用户名 |
| `RPI_DIR` | ~/st2c-runtime | 远端部署目录 |
| `CYCLE_US` | 1000 | PLC 周期（微秒） |

## GPIO 引脚

| 功能 | GPIO | 物理引脚 |
|------|------|----------|
| Jitter 测试（tick 翻转） | 18 | 12 |
| LED 心跳 | 17 | 11 |
| 错误指示 | 27 | 13 |
