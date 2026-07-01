# 运行时 API 参考

## 概述

运行时环境由 `runtime-flat/include/` 下的头文件定义，行为由 `runtime-flat/src/*.cpp` 实现。不依赖任何第三方库（C++17 标准库除外）。

所有 API 位于 `rt_plc` 命名空间。

---

## 1. IEC 61131-3 类型系统 (`core/types.h`)

### 基本类型

| IEC 类型 | C++ 类型 | 定义宏 |
|----------|----------|--------|
| `BOOL` | `int8_t` | `TRUE` / `FALSE` |
| `SINT` | `int8_t` |
| `INT` | `int16_t` |
| `DINT` | `int32_t` |
| `LINT` | `int64_t` |
| `USINT` | `uint8_t` |
| `UINT` | `uint16_t` |
| `UDINT` | `uint32_t` |
| `ULINT` | `uint64_t` |
| `REAL` | `float` |
| `LREAL` | `double` |
| `BYTE` | `uint8_t` |
| `WORD` | `uint16_t` |
| `DWORD` | `uint32_t` |
| `LWORD` | `uint64_t` |
| `TIME` | `int64_t`（微秒） |

### 时间类型

```cpp
TIME T_us(int64_t us);      // → TIME
TIME T_ms(int64_t ms);      // → TIME * 1000
TIME T_s(int64_t s);        // → TIME * 1,000,000
TIME T_min(int64_t m);      // → TIME * 60,000,000
TIME T_h(int64_t h);        // → TIME * 3,600,000,000
TIME T_d(int64_t d);        // → TIME * 86,400,000,000
```

`DATE`（int32_t, 儒略日偏移）、`TIME_OF_DAY`（int64_t, 微秒）、`DATE_AND_TIME`（int64_t, 微秒）均有对应的 `_make` / `_to_*` 函数。

### 类型转换

约 200 个 `TO_*` / `*_TO_*` 函数。示例：

```cpp
INT  TO_INT(REAL v);          // REAL → INT
DINT TO_DINT(LINT v);         // LINT → DINT
REAL TO_REAL(DINT v);         // DINT → REAL
DWORD TO_DWORD(REAL v);       // REAL 的二进制表示 → DWORD
STRING TO_STRING(DINT v);     // → "123"
```

### 字符串操作

```cpp
STRING CONCAT(const STRING& a, const STRING& b);  // 拼接
DINT   LEN(const STRING& s);                       // 长度
STRING LEFT(const STRING& s, DINT L);              // 左侧取 L 个
STRING RIGHT(const STRING& s, DINT L);             // 右侧取 L 个
STRING MID(const STRING& s, DINT L, DINT P);      // 从 P 取 L 个
STRING INSERT(const STRING& s, const STRING& ins, DINT P);
STRING DELETE(const STRING& s, DINT L, DINT P);
STRING REPLACE(const STRING& s, const STRING& rep, DINT L, DINT P);
DINT   FIND(const STRING& s, const STRING& pattern);
```

### 数学函数

```cpp
SINT/DINT/LINT/REAL/LREAL ABS(x);
REAL/LREAL SQRT(x);
REAL EXPT(REAL base, DINT exp);
REAL/LREAL LN(x), LOG(x);
T MIN(T a, T b), MAX(T a, T b);
SINT/INT/DINT/LINT MOD(a, b);
T SEL(BOOL cond, T falseVal, T trueVal);
T LIMIT(T minVal, T val, T maxVal);
T MUX(INT selector, T val0, T val1[, T val2, T val3]);
```

### 位操作

```cpp
T plc_and(T a, T b), plc_or(T a, T b), plc_xor(T a, T b), plc_not(T a);
T SHL(T val, INT n), SHR(T val, INT n);
BYTE ROL(BYTE val, INT n), ROR(BYTE val, INT n);
```

### ARRAY 复合类型

```cpp
template<typename T, size_t N>
struct PLC_Array {
    T& at(size_t index);        // 边界检查
    T& operator[](size_t);      // 无边界检查
    void fill(T val);
    constexpr size_t size() const;
};
```

---

## 2. 过程映像 ProcessImage (`rt_plc.h`)

