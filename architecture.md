# ST2C++ 架构详解

## 1. 符号表类图

Java 编译器的核心数据模型。静态分析阶段构建，代码生成阶段消费。

```mermaid
classDiagram
    class PLCSymbol {
        +int symbolId
        +int typeId
        +String name
        +String sort
        +PLCScope localScope
        +PLCSymbolTable localSymbolTable
        +String runtimeName
        +String runtimeTypeName
    }

    class PLCVariable {
        +List~String~ varSections
        +String accessModifier
        +String retainQualifiers
        +boolean ifConst
        +String assignVar
        +String location
        +PLCSymbol declSymbol
    }

    class PLCTypeDeclSymbol {
        +PLCVariable initVar
        +String varSort
        +Set~Integer~ calculableSet
        +Set~Integer~ comparableSet
        +Set~Integer~ equalitySet
        +Set~Integer~ assignableSet
    }

    class PLCImportScopeTypeDeclType {
        +PLCScope importScope
        +PLCSymbolTable importSymbolTable
    }

    class PLCFCDeclSymbol {
        +int returnTypeId
        +Map accessVars
        +Map variableMap
        +List namespaces
        +boolean ifReturned
    }

    class PLCProgramDeclSymbol {
    }

    class PLCStructDeclSymbol {
        +boolean ifOverlap
        +List~PLCVariable~ variables
    }

    class PLCArrayDeclSymbol {
        +int elementTypeId
        +List dimension
    }

    class PLCScope {
        +int scopeID
        +int scopeDepth
        +String scopeLocation
        +PLCScope parentScope
        +PLCSymbolTable scopeSymbolTable
        +shallowFindSymbol(name)
        +deepFindSymbol(name)
    }

    class PLCSymbolTable {
        -Map symbolNameHashMap
        -Map symbolIDHashMap
        +addSymbol()
        +findSymbol(name)
    }

    class PLCTotalSymbolTable {
        +Map~Integer,PLCTypeDeclSymbol~ totalTypeMap
        +Map~Integer,PLCSymbol~ totalSymbolMap
        +Map~Integer,PLCScope~ totalScopeMap
    }

    PLCSymbol <|-- PLCVariable
    PLCSymbol <|-- PLCTypeDeclSymbol
    PLCTypeDeclSymbol <|-- PLCImportScopeTypeDeclType
    PLCImportScopeTypeDeclType <|-- PLCProgramDeclSymbol
    PLCImportScopeTypeDeclType <|-- PLCFCDeclSymbol
    PLCImportScopeTypeDeclType <|-- PLCStructDeclSymbol
    PLCTypeDeclSymbol <|-- PLCArrayDeclSymbol

    PLCScope --> PLCSymbolTable : has
    PLCScope --> PLCSymbol : declSymbol
    PLCScope --> PLCScope : parentScope
    PLCTotalSymbolTable --> PLCSymbol : totalSymbolMap
    PLCTotalSymbolTable --> PLCScope : totalScopeMap
    PLCTotalSymbolTable --> PLCSymbolTable : totalTableMap
```

## 2. 完整编译+执行序列图

一条 `C := A + B;` 从 ST 源码到运行时执行的全链路：

