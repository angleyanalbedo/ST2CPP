# ST2C — IEC 61131-3 Structured Text to C++ Transpiler

## 项目概述

ST2C 将 IEC 61131-3 结构化文本（ST）编译为 C++ 代码。整个流程为：

```
ST 源码 → ANTLR4 解析 → 静态语义分析 → 代码生成 → C++ → 编译 → PLC 运行时
```

项目有两套 Runtime：旧版（`Runtime/lib/`，Type-Value 包装模型，不可实时）和新版（`runtime-flat/`，平坦内存模型，面向实时）。

## 目录结构

```
ST2C-master/
├── java/                        # Java 编译器（ANTLR4 + 语义分析 + 代码生成）
│   └── src/main/java/
│       ├── Main.java            # 入口（支持 --backend oop|flat）
│       ├── antlr4/              # ANTLR 生成的 Lexer/Parser/Visitor
│       ├── staticCheckVisitor/  # 语义检查 + 表达式预组装
│       ├── PLCTranslator/       # 代码生成器（OOP/Flat 双后端）
│       ├── PLCSymbolAndScope/   # 符号表 + 作用域栈
│       ├── PLCTargetFileOutPut/ # 输出文件写入
│       ├── PLCException/        # 异常体系
│       └── JSON/                # JSON 工具
├── runtime-flat/                # 新实时 Runtime（Flat 后端）
│   ├── rt_plc.h                 # 类型系统 + 功能块 + 内置函数
│   ├── rt_runtime.h             # 调度器 + GVL + PROGRAM 生命周期
│   ├── runtime_main.cpp         # 运行时主程序（Scheduler 调度）
│   ├── tests/                   # 测试用例
│   │   ├── fibonacci.cpp        # 基础类型验证
│   │   ├── multitask_demo.cpp   # 多任务调度演示
│   │   └── framework_test.cpp   # 框架完整性测试（82 项）
│   └── build/                   # CMake 构建输出
├── runtime-oop/                 # 旧 OOP Runtime（PLC_Value 模型）
│   └── ...
├── examples/                    # ST 示例程序
│   └── test.st
├── test-flat.bat                # Flat 后端测试脚本（编译器 + runtime 集成）
├── ST2C实时化改造方案.md         # 详细设计文档
└── AGENTS.md                    # 本文件
```

## 构建

### Flat Runtime（runtime-flat/）

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

### Java 编译器

```bash
cd java
mvn compile

# OOP 后端
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend oop --input ../examples/test.st --output ../output/oop/main.cpp"

# Flat 后端
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend flat --input ../examples/test.st --output ../output/flat/main.cpp"
```

### 集成测试（Flat 后端 + rt_runtime）

```bash
test-flat.bat              # 默认 examples\test.st
test-flat.bat myprog.st    # 指定输入
```

流程：编译 Java → Flat 翻译 → 编译 runtime_main + generated_pou → Scheduler 调度运行

## 代码约定

### C++ Runtime（runtime-flat/）

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
- `CodeGenerator` 接口：所有 `emitXxx()` 返回 `String`（不再直接写文件）
- `PLCTranslatorNew` 泛型：`String`（原 `ArrayList<String>`）

## 架构要点

### Flat Runtime 核心设计

1. **平坦内存模型**：所有变量在一块 `GVL.memory[64KB]` 中，编译器计算偏移，运行时 `ptr<T>(offset)` 一次解引用
2. **原生类型**：`INT = int16_t`, `REAL = float`，运算零开销
3. **PROGRAM 生命周期**：Init → Pre → FirstScan → Cyclic → Post
4. **分阶段扫描**：ReadInputs → LogicSolve → WriteOutputs → Housekeeping
5. **多任务**：Cyclic（周期）/ Event（边沿触发）/ Freewheeling（连续）
6. **冷/暖启动**：RETAIN 区域在暖启动时保留
7. **错误不崩溃**：除零/越界/null 返回安全值 + 记录日志 + fatalMode → ERROR 状态
8. **双后端**：OOP（PLC_Value 调试）和 Flat（GVL 偏移量产）

### OOP vs Flat 后端对比

| 特性 | OOP 后端 | Flat 后端 |
|------|---------|----------|
| 头文件 | `PLC.h` | `rt_plc.h` + `rt_runtime.h` |
| 变量 | `auto* A = new INT(10)` | GVL 偏移量分配 |
| 函数 | `class FUN : public PLC_Function<INT>` | `INT FUN(INT X)` |
| 表达式 | `*A + *B` | `gvl.read<INT>(0) + gvl.read<INT>(2)` |
| 返回值 | `*this->returnValue = expr` | `return expr` |
| 调度 | `main()` 直接调用 | `Scheduler::tick()` 多任务调度 |
| 用途 | 调试/教学 | 生产/实时 |

## TODO 列表

### 高优先级（代码安全）

- [ ] **GVL 偏移量越界检查**：`GVL::read<T>()` / `write<T>()` 添加 `offset + sizeof(T) <= GVL_SIZE` 断言
- [ ] **ProcessImage 偏移量检查**：`readInput<T>()` / `writeOutput<T>()` 添加边界校验
- [ ] **参数校验增强**：`Task::interval` 正值检查、`priority` 范围检查、`offset` 有效性检查
- [ ] **除零/溢出错误码细化**：当前 `safeDiv()` 仅返回 0，需记录具体错误上下文

