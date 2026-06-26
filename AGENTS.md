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
│       ├── Main.java            # 入口（仅 Flat 后端）
│       ├── antlr4/              # ANTLR 生成的 Lexer/Parser/Visitor
│       ├── staticCheckVisitor/  # 语义检查 + 表达式预组装
│       ├── PLCTranslator/       # 代码生成器（Flat 后端）
│       │   ├── CodeGenerator.java       # 代码生成接口
│       │   ├── FlatCodeGenerator.java   # Flat 后端实现（GVL 偏移量风格）
│       │   └── TranslateType/           # 各语法节点的翻译器
│       ├── PLCSymbolAndScope/   # 符号表 + 作用域栈
│       ├── PLCTargetFileOutPut/ # 输出文件写入
│       ├── PLCException/        # 异常体系
│       ├── JSON/                # JSON 工具
│       └── com/st2c/lsp/        # LSP Server（新增）
│           ├── ST2CLanguageServer.java
│           ├── ST2CTextDocumentService.java
│           ├── ST2CWorkspaceService.java
│           └── analyzers/
├── runtime-flat/                # 实时 Runtime（Flat 后端）
│   ├── include/
│   │   ├── rt_plc.h             # 类型系统 + 功能块 + 内置函数
│   │   ├── rt_runtime.h         # 调度器 + GVL + PROGRAM 生命周期
│   │   └── core/                # 核心实现（GVL, ErrorManager, 调度器）
│   ├── runtime_main.cpp         # 运行时主程序（Scheduler 调度）
│   ├── tests/                   # 测试用例
│   │   ├── fibonacci.cpp        # 基础类型验证
│   │   ├── multitask_demo.cpp   # 多任务调度演示
│   │   └── framework_test.cpp   # 框架完整性测试（82 项）
│   └── build/                   # CMake 构建输出
├── examples/                    # ST 示例程序
│   ├── test.st                  # 原始测试（FOR 循环 + 函数调用）
│   ├── test_struct.st           # STRUCT 类型测试
│   └── test_arr_struct.st       # 数组 + STRUCT 测试
├── test.bat                     # 一键测试脚本（编译器 + runtime 集成）
├── README.md                    # 项目入口、架构概览、快速开始
├── CONTRIBUTING.md              # 开发指南、环境搭建、编码规范
├── compiler-to-runtime.md       # 编译器→运行时接口文档
├── st-support.md                # ST 语言支持矩阵
├── architecture.md              # 架构详解（类图、序列图、状态图）
├── ST2C实时化改造方案.md         # 详细设计文档
├── PLC运行时架构知识库.md        # PLC 架构背景知识
└── AGENTS.md                    # 本文件
```

## 构建

### C++ Runtime（runtime-flat/）

使用 CMake 构建，标准 C++17，无外部依赖。

```bash
cd runtime-flat/build
cmake .. -G "MinGW Makefiles"   # Windows / MinGW 首次
cmake --build .                  # 重新编译
```

CMake 变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `-DGEN_CPP_DIR=<path>` | `../output/flat/build` | Java 编译器产出的 `.cpp` 目录（相对于 `runtime-flat/`） |

`GEN_CPP_DIR` 必须指向 Java 编译器输出的目录。如果为空，CMake 不会创建 `runtime` 目标（仅编译独立测试套件）。

生成可执行文件：
- `fibonacci.exe` — 基础类型验证
- `multitask_demo.exe` — 多任务调度演示
- `framework_test.exe` — 框架完整性测试（112 项，应全部 PASS）
- `runtime.exe` — 集成生成的 POU 代码，含调度器主循环（仅当 `GEN_CPP_DIR` 非空）

### Java 编译器（独立步骤）

CMake 不管理 ST 编译。先构建编译器 JAR（只需一次）：

```bash
cd java
mvn package -DskipTests
# 产出: target/st2c-jar-with-dependencies.jar
```

然后使用 JAR 编译 ST 文件：

```bash
# 方式一：指定输出文件
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/test.st --output output/flat/build/test.cpp