```cpp
struct ProcessImage {
    uint8_t inputs[PROCESS_IMAGE_SIZE];     // 默认 65536 字节
    uint8_t outputs[PROCESS_IMAGE_SIZE];

    void clearInputs();
    void clearOutputs();

    template<typename T> T     readInput(size_t offset) const;
    template<typename T> void  writeInput(size_t offset, T val);
    template<typename T> T     readOutput(size_t offset) const;
    template<typename T> void  writeOutput(size_t offset, T val);

    BOOL  readInputBit(size_t byteOff, int bitOff) const;
    void  writeInputBit(size_t byteOff, int bitOff, BOOL val);
    BOOL  readOutputBit(size_t byteOff, int bitOff) const;
    void  writeOutputBit(size_t byteOff, int bitOff, BOOL val);
};
```

I/O 地址宏：

```cpp
PLC_IB(b), PLC_IW(b), PLC_ID(b), PLC_IL(b)   // 输入偏移生成
PLC_IX(b, bit)                                 // 输入位偏移
PLC_QB(b), PLC_QW(b), PLC_QD(b), PLC_QL(b)   // 输出偏移生成
PLC_QX(b, bit)                                 // 输出位偏移
```

便捷宏（编译器生成代码专用）：

```cpp
PI_READ_INPUT(img, type, offset)
PI_WRITE_INPUT(img, type, offset, val)
PI_READ_OUTPUT(img, type, offset)
PI_WRITE_OUTPUT(img, type, offset, val)
PI_READ_INPUT_BIT(img, b, bit)
PI_WRITE_INPUT_BIT(img, b, bit, val)
PI_READ_OUTPUT_BIT(img, b, bit)
PI_WRITE_OUTPUT_BIT(img, b, bit, val)
```

---

## 3. 全局变量表 GVL (`core/gvl.h`)

```cpp
struct GVL {
    uint8_t memory[GVL_SIZE];      // 默认 64KB
    size_t  highWaterMark;         // O(1) 使用量查询

    void clear();
    void clearNonRetain();         // 暖启动清零（保留 RETAIN）

    void setRetainRegion(size_t start, size_t end);
    void saveRetain();             // 保存到备份缓冲区（_post 自动调用）
    void loadRetain();             // 从备份恢复（暖启动）

    template<typename T> T     read(size_t offset) const;
    template<typename T> void  write(size_t offset, T val);
    template<typename T> T*    ptr(size_t offset);
    template<typename T> const T* ptr(size_t offset) const;

    template<typename T>
    T& safeArrayAt(size_t offset, size_t index, size_t count,
                   uint32_t pouId, uint32_t line, TIME sysTime);

    size_t usedBytes() const;      // O(1) 查询
};
```

---

## 4. PROGRAM 生命周期 (`core/program.h`)

```cpp
// 函数签名
typedef void (*PRG_InitFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_PreFunc)(GVL& gvl, ProcessImage& io);
typedef void (*PRG_CyclicFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
typedef void (*PRG_PostFunc)(GVL& gvl, ProcessImage& io);

struct ProgramInstance {
    char name[32];
    ProgramPhase phase;     // UNINIT → INIT → FIRST_SCAN → CYCLIC → STOPPED

    void doInit(GVL&, ProcessImage&);
    void doPre(GVL&, ProcessImage&);
    void doCyclic(GVL&, ProcessImage&, TIME cycleTimeUs);
    void doPost(GVL&, ProcessImage&);
};
```

POU 函数签名（调度器执行）：

```cpp
typedef void (*POUFunc)(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
```

---

## 5. 任务 (`core/task.h`)

```cpp
enum class TaskTrigger : uint8_t { CYCLIC, EVENT, FREEWHEELING };

struct Task {
    char        name[32];
    int         priority;
    TaskTrigger trigger;
    TIME        interval;       // 周期（微秒）
    POUFunc     pous[MAX_POUS_PER_TASK];     // 直接 POU 列表
    int         programIndices[MAX_POUS_PER_TASK];  // PROGRAM 索引列表
    TaskState   state;          // IDLE / READY / RUNNING / OVERRUN / ERROR
    uint64_t    cycleCount;
    TIME        maxExecTime;
    uint32_t    overrunCount;
    TIME        watchdogLimit;

    bool addPOU(POUFunc func);
    bool addProgram(int progIndex);
    void executePOUs(GVL&, ProcessImage&, TIME cycleTimeUs);
    bool isDue(TIME currentTime) const;
};
```

