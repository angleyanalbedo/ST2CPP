## ST2C 实时化改造方案

### 一、现状分析

当前 ST2C 的数据模型是 Type-Value 分离 + 整数 ID 全局注册：

- 每个变量是堆上的 `PLC_Value` 包装对象，通过 `new INT(42)` 创建
- 所有变量注册到全局 `varMap`（RFM），运行时通过 `RFM->getSymbolByID<INT*>(id)` 查找
- 算术运算通过重载的 `operator+` 实现，每次运算都 `new` 临时对象
- 表达式在 Java 的 `staticCheckVisitor` 层预拼成字符串，存入 `PLCVariable.assignVar`

这套设计的核心问题是：**运行时开销全部发生在热路径上**。一次简单的 `a + b` 要经历 map 查找 → dynamic_cast → 虚函数调用 → new 临时对象 → 返回解引用。对 PLC 实时控制来说完全不可接受。

**旧 Runtime 文件结构**（`Runtime/lib/`）：

```
Runtime/lib/
├── PLC.h                    # 主头文件（PLC 命名空间、RFM 全局注册）
├── varMap/varMap.h          # 变量注册表（ID→对象映射）
├── PLC_Class/PLC_Class.h    # 类/方法模型
├── PLC_INNER/               # Type-Value 模板（每种类型一对文件）
│   ├── PLC_Inner_Type.h
│   ├── PLC_Inner_Value.h/.cpp
│   └── PLC_INTEGER/         # INT_VALUE.h, DINT_VALUE.h, ...（每种数值类型）
├── PLC_Function/            # FUNCTION 基类
├── PLC_Exception/           # 异常处理
├── MemoryManager/           # 内存管理
└── Debug.h/.cpp             # 调试
```

---

### 二、设计目标

将 ST2C 从"功能验证原型"升级为"可实时的 PLC 运行时"，核心原则：

1. **编译期完成所有地址计算**，运行时无 map 查找、无 dynamic_cast
2. **变量用原生 C++ 类型**（int16_t、float 等），不做包装
3. **平坦内存模型**：所有变量放在一块连续内存中，编译器算好偏移
4. **扫描周期模型**：分阶段执行（ReadInputs → LogicSolve → WriteOutputs → Housekeeping）
5. **多任务调度**：支持 Cyclic / Event / Freewheeling 三种任务类型
6. **PROGRAM 生命周期**：Init → FirstScan → Cyclic → Post
7. **冷启动/暖启动**：RETAIN 变量在暖启动时保留
8. **运行时错误处理**：除零/越界/null 不崩溃，进入 ERROR 状态
9. **完整 IEC 61131-3 类型系统**：TIME/DATE/TOD/DT、位操作、标准功能块、字符串操作

---

### 三、Runtime 设计

#### 3.1 文件结构

新 Runtime 位于 `Runtime/rt/`，两个头文件 + 测试：

```
Runtime/rt/
├── rt_plc.h              # 类型系统 + 标准功能块 + 内置函数 + 基础框架
├── rt_runtime.h          # 多任务调度器 + GVL + PROGRAM 生命周期 + 错误管理
├── fibonacci.cpp         # 基础类型验证测试（15 项）
├── multitask_demo.cpp    # 多任务调度演示（4 任务，事件触发）
├── framework_test.cpp    # 框架完整性测试（82 项）
├── CMakeLists.txt        # 构建配置
└── build/                # 构建输出
```

#### 3.2 内存模型：GVL（全局变量表）

用一块平坦内存替代 `varMap`。`rt_runtime.h` 中的 `GVL` 结构：

```cpp
struct GVL {
    uint8_t memory[GVL_SIZE];  // 默认 64KB
    size_t retainStart, retainEnd;  // RETAIN 区域标记

    template<typename T> T* ptr(size_t offset);       // 直接指针（零开销）
    template<typename T> T read(size_t offset) const;  // memcpy 读
    template<typename T> void write(size_t offset, T val);  // memcpy 写
    void clear();           // 冷启动：全清零
    void clearNonRetain();  // 暖启动：只清零非 RETAIN 区域
};
```

编译器生成的代码直接用 `ptr<T>(offset)` 获取指针，然后像普通变量一样读写：

```cpp
INT* counter = gvl.ptr<INT>(300);
(*counter)++;

REAL* setpoint = gvl.ptr<REAL>(200);
*output = (*setpoint - *sensor) * 2.0f;
```

