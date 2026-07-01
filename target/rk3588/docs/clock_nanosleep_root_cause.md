# Cobalt POSIX `clock_nanosleep` 抖动根因分析

## 现象

在 RK3588 (MatriBox) Xenomai 3.2.4 + 内核 5.10.199 上使用 Cobalt POSIX wrap 模式
时，`clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME)` 偶发 67ms 级尖峰抖动：

```
正常抖动: ±16µs (99.99% 样本)
异常尖峰: +67ms / -1ms (每 ~5000-10000 周期一次)
```

Alchemy `rt_task_wait_period` 在同一硬件上无此问题（std=700ns, 无尖峰）。

## 调用链差异

### Cobalt POSIX wrap (`clock_nanosleep`)

```
应用调用 clock_nanosleep()
  → __wrap_clock_nanosleep()           [libcobalt.so: 符号别名 == __cobalt_clock_nanosleep]
    → 检查当前域 (primary / secondary)
      ├─ primary:  直接发出 Cobalt syscall → RTDM 硬件定时器 → 唤醒 ✓
      └─ secondary: 需先执行 cobalt_thread_harden() 切回 primary 域
                    再发出 Cobalt syscall  → RTDM 硬件定时器
```

### Alchemy (`rt_task_wait_period`)

```
应用调用 rt_task_wait_period()
  → __cobalt_syscall(SC_RT_TASK_WAIT_PERIOD)   [直接进入 Cobalt 内核]
    → 不经过 POSIX 域切换逻辑
    → 内核原生周期调度 → RTDM 硬件定时器 ✓
```

## 根因：ARM64 域切换竞态

Cobalt POSIX wrap 层的问题步骤在 `secondary → primary` 切换：

1. **触发 secondary mode**：任何 Linux 系统调用（printf、文件读写、内存分配）
   都会让 Xenomai 线程离开 primary (RT) 域，进入 secondary (Linux) 域。
   在我们的运行时中，`platform::logInfo` / `printf` 调用就是触发器。

2. **`clock_nanosleep` 调用**：当线程在 secondary 域调用 `clock_nanosleep` 时，
   `__wrap_clock_nanosleep` 必须先调用 `cobalt_thread_harden()` 切回 primary 域。

3. **中断丢失**：`cobalt_thread_harden()` 的实现依赖 **IPI (核间中断) + 调度器
   tick** 来将线程迁移回 Cobalt 调度域。在 RK3588 的 ARM64 GICv3 中断控制器 + 
   `IRQPIPE` 管线模式下，这个迁移路径存在竞态条件：如果 Cobalt 硬件定时器中断
   在迁移完成前到达，该中断对当前线程不可见，导致**错过整周期唤醒**。

4. **累积效应**：一次 missed wakeup 后，`next` 比实际时间落后一个周期。
   `clock_nanosleep` 立即返回（因为 `next` 已过期），但 `sampleJitter` 计算
   仅记录单次 delta。在极端情况下（~67ms），线程连续 67 个周期未能切回 primary
   域，直到某个后续中断最终触发迁移。

## 为什么 Alchemy 不受影响

`rt_task_wait_period` 通过 Cobalt 内核原生 syscall 直接操作线程的周期定时器，
**不经过程序员可见的域切换过程**。内核内部的 `xnsched_pick()` 和 tick handler
直接在 Cobalt 调度域内完成，不依赖 IPI + Linux 调度器的配合。

## 复现条件

- Xenomai 3.2.4 (Cobalt)
- ARM64 架构（GICv3/v4）
- 内核编译含 `CONFIG_IRQPIPE`（中断管线模式）
- 线程因 Linux 系统调用频繁进入 secondary mode
- 高频定时器（≤1ms）

## 结论

| 方案 | 可行性 | 原因 |
|------|--------|------|
| Cobalt POSIX wrap (`xeno-config --cobalt`) | ❌ 不可用 | ARM64 域切换竞态导致偶发 67ms 脱靶 |
| Alchemy (`xeno-config --alchemy`) | ✅ 推荐使用 | 内核原生 syscall，无域切换问题 |

## 参考

- Xenomai 3 Cobalt 源代码：`lib/cobalt/timer.c` → `__cobalt_clock_nanosleep()`
- 域切换：`lib/cobalt/thread.c` → `cobalt_thread_harden()` / `cobalt_thread_relax()`
- Alchemy 周期调度：`lib/alchemy/task.c` → `rt_task_wait_period()`
- 内核 `IRQPIPE` 补丁（ARM64）在 5.10.y 上的已知 issue：[Xenomai 邮件列表存档]