---

## 6. 事件 (`core/event.h`)

```cpp
typedef BOOL (*EventConditionFunc)(GVL& gvl, ProcessImage& io);

enum class EventEdge : uint8_t { RISING, FALLING, BOTH };

struct Event {
    char               name[32];
    EventConditionFunc  condition;
    EventEdge          edge;
    int                taskIndex;     // 关联的任务索引
    BOOL               lastState;

    bool check(GVL&, ProcessImage& io);  // 返回 true 触发任务
};
```

---

## 7. POU 注册表 (`core/registry.h`)

```cpp
struct POUCallbacks {
    PRG_InitFunc   init;
    PRG_PreFunc    pre;
    PRG_CyclicFunc cyclic;
    PRG_PostFunc   post;
};

class POURegistry {
    static constexpr int MAX_ENTRIES = 64;

    void add(const char* name, const POUCallbacks& cbs);
    void add(const char* name, PRG_InitFunc init, PRG_CyclicFunc cyclic,
             PRG_PreFunc pre = nullptr, PRG_PostFunc post = nullptr);
    const POUCallbacks* lookup(const char* name) const;
    int count() const;
};
```

编译器生成的每个 POU 产出一个 `registerPOU_XXX()` 函数，由 `pou_registry.gen.cpp` 汇总。

---

## 8. 调度器 Scheduler (`scheduler/scheduler.h`)

### 公共字段

```cpp
class Scheduler {
public:
    ProcessImage  image;
    GVL           gvl;
    RetainManager retain{gvl};
    TCI*          tci;
    IoManager     io;
    SystemState   systemState;   // STOP / STARTING / RUN / STOPPING / ERROR / PAUSED
    Watchdog      watchdog;
    ErrorManager  errorMgr;
    ErrorPolicy   errorPolicy;
    DiagStats     diag;
    plc_lock      gvlLock;        // GVL 互斥锁
    uint64_t      totalTicks;
    TIME          systemTime;
    TIME          baseCycleTime;  // 基础节拍（微秒），默认 1000
    ScanPhase     currentPhase;   // IDLE / READ_INPUTS / LOGIC_SOLVE / WRITE_OUTPUTS / HOUSEKEEPING
};
```

### 配置 API

```cpp
void setTCI(TCI* t);
void setBaseCycle(TIME us);
void setErrorHandler(ErrorHandler h);
void setRetainRegion(size_t start, size_t end);

int addProgram(const char* name, PRG_InitFunc init, PRG_CyclicFunc cyclic,
               PRG_PreFunc pre = nullptr, PRG_PostFunc post = nullptr);
int addCyclicTask(const char* name, int priority, TIME intervalUs);
int addFreewheelingTask(const char* name, int priority);
int addEventTask(const char* name, int priority,
                 EventConditionFunc cond, EventEdge edge = EventEdge::RISING);
bool addPOU(int taskIndex, POUFunc func);
bool addProgramToTask(int taskIndex, int programIndex);
void setTaskWatchdog(int taskIndex, TIME limitUs);
```

### 生命周期 API

```cpp
void start(StartupMode mode = StartupMode::COLD);   // 冷/暖启动
void stop();
void pause();
void resume();
void error();
void handleFault(ErrorCode code, const char* message = nullptr);
void resetError();          // 从 ERROR 恢复
void run(StartupMode mode); // 阻塞式主循环
void tick();                // 单次扫描（分阶段）
```

### 查询 API

```cpp
int taskCount() const;
int eventCount() const;
int programCount() const;
const Task& task(int idx) const;
Task& task(int idx);
const ProgramInstance& program(int idx) const;
RuntimeValidationResult validateConfig() const;
DiagSnapshot snapshotDiag() const;
void printDiag() const;
```

### 扫描阶段与启动流程

