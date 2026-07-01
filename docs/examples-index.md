# 示例索引

共 43 个 `.st` 示例文件，按类别组织。每个示例可从项目根目录编译测试：

```bash
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/<file>.st --output-dir output/flat/build
```

---

## 入门

| 文件 | 说明 |
|------|------|
| `test.st` | 最小编译器冒烟测试 — FOR 循环调用简单 FC |
| `test_tricky_mini.st` | 最小 ENUM + FB 实例化测试 |
| `test_lexer.st` | 单行 FUNCTION 声明测试（DWORD AND 函数） |

## 数值与位运算

| 文件 | 说明 |
|------|------|
| `test_hex.st` | 十六进制字面量解析（`16#0007`, `16#FF`）|
| `test_case_hex.st` | CASE 语句中的十六进制字面量 |
| `test_not_int.st` | BOOL AND/OR 生成正确 `&` / `\|` |
| `test_bitwise.st` | 按位 AND/OR/XOR（INT 非 BOOL 操作数） |
| `test_arith.st` | 基本算术运算 |

## 定时器与计数器（标准 FB）

| 文件 | 说明 |
|------|------|
| `test_ton.st` | TON 定时器 FB 声明与调用（`T#500ms`）|
| `test_ton2.st` | TON 调用带显式 `IN` / `PT` 参数绑定 |
| `test_to.st` | 关键字冲突测试：`TO` 作为 FB 输入参数名 |
| `test_ctd.st` | CTD（减计数）FB 调用 |
| `test_ctd2.st` | CTD FB 声明 + `Q` / `CV` 输出 |
| `test_ld.st` | 关键字冲突测试：`LD` 作为 FB 输入参数名 |
| `test_std_fb.st` | 标准 FB 调用（不声明直接使用 TON）|

## 数组

| 文件 | 说明 |
|------|------|
| `test_multidim.st` | 多维数组访问 `ARRAY[0..5, 0..1]` |
| `test_arr_struct.st` | 结构体数组 + FOR 循环字段初始化 |
| `test_fb_array.st` | FB 数组 + FOR 循环调用每个元素 |
| `test_fb_array_simple.st` | FB 数组最简变体（不回读结果）|
| `test_fb_array_real.st` | FB 数组 + REAL 输入/输出 |

## 结构体与类

| 文件 | 说明 |
|------|------|
| `test_struct.st` | 简单 STRUCT（MY_POINT）声明与字段访问 |
| `test_class.st` | CLASS + 多方法 + 成员变量 + 实例化与调用 |
| `test_class_simple.st` | 最简 CLASS（一个方法返回内部状态）|
| `test_class_no_extend.st` | CLASS 不使用 EXTENDS（无继承）|
| `test_class_minimal.st` | 极简 CLASS（一个方法调用）|

## 继承与方法调用

| 文件 | 说明 |
|------|------|
| `test_super_call.st` | Bug #4：`SUPER.method()` 委托到父类 |
| `test_super.st` | BASE/EXTEND 类 + SUPER:: 方法覆盖 |
| `test_method_output.st` | Bug #5：CLASS 方法 `=>` 输出参数代码生成 |

## 功能块高级用法

| 文件 | 说明 |
|------|------|
| `test_fb_init.st` | FB VAR 块中 STRUCT 字面量初始化 `(J1 := 1.0, ...)` |
| `test_fb_output.st` | `=>` 输出参数回读 |

## AT 直接 I/O 地址映射

| 文件 | 说明 |
|------|------|
| `test_at.st` | AT 直接 I/O 映射 + Start/Stop/SpeedRef + Counter |
| `test_at_var.st` | AT 变量声明：`%IW2`, `%QW1`, `%QX0.5` |
| `test_at_struct.st` | AT 映射 STRUCT：`%ID0` / `%QD0` |
| `test_at_output_read.st` | AT 输出变量回读（`%QW2`, `%QX0.1`）|
| `test_at_full.st` | AT 全覆盖：BOOL/USINT/INT/DINT 在 `%I`/`%Q`/`%IB`/`%IW`/`%ID`/`%QB`/`%QW`/`%QD` |
| `test_pre_post.st` | 电机控制演示：`AT %I`/`%Q` + RETAIN + PRE/POST 段 |
| `demo_at.st` | AT 直接 I/O 电机启停演示 |

## 复杂功能

| 文件 | 说明 |
|------|------|
| `test_tricky.st` | 综合压力测试：ENUM、嵌套 FOR、WHILE/REPEAT、结构体数组、多返回路径 FC、混合类型算术、复杂布尔逻辑 |
| `test_retain_simple.st` | RETAIN 变量持久化（`VAR RETAIN`）|
| `test_fc_return.st` | Bug #1：FC 返回值（非 void FC 不能 emit 裸 `return;`）|
| `test_cross_file_type.st` | Bug #2：跨文件类型解析 |
| `traffic_light.st` | 四相位交通灯 FSM（tick 计数器）|
| `iec_stdlib.st` | IEC 61131-3 标准类型转换函数声明（~230 个函数）|

---

## 项目示例

### Smart Factory (`examples/projects/smart_factory/`)

| 文件 | 说明 |
|------|------|
| `types.st` | 类型定义：SENSOR_DATA, MOTOR_STATUS, ALARM_LEVEL ENUM, RECIPE |
| `sensor_ctrl.st` | 传感器控制 FB：滤波 + 报警阈值 |
| `motor_ctrl.st` | 电机控制 FB：加减速 + 方向控制 |
| `line_controller.st` | 产线控制器 CLASS：状态机方法 |
| `base_controller.st` | 基础控制器 CLASS：STARTUP/SHUTDOWN |
| `utils.st` | 工具函数：CLAMP_INT, SCALE_REAL, MAP_REAL |
| `main.st` | 主程序：绑定 sensor/motor/line controller |

### Robot Arm 6-Axis (`examples/projects/robot_arm/`)

| 文件 | 说明 |
|------|------|
| `types.st` | 六轴机器人类型：SERVO_STATE, JOINT_CFG, WAYPOINT, SAFETY |
| `servo_drive.st` | 单轴伺服 FB：位置/速度/力矩模式 + 回零 + 故障复位 |
| `safety.st` | 安全监控 FB：E-stop、关节极限、伺服状态 |
| `robot_ctrl.st` | 六轴主控制器 FB：状态机 + 步进序列器 |
| `motion_planner.st` | 关节空间运动规划 FB：梯形/S-curve 曲线 |
| `math_utils.st` | 数学工具：DEG_TO_RAD, CLAMP, LERP, S_CURVE, CUBIC |
| `io_config.st` | EtherCAT PDO 内存映射：6 轴 `AT %ID`/`%IW`/`%QW`/`%QD` |
| `main.st` | 主程序：AT I/O + FB 绑定 + 状态机编排 |

### HVAC Controller (`examples/projects/hvac_controller/`)

| 文件 | 说明 |
|------|------|
| `types.st` | HVAC 类型：PID_PARAMS, ZONE_STATE, FAN_SPEED, HVAC_MODE |
| `zone_fb.st` | 区域控制器 FB：PID + 加热/制冷/风机 + 报警 |
| `pid_func.st` | PID 控制函数：SCALE_AI, SCALE_AO, PID_CONTROL |
| `io_config.st` | HVAC 全局 I/O 映射：4 区温度、DI/DO、`%IW`/`%QW`/`%IX`/`%QX` |
| `main.st` | 主程序：4 个 PID 区域协调 + 报警处理 |
