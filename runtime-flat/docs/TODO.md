# runtime-flat TODO

本文件列出 runtime-flat 的待办事项，按优先级分组。完整上下文参见项目根目录 `AGENTS.md`。

## 高优先级（代码安全）

- [ ] **GVL 偏移量越界检查**：`GVL::read<T>()` / `write<T>()` 添加 `offset + sizeof(T) <= GVL_SIZE` 断言
- [ ] **ProcessImage 偏移量检查**：`readInput<T>()` / `writeOutput<T>()` 添加边界校验
- [ ] **参数校验增强**：`Task::interval` 正值检查、`priority` 范围检查、`offset` 有效性检查
- [ ] **除零/溢出错误码细化**：当前 `safeDiv()` 仅返回 0，需记录具体错误上下文

## 中优先级（可观测性）

- [ ] **周期报警阈值**：`DiagStats` 增加 `alarmThreshold` 和报警回调接口
- [ ] **抖动（jitter）统计**：标准差、百分位数（P99/P95）
- [ ] **OPC UA 报警接口**：`printDiag()` 仅输出到 stdout，需工业协议输出
- [ ] **错误码标准化**：扩展为 IEC 61131-3 / PLCopen 风格的分层错误码（0x8xxx 格式）
- [ ] **轴级超时**：新增 `Axis` 对象，每轴独立超时保护

## 低优先级（工业认证）

- [ ] **STO（安全转矩关闭）**：新增 `SafetyModule` 接口，硬件安全回路抽象
- [ ] **硬件看门狗**：独立硬件 WDT 接口（与软件看门狗互补）
- [ ] **安全状态机**：与主状态机物理隔离或逻辑强隔离的独立安全路径
- [ ] **TÜV 认证准备**：需求追溯矩阵、FMEA/FTA 文档、故障注入测试框架
- [ ] **SIL/PL 等级**：SIL2/SIL3 所需的冗余、诊断覆盖度

## 编译器改进（Flat 后端相关）

- [ ] **临时变量类型推断**：`auto _X0=A` 改为具体类型 `INT _X0=A`
- [ ] **GVL 变量在表达式中的转换**：`(A)` 改为 `(gvl.read<INT>(0))`
- [ ] **RETAIN 区域标记**：编译器生成 `setRetainRegion()` 调用
- [ ] **FB（功能块）翻译**：当前仅支持 FUNCTION 和 PROGRAM
- [ ] **ARRAY 类型支持**：当前生成 `// Flat: TODO`
- [ ] **STRUCT 类型支持**：当前生成 `// Flat: TODO`

## Runtime 架构扩展

- [ ] **子模块拆分**：`include/core/`、`include/scheduler/`、`include/safety/` 目前为空，需按职责拆分 `rt_plc.h` / `rt_runtime.h`
- [ ] **多核调度**：当前单线程串行，需评估 RTOS 集成方案
- [ ] **动态任务注册**：当前任务在编译期固定，需支持运行时动态创建/销毁
- [ ] **OPC UA 集成**：工业协议通信接口
- [ ] **MQTT 集成**：物联网上报通道