```
tick() 内部：
  ScanPhase::IDLE
    → 检查事件触发器（Event 任务）
    → 判断是否应执行任务（CYCLIC 检查是否到期，FREEWHEELING 总是执行）
    → 按优先级排序执行任务
  ScanPhase::READ_INPUTS
    → TCI::syncInputs(image)  ← 硬件输入刷新
  ScanPhase::LOGIC_SOLVE
    → 执行当前任务挂载的所有 POU
    → 每个 POU 内部：doInit() → doPre() → doCyclic() → doPost()
  ScanPhase::WRITE_OUTPUTS
    → TCI::syncOutputs(image)  ← 硬件输出刷新
  ScanPhase::HOUSEKEEPING
    → 看门狗检查、错误状态检查、RETAIN 保存、诊断统计
  → systemTime += baseCycleTime
  → totalTicks++

启动流程（start() 内部）：
  冷启动：
    → gvl.clear() (全部清零)
    → 所有 PROGRAM.init() 执行
    → phase = CYCLIC
  暖启动：
    → gvl.clearNonRetain() (非 RETAIN 区域清零)
    → gvl.loadRetain() (恢复 RETAIN)
    → 所有 PROGRAM.init() 执行
    → phase = CYCLIC
```

---

## 9. 看门狗 (`core/watchdog.h`)

```cpp
struct Watchdog {
    TIME defaultLimit;
    bool tripped;
    int  trippedTask;       // 触发的任务索引

    void setDefault(TIME limit);
    void reset();
    bool check(Task& task, int taskIndex);
};
```

每个任务可设置独立的 `watchdogLimit`。默认限时由 `Watchdog::defaultLimit` 指定。`check()` 在超时时返回 `true` 并记录 `trippedTask`。

---

## 10. 错误管理器 (`core/error_manager.h`)

```cpp
constexpr int MAX_ERROR_LOG = 32;   // Ring buffer 大小

struct ErrorEntry {
    ErrorCode code;          // 见 ErrorCode 枚举
    uint32_t  pouId;
    uint32_t  lineNo;       // ST 源码行号
    TIME      timestamp;
    int64_t   operandA, operandB;   // 上下文操作数
    char      message[64];
};

struct ErrorManager {
    ErrorEntry  log[MAX_ERROR_LOG];   // Ring buffer
    bool        fatalMode;            // 致命错误标记
    ErrorHandler handler;             // 外部回调

    void setHandler(ErrorHandler h);
    void report(ErrorCode code, uint32_t pouId, uint32_t line,
                const char* msg, TIME sysTime, int64_t opA = 0, int64_t opB = 0);
    void reset();
    void printLog() const;
    int count() const;
    bool isFatal() const;

    // 安全运算（溢出/除零检测）
    T safeDiv(T a, T b, ...);
    T safeMod(T a, T b, ...);
    T safeAdd(T a, T b, ...);
    T safeSub(T a, T b, ...);
    T safeMul(T a, T b, ...);
    T& safeArrayAt(T (&arr)[N], size_t index, ...);
};
```

错误码：

```cpp
enum class ErrorCode : uint16_t {
    NONE, DIV_BY_ZERO, ARRAY_OUT_OF_BOUNDS, INT_OVERFLOW,
    FLOAT_OVERFLOW, NULL_POINTER, STRING_OVERFLOW, STACK_OVERFLOW,
    WATCHDOG_TIMEOUT, IO_ERROR, TASK_OVERRUN, CONFIG_ERROR,
    SAFETY_VIOLATION, RETAIN_CORRUPTED, MISC_ERROR, USER_ERROR
};
```

---

## 11. 安全运算宏 (`rt_plc.h`)

编译器在 ST 运算符代码生成时使用以下宏（带 ErrorManager）：

```cpp
PLC_DIV(a, b, em, pouId, line, sysT)
PLC_MOD(a, b, em, pouId, line, sysT)
PLC_ADD(a, b, em, pouId, line, sysT)
PLC_SUB(a, b, em, pouId, line, sysT)
PLC_MUL(a, b, em, pouId, line, sysT)
PLC_ARRAY_AT_SAFE(arr, idx, lower, em, pouId, line, sysT)
```

---

## 12. 标准功能块 (`core/types.h:692`)

### 定时器

```cpp
struct TON {     // 接通延时
    BOOL  IN; TIME PT; BOOL Q; TIME ET;
    void update(TIME dt);
};

struct TOF {     // 断开延时
    BOOL  IN; TIME PT; BOOL Q; TIME ET;
    void update(TIME dt);
};

struct TP {      // 脉冲
    BOOL  IN; TIME PT; BOOL Q; TIME ET;
    void update(TIME dt);
};
```

### 计数器