# 方式二：指定输出目录（自动命名）
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/test.st --output-dir output/flat/build
```

选项：

| 参数 | 说明 |
|------|------|
| `--input <file>` | 输入 ST 源文件 |
| `--output <file>` | 输出 C++ 文件路径 |
| `--output-dir <dir>` | 输出目录，自动命名 `<dir>/<stem>.cpp` |
| `--file-id <id>` | POU 注册 ID（默认使用输出文件名 stem） |
| `--verbose` | 打印详细统计信息 |

生成多个 `.st` 文件时，每个文件独立调用一次 JAR。

### 集成运行

```bash
# 1. 构建 JAR（首次）
cd java && mvn package -DskipTests

# 2. ST → C++
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/test.st --output-dir output/flat/build

# 3. CMake 构建 C++
cd runtime-flat/build && cmake .. -G "MinGW Makefiles" -DGEN_CPP_DIR=../../output/flat/build && cmake --build .

# 4. 运行
./build/runtime.exe                    # 读取 tasks.json（默认）
./build/runtime.exe my_config.json     # 自定义配置
```

### 独立测试

```bash
test.bat              # 一键测试（编译器 + runtime 集成）
test.bat myprog.st    # 指定输入
```

`test.bat` 自动执行 Java 编译 → CMake 构建 → 运行的全流程。

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

### Flat 后端特征

| 特性 | 说明 |
|------|------|
| 头文件 | `rt_plc.h` + `rt_runtime.h` |
| 变量 | GVL 偏移量分配 |
| 函数 | `INT FUN(INT X)` |
| 表达式 | `gvl.read<INT>(0) + gvl.read<INT>(2)` |
| 返回值 | `return expr` |
| 调度 | `Scheduler::tick()` 多任务调度 |

## TODO 列表

### 高优先级（代码安全）

- [x] **GVL 偏移量越界检查**：`GVL::read<T>()` / `write<T>()` 已有边界检查（framework_test #13）
- [x] **ProcessImage 偏移量检查**：`readInput<T>()` / `writeOutput<T>()` 已有边界检查（framework_test #14）
- [x] **参数校验增强**：`Task::interval` / `priority` 已有 clamp 和校验（framework_test #15）
- [x] **除零/溢出错误码细化**：`safeDiv()` / `safeMod()` 已记录操作数（framework_test #16）

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
- [x] **RETAIN 区域标记**：编译器收集 RETAIN 变量并生成 `setRetainRegion()` 调用（已修复参数 bug：传 end 偏移而非长度）
- [x] **ARRAY 类型支持**：Flat 模式支持 `VAR` 内联数组声明和元素访问
- [x] **STRUCT 类型支持**：STRUCT 声明生成 C++ struct（对齐偏移），支持 `STRUCT.FIELD` 和 `ARR[I].FIELD`（读/写）
- [x] **FOR 循环 GVL 局部阴影**：FOR 循环控制变量使用局部副本，循环体引用跳过 GVL 替换，循环结束后写回 GVL
- [x] **WHILE / REPEAT 控制流**：翻译为原生 C++ `while()` / `do{}while()`
- [x] **CASE 控制流**：翻译为 if/else if 链
- [x] **ASSERT 断言**：翻译为条件检查 + 错误输出
- [x] **数组下标 GVL 替换**：readExpr/writeExpr/translateExpr 三处均已修复
- [x] **CASE 翻译修复**：case_list_elem 已走 translateExpr
- [x] **fileId 后缀修复**：从输入文件名派生，去掉 .st 后缀
- [x] **ErrorManager 循环依赖**：提取到独立头文件 error_manager.h


### Runtime 架构扩展

- [ ] **runtime_main 链接失败**：CMake 的 `runtime_main` 目标未链接 `pou_registry.gen.cpp`
- [ ] **多核调度**：当前单线程串行
- [ ] **动态任务注册**：当前任务在编译期固定
- [ ] **OPC UA / MQTT 集成**：工业协议通信接口
- [ ] **在线下载/热更新**

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
2. `FlatCodeGenerator` 实现具体后端
3. `PLCTranslatorNew`（Visitor 调度器）遍历语法树，调用 `TranslateXxx`
4. `TranslateXxx` 类使用 `PLCTranslatorNew.codeGen` 代码生成器
5. `FlatCodeGenerator.translateExpr()` 将中间表达式转换为原生 C++

## 陷阱

### Include 文件阴影（Shadow）

`runtime-flat/` 目录下有旧版 `rt_runtime.h`（已删除），同名的 `runtime-flat/include/rt_runtime.h` 是正确版本。`#include "..."` 优先在同目录下查找，因此编译 `runtime_main.cpp` 时会找到错误的旧版头文件。旧版不含 `core/registry.h`，会导致 `POURegistry` not in scope。

