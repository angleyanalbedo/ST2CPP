# target/ — 平台特定运行时入口

各子目录对应不同硬件平台：

| 目录 | 平台 | 说明 |
|------|------|------|
| `desktop/` | Windows / macOS | 跨平台桌面测试，`runtime_main.cpp` |
| `windows/` | Windows | MinGW 专用，QPC + WaitableTimer |
| `linux/` | Linux 通用 | timerfd + SCHED_FIFO |
| `rpi/` | Raspberry Pi | GPIO + POSIX timer |
| `stm32f1/` | STM32F103C8T6 | 裸机，20KB RAM / 64KB Flash |
| `stm32h7/` | STM32H7 | 裸机，DWT + FPU |

共享源文件（`target/` 根目录）：

- `pou_stub.cpp` — POU 存根，用于无编译器输出的最小构建
- `network_tci.h` / `network_tci.cpp` — TCP 网络 TCI 实现

---

## NetworkTCI — TCP 网络 I/O

NetworkTCI 将耗时 socket I/O 放到后台线程，PLC 周期中只做 `memcpy`，保证确定性：

```
PLC 周期 (微秒级)
  ├── syncInputs()  ←  memcpy(rxBuf → ProcessImage)
  ├── 用户逻辑      ←  读写 ProcessImage
  └── syncOutputs() →  memcpy(ProcessImage → txBuf)
          ↕
后台线程 (非实时)
  ├── select() + recv() → 写入 rxBuf
  └── select() + send() ← 读取 txBuf
```

### 构建

各平台 Makefile 已显式包含 `../network_tci.cpp`。Windows 需 `-lws2_32`（已配置）。

```bash
# Windows
cd target/windows && make stub

# Desktop (跨平台)
cd target/desktop && make stub

# Linux
cd target/linux && make stub

# RPi
cd target/rpi && make stub
```

### 用法示例

```cpp
#include "network_tci.h"

rt_plc::Runtime rt;
rt_plc::CompositeTCI composite;

rt_plc::NetworkTCI net;

// 客户端模式：连接远程服务器
net.begin("192.168.1.100", 5000);

// 服务端模式：监听本地端口
// net.begin(502);

// 映射 PLC 地址 ↔ 网络报文偏移
//              (plcByte, netByte, length)
net.mapInput(0, 0, 2);    // %IW0 ← 网络 byte 0-1
net.mapOutput(0, 4, 2);   // %QW0 → 网络 byte 4-5

// 注册到运行时
composite.add(&net);
rt.setTCI(&composite);

rt.init();
rt.start();

while (rt.running()) {
    rt.scanOnce();
}
```

对应的 ST 代码：

```
VAR
    sensor AT %IW0 : INT;   // ← 来自网络
    cmd    AT %QW0 : INT;   // → 发送到网络
END_VAR
```

### 多区域映射

```cpp
net.mapInput(0, 0, 4);     // %ID0     ← 网络 byte 0-3
net.mapInput(8, 4, 2);     // %IW4     ← 网络 byte 4-5
net.mapInput(10, 6, 1);    // %IB5     ← 网络 byte 6
net.mapOutput(0, 0, 8);    // %QD0+QW2 → 网络 byte 0-7
```

### 自动重连

`begin()` 第三个参数控制重连间隔（毫秒），0 表示不重连：

```cpp
net.begin("10.0.0.1", 502, 5000);   // 每 5 秒重连
net.begin("10.0.0.1", 502, 0);      // 不重连
```

### 诊断

```cpp
bool     ok   = net.isConnected();
uint64_t rx   = net.rxBytes();
uint64_t tx   = net.txBytes();
uint32_t recon = net.reconnectCount();
```

### 注意事项

- **非安全**：TCP 无加密，适合内部网络
- **缓冲区**：固定 4KB（`NET_BUF_SIZE`），收发共享
- **后台线程**：`select()` 100ms 超时检查退出标志
- **线程安全**：`syncInputs/syncOutputs` 与后台线程用 `std::mutex` 保护
- **平台**：`#ifdef _WIN32` 处理 WinSock2 / POSIX 差异，跨平台可用

### 测试

```bash
# Windows
cd target/windows && make network-test

# Desktop
cd target/desktop && make network-test

# Linux
cd target/linux && make network-test
```

测试启动 echo server，NetworkTCI 客户端连接后发送 `0123456789ABCDEF`，验证回显一致。