`ptr<T>(offset)` 只是一条 `reinterpret_cast`，运行时仅一次指针解引用。

**对齐要求**：编译器在计算偏移时必须保证类型对齐——REAL（4字节）放 4 的倍数，LREAL/LINT（8字节）放 8 的倍数，中间插 padding。x86/x64 容忍非对齐访问（稍慢），ARM 上非对齐可能触发 bus error。

#### 3.3 内存布局

编译器在 Memory Layout Pass 中规划整块内存的分布：

```
GVL 内存布局（编译器分配）：
┌─────────────────────────────────┐
│  输入映像 (%IX/%IW/%ID)         │  offset 0
│  由 syncInputs() 写入           │
├─────────────────────────────────┤
│  输出映像 (%QX/%QW/%QD)         │  offset N
│  由 syncOutputs() 读出          │
├─────────────────────────────────┤
│  VAR_GLOBAL                     │  offset M
│  全局变量表 (GVL)               │
├─────────────────────────────────┤
│  RETAIN 区域                    │  retainStart
│  暖启动时保留                   │  retainEnd
├─────────────────────────────────┤
│  PLC_PRG 的 VAR                 │  offset P1
│  PROGRAM 级持久变量             │
├─────────────────────────────────┤
│  FB_instance_1 的 VAR           │  offset P2
│  FB_instance_2 的 VAR           │  offset P3
│  ...                            │
└─────────────────────────────────┘
```

#### 3.4 基本类型（rt_plc.h）

直接用 C++ 原生类型，不做任何包装：

| IEC 61131-3 类型 | C++ 类型 | 大小 | 对齐 |
|---|---|---|---|
| BOOL | int8_t | 1 | 1 |
| SINT | int8_t | 1 | 1 |
| INT | int16_t | 2 | 2 |
| DINT | int32_t | 4 | 4 |
| LINT | int64_t | 8 | 8 |
| USINT | uint8_t | 1 | 1 |
| UINT | uint16_t | 2 | 2 |
| UDINT | uint32_t | 4 | 4 |
| ULINT | uint64_t | 8 | 8 |
| REAL | float | 4 | 4 |
| LREAL | double | 8 | 8 |
| BYTE | uint8_t | 1 | 1 |
| WORD | uint16_t | 2 | 2 |
| DWORD | uint32_t | 4 | 4 |
| LWORD | uint64_t | 8 | 8 |
| TIME | int64_t（微秒） | 8 | 8 |
| DATE | int32_t（天数） | 4 | 4 |
| TIME_OF_DAY | int64_t（微秒） | 8 | 8 |
| DATE_AND_TIME | int64_t（Unix us） | 8 | 8 |
| STRING | char[256] | 256 | 1 |

运算就是原生运算符，`a + b` 就是 `+`，没有虚函数、没有 new、没有临时对象。

#### 3.5 TIME / DATE / TOD 运算

TIME 类型是 `int64_t` 微秒，直接支持加减比较。提供辅助构造函数：

```cpp
TIME t = T_ms(100);     // 100ms
TIME t = T_s(5);        // 5秒
TIME t = T_min(1);      // 1分钟
TIME t = T_h(2);        // 2小时
TIME t = T_d(1);        // 1天
```

比较辅助函数：`T_eq`, `T_ne`, `T_lt`, `T_le`, `T_gt`, `T_ge`。

DATE / TIME_OF_DAY / DATE_AND_TIME 的构造和分解：

```cpp
DATE d = DATE_make(2025, 1, 15);
TIME_OF_DAY tod = TOD_make(14, 30, 0);
DATE_AND_TIME dt = DT_make(2025, 1, 15, 14, 30, 0);
DATE_AND_TIME now = NOW();         // 系统时间
TIME_OF_DAY now_tod = NOW_TOD();   // 当前时刻
```

#### 3.6 位操作

IEC 61131-3 标准位运算符，适用于所有整数类型：

```cpp
plc_and(a, b)  // AND
plc_or(a, b)   // OR
plc_xor(a, b)  // XOR
plc_not(a)     // NOT
SHL(val, n)    // 左移
SHR(val, n)    // 右移
ROL(val, n)    // 循环左移（BYTE）
ROR(val, n)    // 循环右移（BYTE）
```

