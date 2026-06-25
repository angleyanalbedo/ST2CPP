# ST2C++ — IEC 61131-3 Structured Text to C++ Transpiler

将 IEC 61131-3 结构化文本（ST）编译为 C++ 代码，由实时运行时（runtime-flat）执行。面向工业 PLC 控制场景。

## 系统架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ST2C++ 系统架构                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    Java 编译器 (java/)                       │   │
│  │                                                             │   │
│  │  ST 源码 (.st)                                              │   │
│  │      │                                                      │   │
│  │      ▼                                                      │   │
│  │  ┌──────────┐    ┌──────────────┐    ┌──────────────────┐  │   │
│  │  │ ANTLR4   │───▶│ 静态语义分析  │───▶│ 代码生成          │  │   │
│  │  │ Lexer +  │    │ PLCVisitor   │    │ PLCTranslatorNew │  │   │
│  │  │ Parser   │    │ + Strategy   │    │ + FlatCodeGen     │  │   │
│  │  └──────────┘    └──────────────┘    └──────────────────┘  │   │
│  │                                       │                     │   │
│  │                                       ▼                     │   │
│  │                              ParseTreeProperty              │   │
│  │                              (符号表数据桥梁)                │   │
│  └───────────────────────────────┬─────────────────────────────┘   │
│                                  │                                  │
│                                  ▼                                  │
│                    输出: C++ 源码 (.cpp)                             │
│                    ┌─────────────────────────────┐                  │
│                    │ #include "rt_plc.h"          │                  │
│                    │ #include "rt_runtime.h"      │                  │
│                    │                              │                  │
│                    │ struct TYPE { ... };          │                  │
│                    │ INT FUNC(INT X) { ... }      │                  │
│                    │ void PROGRAM_X(GVL&,         │                  │
│                    │   ProcessImage&, TIME) { ... }│                 │
│                    │ void registerPOU_x(           │                  │
│                    │   POURegistry& reg) { ... }   │                  │
│                    └──────────────┬──────────────┘                  │
│                                   │                                 │
│                                   ▼                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 runtime-flat/ (C++17)                        │   │
│  │                                                             │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐  │   │
│  │  │  Scheduler  │  │    GVL     │  │   POURegistry        │  │   │
│  │  │  多任务调度  │  │ 64KB 平坦  │  │   名字→函数指针      │  │   │
│  │  │  Cyclic/    │  │ 内存模型   │  │   绑定               │  │   │
│  │  │  Event/     │  │            │  │                      │  │   │
│  │  │  Free-wheel │  │ read<T>()  │  │ lookup("MAIN")       │  │   │
│  │  └──────┬─────┘  │ write<T>() │  └──────────┬───────────┘  │   │
│  │         │        └────────────┘             │              │   │
│  │         ▼                                   │              │   │
│  │  ┌────────────┐    tasks.json 配置          │              │   │
│  │  │ Task[]     │◀───────────────────────────┘              │   │
│  │  │ priority   │                                           │   │
│  │  │ interval   │    ┌──────────────┐  ┌────────────────┐   │   │
│  │  │ POUFunc[]  │    │ ProcessImage │  │  ErrorManager  │   │   │
│  │  └──────┬─────┘    │ 硬件 I/O     │  │  安全算术      │   │   │
│  │         │          │ 双缓冲      │  │  除零/越界保护  │   │   │
│  │         ▼          └──────────────┘  └────────────────┘   │   │
│  │  ┌──────────────────────────────────────────────────┐     │   │
│  │  │           tick() 主循环                           │     │   │
│  │  │  READ_INPUTS → LOGIC_SOLVE → WRITE_OUTPUTS →     │     │   │
│  │  │  HOUSEKEEPING                                    │     │   │
│  │  └──────────────────────────────────────────────────┘     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 核心数据流

```
ST 源码                    编译器内部                    生成的 C++
─────────                  ──────────                    ──────────
VAR A : INT := 42;    →    PLCVariable(A, INT)      →    gvl.write<INT>(0, (42));
B := A + 10;          →    assignVar = RFM 中间表达式 →    gvl.write<INT>(2, gvl.read<INT>(0) + (10));
FOR I := 0 TO 4 DO    →    遮盖 GVL 变量 I          →    INT I = (0); for(...){ ... }
END_FOR                     局部副本 + 写回               gvl.write<INT>(offset, I);
```

