# ST 语言支持矩阵

IEC 61131-3 结构化文本（ST）特性在 ST2C++ 中的支持状态。

## 状态说明

| 标记 | 含义 |
|------|------|
| ✅ | 已完整支持 |
| ⚠️ | 部分支持（有已知限制） |
| 🔲 | 未支持（计划中） |
| ❌ | 不计划支持 |

---

## 1. 程序组织单元（POU）

| 特性 | 状态 | 说明 |
|------|------|------|
| PROGRAM | ✅ | 编译为 `void PROGRAM_X(GVL&, ProcessImage&, TIME)` |
| FUNCTION | ✅ | 编译为独立 C++ 函数 |
| FUNCTION_BLOCK | 🔲 | Runtime 已预留接口，编译器未实现 |
| CLASS | 🔲 | 语法解析通过，代码生成未实现 |
| INTERFACE | 🔲 | 语法解析通过，代码生成未实现 |
| NAMESPACE | 🔲 | 语法解析通过，代码生成未实现 |
| METHOD | 🔲 | 语法解析通过，代码生成未实现 |

## 2. 变量声明

| 特性 | 状态 | 说明 |
|------|------|------|
| VAR | ✅ | GVL 偏移量分配 |
| VAR_INPUT | ⚠️ | 语法解析，FUNCTION 参数传递 |
| VAR_OUTPUT | 🔲 | 未实现 |
| VAR_IN_OUT | 🔲 | 未实现 |
| VAR_GLOBAL | ⚠️ | 通过 PROGRAM 的 VAR 实现 |
| VAR_TEMP | 🔲 | 未实现 |
| VAR_EXTERNAL | 🔲 | 未实现 |
| VAR_ACCESS | 🔲 | 未实现 |
| RETAIN | ⚠️ | 语法解析，编译器未生成 `setRetainRegion()` |
| CONSTANT | 🔲 | 未实现 |
| AT（地址绑定） | 🔲 | 未实现 |

## 3. 数据类型

| 类型 | 状态 | C++ 映射 | 大小 |
|------|------|---------|------|
| BOOL | ✅ | `bool` | 1B |
| SINT | ✅ | `int8_t` | 1B |
| INT | ✅ | `int16_t` | 2B |
| DINT | ✅ | `int32_t` | 4B |
| LINT | ✅ | `int64_t` | 8B |
| USINT | ✅ | `uint8_t` | 1B |
| UINT | ✅ | `uint16_t` | 2B |
| UDINT | ✅ | `uint32_t` | 4B |
| ULINT | ✅ | `uint64_t` | 8B |
| REAL | ✅ | `float` | 4B |
| LREAL | ✅ | `double` | 8B |
| BYTE | ✅ | `uint8_t` | 1B |
| WORD | ✅ | `uint16_t` | 2B |
| DWORD | ✅ | `uint32_t` | 4B |
| LWORD | ✅ | `uint64_t` | 8B |
| STRING | ✅ | `char[256]` | 256B |
| TIME | ✅ | `int64_t`（微秒） | 8B |
| DATE | 🔲 | — | — |
| TIME_OF_DAY | 🔲 | — | — |
| DATE_AND_TIME | 🔲 | — | — |
| WCHAR | 🔲 | — | — |
| WSTRING | 🔲 | — | — |
| STRUCT | ✅ | C++ struct（对齐偏移） | 按字段累加 |
| ARRAY | ✅ | `PLC_Array<T,N>` 或连续内存 | 元素大小 × 数量 |
| ENUM | 🔲 | — | — |
| REF_TO | 🔲 | — | — |
| POINTER TO | ⚠️ | 语法解析 | — |
| ARRAY + STRUCT 混合 | ✅ | `gvl.safeArrayAt<STRUCT>(...).FIELD` | — |

## 4. 表达式与运算

| 特性 | 状态 | 说明 |
|------|------|------|
| 赋值 `:=` | ✅ | `gvl.write<T>(offset, expr)` |
| 算术运算 `+ - * /` | ✅ | 原生 C++ 运算符 |
| 比较运算 `= <> < > <= >=` | ✅ | 原生 C++ 运算符 |
| 逻辑运算 `AND OR XOR NOT` | ✅ | 原生 C++ 运算符 |
| 括号优先级 | ✅ | — |
| 函数调用 | ✅ | `FUNC(args)` |
| 标准函数调用 | ✅ | ABS, SQRT, SEL, MIN, MAX 等 |
| 类型转换 | ✅ | TO_INT, TO_REAL, TO_BOOL 等 |
| 字符串连接 | ⚠️ | PRINT 语句中支持 |
| 字符串操作 | ⚠️ | CONCAT, LEN, LEFT, RIGHT 等（运行时已实现，编译器未完全集成） |
| 位运算 | ⚠️ | 运行时已实现（SHL, SHR, ROL, ROR），编译器未完全集成 |
| 取模运算 `MOD` | ✅ | `safeMod()` |
| 幂运算 `**` | 🔲 | — |
| 三元运算 `? :` | 🔲 | — |