#### 3.7 复合类型

**STRUCT**：编译器生成 C++ struct，字段按声明顺序排列，编译器计算每个字段的偏移。

```cpp
struct Point { REAL x; REAL y; };  // 编译器生成
Point* p = gvl.ptr<Point>(300);
p->x = 1.0f;
```

**ARRAY**：`PLC_Array<T, N>` 模板，固定大小，带越界检查（debug 模式）：

```cpp
PLC_Array<INT, 10> arr;
arr.at(3) = 42;        // 带越界检查
arr[3] = 42;           // 无检查（编译器已验证安全时使用）
arr.fill(0);           // 全部填充
```

编译器辅助宏 `PLC_ARRAY_AT(arr, idx, lower)` 处理下标偏移（`ARRAY[3..12]` 的访问）。

**ENUM**：编译器直接生成 C++ `enum class`。

```cpp
enum class Color : DINT { Red = 0, Green = 1, Blue = 5, Yellow = 6 };
```

**SUBRANGE**：`PLC_Subrange<T, Low, High>` 模板，赋值时自动钳位。

```cpp
typedef PLC_Subrange<INT, 0, 100> Percentage;
Percentage p = 150;  // 自动钳位为 100
```

**REF / POINTER**：直接用 C++ 指针，`plc_deref(ptr)` 提供空指针安全检查。

#### 3.8 过程映像与 I/O 地址

`ProcessImage` 提供 64KB 输入/输出缓冲区，支持类型化读写和位级访问：

```cpp
ProcessImage io;
REAL val = io.readInput<REAL>(PLC_IW(4));         // %IW4
BOOL bit = io.readInputBit(PLC_IX(0, 3));          // %IX0.3
io.writeOutput(PLC_QW(2), (INT)1234);              // %QW2
io.writeOutputBit(PLC_QX(1, 3), TRUE);             // %QX1.3
```

I/O 地址宏将 ST 的直接地址翻译为字节偏移：`PLC_IB(b)`, `PLC_IW(b)`, `PLC_ID(b)`, `PLC_IX(b, bit)`, `PLC_QB(b)`, `PLC_QW(b)`, `PLC_QD(b)`, `PLC_QX(b, bit)`。

硬件驱动实现 `TCI` 接口：

```cpp
struct TCI {
    virtual void syncInputs(ProcessImage& img) = 0;
    virtual void syncOutputs(ProcessImage& img) = 0;
};
```

#### 3.9 POU 函数签名

所有 POU 的函数签名统一为：

```cpp
typedef void (*POUFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
```

编译器生成的 POU 函数示例：

```cpp
namespace Offsets {
    constexpr size_t SENSOR   = 200;  // GVL.sensor_value : REAL
    constexpr size_t SETPOINT = 204;  // GVL.setpoint : REAL
    constexpr size_t COUNTER  = 300;  // PLC_PRG.counter : INT
}

void PLC_PRG(GVL& gvl, ProcessImage& io, TIME dt) {
    INT* counter   = gvl.ptr<INT>(Offsets::COUNTER);
    REAL* sensor   = gvl.ptr<REAL>(Offsets::SENSOR);
    REAL* setpoint = gvl.ptr<REAL>(Offsets::SETPOINT);
    (*counter)++;
    REAL* output = gvl.ptr<REAL>(Offsets::OUTPUT);
    *output = (*setpoint - *sensor) * 2.0f;
}
```

FUNCTION 的 VAR_INPUT/VAR_OUTPUT 可直接用函数参数传递（性能更好）。

#### 3.10 PROGRAM 实例生命周期

每个 PROGRAM 有 4 个生命周期阶段，由 `ProgramInstance` 管理：

```
UNINIT → INIT → FIRST_SCAN → CYCLIC → STOPPED
           ↑         ↑          ↑        ↑
       doInit()  doPre()   doCyclic()  doPost()
```

```cpp
// 编译器为每个 PROGRAM 生成 4 个函数
void PRG_init(GVL& gvl, ProcessImage& io);       // 变量初始化
void PRG_pre(GVL& gvl, ProcessImage& io);        // 首扫描前（可选）
void PRG_cyclic(GVL& gvl, ProcessImage& io, TIME dt);  // 主循环体
void PRG_post(GVL& gvl, ProcessImage& io);       // 停止清理（可选）

// 注册到 Scheduler
int pIdx = sched.addProgram("MainProg", PRG_init, PRG_cyclic, PRG_pre, PRG_post);
sched.addProgramToTask(taskIdx, pIdx);
```