```cpp
struct CTU {     // 增计数
    BOOL CU, R; INT PV; BOOL Q; INT CV;
    void update(TIME dt);
};

struct CTD {     // 减计数
    BOOL CD, LD; INT PV; BOOL Q; INT CV;
    void update(TIME dt);
};

struct CTUD {    // 增减计数
    BOOL CU, CD, R, LD; INT PV;
    BOOL QU, QD; INT CV;
    void update(TIME dt);
};
```

### 边沿检测

```cpp
struct R_TRIG {  // 上升沿
    BOOL CLK; BOOL Q;
    void update(TIME dt);
};

struct F_TRIG {  // 下降沿
    BOOL CLK; BOOL Q;
    void update(TIME dt);
};
```

### 双稳

```cpp
struct SR {      // 置位优先
    BOOL S1, R; BOOL Q1;
    void update(TIME dt);
};

struct RS {      // 复位优先
    BOOL S, R1; BOOL Q1;
    void update(TIME dt);
};
```

### 用法

```cpp
TON ton1;
ton1.IN = (counter >= 100);
ton1.update(cycleTime);    // dt = 1 cycle 时长
if (ton1.Q) { /* 500ms 后触发 */ }
```

`update()` 参数 `dt` 为上次调用以来经过的时间（微秒）。编译器生成的 POU 代码在 `cyclicFunc` 中传入 `cycleTimeUs`。

---

## 13. TCI — 硬件 I/O 抽象 (`rt_plc.h`)

```cpp
struct TCI {
    virtual ~TCI() = default;
    virtual void syncInputs(ProcessImage& img) = 0;
    virtual void syncOutputs(ProcessImage& img) = 0;
};

struct CompositeTCI : public TCI {
    void add(TCI* t);          // 最多 4 个叠加
    int  count() const;
    void syncInputs(ProcessImage& img) override;
    void syncOutputs(ProcessImage& img) override;
};
```

平台实现包括：

| 实现 | 位置 | 说明 |
|------|------|------|
| `GpioTCI` | `target/rpi/` | BCM GPIO 寄存器映射 |
| `NetworkTCI` | `runtime-flat/src/network_tci.cpp` | TCP socket I/O（后台线程） |
| `EthercatTCI` | `target/rpi/` 等 | SOEM 协议栈（EtherCAT） |

---

## 14. 互斥锁 (`rt_plc.h`)

```cpp
struct plc_lock {
    void lock();
    void unlock();
    bool tryLock();
};

struct plc_lock_guard {
    plc_lock_guard(plc_lock& l);   // lock()
    ~plc_lock_guard();             // unlock()
};
```

裸机：空操作（单线程）。桌面/Linux：`std::atomic_flag` spinlock。

---

## 15. RetainManager (`core/retain_manager.h`)

```cpp
class RetainManager {
    bool setRegion(size_t start, size_t end);
    void clearForStartup(StartupMode mode);
    void save();
    bool restore();
    void invalidate();
    bool hasRegion() const;
    bool hasBackup() const;
};
```

- `save()`：在 `_post` 阶段自动调用，将 RETAIN 区域拷贝到备份缓冲区
- `restore()`：暖启动时调用，从备份恢复
- `invalidate()`：标记备份无效（CRC 错误等）

---

## 16. 诊断 (`core/diag.h`)

```cpp
struct PhaseDiagStats {
    uint64_t count;
    TIME minTime, maxTime, totalTime, lastTime;
    void record(TIME elapsedUs);
    TIME avgTime() const;
};

struct DiagStats {
    uint64_t totalScanCount;
    TIME minScanTime, maxScanTime, totalScanTime, lastScanTime;
    uint64_t totalOverruns;
    PhaseDiagStats phases[SCAN_PHASE_COUNT];   // 5 阶段统计

    void recordScan(TIME scanTime);
    void recordPhase(ScanPhase phase, TIME elapsedUs);
    TIME avgScanTime() const;
};

struct DiagSnapshot { /* 可序列化的诊断快照 */ };
```

---

## 17. 错误策略 (`core/error_policy.h`)

```cpp
struct FaultPolicyDecision {
    bool recordError = true;
    bool enterError = false;
    bool applySafeOutputs = false;
    bool latch = false;
};

class ErrorPolicy {
    FaultPolicyDecision decide(ErrorCode code) const;
};
```

