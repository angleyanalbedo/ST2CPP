# ST2C — IEC 61131-3 Structured Text to C++ Transpiler

## 项目概述

ST2C 将 IEC 61131-3 结构化文本（ST）编译为 C++ 代码。整个流程为：

```
ST 源码 → ANTLR4 解析 → 静态语义分析 → 代码生成 → C++ → 编译 → PLC 运行时
```

项目有两套 Runtime：旧版（`Runtime/lib/`，Type-Value 包装模型，不可实时）和新版（`Runtime/rt/`，平坦内存模型，面向实时）。新 Runtime 已完成，Java 编译器改造待进行。

## 目录结构

```
ST2C-master/
├── java/                        # Java 编译器（ANTLR4 + 语义分析 + 代码生成）
│   └── src/main/java/
│       ├── Main.java            # 入口
│       ├── antlr4/              # ANTLR 生成的 Lexer/Parser/Visitor
│       ├── staticCheckVisitor/  # 语义检查 + 表达式预组装（核心，改造重点）
│       ├── PLCTranslator/       # 代码生成器（~49 个文件）
│       ├── PLCSymbolAndScope/   # 符号表 + 作用域栈
│       ├── PLCTargetFileOutPut/ # 输出文件写入
│       ├── PLCException/        # 异常体系
│       └── JSON/                # JSON 工具
├── Runtime/
│   ├── lib/                     # 旧 Runtime（Type-Value 模型，~57 文件）
│   │   ├── PLC.h               # 主头文件
│   │   ├── varMap/              # 变量注册表
│   │   ├── PLC_INNER/           # Type-Value 模板
│   │   └── ...
│   └── rt/                      # 新实时 Runtime（已完成）
│       ├── rt_plc.h             # 类型系统 + 功能块 + 内置函数
│       ├── rt_runtime.h         # 调度器 + GVL + PROGRAM 生命周期
│       ├── fibonacci.cpp        # 基础验证
│       ├── multitask_demo.cpp   # 多任务演示
│       ├── framework_test.cpp   # 框架完整性测试（82 项）
│       └── CMakeLists.txt
├── ST2C实时化改造方案.md         # 详细设计文档
└── AGENTS.md                    # 本文件
```

## 构建

### 新 Runtime（Runtime/rt/）

```bash
cd Runtime/rt
mkdir build && cd build
cmake .. -G "MinGW Makefiles"   # Windows / MinGW
# cmake ..                       # Linux / macOS
mingw32-make                     # 或 make
```

生成三个可执行文件：
- `fibonacci` — 基础类型验证
- `multitask_demo` — 多任务调度演示
- `framework_test` — 框架完整性测试（82 项，应全部 PASS）

C++17 标准，无外部依赖。

### Java 编译器

```bash
cd java
mvn compile                     # 或 gradle build
java -jar target/st2c.jar input.st
```

### 旧 Runtime

```bash
cd Runtime
# 使用旧的 CMakeLists.txt 或 Makefile
```

## 代码约定

### C++ Runtime（Runtime/rt/）

- 命名空间：`rt_plc`
- IEC 类型名大写：`INT`, `REAL`, `BOOL`, `STRING`
- 函数名遵循 IEC 标准：`TON`, `CTU`, `ABS`, `SEL`, `CONCAT`
- 模板类用 `PLC_` 前缀：`PLC_Array`, `PLC_Subrange`
- 内部辅助用 `plc_` 前缀：`plc_lock`, `plc_deref`
- 编译器生成的偏移常量用 `Offsets::` 命名空间
- POU 函数签名：`void name(GVL& gvl, ProcessImage& io, TIME cycleTimeUs)`
- 错误安全操作通过 `ErrorManager` 方法：`safeDiv()`, `safeArrayAt()`, `safeMod()`

### Java 编译器

- 包名驼峰：`PLCTranslator`, `staticCheckVisitor`
- Visitor 方法大写开头：`VisitFunc_decl`, `VisitProg_decl`
- 翻译器类 `Translate` 前缀：`TranslateFunc_decl`, `TranslateCallFunc`
- 工厂方法在 `packageFactory` 中

## 架构要点

### 新 Runtime 核心设计