`doInit()` 在 `start()` 时调用，`doPre()` 紧随其后，`doCyclic()` 每个扫描周期执行，首次 cyclic 后 phase 自动从 FIRST_SCAN 变为 CYCLIC，`doPost()` 在 `stop()` 时调用。

#### 3.11 分阶段扫描周期

`Scheduler::tick()` 按 4 个阶段执行：

```
每个 tick:
  Phase 1: READ_INPUTS     → TCI::syncInputs() + 更新 systemTime + 检查事件
  Phase 2: LOGIC_SOLVE     → 按优先级执行所有就绪任务（POU + PROGRAM）
  Phase 3: WRITE_OUTPUTS   → TCI::syncOutputs()
  Phase 4: HOUSEKEEPING    → 更新 DiagStats（min/max/avg 扫描时间）
```

`currentPhase` 字段可在外部查询当前处于哪个阶段。

#### 3.12 多任务调度

三种任务类型：

- **Cyclic**：固定周期执行，`isDue(systemTime)` 判断是否到期
- **Event**：由条件函数触发（上升沿/下降沿/双边沿），`checkEvents()` 在每个 tick 的 READ_INPUTS 阶段检查
- **Freewheeling**：每个扫描周期都执行

同一任务下可挂载多个 POU 和 PROGRAM 实例，按注册顺序依次执行。任务按优先级排序（低数字 = 高优先级）。

#### 3.13 标准功能块

定时器（TON、TOF、TP）和计数器（CTU、CTD、CTUD）：

```cpp
TON timer;
timer.update(IN, PT, cycleTimeUs);  // 每扫描周期调用
BOOL q = timer.output;
```

边沿检测（R_TRIG / F_TRIG）：保存上次状态，比较当前状态。

所有功能块作为普通 struct 存放在 GVL 中，编译器为每个实例分配偏移。

#### 3.14 字符串操作（IEC 61131-3 标准）

`STRING` 是固定 256 字节结构体。支持全部标准操作：

```cpp
STRING s = CONCAT(s1, s2);
DINT len = LEN(s);
STRING left = LEFT(s, 5);
STRING right = RIGHT(s, 5);
STRING mid = MID(s, 3, 7);
STRING ins = INSERT(s, pattern, 6);
STRING del = DELETE(s, 3, 6);
STRING rep = REPLACE(s, newStr, 3, 6);
DINT pos = FIND(s, "hello");
BOOL eq = STR_EQ(s1, s2);  // 以及 NE, LT, LE, GT, GE
```

#### 3.15 数学与选择函数

```cpp
ABS(x)            // 全类型重载（SINT→LREAL）
SQRT(x)           // Newton 法实现
EXPT(base, exp)   // 幂运算（整数指数）
LN(x) / LOG(x)    // 对数
MIN(a, b) / MAX(a, b)
MOD(a, b)         // 安全（除零返回 0）
SEL(cond, f, t)   // 二选一
LIMIT(min, v, max) // 钳位
MUX(sel, v0, v1, ...) // 多选一
```

#### 3.16 类型转换

全覆盖的类型转换函数，所有 IEC 数值类型间互转：

`TO_SINT`, `TO_INT`, `TO_UINT`, `TO_DINT`, `TO_UDINT`, `TO_LINT`, `TO_ULINT`, `TO_REAL`, `TO_LREAL`, `TO_BOOL`, `TO_STRING`

#### 3.17 运行时错误处理

`ErrorManager` 提供不崩溃的错误处理机制：

```cpp
enum class ErrorCode {
    DIV_BY_ZERO, ARRAY_OUT_OF_BOUNDS, INT_OVERFLOW,
    NULL_POINTER, STRING_OVERFLOW, STACK_OVERFLOW,
    WATCHDOG_TIMEOUT, IO_ERROR, CONFIG_ERROR, USER_ERROR
};
```

编译器在可能出错的点生成安全调用：

```cpp
// 编译器为除法生成：
result = errorMgr.safeDiv(a, b, pouId, lineNo, systemTime);
// 除零时返回 0，记录错误，设置 fatalMode

// 编译器为数组访问生成：
val = errorMgr.safeArrayAt(arr, index, pouId, lineNo, systemTime);
```