**修复**：删除旧版，确保 `-I runtime-flat/include` 下只有正确版本。

## 语法修改记录

### 2025-06-25-4: Bug 修复 + 审计 + 文档更新

**修复 Bug**：
1. **数组下标 GVL 替换缺失**：`readExpr()`、`writeExpr()`、`translateExpr()` step 5 三处数组下标未调用 `translateExpr()`，导致非 FOR 场景下生成裸变量名
2. **CASE 翻译遗漏**：`TranslateCase_stmt.java` 中多个 case_list_elem 直接拼接原始文本
3. **fileId 后缀**：`Main.java` 从输出文件名派生 fileId，改为从输入文件名派生并去掉 `.st`
4. **gvl.h 循环依赖**：`ErrorManager` 提取到独立头文件 `error_manager.h`，解决 forward declaration 警告
5. **RETAIN 参数 bug**：`TranslateProg_decl.java` 传长度 `retainLength` 给 `setRetainRegion(start, end)`，改为传结束偏移 `retainEnd`

**文档更新**：
- st-support.md / README.md：WHILE/REPEAT/CASE 标为已实现
- AGENTS.md / runtime-flat/docs/TODO.md：基于全项目审计重写 TODO 列表

**全项目审计发现**：
- 24 处无保护的 PLCVariable 强转（ClassCastException 风险）
- 多文件同名 PROGRAM 链接冲突
- runtime_main 未链接 pou_registry.gen.cpp
- FB 翻译、ENUM 类型、VAR_INPUT/OUTPUT 为最大功能缺失

### 2025-06-25-3: POURegistry + 通用 runtime + 构建脚本（清理遗留文件）

**完成内容**：
1. POURegistry 类
2. 编译器生成 `registerPOU_<fileId>()`
3. runtime_main.cpp 重构为通用版（JSON 解析 + registry 查找）
4. build.ps1 / build.bat 构建脚本
5. tasks.json 配置文件
6. **删除** `runtime-flat/generated_pou.cpp` 和 `runtime-flat/rt_runtime.h`（遗留旧版）

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

### 2025-06-25-2: PRINT 语句 + 外部函数声明 + 字符串字面量 lexer 修复

**PRINT 功能**：
1. PRINT 语句语法（`PRINT(elem + elem ...)`），每个元素可以是标识符或字符串字面量
2. 静态检查 `VisitPrint_stmt_element.java`：标识符 → `assignVar=变量名`（代码生成 step 7 转 GVL 读），字符串 → 创建 `sort=STRING` 的变量
3. 代码生成 `TranslatePrint_stmt.java`：逐元素调用 `emitPrintElement`，末尾加 `\n`；字符串跳过 `translateExpr` 避免 step 7 误替换
4. `FlatCodeGenerator.emitPrintElement`：字符串输出 `printf(literal)`，整型输出 `printf("%d", (int)(expr))`

**外部函数声明**：
1. `VisitFunc_decl.java`：检查 `func_body` 是否含实际语句（ANTLR 空 `stmt_list` 视为无 body），无 body 时不要求 RETURN
2. `TranslateFunc_decl.java`：同样检测有 body 才生成完整函数，否则生成 C++ 原型（`RET_TYPE FUNC(PARAMS);`）

**字符串字面量 lexer 修复**：
- **问题**：`Identifier` 在 `S_byte_char_value`/`D_byte_char_value` 之前定义，导致 `"hello"` 中字母被 lex 为 `Identifier` 而非字符串内容 token，字符串字面量无法解析
- **方案**：添加 `StringLiteralD`/`StringLiteralS` 完整字符串 lexer token（含引号），定义在 `Identifier` 之前
- 替换 `s_byte_char`/`D_byte_char` 解析器规则为单 token 匹配，删弃 `d_byte_char_value` 旧规则
- ANTLR 4.10.1 重新生成