## 5. 控制流

| 特性 | 状态 | 说明 |
|------|------|------|
| IF / THEN / ELSIF / ELSE / END_IF | ✅ | 原生 C++ `if/else if/else` |
| FOR / TO / BY / DO / END_FOR | ✅ | GVL 变量遮盖 + 循环后写回 |
| WHILE / END_WHILE | ✅ | 原生 C++ `while()` |
| REPEAT / END_REPEAT | ✅ | 原生 C++ `do{}while()` |
| CASE / END_CASE | ⚠️ | 翻译为 if/else if 链，部分 case_list_elem 未走 translateExpr |
| EXIT（循环跳出） | 🔲 | — |
| RETURN | ✅ | 函数内 `return` |
| GOTO / LABEL | ❌ | 不计划支持 |

## 6. PRINT 调试

| 特性 | 状态 | 说明 |
|------|------|------|
| PRINT(变量) | ✅ | `printf("%d", (int)(gvl.read<INT>(offset)))` |
| PRINT("字符串") | ✅ | `printf("字符串")` |
| PRINT(变量 + 变量) | ✅ | 逐元素输出 |

## 7. 语义检查

| 检查 | 状态 | 说明 |
|------|------|------|
| 重名检查 | ✅ | 同一作用域内不允许同名符号 |
| 类型兼容性 | ✅ | 表达式操作数类型检查 |
| 未声明变量 | ✅ | 变量必须先声明后使用 |
| 函数参数数量 | ✅ | 调用时参数数量匹配 |
| RETURN 检查 | ✅ | 非 void 函数必须有 RETURN（无 body 的外部函数除外） |
| 数组越界 | ⚠️ | 运行时 `safeArrayAt()` 检查 |

## 8. 标准功能块

| 功能块 | 运行时状态 | 编译器集成 |
|--------|-----------|-----------|
| TON（接通延时） | ✅ | 🔲 |
| TOF（断开延时） | ✅ | 🔲 |
| TP（脉冲） | ✅ | 🔲 |
| CTU（加计数） | ✅ | 🔲 |
| CTD（减计数） | ✅ | 🔲 |
| CTUD（双向计数） | ✅ | 🔲 |
| R_TRIG（上升沿） | ✅ | 🔲 |
| F_TRIG（下降沿） | ✅ | 🔲 |
| SR / RS（锁存） | 🔲 | — |
| SR flip-flop | 🔲 | — |

> **注意**：标准功能块在运行时（`rt_plc.h`）中已完整实现，但编译器尚未将 ST 的 FB 调用语法翻译为对应的 C++ 调用。目前只能在手写的 C++ 中使用这些功能块。

## 9. 运行时特性

| 特性 | 状态 | 说明 |
|------|------|------|
| GVL 平坦内存模型 | ✅ | 64KB，偏移量访问 |
| ProcessImage 硬件 I/O | ✅ | 双缓冲，TCI 接口 |
| Cyclic 任务 | ✅ | 周期触发 |
| Event 任务 | ✅ | 边沿触发 |
| Freewheeling 任务 | ✅ | 每 tick 执行 |
| 任务优先级 | ✅ | 0-31 |
| 软件看门狗 | ✅ | 默认 + 每任务独立 |
| 冷启动 | ✅ | GVL 全清零 |
| 暖启动 | ✅ | RETAIN 区域保留 |
| 错误安全算术 | ✅ | safeDiv/safeMod/safeArrayAt |
| POURegistry | ✅ | 名字→函数指针绑定 |
| 多文件编译 | ✅ | 每文件独立 registerPOU_x() |
| 多核调度 | ❌ | 当前单线程串行 |

## 10. 编译器基础设施

| 特性 | 状态 | 说明 |
|------|------|------|
| ANTLR4 词法/语法分析 | ✅ | PLCSTLEXER.g4 + PLCSTPARSER.g4 |
| 策略模式语义检查 | ✅ | @StrategyForVisit + Factory |
| 符号表 + 作用域栈 | ✅ | PLCSymbolTable + PLCScopeStack |
| 中间表达式（RFM） | ✅ | 正则替换转 GVL 偏移 |
| FOR 循环 GVL 遮盖 | ✅ | 局部副本 + 写回 |
| STRUCT 字段访问 | ✅ | read<T>().FIELD / safeArrayAt<>().FIELD |
| ARRAY 元素访问 | ✅ | safeArrayAt<T>(offset, idx, count) |
| CodeGenerator 接口 | ✅ | 可扩展多后端 |
| 代码输出到字符串 | ✅ | 不直接写文件，便于测试 |
