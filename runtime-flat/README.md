# runtime-flat — PLC 核心运行时静态库

IEC 61131-3 硬实时运行时核心，平台无关。

## 构建

```bash
cd runtime-flat && mkdir build && cd build
cmake .. -G "MinGW Makefiles"
cmake --build .

# 启用测试
cmake .. -G "MinGW Makefiles" -DRT_BUILD_TESTS=ON
cmake --build .

# 启用 NetworkTCI（TCP 网络 I/O）
cmake .. -G "MinGW Makefiles" -DRT_BUILD_NETWORK=ON
cmake --build .
```

## 用法

### 最小 PLC Runtime

```cpp
#include "rt_runtime.h"
#include "pou_registry.gen.h"

int main() {
    rt_plc::Runtime rt;
    rt.init();          // 加载 GVL + POU 注册表
    rt.start();         // 启动调度器

    while (rt.running()) {
        rt.scanOnce();  // ReadInputs → LogicSolve → WriteOutputs
    }

    rt.stop();
    return 0;
}
```

### TCI 硬件抽象

TCI (`Target Communication Interface`) 将物理 IO 与 PLC 周期解耦：

```
PLC 扫描周期
  ├── TCI::syncInputs()   ← 硬件 → ProcessImage（非阻塞）
  ├── 用户逻辑执行         ← 读写 ProcessImage
  └── TCI::syncOutputs()  → ProcessImage → 硬件（非阻塞）
```

#### 组合多个 TCI

```cpp
rt_plc::CompositeTCI composite;
composite.add(&ethercatTCI);
composite.add(&gpioTCI);
rt.setTCI(&composite);
```

## NetworkTCI — TCP 网络 I/O

NetworkTCI 是 TCI 的 TCP 实现。后台线程维护 socket 连接，PLC 周期中只做 `memcpy`：

```
PLC 周期 (微秒级, 确定性)
  ├── syncInputs()  ←  memcpy(rxBuf → ProcessImage)
  ├── 用户逻辑      ←  读写 ProcessImage
  └── syncOutputs() →  memcpy(ProcessImage → txBuf)
          ↕
NetworkTCI 后台线程 (非实时)
  ├── select() + recv() → 写入 rxBuf
  └── select() + send() ← 读取 txBuf（PLC 周期标记后发送）
```

### 客户端模式用法

```cpp
#include "network_tci.h"

rt_plc::NetworkTCI net;

// 连接到远程 TCP 服务器（自动重连，间隔 3 秒）
net.begin("192.168.1.100", 5000);

// 映射 PLC 地址 ↔ 网络报文偏移
// ST 中的 %IW0（PLC byte 0-1）对应网络报文 byte 0-1
net.mapInput(0, 0, 2);    // 网络 → PLC: 网络 byte 0 写回 PLC byte 0，2 字节
// ST 中的 %QW0（PLC byte 0-1）对应网络报文 byte 4-5
net.mapOutput(0, 4, 2);   // PLC → 网络: PLC byte 0 发送到网络 byte 4，2 字节

// 注册到运行时
composite.add(&net);
rt.setTCI(&composite);

// 之后 ST 用户代码正常读写 %IW0 / %QW0：
//   sensor AT %IW0 : INT;   // ← 网络接收的数据
//   cmd    AT %QW0 : INT;   // → 网络发送的数据
```

### 服务端模式用法

```cpp
// 监听本地端口，接受一个 TCP 客户端连接
net.begin(502);  // Modbus TCP 端口
```

### 多区域映射

```cpp
// 多个不连续区域可分别映射
net.mapInput(0, 0, 4);     // %ID0 (DWORD) ← 网络 byte 0-3
net.mapInput(8, 4, 2);     // %IW4 (INT)   ← 网络 byte 4-5
net.mapOutput(0, 0, 8);    // %QD0 (DWORD) → 网络 byte 0-3
net.mapOutput(8, 4, 2);    // %QW4 (INT)   → 网络 byte 4-5
```

### 诊断

```cpp
bool connected = net.isConnected();   // 连接状态
uint64_t rx    = net.rxBytes();       // 已接收字节数
uint64_t tx    = net.txBytes();       // 已发送字节数
uint32_t recon = net.reconnectCount(); // 重连次数
```

### 注意事项

- NetworkTCI 是**非安全**实现（TCP 无加密），适合内部网络
- 后台线程用 `select()` + 非阻塞 socket，100ms 超时检查退出标志
- 重连间隔默认 3 秒，构造函数可配
- 网络缓冲区固定 4KB（`NET_BUF_SIZE`），收发共享
- 多平台支持：`#ifdef _WIN32` 处理 WinSock2 / POSIX 差异

## 平台支持

| 平台 | socket | 线程 | 自动包含 |
|------|--------|------|----------|
| Windows (MinGW) | ws2_32 | winmm | 是（wildcard） |
| Linux | libc | pthread | 是（wildcard） |
| RPi | libc | pthread | 是（wildcard） |
| STM32F1 | 无 | 无 | 否（手动列表） |