**修改文件**：
1. `java/src/main/resources/antlr4/PLCSTLEXER.g4` — 新增 `StringLiteralS`/`StringLiteralD` 在 `Identifier` 之前
2. `java/src/main/resources/antlr4/PLCSTPARSER.g4` — 简化 `s_byte_char`/`D_byte_char`，恢复 `print_stmt_element` 三路选择，删 `d_byte_char_value`
3. `java/src/main/java/staticCheckVisitor/strategy/pou_decl/VisitFunc_decl.java` — 修正空 func_body 检测
4. `java/src/main/java/PLCTranslator/TranslateType/Func_decl/TranslateFunc_decl.java` — 同上
5. `java/src/main/java/staticCheckVisitor/strategy/fb_body/VisitPrint_stmt_element.java` — 字符串 + 标识符处理
6. `java/src/main/java/PLCTranslator/TranslateType/Stmt/Print_stmt/TranslatePrint_stmt.java` — 字符串跳过 translateExpr
7. `java/src/main/java/PLCTranslator/CodeGenerator.java` — `emitPrintStmt` → `emitPrintElement`
8. `java/src/main/java/PLCTranslator/FlatCodeGenerator.java` — `emitPrintElement` 实现，移除 `emitMain`/`programNames`

**测试**：
- `examples/test_print.st` — PRINT(字符串) + PRINT(变量) + 外部函数 PRINTF ✅
- `examples/test.st` — FOR 循环 + 函数调用 ✅
- `examples/test_struct.st` — STRUCT ✅
- `examples/test_arr_struct.st` — 数组 + STRUCT + FOR ✅
- Framework tests 112/112 PASS ✅

### 2025-06-25: 实现 STRUCT 完整支持 + FOR 循环 GVL 局部阴影

**完成内容**：
1. STRUCT 声明 → C++ struct 定义（FlatCodeGenerator + TranslateStruct_type_decl）
2. `STRUCT.FIELD` 读/写 — 静态检查（`VisitThis_symbol`）→ 代码生成（`writeExpr` LHS + `translateExpr` step 7 RHS）
3. `ARR[I].FIELD` 混合访问 — 新 `VisitNamespaceSymbolic.java`（处理 `multi_elem_var` 交替的 `subscript_list`/`struct_variable`）
4. `PLCVisitor.java:949` — `visitNamespaceSymbolic()` 修复分支参数（`factory.getStrategy(ctx.getRuleIndex(), 1)`）
5. FOR 循环 GVL 变量 bug — `emitForBegin` 创建局部副本 + `shadowedGvlVars`/`shadowStack` 跟踪；`translateExpr` step 7 跳过遮盖变量；`emitForEnd` 写回 GVL 并弹出 shadow

**新文件**：
- `java/src/main/java/staticCheckVisitor/strategy/variable_access/VisitNamespaceSymbolic.java`

**测试**：
- `examples/test.st` — 原始 FOR 循环 ✅
- `examples/test_struct.st` — 简单 STRUCT ✅
- `examples/test_arr_struct.st` — 数组 + STRUCT + FOR 循环 ✅

## 注意事项

- **Windows 计时精度**：`std::chrono::steady_clock` 在 Windows 上精度约 15ms，测试中连续 `tick()` 可能不触发 `isDue()`。测试时用 1us 间隔或直接调用 `ProgramInstance` 方法绕过计时。
- **对齐**：编译器计算 GVL 偏移时必须保证 REAL 4 字节对齐、LREAL/LINT 8 字节对齐。ARM 平台非对齐访问会 bus error。
- **STRING 固定 256 字节**：不支持动态长度，超长截断。
- **GVL 固定 64KB**：RETAIN 区域固定 8KB，可按需调整常量。
- **单线程调度**：当前不支持多核。多任务通过优先级在单线程内串行执行。
- **功能块（FB）**：Runtime 已预留接口，但具体 FB 翻译逻辑待实现。
- **标识符大小写**：ST 语法要求标识符必须大写（`[A-Z][A-Z0-9$_]*`），小写变量名会导致解析失败。
