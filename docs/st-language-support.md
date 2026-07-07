# ST 语言支持

**已支持**：

| 类别 | 内容 |
|------|------|
| POU | FUNCTION, FUNCTION_BLOCK, PROGRAM, METHOD, CLASS, ACTION |
| 基本类型 | BOOL, SINT, INT, DINT, LINT, USINT, UINT, UDINT, ULINT, REAL, LREAL, BYTE, WORD, DWORD, LWORD |
| 复合类型 | STRUCT, CLASS (含继承 EXTENDS + SUPER), ENUM, ARRAY (含多维), 子区间类型 |
| 时间/日期 | TIME (T#), DATE, TIME_OF_DAY (TOD#), DATE_AND_TIME (DT#) |
| 字符串 | STRING, WSTRING — CONCAT, LEN, LEFT, RIGHT, MID, INSERT, DELETE, REPLACE, FIND |
| 控制流 | IF / ELSIF / ELSE, FOR, WHILE, REPEAT, CASE, 赋值, 比较 |
| 运算 | 算术(+-\*/), 位运算(AND, OR, XOR, NOT, SHL, SHR, ROL, ROR), MOD, EXPT |
| 内置函数 | ABS, SQRT, LN, LOG, EXP, SIN, COS, TAN, ASIN, ACOS, ATAN, MIN, MAX, LIMIT, SEL, MUX, MOVE, TRUNC, FLOOR |
| 类型转换 | 约 200 个标准转换: TO_INT, TO_DINT, TO_REAL, TO_LREAL, TO_STRING, TO_TIME, TO_BYTE, TO_WORD, TO_DWORD, TO_BOOL 等 + 各类型间双向 (DINT_TO_REAL, REAL_TO_DWORD 等) |
| 标准 FB | TON, TOF, TP, CTU, CTD, CTUD, R_TRIG, F_TRIG, SR, RS |
| 直接 I/O | AT %I, %Q, %IB, %IW, %ID, %IL, %IX.b, %QB, %QW, %QD, %QL, %QX.b |
| RETAIN | VAR RETAIN 持久化区域, 暖启动恢复 |
| 生命周期 | PRE / POST / FirstScan 程序段 |
| 调试 | PRINT, ASSERT |
| 跨文件 | 跨编译单元类型解析, METHOD `=>` 输出参数绑定 |
| 字面量 | 十进制、十六进制(16#)、二进制(2#)、实数、科学计数法、TIME 字面量 |

**部分支持**：
- STRING/WSTRING 运行时边界检查（溢出截断）

**未支持**：
- 在线下载与热更新
- REF / POINTER / REF_TO
- UNION / ALIAS 类型
