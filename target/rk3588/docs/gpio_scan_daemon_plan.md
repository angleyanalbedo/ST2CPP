# RK3588 GPIO TCI 方案演进

## 最终方案：`/dev/mem` 直读寄存器（2026-07-07 固化）

### 架构

```
Alchemy RT 任务 (primary mode, cpu6)
  └─ TCI::syncInputs()
       └─ uint32_t ext = m_gpioRegs[0x08/4];   ← 一条 load 指令
       └─ 位提取 + active_low 映射
       └─ ProcessImage::writeInputBit()
```

- 不需要扫描线程
- 不需要 cdev ioctl
- 不需要共享内存 / 原子变量
- 不需要域切换

### 寄存器发现

通过 `/dev/mem` 扫描 gpio4 寄存器空间（0xFEC50000，256 字节）发现：

| 偏移 | 寄存器 | 值（示例） | 说明 |
|:----:|:------:|:----------:|------|
| 0x00 | DR | 0x00000004 | 数据寄存器（读写） |
| 0x04 | DDR | 0x00000000 | 方向寄存器（pinctrl 控制） |
| **0x08** | **EXT_PORT** | **0x00000204** | **外部端口 — 实时读引脚状态** |
| 0x30 | — | 0x00003002 | 版本/ID 寄存器 |

- EXT_PORT 偏移 0x08 在所有 5 个 bank 上一致
- 读稳定性：10000 次连续读取 0 次变化
- pinctrl 拦截 DDR/DR 写入 → `/dev/mem` 只读，写走 cdev（当前不可用）

### MatriBox GPIO 引脚布局

| 引脚 | 标号 | bit | 方向 | 用途 | 活跃状态 |
|:----:|:----:|:---:|:----:|:----:|:--------:|
| gpio139 | GPIO4_B3 | 11 | 输出 | `%QX0.0` (LED) | 高电平有效（写暂不可用） |
| gpio140 | GPIO4_C4 | 12 | 输入 | `%IX0.0` (plc_run) | 低电平有效 |
| gpio141 | GPIO4_C5 | 13 | 输入 | `%IX0.1` (plc_stop) | 低电平有效 |

### 对比：三种 GPIO 方案实测

**测试条件**：Alchemy STRESS_LOAD=5 @ 1ms，30 秒运行

| 指标 | **`/dev/mem` 直读** | GPIO cdev 扫描线程 | GPIO cdev 直接调用 |
|:----:|:-------------------:|:-------------------:|:------------------:|
| Max jitter | **+10.3µs** | +4.1 **秒** | 系统崩溃 |
| Std | **1.0µs** | 无效 | — |
| Overrun | **0** | 50+ | — |
| syscall/cycle | **0** | 1-2 (ioctl) | 2 (ioctl) |
| SSH 存活 | ✅ | ❌ | ❌ |
| 输出写支持 | ❌ (pinctrl) | ✅ (cdev) | ✅ (cdev) |

### 结论

- **输入读**：`/dev/mem` EXT_PORT[0x08] 直读是唯一 RT-safe 路径，零 jitter 代价
- **输出写**：当前无 RT-safe 方案，pinctrl 阻止 `/dev/mem` 写，cdev ioctl 导致系统崩溃
- **下一步**：RTDM GPIO 驱动（纯 primary mode 寄存器读写）或硬件 I/O 扩展（SPI/I2C GPIO expander by FPGA/MCU）

---

## 已废弃方案：GPIO Scan Daemon

### 设计目标

将 GPIO cdev ioctl 从 Alchemy RT 周期中剥离到 Linux 域线程，通过共享内存交换数据。

```text
Linux 域 (SCHED_OTHER)
  GpioScanDaemon ← atomic 共享内存 → Alchemy RT 任务 (FIFO/90)
  ioctl(gpiochip)                     syncInputs/syncOutputs
```

`hal/gpio_scan.h/cpp` 中的 `GpioScanDaemon` 实现已完整包含：
- `std::atomic<int>` 共享缓冲区
- `clock_nanosleep(TIMER_ABSTIME)` 周期性扫描
- `readInput()` / `writeOutput()` API

### 废弃原因

即使将 cdev ioctl 完全隔离到 Linux 域的低优先级线程，**任何 GPIO cdev ioctl 都会导致系统崩溃**：
1. 最轻量扫描配置（SCHED_OTHER, 不同 CPU, 100µs sleep）→ max jitter 4.1s
2. SSHD 在 10 秒内不可响应
3. 需要物理断电恢复

根因：gpiolib-cdev 内核路径使用常规 mutex/spinlock，与 Xenomai Cobalt IRQPIPE 的域切换机制不兼容。并非调度架构问题，而是内核 GPIO 子系统与 Xenomai 的根本冲突。

### 文件状态

| 文件 | 状态 | 说明 |
|:----|:----:|------|
| `hal/gpio_hal.h` | ❌ 已删除 | cdev 接口封装，不再使用 |
| `hal/gpio_hal.cpp` | ❌ 已删除 | cdev 实现 |
| `hal/gpio_scan.h` | 保留 | GpioScanDaemon 参考实现 |
| `hal/gpio_scan.cpp` | 保留 | 含 std::atomic 和 sleep 调度 |
| `hal/gpio_tci.h` | ✅ 当前 | `/dev/mem` 直读 TCI |
| `hal/gpio_tci.cpp` | ✅ 当前 | EXT_PORT 直读实现 |
| `docs/gpio_scan_daemon_plan.md` | ✅ 本文件 | 演进记录 |