### 中优先级（可观测性）

- [ ] **周期报警阈值**：`DiagStats` 增加 `alarmThreshold` 和报警回调接口
- [ ] **抖动（jitter）统计**：标准差、百分位数（P99/P95）
- [ ] **OPC UA 报警接口**：`printDiag()` 仅输出到 stdout，需工业协议输出
- [ ] **错误码标准化**：扩展为 IEC 61131-3 / PLCopen 风格的分层错误码（0x8xxx 格式）
- [ ] **轴级超时**：新增 `Axis` 对象，每轴独立超时保护

### 低优先级（工业认证）

- [ ] **STO（安全转矩关闭）**：新增 `SafetyModule` 接口，硬件安全回路抽象
- [ ] **硬件看门狗**：独立硬件 WDT 接口（与软件看门狗互补）
- [ ] **安全状态机**：与主状态机物理隔离或逻辑强隔离的独立安全路径
- [ ] **TÜV 认证准备**：需求追溯矩阵、FMEA/FTA 文档、故障注入测试框架
- [ ] **SIL/PL 等级**：SIL2/SIL3 所需的冗余、诊断覆盖度

### 编译器改进

- [x] **临时变量类型推断**：`auto _X0=A` 改为具体类型 `INT _X0=A`
- [x] **GVL 变量在表达式中的转换**：`(A)` 改为 `(gvl.read<INT>(0))`
- [x] **RETAIN 区域标记**：编译器生成 `setRetainRegion()` 调用
- [x] **ARRAY 类型支持**：Flat 模式支持 `VAR` 内联数组声明和元素访问
- [ ] **FB（功能块）翻译**：当前仅支持 FUNCTION 和 PROGRAM
- [ ] **STRUCT 类型支持**：当前生成 `// Flat: TODO`

## 开发指南

### 修改 Runtime

1. 改 `rt_plc.h` 或 `rt_runtime.h`
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

### Java 编译器改造

参考 `ST2C实时化改造方案.md`：

1. `CodeGenerator` 接口定义后端行为（`emitHeader`, `emitAssign`, `emitForBegin` 等）
2. `OOPCodeGenerator` / `FlatCodeGenerator` 实现具体后端
3. `PLCTranslatorNew`（Visitor 调度器）遍历语法树，调用 `TranslateXxx`
4. `TranslateXxx` 类根据 `translatorNew.codeGen` 类型选择 OOP/Flat 代码生成路径
5. `FlatCodeGenerator.translateExpr()` 将 OOP 风格表达式转换为原生 C++

## 语法修改记录

### 2025-06-24: 修复 ARRAY/STRUCT/CLASS 关键字识别

**问题**：`Identifier` 规则在 `ReservedKeyword` 之前定义，导致 `ARRAY`、`STRUCT`、`TYPE` 等关键字被识别为标识符，无法用于类型声明。

**修改文件**：
1. `java/src/main/resources/antlr4/PLCSTLEXER.g4`
   - 在 `Identifier` 之前添加独立关键字规则：`ARRAY_KW`、`OF_KW`、`STRUCT_KW`、`END_STRUCT_KW`、`OVERLAP_KW`、`TYPE_KW`、`END_TYPE_KW`、`REF_TO_KW`、`REF_KW`、`THIS_KW`
   - 从 `ReservedKeyword` 中移除已单独定义的关键字

2. `java/src/main/resources/antlr4/PLCSTPARSER.g4`
   - `startpoint` 规则：添加 `data_type_decl`、`class_decl`、`interface_decl` 直接支持
   - 将所有 `'ARRAY'` 替换为 `ARRAY_KW`，`'OF'` 替换为 `OF_KW`，`'STRUCT'` 替换为 `STRUCT_KW`，等等
   - `derived_type_access` 添加 `array_spec_init` 和 `struct_spec_init` 支持

**验证结果**：
- `test.st` 正常工作 ✅
- `test_array.st` 语法解析成功（`ARRAY[0..4] OF INT`）✅
- 语义检查阶段报错：`type mismatch : ARR[I]:=I*10`（需修复静态检查器）

**待修复**：静态检查器 `VisitVariableAssignExpression` 需要支持数组元素访问的类型推断。

## 注意事项

- **Windows 计时精度**：`std::chrono::steady_clock` 在 Windows 上精度约 15ms，测试中连续 `tick()` 可能不触发 `isDue()`。测试时用 1us 间隔或直接调用 `ProgramInstance` 方法绕过计时。
- **对齐**：编译器计算 GVL 偏移时必须保证 REAL 4 字节对齐、LREAL/LINT 8 字节对齐。ARM 平台非对齐访问会 bus error。
- **STRING 固定 256 字节**：不支持动态长度，超长截断。
- **GVL 固定 64KB**：RETAIN 区域固定 8KB，可按需调整常量。
- **单线程调度**：当前不支持多核。多任务通过优先级在单线程内串行执行。
- **功能块（FB）**：Runtime 已预留接口，但具体 FB 翻译逻辑待实现。
- **标识符大小写**：ST 语法要求标识符必须大写（`[A-Z][A-Z0-9$_]*`），小写变量名会导致解析失败。