环形错误日志（32 条），支持 `ErrorHandler` 回调。致命错误（除零/越界/null）自动设置 `fatalMode`，Scheduler 在下次 tick 时检测到后进入 ERROR 状态。

#### 3.18 看门狗

每个任务可设置独立的执行超时限制：

```cpp
sched.watchdog.setDefault(T_ms(100));      // 全局默认 100ms
sched.setTaskWatchdog(taskIdx, T_ms(50));   // 单任务 50ms
```

超时时记录 overrun 计数并输出告警。连续 10 次超时自动进入 ERROR 状态（`MAX_CONSECUTIVE_OVERRUNS = 10`）。

#### 3.19 系统状态机

```
           start(COLD/WARM)
STOP ──────────────────────→ STARTING ──→ RUN ──error()──→ ERROR
  ↑                            (Init/Pre)   │                  │
  │                                         │ pause()          │ resetError()
  │                                         ↓                  ↓
  └──────────── stop() ←──────────────── PAUSED           STOP
                   (Post)
```

- **STOP**：所有任务停止，不执行 POU，不做 I/O 同步
- **STARTING**：正在执行 Init/Pre 阶段
- **RUN**：正常扫描循环
- **PAUSED**：暂停扫描（可 resume）
- **STOPPING**：正在执行 Post 阶段
- **ERROR**：致命错误，需 resetError() 后重新启动

#### 3.20 冷启动 / 暖启动

```cpp
sched.start(StartupMode::COLD);  // 全部清零
sched.start(StartupMode::WARM);  // RETAIN 区域保留，其余清零
```

GVL 支持标记 RETAIN 区域：

```cpp
sched.setRetainRegion(retainStart, retainEnd);  // 编译器计算
```

暖启动时 `clearNonRetain()` 只清零 `[0, retainStart)` 和 `[retainEnd, GVL_SIZE)` 两段。

#### 3.21 互斥锁

`plc_lock`（基于 `std::atomic_flag` 的 spinlock）和 `plc_lock_guard`（RAII 包装）：

```cpp
plc_lock sharedLock;
{
    plc_lock_guard g(sharedLock);
    // 访问共享变量
}
```

编译器为 `VAR_ACCESS` 块生成加锁/解锁代码。

#### 3.22 诊断统计

`DiagStats` 自动记录扫描时间统计：

```cpp
struct DiagStats {
    uint64_t totalScanCount;
    TIME minScanTime, maxScanTime, avgScanTime, lastScanTime;
    uint64_t totalOverruns;
};
```

`sched.printDiag()` 输出完整的诊断报告（任务状态、PROGRAM 阶段、GVL 使用量、扫描统计、错误日志）。

---

### 四、Java 编译器改造

#### 4.1 需要新增的模块

**Memory Layout Pass**（新增，核心模块）

在语义分析之后、代码生成之前，插入一个新的编译阶段。职责：

1. 遍历所有已解析的 POU（PROGRAM、FUNCTION、FUNCTION_BLOCK），收集所有变量声明
2. 为每个变量计算类型大小和对齐要求
3. 按区域（输入映像、输出映像、GVL、各 POU 的 VAR）分配偏移
4. 处理对齐 padding
5. 标记 RETAIN 变量区域
6. 输出一张符号偏移表（变量全限定名 → 偏移 + 类型 + 是否 RETAIN）

**偏移常量生成器**（新增）

为每个 POU 生成偏移常量定义：

```cpp
namespace PLC_PRG_Offsets {
    constexpr size_t counter  = 300;
    constexpr size_t integral = 304;
}
```

#### 4.2 需要重写的模块

**表达式翻译（staticCheckVisitor 中的 Visit 方法）**

当前生成 `(*(new INT(42)))`，改为生成原生表达式 `(INT)42`。

| 当前生成 | 目标生成 |
|---|---|
| `(*(new INT(42)))` | `(INT)42` |
| `(*X) + (*Y)` | `(*X_ptr) + (*Y_ptr)` |
| `*X / *Y` | `errorMgr.safeDiv(*X, *Y, pouId, line, sysT)` |

**TranslateFunc_decl** → 生成普通 C++ 函数：`INT FUN(INT X) { ... }`