## 快速开始

### 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| Java | 8+ | 运行编译器 |
| Maven | 3.x | 构建编译器 |
| MinGW | GCC 7+ | 编译 C++ |
| CMake | 3.10+ | 构建运行时 |

### 一键测试

```bash
test.bat                    # 默认编译 examples/test.st
test.bat examples/test_print.st   # 指定输入
```

### 手动构建

```bash
# 1. ST → C++
cd java
mvn compile
java -cp "target/classes;lib/antlr4-runtime-4.10.1.jar;lib/slf4j-api-1.7.32.jar;lib/slf4j-simple-1.7.32.jar" \
  Main --input ../examples/test.st --output ../output/flat/main.cpp

# 2. CMake 构建 C++
cd runtime-flat/build
cmake .. -G "MinGW Makefiles" -DGEN_CPP_DIR=../../output/flat
cmake --build .

# 3. 运行
./runtime.exe
```

## 项目结构

```
ST2C-master/
├── java/                        # Java 编译器
│   └── src/main/java/
│       ├── Main.java            # 入口（仅 Flat 后端）
│       ├── antlr4/              # ANTLR 生成的 Lexer/Parser
│       ├── staticCheckVisitor/  # 语义检查 + 表达式预组装
│       ├── PLCTranslator/       # 代码生成器
│       │   ├── CodeGenerator.java       # 代码生成接口
│       │   ├── FlatCodeGenerator.java   # Flat 后端实现
│       │   └── TranslateType/           # 各语法节点翻译器
│       ├── PLCSymbolAndScope/   # 符号表 + 作用域栈
│       └── PLCException/        # 异常体系
├── runtime-flat/                # C++17 实时运行时
│   ├── include/
│   │   ├── rt_plc.h             # 类型系统 + 功能块 + 内置函数
│   │   ├── rt_runtime.h         # 调度器 + GVL + 生命周期
│   │   └── core/                # GVL, ErrorManager, Task, Registry
│   ├── src/                     # 调度器、程序、任务实现
│   ├── tests/                   # 框架测试（112 项）
│   ├── runtime_main.cpp         # 运行时主程序
│   └── CMakeLists.txt
├── examples/                    # ST 示例程序
├── output/flat/                 # 编译器输出的 .cpp 文件
├── tasks.json                   # 调度配置
├── test.bat                     # 一键测试脚本
├── compiler-to-runtime.md       # 编译器→运行时接口文档
├── ST2C实时化改造方案.md         # 详细设计文档
├── PLC运行时架构知识库.md        # PLC 架构背景知识
└── AGENTS.md                    # AI 代理指令
```

## ST 语言支持

详见 [ST 语言支持矩阵](st-support.md)。

**已支持**：
- FUNCTION、PROGRAM 声明与调用
- 基本类型：BOOL, INT, REAL, STRING, TIME
- STRUCT 类型定义与字段访问
- ARRAY 类型与元素访问
- FOR / IF / ELSE 控制流
- 赋值、算术运算、比较运算
- PRINT 调试输出
- 外部函数声明（无 body）

**未支持**：
- FUNCTION_BLOCK（FB）
- WHILE / REPEAT / CASE 控制流
- ENUM 类型
- VAR_INPUT / VAR_OUTPUT / VAR_IN_OUT
- RETAIN 变量区域标记
- 在线下载与热更新

## 文档导航

| 文档 | 用途 |
|------|------|
| [本文件](README.md) | 项目入口、架构概览、快速开始 |
| [编译器→运行时接口](compiler-to-runtime.md) | 编译流程、接口契约、表达式转换、GVL 偏移量 |
| [ST 语言支持矩阵](st-support.md) | 各 ST 特性的支持状态 |
| [实时化改造方案](ST2C实时化改造方案.md) | 详细设计决策与实现方案 |
| [PLC 运行时架构知识库](PLC运行时架构知识库.md) | 工业 PLC 架构背景知识 |
| [Runtime 文档](runtime-flat/docs/README.md) | 运行时目录结构、构建、开发指南 |

## 许可证

见 [LICENSE](LICENSE)。
