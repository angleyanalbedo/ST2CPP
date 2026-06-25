# PLC 运行时架构知识库

> 本文档整理了 PLC 运行时的主流架构设计、工业界实践、以及 ST2C++ 编译型运行时的定位分析。

---

## 目录

1. [PLC 运行时的 4 种架构](#1-plc-运行时的-4-种架构)
2. [主流方案：字节码解释型（CODESYS）](#2-主流方案字节码解释型codesys)
3. [编译型方案：ST2C++ 的设计](#3-编译型方案st2c-的设计)
4. [CODESYS 运行时架构详解](#4-codesys-运行时架构详解)
5. [实时性分析](#5-实时性分析)
6. [速度对比](#6-速度对比)
7. [市场格局与定位](#7-市场格局与定位)
8. [ST2C++ 运行时设计评审](#8-st2c-运行时设计评审)

---

## 1. PLC 运行时的 4 种架构

PLC 运行时按执行方式可分为 4 种主流架构：

```
                    执行方式
              ┌───────────────────┐
              │                   │
         解释执行              编译执行
              │                   │
     ┌────────┴────────┐  ┌──────┴──────┐
     │                 │  │             │
  字节码解释器      AST 遍历   AOT 编译     JIT 编译
  (CODESYS)      (简单PLC)  (ST2C++)   (部分SoftPLC)
```

### 1.1 字节码解释型

```
ST 源码 → 编译为字节码 → 下载到运行时 → 虚拟机解释执行

┌──────────────┐
│  字节码 VM    │  ← 运行时是通用容器
│  ┌──────────┐ │
│  │ 解码器   │ │  ← 每条指令：解码 → 查表 → 执行
│  │ 变量表   │ │  ← 运行时维护符号表
│  │ 栈帧     │ │  ← 虚拟机栈
│  └──────────┘ │
└──────────────┘
```

- ✅ 灵活：在线下载、热更新、远程调试
- ❌ 慢：每条指令都有解释开销
- 代表：CODESYS、Beckhoff TwinCAT、Siemens TIA（部分）

### 1.2 AOT 编译型

```
ST 源码 → 编译为 C/C++ → 编译为原生机器码 → 直接执行

┌──────────────┐
│  原生代码     │  ← 运行时是静态链接库
│  ┌──────────┐ │
│  │ 直接执行 │ │  ← CPU 直接跑机器码
│  │ 偏移访问 │ │  ← 变量 = 内存偏移
│  │ inline   │ │  ← 内置函数全部内联
│  └──────────┘ │
└──────────────┘
```

- ✅ 最快：零解释开销，编译器充分优化
- ❌ 不灵活：改程序需要重新编译
- 代表：ST2C++、部分安全 PLC（编译后固件）

### 1.3 JIT 编译型

```
ST 源码 → 编译为 IR → 运行时 JIT 编译为机器码 → 执行

┌──────────────┐
│  JIT 引擎    │  ← 运行时包含编译器
│  ┌──────────┐ │
│  │ IR 缓存  │ │  ← 中间表示
│  │ 热点检测  │ │  ← 频繁执行的代码编译为原生
│  │ 寄存器分配│ │  ← 运行时优化
│  └──────────┘ │
└──────────────┘
```

- ✅ 兼顾：首次下载后 JIT 编译，后续原生速度
- ❌ 复杂：JIT 引擎本身有开销和不确定性
- 代表：部分基于 LLVM 的 SoftPLC 方案

### 1.4 解释遍历型

```
ST 源码 → 解析为 AST → 遍历 AST 逐节点执行

┌──────────────┐
│  AST 遍历器  │
│  ┌──────────┐ │
│  │ IfNode   │ │  ← if (cond) { visit(then) }
│  │ ForNode  │ │  ← for (...) { visit(body) }
│  │ AssignN  │ │  ← vars[name] = eval(expr)
│  └──────────┘ │
└──────────────┘
```

- ✅ 最简单：实现几十行就够
- ❌ 最慢：每条语句都要遍历树结构
- 代表：教学用 PLC、Arduino 上的简易 PLC 解释器

### 1.5 四种架构对比

| 维度 | 字节码解释 | AOT 编译 | JIT 编译 | AST 遍历 |
|------|-----------|----------|----------|----------|
| 执行速度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐ |
| 启动速度 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 在线更新 | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 实时确定性 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| 实现复杂度 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ |
| 内存占用 | 中 | 最小 | 大 | 小 |
| 调试能力 | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 2. 主流方案：字节码解释型（CODESYS）

### 2.1 为什么字节码型统治市场

CODESYS 市场占有率：全球 500+ 厂商用它的运行时，包括 ABB、施耐德、博世力士乐、倍福、研华、和利时等。

核心原因不是技术，是**商业模式**：

```
字节码型的商业逻辑：

  运行时厂商卖的是"通用运行时"  ← 一套代码跑所有硬件
  硬件厂商买的是"授权"          ← 不用自己写 PLC 内核
  用户买的是"许可证"            ← 换硬件不用改程序

  三方都满意 → 市场垄断
```

| 厂商角色 | 做什么 | 买什么 |
|---------|--------|--------|
| 运行时厂商 (如 3S) | 卖运行时授权 | — |
| 硬件厂商 (如 ABB) | 卖 PLC 硬件 | 买 CODESYS 授权 |
| 终端用户 | 写 PLC 程序 | 买硬件 + IDE 许可 |

硬件厂商不需要养一个编译器团队，直接买 CODESYS 授权就能出货。

### 2.2 主流 PLC 运行时产品

| 产品 | 厂商 | 架构 | 特点 |
|------|------|------|------|
| CODESYS Control | 3S-Smart Software | 字节码解释 | 最广泛，500+ 硬件厂商授权 |
| TwinCAT 3 Runtime | Beckhoff | 字节码 + 部分编译 | Windows 生态，XAE 集成 |
| TIA Portal Runtime | Siemens | 字节码 | 西门子专属生态 |
| RSLogix / Studio 5000 | Rockwell | 字节码 | 美系 PLC 主流 |
| Unity Pro / EcoStruxure | Schneider | 字节码 | 基于 CODESYS 或自研 |
| OpenPLC | 开源 | 解释/编译混合 | 教育和小型项目 |

---

## 3. 编译型方案：ST2C++ 的设计

### 3.1 设计目标

ST2C++ 是一个 **ST（Structured Text）到 C++ 的编译器**，其运行时采用 AOT 编译型架构：

```
ST 源码 → ST2C++ 编译器 → 生成 C++ 源码 → GCC/Clang 编译 → 原生机器码
         ↑                    ↑
      前端/IR              后端代码生成
```

核心设计原则：
- **零运行时开销**：无堆分配、无 map 查找、无 dynamic_cast
- **变量为原生 C++ 类型**：直接内存偏移访问
- **编译器生成代码直接 `#include` 运行时头文件**：无需构建运行时库

### 3.2 运行时结构

```
Runtime/rt/
├── rt_plc.h          ← 基础层：类型 + 功能块 + 内置函数 + 错误处理
└── rt_runtime.h      ← 调度层：多任务调度器 + GVL + PROGRAM 生命周期
```

**rt_plc.h** 包含：
- IEC 61131-3 基本类型（SINT~LINT, REAL/LREAL, BOOL, TIME, DATE, DT 等）
- 位操作（AND/OR/XOR/SHL/SHR/ROL/ROR）
- 过程映像（ProcessImage）+ I/O 地址宏
- TCI 硬件抽象接口
- 错误处理（ErrorManager + 安全除法/数组访问/MOD）
- 数学函数（ABS/SQRT/LN/LOG/MIN/MAX/MOD/EXPT/LIMIT/SEL/MUX）
- 边沿检测（R_TRIG / F_TRIG）
- 标准功能块（TON/TOF/TP/CTU/CTD/CTUD）
- 复合类型（ARRAY/SUBRANGE/ENUM/STRUCT）
- 字符串操作（CONCAT/LEN/LEFT/RIGHT/MID/INSERT/DELETE/REPLACE/FIND）
- REF/POINTER 支持
- 互斥锁（plc_lock）

**rt_runtime.h** 包含：
- GVL（全局变量表）+ RETAIN 区域
- PROGRAM 实例生命周期（INIT → PRE → FIRST_SCAN → CYCLIC → POST）
- Task 定义（Cyclic / Event / Freewheeling）
- Scheduler 调度器（分阶段扫描：ReadInputs → LogicSolve → WriteOutputs → Housekeeping）
- 看门狗（任务级 + 全局）
- 诊断统计（DiagStats）
- 系统状态机（STOP → STARTING → RUN → STOPPING → ERROR → PAUSED）

### 3.3 零开销设计选择

| 设计选择 | 速度收益 | 对应字节码型的开销 |
|---------|---------|------------------|
| `GVL::read<T>(offset)` — memcpy 偏移量访问 | 1 条 load 指令 | 符号表查找 + 间接寻址 |
| `ProcessImage` — 固定 64KB 数组 | 缓存友好，O(1) | 动态 I/O 映射，可能有间接层 |
| 功能块（TON/CTU）是 POD 结构体 | 栈上分配，缓存行对齐 | 堆分配对象 + 虚函数表 |
| `POUFunc` 是函数指针 | 直接调用，无 dispatch | 中间码解释循环 |
| `Scheduler::tick()` 单线程协作 | 无线程切换/锁竞争 | pthread 切换 + 信号量等待 |
| 所有内置函数 `inline` | 编译器内联展开 | 函数调用开销 |
| 模板元编程（`PLC_Array<T,N>`） | 编译期确定大小，零运行时开销 | 运行时类型信息 |

---

## 4. CODESYS 运行时架构详解

### 4.1 分层架构（CAA）

CODESYS Runtime System (CRS) 采用 CAA（Component Architecture for Automation）架构：

```
┌─────────────────────────────────────────────────────────┐
│                    CODESYS IDE (PC)                      │
│              编辑、编译、下载、在线调试                     │
└──────────────────────┬──────────────────────────────────┘
                       │ TCP/IP (Gateway)
┌──────────────────────▼──────────────────────────────────┐
│                   PLC Manager                            │
│          程序生命周期管理、在线变更、状态切换                │
├─────────────────────────────────────────────────────────┤
│                  IEC Application                         │
│         PROGRAM / FB / FC 实例化与执行                     │
│         变量表 (GVL) + 符号表 + 交叉引用                   │
├─────────────────────────────────────────────────────────┤
│               Task System (调度器)                        │
│    Cyclic / Event / Freewheeling / SFC 控制任务           │
│    优先级抢占 + 看门狗 + 时限监控                          │
├─────────────────────────────────────────────────────────┤
│              I/O Subsystem (I/O 子系统)                   │
│    Process Image ←→ Fieldbus Abstraction Layer            │
│         支持 EtherCAT / PROFINET / CANopen / Modbus       │
├─────────────────────────────────────────────────────────┤
│           Communication Stack                            │
│         OPC UA / MQTT / TCP/UDP / WebVisu                │
├─────────────────────────────────────────────────────────┤
│           Device Abstraction Layer (DAL)                  │
│       硬件抽象：CPU、内存、定时器、中断、看门狗              │
├─────────────────────────────────────────────────────────┤
│              OS / RTOS (底层操作系统)                      │
│         Linux / VxWorks / Windows / 裸机                  │
└─────────────────────────────────────────────────────────┘
```

### 4.2 CODESYS 的关键设计决策

**程序是"下载"到运行时的，不是编译进固件的：**

```
CODESYS 工作流：
  IDE 编译 → 生成字节码/中间码 → 通过网络下载到运行时 → 运行时加载执行

ST2C++ 工作流：
  ST 源码 → ST2C++ 编译器 → 生成 C++ 源码 → 编译为原生二进制 → 烧写到设备
```

**Task System 是线程级抢占：**

```cpp
// CODESYS 内部（伪代码）：
class Task {
    pthread_t thread;          // 每个任务一个独立线程
    int priority;              // RTOS 优先级
    struct timespec period;    // 周期
    sem_t trigger;             // 信号量触发

    void run() {
        while (running) {
            sem_wait(&trigger);       // 等待调度
            syncInputs();             // 读输入
            executePrograms();        // 执行逻辑
            syncOutputs();            // 写输出
        }
    }
};

// 调度器用 pthread_setschedparam() 设置 SCHED_FIFO 实时调度
// 高优先级任务可以抢占低优先级任务
```

**I/O 子系统有 Fieldbus Abstraction Layer：**

```cpp
// CODESYS 的 I/O 层（概念）：
class FieldbusDriver {
    virtual void cyclic() = 0;              // 周期性同步
    virtual void readInput(int addr, void* buf, int len) = 0;
    virtual void writeOutput(int addr, void* val, int len) = 0;
    virtual bool diagnose() = 0;            // 总线诊断
};

// 具体驱动：
class EtherCAT_Driver : public FieldbusDriver { ... };
class PROFINET_Driver : public FieldbusDriver { ... };
class ModbusTCP_Driver : public FieldbusDriver { ... };
```

---

## 5. 实时性分析

### 5.1 "实时"的定义

```
实时 ≠ 快
实时 = 可预测、有界、确定

        非实时（Windows/Linux）         硬实时（RTOS）
        ─────────────────────         ──────────────
周期 1:   1.0ms ✓                      1.00ms ✓
周期 2:   0.8ms ✓                      1.00ms ✓
周期 3:   5.2ms ✗ ← 被 OS 抢走         1.00ms ✓
周期 4:   1.1ms ✓                      1.00ms ✓
周期 5:   0.3ms ✓                      1.00ms ✓
周期 6:  12.0ms ✗ ← GC/中断/调度        1.00ms ✓

抖动:     ±10ms（不可预测）              ±几μs（确定性）
```

### 5.2 CODESYS 的实时性取决于底层 OS

| CODESYS 产品 | 底层 OS | 实时性 | 抖动 |
|---|---|---|---|
| CODESYS Control for VxWorks | RTOS | ✅ 硬实时 | <10μs |
| CODESYS Control for QNX | RTOS | ✅ 硬实时 | <20μs |
| CODESYS Control for RT-Linux | 实时补丁 Linux | ⚠️ 软实时 | 50-200μs |
| CODESYS Control for Linux | 普通 Linux | ❌ 非实时 | 1-10ms |
| CODESYS Control for Windows | 普通 Windows | ❌ 非实时 | 5-50ms |

CODESYS 本身不是实时的，它跑在什么操作系统上才是关键。

### 5.3 ST2C++ 的实时性优势

```
CODESYS + RTOS:
  硬件定时器 → RTOS 内核 → CODESYS 调度器 → 字节码解释 → 执行
  延迟: ~50-200μs（RTOS 内核 + 解释器开销）

ST2C++ + RTOS:
  硬件定时器 → RTOS 内核 → Scheduler::tick() → 原生代码
  延迟: ~10-50μs（只有 RTOS 内核开销，无解释器）
```

原生执行 + RTOS = 比 CODESYS 更低的延迟和抖动。

---

## 6. 速度对比

### 6.1 执行速度对比

假设一个典型的 PLC 扫描周期（1ms 周期，10 个任务，每个任务 50 条 ST 指令）：

```
                        CODESYS          ST2C++
                        ───────          ──────
指令解码开销             ~200ns/条        0（原生执行）
变量查找                 ~50ns/次         ~1ns（偏移量访问）
函数调用                 ~30ns/次         ~0-5ns（inline）
任务调度                 ~500ns           ~50ns
I/O 同步                 ~1μs             ~1μs（取决于驱动）
─────────────────────────────────────────────────
单周期总开销             ~15-20μs         ~2-3μs
吞吐量                   ~50kHz           ~300-500kHz
```

### 6.2 ST2C++ 热路径分析

```cpp
// Scheduler::tick() — 最关键的代码路径
void tick() {
    // ① 读输入 — 1次虚函数调用（TCI）
    syncInputs();

    // ② 执行任务 — 核心热循环
    for (int i = 0; i < taskCount_; i++) {
        if (shouldRun(task)) {
            task.executePOUs(gvl, image, baseCycleTime);
            // ↑ 函数指针直接调用，编译器可以内联
        }
    }

    // ③ 写输出 — 1次虚函数调用（TCI）
    syncOutputs();
}
```

热路径只有 ② 是高频的，①③ 每个周期各一次。② 里面是直接函数指针调用，没有虚函数、没有查找。

### 6.3 进一步优化建议

| 优化点 | 收益 | 复杂度 |
|--------|------|--------|
| TSC 替代 `std::chrono` | 测量开销减少 5-10x | 低 |
| 诊断统计加编译期开关 | 热路径省 2-3 条指令 | 低 |
| `ProcessImage` 对齐到缓存行（64B） | 避免 false sharing | 低 |
| 任务执行函数用 `__attribute__((always_inline))` | 强制内联 | 低 |
| 用 `__builtin_expect` 提示分支预测 | shouldRun 热路径优化 | 低 |
| 支持 RTOS 线程优先级（可选） | 真正的抢占式调度 | 高 |

---

## 7. 市场格局与定位

### 7.1 主流市场被字节码型垄断的原因

不是技术不行，是**商业上不划算**：

```
编译型的困境：

  每个硬件平台 → 要写/移植一个编译器后端
  每个 OS       → 要适配运行时
  每次改程序   → 要重新编译、重新部署

  → 对硬件厂商来说，成本太高
  → 对用户来说，在线修改不方便
```

### 7.2 编译型的优势领域

| 领域 | 为什么用编译型 | 代表 |
|------|--------------|------|
| 安全 PLC (SIL3/SIL4) | 认证要求确定性执行，不能有解释器 | Pilz PNOZ、部分西门子 F-CPU |
| 高端运动控制 | 微秒级周期，解释器太慢 | 部分 Beckhoff TwinCAT NC |
| 汽车 ECU | 资源受限，几 KB RAM | AUTOSAR（编译型） |
| 航空航天 | DO-178C 认证，不允许动态行为 | Ada/编译型 PLC |

### 7.3 ST2C++ 的市场定位

```
主流市场（通用 PLC）     ← CODESYS 垄断，不竞争

细分市场（性能/安全关键） ← ST2C++ 的机会
  ├── 安全 PLC 后端（SIL3 认证要求确定性执行）
  ├── 运动控制器内核（微秒级周期）
  ├── 嵌入式 MCU PLC（几 KB RAM，无 OS）
  └── 编译器自动生成代码的后端运行时
```

CODESYS 卖的是"通用"，ST2C++ 卖的是"极致"。两个赛道，互不竞争。

---

## 8. ST2C++ 运行时设计评审

### 8.1 架构评价

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐⭐ | 分层清晰，职责明确，符合 IEC 61131-3 标准 |
| 代码质量 | ⭐⭐⭐⭐ | 命名规范、注释充分，少量边界问题 |
| 实时性 | ⭐⭐⭐⭐⭐ | 零堆分配、零虚函数（除 TCI）、全 inline |
| 测试覆盖 | ⭐⭐⭐⭐ | 12 维测试 + 多任务演示，缺压力测试和边界测试 |
| 完整度 | ⭐⭐⭐⭐ | 核心功能完整，WSTRING/ANY 类型待补充 |

### 8.2 头文件架构合理性

当前 `rt_plc.h` + `rt_runtime.h` 全部使用头文件（header-only）是**合理的设计选择**：

- 编译器生成的 C++ 代码只需 `#include` 即可使用，无需构建运行时库
- 模板和 inline 函数天然需要在头文件中定义
- 当前规模（~2000 行）下编译时间可控
- 如需优化编译时间，可添加预编译头（PCH）

### 8.3 已知待改进项

| 优先级 | 项目 | 说明 |
|--------|------|------|
| 高 | SQRT/LN/LOG 精度 | 当前自实现精度有限，建议用 `<cmath>` 硬件指令 |
| 高 | ProcessImage 边界检查 | readInput/writeOutput 缺少 debug 模式下的边界断言 |
| 中 | EXPT 浮点指数 | 当前只支持整数指数，IEC 61131-3 要求支持浮点指数 |
| 中 | plc_deref 线程安全 | NULL 回退用 static 局部变量，多线程有数据竞争风险 |
| 低 | findFreeTask 逻辑 | 空闲判断条件过于特殊，建议加 `bool active` 标志 |
| 低 | WSTRING 实现 | 几乎为空，需补充或标记为 TODO |

---

## 附录：参考资源

- IEC 61131-3 标准（工业自动化编程语言标准）
- [CODESYS 官方文档](https://help.codesys.com)
- [CODESYS Store（运行时产品）](https://store.codesys.com)
- 3S-Smart Software Solutions (CODESYS 厂商)
- Beckhoff TwinCAT 技术文档
- OpenPLC 开源项目

---

*文档创建时间：2026-06-23*
*对应项目：ST2C-master（ST→C++ 编译器 + 运行时）*
