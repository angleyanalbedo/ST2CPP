# runtime-flat TODO

本文件列出 runtime-flat 的待办事项，基于 2025-06-25 全项目审计更新。完整上下文参见项目根目录 `AGENTS.md`。

## ✅ 已完成（2025-06-25 审计中确认）

- [x] GVL 偏移量越界检查（framework_test #13）
- [x] ProcessImage 偏移量越界检查（framework_test #14）
- [x] Task 参数校验：interval/priority clamp（framework_test #15）
- [x] 除零/溢出错误码细化：safeDiv/safeMod 记录操作数（framework_test #16）
- [x] ARRAY 类型支持：emitVarDecl + safeArrayAt
- [x] STRUCT 类型支持：C++ struct 生成 + 字段访问
- [x] RETAIN 区域标记：编译器收集 RETAIN 变量并生成 setRetainRegion()（已修复参数 bug）
- [x] 临时变量类型推断：INT _X0=A（非 auto）
- [x] GVL 变量在表达式中的转换：gvl.read/write<T>(offset)
- [x] WHILE / REPEAT / CASE 控制流翻译
- [x] ErrorManager 提取到独立头文件 error_manager.h（解决 gvl.h 循环依赖）
- [x] 数组下标 GVL 变量转换（readExpr/writeExpr/translateExpr 三处修复）
- [x] fileId 后缀修复（从输入文件名派生）
- [x] CASE 翻译遗漏修复（case_list_elem 走 translateExpr）
- [x] 子模块拆分：core/ 下已有 gvl.h, types.h, error_manager.h, registry.h, task.h, program.h, diag.h, watchdog.h, event.h, constants.h

## 🔴 高优先级（阻塞产品化）

### 编译器 Bug

- [ ] **24 处无保护的 PLCVariable 强转**：多个 TranslateXxx 中 `(PLCVariable) properties.get(ctx).get(0)` 未检查类型，若 properties 含 PLCTypeDeclSymbol 等会 ClassCastException
  - 涉及文件：TranslateCase_stmt, TranslateAssert_stmt, TranslateFunc_call, TranslateCallFunc, TranslateStruct_elem_decl, TranslateElsif_stmt, TranslateIf_stmt, TranslateWhile_stmt, TranslateRepeat_stmt, TranslateFor_stmt, TranslateVariableAssignExpression, TranslateInvocation1/2
- [ ] **多文件同名 PROGRAM 链接冲突**：多个 .st 定义同名 PROGRAM 导致 multiple definition 错误
  - 需要：编译器为 PROGRAM 生成 namespace 前缀，或运行时用 programId 区分
- [ ] **runtime_main 链接失败**：CMake 的 `runtime_main` 目标未链接 `pou_registry.gen.cpp`，`registerAllPOUs` 未定义
  - 需要：CMakeLists.txt 中 `runtime_main` 目标添加 `${POU_REGISTRY}` 依赖

### 编译器功能缺失

- [ ] **FB（功能块）翻译**：当前仅支持 FUNCTION 和 PROGRAM，无法翻译 FUNCTION_BLOCK 声明和实例化
- [ ] **ENUM 类型声明**：TYPE...END_TYPE 中的 ENUM 在 PROGRAM 变量段输出 `// TODO` 注释
- [ ] **VAR_INPUT/VAR_OUTPUT/VAR_IN_OUT**：PROGRAM 的 IO 参数声明未正确处理
- [ ] **标准 FB 调用集成**：运行时有 TON/CTU/CTD/R_TRIG/F_TRIG 实现，但编译器不会生成调用代码
- [ ] **FB 实例化**：无法 `VAR btn : TON; END_VAR` 声明 FB 实例

## 🟡 中优先级（可观测性 + 质量）

- [ ] **测试覆盖**：WHILE / REPEAT / CASE 无独立测试用例（仅通过 framework_test 间接验证）
- [ ] **CASE 翻译改用原生 switch**：当前翻译为 if/else if 链，效率低于 switch/case
- [ ] **周期报警阈值**：`DiagStats` 增加 `alarmThreshold` 和报警回调接口
- [ ] **抖动（jitter）统计**：标准差、百分位数（P99/P95）
- [ ] **OPC UA 报警接口**：`printDiag()` 仅输出到 stdout，需工业协议输出
- [ ] **错误码标准化**：扩展为 IEC 61131-3 / PLCopen 风格的分层错误码（0x8xxx 格式）
- [ ] **轴级超时**：新增 `Axis` 对象，每轴独立超时保护
- [ ] **emitVarDecl 代码重复**：`emitVarDecl`/`emitGlobalVarDecl`/`emitProgVarDecl` 三份几乎相同，需合并

## 🟢 低优先级（工业认证 + 扩展）

- [ ] **STO（安全转矩关闭）**：新增 `SafetyModule` 接口，硬件安全回路抽象
- [ ] **硬件看门狗**：独立硬件 WDT 接口（与软件看门狗互补）
- [ ] **安全状态机**：与主状态机物理隔离或逻辑强隔离的独立安全路径
- [ ] **TÜV 认证准备**：需求追溯矩阵、FMEA/FTA 文档、故障注入测试框架
- [ ] **SIL/PL 等级**：SIL2/SIL3 所需的冗余、诊断覆盖度
- [ ] **多核调度**：当前单线程串行，需评估 RTOS 集成方案
- [ ] **动态任务注册**：当前任务在编译期固定，需支持运行时动态创建/销毁
- [ ] **OPC UA 集成**：工业协议通信接口
- [ ] **MQTT 集成**：物联网上报通道
- [ ] **在线下载/热更新**：不支持
- [ ] **错误信息国际化**：当前中英混杂