```mermaid
sequenceDiagram
    participant User as 用户/Editor
    participant Main as Main.java
    participant Lexer as PLCSTPARSERLexer
    participant Parser as PLCSTPARSERParser
    participant Visitor as PLCVisitor<br/>(静态语义分析)
    participant Factory as Factory<br/>(策略注册表)
    participant Strategy as VisitVariableAssignExpression
    participant Translator as PLCTranslatorNew<br/>(代码生成)
    participant CodeGen as FlatCodeGenerator
    participant File as output/flat/main.cpp
    participant CMake as CMake + GCC
    participant Runtime as runtime.exe
    participant Scheduler as Scheduler
    participant GVL as GVL.memory[64KB]

    User->>Main: --input test.st --output main.cpp
    Main->>Main: Registrant.autoRegister()<br/>扫描 @StrategyForVisit 注解

    rect rgb(230, 240, 255)
        Note over Main,Parser: 阶段阶段①②：词法+语法分析
        Main->>Lexer: CharStreams.fromFileName("test.st")
        Lexer-->>Parser: CommonTokenStream
        Main->>Parser: startpoint()
        Parser-->>Main: ParseTree
    end

    rect rgb(255, 245, 230)
        Note over Main,Strategy: 阶段③：静态语义分析
        Main->>Visitor: visit(parseTree)
        loop 每个语法树节点
            Visitor->>Factory: getStrategy(ruleIndex)
            Factory-->>Visitor: Strategy 实例
            Visitor->>Strategy: invoke(ctx, visitor)
            Strategy->>Visitor: checkNameOnly("C")<br/>checkNameOnly("A")<br/>checkNameOnly("B")
            Strategy->>Visitor: resolveType("INT")<br/>在作用域链中查找
            Strategy->>Visitor: inferType(A+B) → INT
            Strategy-->>Visitor: ArrayList~PLCSymbol~
            Note right of Visitor: 写入 ParseTreeProperty<br/>attach 到节点
        end
    end

    rect rgb(230, 255, 230)
        Note over Translator,CodeGen: 阶段④：代码生成
        Main->>Translator: visit(parseTree)
        loop 每个语法树节点
            Translator->>CodeGen: emitVarDecl("A", "INT", "42")
            CodeGen->>CodeGen: allocateOffset("A", "INT")<br/>offset=0, currentOffset=2
            CodeGen-->>Translator: ""
            Translator->>CodeGen: emitVarDecl("B", "INT", "10")
            CodeGen->>CodeGen: allocateOffset("B", "INT")<br/>offset=2, currentOffset=4
            CodeGen-->>Translator: ""
            Translator->>CodeGen: emitVarDecl("C", "INT", null)
            CodeGen->>CodeGen: allocateOffset("C", "INT")<br/>offset=4, currentOffset=6
            CodeGen-->>Translator: ""
            Translator->>CodeGen: emitAssign("C", "A+B")
            CodeGen->>CodeGen: translateExpr("A+B")<br/>RFM → gvl.read
            CodeGen->>CodeGen: writeExpr("C", "expr")
            CodeGen-->>Translator: "gvl.write&lt;INT&gt;(4, gvl.read&lt;INT&gt;(0)+gvl.read&lt;INT&gt;(2))"
        end
        Translator-->>Main: fullCode (完整 C++ 字符串)
    end

    Main->>File: BufferedWriter.write(fullCode)

    rect rgb(255, 230, 240)
        Note over CMake: 阶段⑤：C++ 编译
        CMake->>CMake: gcc main.cpp + rt_plc.h + runtime sources → runtime.exe
    end

    rect rgb(240, 230, 255)
        Note over Runtime,GVL: 阶段⑥：运行时执行
        Runtime->>Runtime: registerAllPOUs(reg)<br/>reg.add("MAIN", PROGRAM_MAIN)
        Runtime->>Scheduler: addCyclicTask("MainTask", priority=5, interval=10ms)
        Runtime->>Scheduler: addPOU(taskIdx, PROGRAM_MAIN)
        loop tick() × 100
            Scheduler->>Scheduler: READ_INPUTS
            Scheduler->>Scheduler: LOGIC_SOLVE
            Scheduler->>GVL: PROGRAM_MAIN(gvl, io, dt)
            GVL->>GVL: read&lt;INT&gt;(0) → 42
            GVL->>GVL: read&lt;INT&gt;(2) → 10
            GVL->>GVL: write&lt;INT&gt;(4, 52)
            Scheduler->>Scheduler: WRITE_OUTPUTS
            Scheduler->>Scheduler: HOUSEKEEPING
        end
    end
```

## 3. 调度器状态图

```mermaid
stateDiagram-v2
    [*] --> INIT : runtime_main 启动

    state INIT {
        [*] --> registerAllPOUs
        registerAllPOUs --> loadConfig : 加载 tasks.json
        loadConfig --> createScheduler : 创建 Scheduler
        createScheduler --> addTasks : addCyclicTask() + addPOU()
        addTasks --> [*]
    }

    INIT --> RUNNING : sched.tick() 开始

    state RUNNING {
        [*] --> READ_INPUTS
        READ_INPUTS --> LOGIC_SOLVE : syncInputs() 完成
        LOGIC_SOLVE --> WRITE_OUTPUTS : 所有 Task 执行完毕
        WRITE_OUTPUTS --> HOUSEKEEPING : syncOutputs() 完成
        HOUSEKEEPING --> READ_INPUTS : 诊断 + 看门狗检查
    }

    state ERROR {
        [*] --> fatalMode
        fatalMode --> [*] : 停止所有 Task
    }

    RUNNING --> ERROR : fatalMode = true
    ERROR --> [*]

    RUNNING --> STOPPED : tick 次数耗尽
    STOPPED --> [*]
```

## 4. PROGRAM 生命周期

```mermaid
stateDiagram-v2
    [*] --> STOPPED

    STOPPED --> INIT : start()
    INIT --> FIRST_SCAN : initFunc(gvl, io)
    FIRST_SCAN --> CYCLIC : preFunc(gvl, io)
    CYCLIC --> CYCLIC : cyclicFunc(gvl, io, dt)<br/>每次 tick 调用
    CYCLIC --> STOPPED : stop()

    note right of CYCLIC
        当前编译器仅生成
        POUFunc 路径
        (直接函数指针)
    end note

    note right of INIT
        编译器未生成
        initFunc 回调
    end note
```

## 5. Java 编译器包依赖图

