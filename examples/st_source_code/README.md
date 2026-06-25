# OSCAT ST 源码库 — 分类说明

来源：OSCAT (Open Source Community for Automation Technology) 开源 PLC 库
格式：IEC 61131-3 Structured Text (ST)
文件数：729 个 .ST 文件

## 分类

| 目录 | 数量 | 说明 |
|------|------|------|
| `math/` | 133 | 数学函数：三角函数、复数、向量、统计、插值、取整等 |
| `string/` | 91 | 字符串/文本处理：格式化、编解码、正则、BASE64、MD5、SHA1、HTML/URL |
| `datetime/` | 88 | 日期时间：日期计算、时间转换、夏令时、闰年、日历、天文 |
| `analog/` | 70 | 模拟量/传感器：缩放、线性化、多项式、热电偶、热电阻、压力、流量 |
| `bitwise/` | 56 | 位/字节操作：位提取、字节序转换、Gray码、BCD、奇偶校验 |
| `timer/` | 51 | 定时器/脉冲：TON、TOF、TP、时钟分频、脉冲生成、斜坡、延时 |
| `signal/` | 48 | 信号处理：滤波、死区、滞回、积分、微分、联锁、序列 |
| `comm/` | 44 | 通信/网络：Modbus、SNTP、SMTP、Telnet、IRC、网络变量 |
| `control/` | 40 | 控制：PID、PWM、手动控制、整定、驱动控制 |
| `array/` | 23 | 数组/集合：排序、洗牌、统计、FIFO、栈、矩阵 |
| `internal/` | 22 | 内部辅助函数（以 `_` 开头） |
| `actuator/` | 19 | 执行器/驱动：阀门、百叶窗、电机、灯光 |
| `other/` | 18 | 未分类 |
| `system/` | 17 | 系统/配置：报警、事件、调度器、版本、日志 |
| `counter/` | 5 | 计数器：CTU、CTD、CTUD、递增/递减 |
| `thermal/` | 4 | 温控：锅炉、燃烧器、军团菌防护 |

## ST2C 编译器兼容性

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 可编译 | 147 | 纯 FUNCTION，无 FUNCTION_BLOCK/CONFIGURATION，标识符全大写 |
| ❌ FUNCTION_BLOCK | 461 | 编译器不支持 FB 翻译 |
| ❌ 小写标识符 | 106 | 文件中含小写标识符（如 `program0`），ST2C 要求全大写 |
| ❌ 其他 | 15 | 其他语法问题 |

## 原始出处

OSCAT 库文件原本在一个扁平目录中，文件名即功能描述。本目录按功能分类整理，便于查找和测试。