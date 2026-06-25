# runtime-flat — IEC 61131-3 平坦内存运行时

## 概述

`runtime-flat` 是 ST2C 项目的实时运行时，采用平坦内存模型（Flat Memory Model），面向工业 PLC 控制场景。所有变量存储在一块 64KB 的 `GVL.memory` 数组中，编译器计算偏移量，运行时通过 `ptr<T>(offset)` 一次解引用访问，实现零额外开销。

采用平坦内存模型（Flat Memory Model），编译器计算偏移量，运行时通过 `ptr<T>(offset)` 一次解引用，零额外开销。

## 目录结构

```
runtime-flat/
├── include/                      # 头文件
│   ├── rt_plc.h                  # 核心：类型系统 + 功能块 + 内置函数
│   ├── rt_runtime.h              # 核心：调度器 + GVL + PROGRAM 生命周期
│   ├── core/                     # 核心子模块（待扩展）
│   ├── scheduler/                # 调度器子模块（待扩展）
│   └── safety/                   # 安全模块接口（待扩展）
├── src/                          # 源文件实现
│   ├── scheduler.cpp             # 任务调度器
│   ├── program.cpp               # PROGRAM 生命周期管理
│   ├── gvl.cpp                   # GVL（全局变量列表）实现
│   ├── task.cpp                  # 任务定义与管理
│   ├── event.cpp                 # 事件触发机制
│   ├── watchdog.cpp              # 软件看门狗
│   └── diag.cpp                  # 诊断统计
├── tests/                        # 测试用例
│   ├── framework_test.cpp        # 框架完整性测试（82 项）
│   ├── fibonacci.cpp             # 基础类型验证
│   └── multitask_demo.cpp        # 多任务调度演示
├── CMakeLists.txt                # CMake 构建配置
├── runtime_main.cpp              # 运行时主程序入口（Scheduler 调度）
└── docs/                         # 本目录
    ├── README.md                 # 项目结构与开发指南
    └── TODO.md                   # 待办事项
```

## 构建

```bash
cd runtime-flat
mkdir build && cd build
cmake .. -G "MinGW Makefiles"   # Windows / MinGW
mingw32-make                     # 或 make
```

生成可执行文件：
- `fibonacci.exe` — 基础类型验证
- `multitask_demo.exe` — 多任务调度演示
- `framework_test.exe` — 框架完整性测试（82 项，应全部 PASS）

C++17 标准，无外部依赖。

## 核心架构

### 1. 平坦内存模型

```
GVL.memory[65536]
├── [0 .. offset1]      变量 A (INT, 2 bytes)
├── [offset1 .. offset2] 变量 B (REAL, 4 bytes, 4-byte aligned)
├── [offset2 .. offset3] 变量 C (BOOL, 1 byte)
└── ...
```

- 编译器在生成 C++ 代码时计算每个变量的偏移量（`Offsets::` 命名空间）
- 运行时通过 `gvl.read<T>(offset)` / `gvl.write<T>(offset, value)` 访问
- 对齐要求：REAL 4 字节、LREAL/LINT 8 字节（ARM 非对齐访问会 bus error）

### 2. 类型系统

在 `rt_plc.h` 中定义，遵循 IEC 61131-3 标准：

| IEC 类型 | C++ 类型 | 大小 | 说明 |
|----------|---------|------|------|
| BOOL | `bool` | 1B | 布尔值 |
| INT | `int16_t` | 2B | 16 位有符号整数 |
| DINT | `int32_t` | 4B | 32 位有符号整数 |
| LINT | `int64_t` | 8B | 64 位有符号整数 |
| REAL | `float` | 4B | 32 位浮点 |
| LREAL | `double` | 8B | 64 位浮点 |
| STRING | char[256] | 256B | 定长字符串 |
| TIME | `int32_t` | 4B | 时间（微秒） |

### 3. 调度器（Scheduler）

`rt_runtime.h` 中定义的 `Scheduler` 负责多任务调度：

- **Cyclic Task**：周期执行，按固定间隔触发
- **Event Task**：边沿触发，信号变化时执行一次
- **Freewheeling Task**：连续执行，每个 tick 都运行

调度流程：
```
tick()
  → ReadInputs（读取 ProcessImage）
  → 按优先级执行所有 Task
  → WriteOutputs（写入 ProcessImage）
  → Housekeeping（诊断/看门狗）
```

### 4. PROGRAM 生命周期

```
Init → Pre → FirstScan → Cyclic → Post
```

每个 `ProgramInstance` 绑定到一个 GVL 和 ProcessImage，POU 函数签名为：
```cpp
void name(GVL& gvl, ProcessImage& io, TIME cycleTimeUs);
```

### 5. 错误安全

- `ErrorManager::safeDiv()` — 除零返回 0 + 记录日志
- `ErrorManager::safeArrayAt()` — 越界返回安全值
- `ErrorManager::safeMod()` — 模零返回 0
- `fatalMode` 触发后进入 ERROR 状态，停止执行

## 代码约定

- 命名空间：`rt_plc`
- IEC 类型名大写：`INT`, `REAL`, `BOOL`, `STRING`
- 函数名遵循 IEC 标准：`TON`, `CTU`, `ABS`, `SEL`, `CONCAT`
- 模板类用 `PLC_` 前缀：`PLC_Array`, `PLC_Subrange`
- 内部辅助用 `plc_` 前缀：`plc_lock`, `plc_deref`
- 编译器生成的偏移常量用 `Offsets::` 命名空间

## 开发指南

### 修改 Runtime

1. 改 `include/rt_plc.h` 或 `include/rt_runtime.h`（或 `src/` 下对应实现）
2. 在 `tests/framework_test.cpp` 中添加对应测试
3. 构建并确认 82+ 项全部 PASS
4. 确认 `fibonacci` 和 `multitask_demo` 仍然通过（向后兼容）

### 添加新 IEC 类型

1. 在 `rt_plc.h` 的「基本类型」区域添加 typedef
2. 如需运行时支持（如 STRING），添加 struct + 操作函数
3. 如纯编译期处理（如 ENUM），只需文档说明编译器生成方式
4. 添加类型转换函数 `TO_XXX()`

### 添加新功能块

1. 在 `rt_plc.h` 的「标准功能块」区域添加 struct
2. struct 内包含 `update()` 方法
3. 在 `tests/framework_test.cpp` 中添加测试

## 注意事项

- **Windows 计时精度**：`std::chrono::steady_clock` 在 Windows 上精度约 15ms，测试中连续 `tick()` 可能不触发 `isDue()`。测试时用 1us 间隔或直接调用 `ProgramInstance` 方法绕过计时。
- **对齐**：编译器计算 GVL 偏移时必须保证 REAL 4 字节对齐、LREAL/LINT 8 字节对齐。ARM 平台非对齐访问会 bus error。
- **STRING 固定 256 字节**：不支持动态长度，超长截断。
- **GVL 固定 64KB**：RETAIN 区域固定 8KB，可按需调整常量。
- **单线程调度**：当前不支持多核。多任务通过优先级在单线程内串行执行。
- **功能块（FB）**：Runtime 已预留接口，但具体 FB 翻译逻辑待实现。