```mermaid
graph TB
    subgraph 入口
        Main["Main.java"]
    end

    subgraph 语法分析
        ANTLR["antlr4/<br/>PLCSTPARSERLexer<br/>PLCSTPARSERParser"]
    end

    subgraph 语义分析
        PLCVisitor["staticCheckVisitor/<br/>PLCVisitor"]
        Factory["factory/<br/>Factory"]
        Registrant["register/<br/>Registrant"]
        Strategy["strategy/<br/>~100 个策略类"]
        GenBasic["GenerateBasicTypes"]
    end

    subgraph 符号表
        Symbol["PLCSymbols/<br/>PLCSymbol, PLCVariable<br/>PLCTypeDeclSymbol, ..."]
        Scope["PLCScope/<br/>PLCScope"]
        Table["PLCSymbolTables/<br/>PLCSymbolTable<br/>PLCTotalSymbolTable"]
        ScopeStack["PLCScopeStack"]
        IDGen["IDGenerator"]
    end

    subgraph 代码生成
        Translator["PLCTranslatorNew"]
        CodeGen["CodeGenerator 接口"]
        FlatCode["FlatCodeGenerator"]
        Translate["TranslateType/<br/>~30 个翻译器类"]
    end

    subgraph 异常
        Exception["PLCException"]
    end

    Main --> ANTLR
    Main --> PLCVisitor
    Main --> Translator

    PLCVisitor --> Factory
    Factory --> Strategy
    Registrant --> Factory
    PLCVisitor --> Symbol
    PLCVisitor --> Scope
    PLCVisitor --> Table
    PLCVisitor --> ScopeStack
    PLCVisitor --> GenBasic
    Symbol --> IDGen

    Translator --> CodeGen
    CodeGen -.- FlatCode
    Translator --> Translate
    Translate --> FlatCode

    PLCVisitor -.-> |"ParseTreeProperty"| Translator
```

## 6. 表达式转换：RFM → GVL

```mermaid
flowchart TB
    subgraph 输入["静态检查产出的中间表达式"]
        RFM["(*::PLC::RFM->getSymbolByID&lt;INT*&gt;(123))"]
    end

    subgraph Step1["轮次1: 字面量"]
        LIT["(*(new INT(2)))"] --> LIT2["(2)"]
    end

    subgraph Step2["轮次2: RFM 变量"]
        VAR["(*::PLC::RFM->getSymbolByID&lt;INT*&gt;(123))"] --> VAR2["gvl.read&lt;INT&gt;(0)"]
    end

    subgraph Step3["轮次3: RFM 函数调用"]
        CALL["*::PLC::RFM->getSymbolByID&lt;FUNC*&gt;(456)->callFunc(args)"] --> CALL2["FUNC(args)"]
    end

    subgraph Step4["轮次4: 无括号变量"]
        SIMPLE["::PLC::RFM->getSymbolByID&lt;INT*&gt;(123)"] --> SIMPLE2["gvl.read&lt;INT&gt;(0)"]
    end

    subgraph Step5["轮次5: 数组访问"]
        ARR["ARR[I]"] --> ARR2["gvl.safeArrayAt&lt;INT&gt;(offset, I, count)"]
    end

    subgraph Step6["轮次6: 清理 *"]
        STAR["*varName"] --> STAR2["varName"]
    end

    subgraph Step7["轮次7: 兜底替换"]
        BARE["A"] --> BARE2["gvl.read&lt;INT&gt;(0)"]
    end

    RFM --> Step1
    Step1 --> Step2
    Step2 --> Step3
    Step3 --> Step4
    Step4 --> Step5
    Step5 --> Step6
    Step6 --> Step7

    subgraph 输出["原生 C++ 表达式"]
        CPP["gvl.write&lt;INT&gt;(4, gvl.read&lt;INT&gt;(0) + (10))"]
    end

    Step7 --> CPP
```

## 7. 数据桥梁：ParseTreeProperty

```mermaid
flowchart LR
    subgraph Phase1["阶段③ 静态语义分析"]
        Visitor["PLCVisitor"]
        Strategy["Strategy.invoke()"]
        PTP["ParseTreeProperty&lt;<br/>ArrayList&lt;PLCSymbol&gt;&gt;"]
    end

    subgraph Node["语法树节点"]
        FuncDecl["Func_declContext"]
        VarDecl["Var_declContext"]
        Expr["ExpressionContext"]
    end

    subgraph Phase2["阶段④ 代码生成"]
        Translator["PLCTranslatorNew"]
        TranslateFunc["TranslateFunc_decl"]
        TranslateVar["TranslateVar_decl"]
        TranslateExpr["TranslateExpression"]
    end

    Visitor --> Strategy
    Strategy --> |"写入符号列表"| PTP
    PTP --> FuncDecl
    PTP --> VarDecl
    PTP --> Expr
    FuncDecl --> |"读取"| TranslateFunc
    VarDecl --> |"读取"| TranslateVar
    Expr --> |"读取"| TranslateExpr
    Translator --> TranslateFunc
    Translator --> TranslateVar
    Translator --> TranslateExpr

    style PTP fill:#FFE0B2,stroke:#FF9800
```