1. **平坦内存模型**：所有变量在一块 `GVL.memory[64KB]` 中，编译器计算偏移，运行时 `ptr<T>(offset)` 一次解引用
2. **原生类型**：`INT = int16_t`, `REAL = float`，运算零开销
3. **PROGRAM 生命周期**：Init → Pre → FirstScan → Cyclic → Post
4. **分阶段扫描**：ReadInputs → LogicSolve → WriteOutputs → Housekeeping
5. **多任务**：Cyclic（周期）/ Event（边沿触发）/ Freewheeling（连续）
6. **冷/暖启动**：RETAIN 区域在暖启动时保留
7. **错误不崩溃**：除零/越界/null 返回安全值 + 记录日志 + fatalMode → ERROR 状态

### 旧 Runtime 问题（理解为什么需要改造）

- 每个变量是堆对象 `new INT(42)`，运算时 `new` 临时对象
- 全局 `varMap` 做 ID → 对象查找，每次变量访问都是 map lookup
- `dynamic_cast` 遍布热路径
- 表达式在 Java 语义检查层拼成字符串，代码生成质量不可控

### Java 编译器改造方向

改造的核心是把「Type-Value 包装」翻译成「原生类型 + 偏移访问」：

| 旧生成 | 新生成 |
|---|---|
| `new INT(42)` | `(INT)42` |
| `RFM->getSymbolByID<INT*>(id)` | `gvl.ptr<INT>(offset)` |
| `*X + *Y`（解引用包装对象） | `*X_ptr + *Y_ptr`（解引用原生指针） |
| `PLC_Function<>` 基类 | 普通 C++ 函数 |

详见 `ST2C实时化改造方案.md` 第四节。

## 开发指南

### 修改 Runtime

1. 改 `rt_plc.h` 或 `rt_runtime.h`
2. 在 `framework_test.cpp` 中添加对应测试
3. 构建并确认 82+ 项全部 PASS
4. 确认 `fibonacci` 和 `multitask_demo` 仍然通过（向后兼容）

### 添加新 IEC 类型

1. 在 `rt_plc.h` 的「基本类型」区域添加 typedef
2. 如需运行时支持（如 STRING），添加 struct + 操作函数
3. 如纯编译期处理（如 ENUM），只需文档说明编译器生成方式
4. 添加类型转换函数 `TO_XXX()`

### 添加新功能块

1. 在 `rt_plc.h` 的「标准功能块」区域添加 struct
2. struct 内包含 `update()` 方法，签名统一为 `void update(BOOL IN, TIME PT, TIME cycleTimeUs)` 或类似
3. 在 `framework_test.cpp` 中添加测试

### Java 编译器改造步骤

参考 `ST2C实时化改造方案.md` 4.4 节的 Phase 1-4：

1. **Phase 1**：先做 FUNCTION + PROGRAM + INT/REAL/BOOL/STRING
2. 先实现 Memory Layout Pass（最关键的新模块）
3. 再改 packageFactory 和表达式翻译
4. 最后改各 Translate 类

### 测试策略

- **Runtime 单元测试**：`framework_test.cpp`，覆盖所有框架功能
- **Runtime 集成测试**：`fibonacci.cpp`（类型系统）、`multitask_demo.cpp`（调度器）
- **编译器测试**：用 `input.st` 文件编译后链接新 Runtime 运行
- **回归测试**：每次修改 Runtime 后确认旧的 fibonacci 和 multitask_demo 仍然编译通过

## 注意事项

- **Windows 计时精度**：`std::chrono::steady_clock` 在 Windows 上精度约 15ms，测试中连续 `tick()` 可能不触发 `isDue()`。测试时用 1us 间隔或直接调用 `ProgramInstance` 方法绕过计时。
- **对齐**：编译器计算 GVL 偏移时必须保证 REAL 4 字节对齐、LREAL/LINT 8 字节对齐。ARM 平台非对齐访问会 bus error。
- **STRING 固定 256 字节**：不支持动态长度，超长截断。
- **GVL 固定 64KB**：RETAIN 区域固定 8KB，可按需调整常量。
- **单线程调度**：当前不支持多核。多任务通过优先级在单线程内串行执行。
- **功能块（FB）**：Runtime 已预留接口（POUFunc + state struct 模式），但具体 FB 翻译逻辑待 Java 编译器 Phase 3 实现。