---

## 18. 运行时验证 (`core/runtime_validator.h`)

```cpp
class RuntimeValidator {
    RuntimeValidationResult validate(const Scheduler& sched) const;
};

struct RuntimeValidationResult {
    bool valid;
    char message[128];
};
```

---

## 19. 安全模块 (`safety/safety_module.h`)

```cpp
class SafetyModule {
    void feedWatchdog(BOOL safeInput);
    void check();
    BOOL isInSafeState() const;
    void setSafeOutputs(ProcessImage& img);
};
```

---

## 20. 平台抽象层 (`core/platform.h`)

三个编译模式通过预处理器选择：

| 宏 | 平台 | 定时 API |
|------|------|----------|
| (无) | 桌面 Windows/macOS | `chrono::steady_clock`, `thread::sleep_for` |
| `RT_PLATFORM_LINUX` | Linux | `clock_gettime(CLOCK_MONOTONIC_RAW)`, `nanosleep` |
| `RT_PLATFORM_BARE_METAL` | 裸机 | 用户提供实现 |

统一 API：

```cpp
namespace rt_plc::platform {
    int64_t nowUs();              // 系统实时时间戳（微秒）
    int64_t steadyUs();           // 单调时间（微秒）
    void    sleepUs(int64_t us);  // 微秒级睡眠
    void    logErr(const char* fmt, ...);
    void    logInfo(const char* fmt, ...);
}
```

日志宏：

```cpp
RT_LOG_ERR(...)    → platform::logErr(...)
RT_LOG_INFO(...)   → platform::logInfo(...)
```

---

## 21. 简单运行时 Runtime（向后兼容）(`rt_plc.h`)

```cpp
struct Runtime {
    ProcessImage image;
    TCI*         tci;
    uint64_t     cycleCount;
    uint32_t     cycleTimeUs;

    void setCycleTime(uint32_t us);
    void setTCI(TCI* t);

    void scanOnce(userProgram);   // syncInputs → userProgram → syncOutputs
    void run(userProgram);        // 阻塞循环，含补偿睡眠
    void stop();
};
```

---

## 常用组合模式

### 标准调度器模式

```cpp
#include "scheduler/scheduler.h"

rt_plc::Scheduler sched;
sched.setBaseCycle(1000);                       // 1ms 基础节拍

// 注册 PROGRAM（由编译器生成的 POU 代码提供）
sched.addProgram("main", init_main, cyclic_main, pre_main, post_main);

// 添加周期任务
int t1 = sched.addCyclicTask("fast", 10, 1000);    // 1ms 高优先级
sched.addProgramToTask(t1, 0);                      // 挂载 program 0

// 设置看门狗
sched.setTaskWatchdog(t1, 5000);                    // 5ms 超时

// 启动
sched.start();                                      // 冷启动
while (true) { sched.tick(); }                      // 周期性调用
```

### POURegistry + 配置桥接

```cpp
// pou_registry.gen.cpp（由构建脚本生成）
void registerAllPOUs(rt_plc::POURegistry& reg);

// runtime_main.cpp
void registerAllPOUs(rt_plc::POURegistry& reg) {
    // 每个 POU 注册一次：
    // registerPOU_xxx(reg);
}
```

### TCI 自定义

```cpp
struct MyGpioTCI : public rt_plc::TCI {
    void syncInputs(rt_plc::ProcessImage& img) override {
        img.writeInput<uint8_t>(0, readGpioPortA());
    }
    void syncOutputs(rt_plc::ProcessImage& img) override {
        writeGpioPortA(img.readOutput<uint8_t>(0));
    }
};

MyGpioTCI gpio;
sched.setTCI(&gpio);
```

### PLC 周期注入（STM32 中断）

```cpp
// TIM2 中断服务函数中
extern rt_plc::Scheduler g_scheduler;

void TIM2_IRQHandler() {
    if (TIM2->SR & TIM_SR_UIF) {
        TIM2->SR = ~TIM_SR_UIF;
        g_scheduler.tick();       // 所有 PLC 逻辑在 ISR 中执行
    }
}

int main() {
    plc_platform_init();
    plc_runtime_init();   // 注册 POU、配置调度器
    while (1) { __WFI(); }
}
```