**TranslateProg_decl** → 生成 PROGRAM 生命周期函数（init/pre/cyclic/post）+ Scheduler 初始化

**TranslateClass_decl** → 生成 state struct + POU 函数（FB 支持）

**TranslateVariableAssignExpression** → `*gvl.ptr<T>(offset) = expr;`

**TranslateFor_stmt / While / Repeat** → 去掉 new 包装

**TranslateCallFunc** → 直接函数调用 `FUN(arg1, arg2)`

**packageFactory** → 约 30 个模板全部重写

#### 4.3 不需要改动的模块

- ANTLR 语法文件（PLCSTLEXER.g4、PLCSTPARSER.g4）
- PLCSymbolAndScope/（符号表和作用域栈）
- staticCheckVisitor/ 的类型检查逻辑
- PLCException/
- JSON/

#### 4.4 改造步骤

**Phase 1** — FUNCTION + PROGRAM + 基本类型（INT/REAL/BOOL/STRING）

**Phase 2** — 复合类型（STRUCT/ARRAY/ENUM/SUBRANGE）

**Phase 3** — 功能块（FB）

**Phase 4** — 高级特性（INTERFACE/METHOD/REF/POINTER/SUPER）

---

### 五、验证状态

#### 5.1 已实现的 Runtime 测试

**fibonacci.cpp**（15 项）：FUNCTION 调用、PROGRAM 执行、扫描周期、类型运算、边沿检测、STRING、STRUCT、ARRAY（含多维和越界检查）、ENUM、SUBRANGE（自动钳位）、REF/POINTER（null 安全）、TP 脉冲定时器、CTD 递减计数器、类型转换、过程映像。

**multitask_demo.cpp**：4 个任务（FastControl 10ms + Monitoring 50ms + AlarmHandler 事件触发 + Diagnostics 200ms），跨 POU GVL 变量访问，SimTCI 模拟 I/O，看门狗检测。

**framework_test.cpp**（82 项）：TIME 运算与比较、DATE/TOD/DT 类型、位操作（AND/OR/XOR/SHL/SHR）、I/O 地址宏与位访问、错误处理（安全除法/越界/MOD 零/致命标记/环形日志）、扩展字符串（LEFT/RIGHT/MID/INSERT/DELETE/REPLACE/FIND/比较）、互斥锁（spinlock + RAII guard）、PROGRAM 实例生命周期（UNINIT→INIT→FIRST_SCAN→CYCLIC→STOPPED）、分阶段扫描周期验证、冷启动/暖启动（RETAIN 保留）、系统状态机（STOP→STARTING→RUN→PAUSED→ERROR→resetError）、看门狗与诊断统计、错误集成（fatalMode → ERROR 状态自动切换）。

#### 5.2 已知限制

- Windows `steady_clock` 精度约 15ms，连续 tick() 间隔可能不够触发 isDue()，测试时需使用 1us 间隔或直接调用方法绕过计时
- TP 定时器依赖每个扫描周期被调用一次，如果跳周期则计时不准
- SQRT/LN/LOG 用 Newton 法/级数实现，精度足够但非硬件级
- STRING 固定 256 字节，不支持动态长度
- GVL 固定 64KB，RETAIN 区域固定 8KB
- 任务间同步仅提供 spinlock，未实现信号量/消息队列
- 不支持多核调度（单线程 tick）

---

### 六、工作量估计

| 模块 | 预估改动 | 状态 |
|---|---|---|
| rt_plc.h（类型+功能块+内置函数+框架） | ~1100 行 C++ | **已完成** |
| rt_runtime.h（调度器+GVL+PROGRAM+错误） | ~550 行 C++ | **已完成** |
| fibonacci.cpp 验证 | ~400 行 | **已完成** |
| multitask_demo.cpp 验证 | ~300 行 | **已完成** |
| framework_test.cpp 验证 | ~450 行 | **已完成** |
| Memory Layout Pass（新增 Java） | ~500 行 | 待开发 |
| packageFactory 重写 | ~300 行 | 待开发 |
| 表达式翻译重写 | ~400 行 | 待开发 |
| POU 翻译重写 | ~500 行 | 待开发 |
| Phase 2-4 追加 | ~1000 行 | 待开发 |

Java 编译器改造总计约 1700 行改动，对比现有 55K 行代码库约 3%。
