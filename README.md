# ST2C++ — IEC 61131-3 Structured Text to C++ Transpiler

将 IEC 61131-3 结构化文本（ST）编译为 C++ 代码，由实时运行时（runtime-flat）执行。面向工业 PLC 控制场景。

## 系统架构

```mermaid
graph TB
    subgraph Compiler["Java 编译器 (java/)"]
        direction TB
        ST["ST 源码 (.st)"]
        ST --> Lexer["ANTLR4 Lexer + Parser"]
        Lexer --> AST["ParseTree"]
        AST --> Visitor["静态语义分析<br/>PLCVisitor + Strategy"]
        Visitor --> |"ParseTreeProperty<br/>(符号表数据桥梁)"| Translator["代码生成<br/>PLCTranslatorNew + FlatCodeGenerator"]
        Translator --> CPP["C++ 源码 (.cpp)"]
    end

    subgraph Output["生成的 C++ 文件"]
        direction TB
        INC["#include rt_plc.h / rt_runtime.h"]
        STRUCT["struct TYPE { ... };"]
        FUNC["INT FUNC(INT X) { ... }"]
        PROG["void PROGRAM_X(GVL& gvl,<br/>ProcessImage& io, TIME dt) { ... }"]
        REG["void registerPOU_x(POURegistry& reg)"]
    end

    subgraph Runtime["runtime-flat/ (C++17)"]
        direction TB
        REG2["POURegistry<br/>名字 → 函数指针"]
        CFG["tasks.json<br/>调度配置"]
        SCHED["Scheduler<br/>多任务调度 Cyclic/Event/Free"]
        TASK["Task[]<br/>priority + interval + POUFunc[]"]
        GVL["GVL<br/>64KB 平坦内存<br/>read&lt;T&gt;() / write&lt;T&gt;()"]
        IO["ProcessImage<br/>硬件 I/O 双缓冲"]
        ERR["ErrorManager<br/>安全算术 / 除零保护"]
        TICK["tick() 主循环<br/>READ_INPUTS → LOGIC_SOLVE<br/>→ WRITE_OUTPUTS → HOUSEKEEPING"]

        CFG --> SCHED
        REG2 --> SCHED
        SCHED --> TASK
        TASK --> PROG2["执行 PROGRAM_X()"]
        PROG2 --> GVL
        PROG2 --> IO
        GVL --> ERR
    end

    CPP --> Output
    Output --> REG2
```

## 编译流程

```mermaid
flowchart LR
    A["ST 源码"] --> B["词法分析<br/>PLCSTLEXER.g4"]
    B --> C["语法分析<br/>PLCSTPARSER.g4"]
    C --> D["静态语义分析<br/>PLCVisitor"]
    D --> E["代码生成<br/>PLCTranslatorNew"]
    E --> F["C++ 源码<br/>output/flat/*.cpp"]

    D -.-> |"写入"| SP["ParseTreeProperty<br/>ArrayList&lt;PLCSymbol&gt;"]
    SP -.-> |"读取"| E
```

## 运行时调度

```mermaid
flowchart TB
    START["启动"] --> REG["registerAllPOUs(reg)<br/>注册所有编译的 POU"]
    REG --> LOAD["加载 tasks.json"]
    LOAD --> CREATE["创建 Scheduler"]
    CREATE --> ADD["为每个 Task<br/>addCyclicTask() + addPOU()"]
    ADD --> LOOP["tick() 循环"]

    LOOP --> RI["READ_INPUTS<br/>syncInputs()"]
    RI --> LS["LOGIC_SOLVE<br/>按优先级执行 Task"]
    LS --> WO["WRITE_OUTPUTS<br/>syncOutputs()"]
    WO --> HK["HOUSEKEEPING<br/>诊断 / 看门狗"]
    HK --> LOOP

    style START fill:#4CAF50,color:#fff
    style LOOP fill:#2196F3,color:#fff
```

## GVL 内存模型

```mermaid
block-beta
    columns 1
    block:GVL["GVL.memory[65536]"]
        columns 1
        A["INT A — offset 0 (2B)"]
        B["REAL B — offset 4 (4B, 对齐到 4)"]
        C["STRING C — offset 8 (256B)"]
        D["ARRAY[0..4] OF INT — offset 264 (10B)"]
        E["..."]
    end

    style GVL fill:#FFF3E0
```

| 变量 | 类型 | 大小 | 对齐后偏移 |
|------|------|------|-----------|
| A | INT | 2B | 0 |
| B | REAL | 4B | 4（跳过 2-3） |
| C | STRING | 256B | 8 |
| D | ARRAY[0..4] OF INT | 10B | 264 |

## 核心数据流

| ST 源码 | 编译器内部 | 生成的 C++ |
|---------|-----------|-----------|
| `VAR A : INT := 42;` | PLCVariable(A, INT) | `gvl.write<INT>(0, (42));` |
| `B := A + 10;` | assignVar = RFM 中间表达式 | `gvl.write<INT>(2, gvl.read<INT>(0) + (10));` |
| `FOR I := 0 TO 4 DO` | 遮盖 GVL 变量 I | `INT I = (0); for(...){ ... }` |
| `END_FOR` | 局部副本 + 写回 | `gvl.write<INT>(offset, I);` |

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
├── README.md                    # 本文件
├── CONTRIBUTING.md              # 开发指南、环境搭建、编码规范
├── compiler-to-runtime.md       # 编译器→运行时接口文档
├── st-support.md                # ST 语言支持矩阵
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
- FOR / IF / ELSIF / ELSE / WHILE / REPEAT / CASE 控制流
- 赋值、算术运算、比较运算
- PRINT 调试输出
- ASSERT 断言
- 外部函数声明（无 body）

**未支持**：
- FUNCTION_BLOCK（FB）
- ENUM 类型声明
- VAR_INPUT / VAR_OUTPUT / VAR_IN_OUT
- RETAIN 变量区域标记（编译器有收集但 `setRetainRegion` 调用待验证）
- 在线下载与热更新

## 文档导航

| 文档 | 用途 |
|------|------|
| [本文件](README.md) | 项目入口、架构概览、快速开始 |
| [架构详解](architecture.md) | 类图、序列图、状态图、依赖图（Mermaid） |
| [编译器→运行时接口](compiler-to-runtime.md) | 编译流程、接口契约、表达式转换、GVL 偏移量 |
| [ST 语言支持矩阵](st-support.md) | 各 ST 特性的支持状态 |
| [开发指南](CONTRIBUTING.md) | 环境搭建、如何加新语法/类型/FB、编码规范 |
| [实时化改造方案](ST2C实时化改造方案.md) | 详细设计决策与实现方案 |
| [PLC 运行时架构知识库](PLC运行时架构知识库.md) | 工业 PLC 架构背景知识 |
| [Runtime 文档](runtime-flat/docs/README.md) | 运行时目录结构、构建、开发指南 |

## 许可证

见 [LICENSE](LICENSE)  [COPYING](COPYING)。
