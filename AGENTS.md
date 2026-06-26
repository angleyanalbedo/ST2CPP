# ST2C — Hard Real-Time Embedded PLC Toolchain

## 战略定位

从"实验室玩具"到"工业凶器"。不再追逐语法糖和 IDE 补全，全部精力砸向 **底层性能** 和 **硬实时通讯**。

```
ST 源码 → ANTLR4 解析 → 静态语义分析 → 代码生成（架构优化） → 裸机/RTOS → EtherCAT → 真实电机
```

## 四步破局方案

### 第一步：冻结前端，重构运行时调度机制

- **停止** ANTLR 解析树新特性、LSP 新功能
- **目标硬件**：带硬件浮点和高频定时器的 MCU（STM32H7xx / RISC-V 带向量扩展）
- **剥离通用 OS**：运行时从 POSIX/Windows 线程模型改为纯裸机或极简 RTOS
- **核心指标**：硬件定时器中断驱动 PLC Cycle，微秒级确定性（1ms 周期抖动 <几 μs）

### 第二步：架构特定代码生成（核心竞争力）

- 检测 AST 中的大批量数据块移动 / 矩阵运算 / 数组复制
- 不再生成普通 `for` 循环，而是注入目标架构的向量扩展指令（RVV Intrinsic）或内联汇编
- 目标：同一段 ST 代码，经 ST2C 编译后在相同芯片上把 GCC 默认输出按在地上摩擦

### 第三步：工业现场总线（EtherCAT）

- 集成 **SOEM (Simple Open EtherCAT Master)** 开源协议栈
- ST 语言绝对地址变量 `%IX0.0` / `%QX0.0` 与 EtherCAT PDO 内存映射直接绑定
- 里程碑：ST 代码 → 网口 EtherCAT 报文 → 真实伺服电机运转

### 第四步：非对称作战 — AI 边缘控制终端

- 不碰大厂的大型汽车流水线（几万点位）
- 瞄准 **下一代 AI 边缘控制终端** + **极客定制化硬件**
- 全链路：AI 推理 → ST 高级逻辑 → 架构优化机器码 → EtherCAT → 机械臂
- 这是任何封闭商业 PLC 巨头目前给不了的能力

## 目录结构

```
ST2C-master/
├── java/                        # Java 编译器（ANTLR4 + 语义分析 + 代码生成）
│   └── src/main/java/
│       ├── Main.java            # 入口
│       ├── antlr4/              # ANTLR 生成的 Lexer/Parser/Visitor
│       ├── staticCheckVisitor/  # 语义检查 + 表达式预组装
│       ├── PLCTranslator/       # 代码生成器
│       │   ├── CodeGenerator.java
│       │   ├── FlatCodeGenerator.java
│       │   └── TranslateType/
│       ├── PLCSymbolAndScope/
│       ├── PLCTargetFileOutPut/
│       ├── PLCException/
│       └── JSON/
├── runtime-flat/                # 当前运行时（待迁移至裸机）
│   ├── include/
│   ├── runtime_main.cpp
│   ├── tests/
│   └── build/
├── target/                      # 目标平台 BSP + 固件（新增）
│   ├── stm32h7/                 # STM32H7 裸机工程
│   │   ├── startup.s
│   │   ├── linker.ld
│   │   ├── hal/                 # 定时器/GPIO/ETH 驱动
│   │   └── Makefile
│   ├── riscv/                   # RISC-V 向量扩展平台（新增）
│   │   ├── startup.s
│   │   ├── linker.ld
│   │   ├── hal/
│   │   └── Makefile
│   └── soem/                    # SOEM EtherCAT Master 移植（新增）
│       ├── ethercat_main.cpp
│       ├── pdo_map.h
│       └── soem_config/
├── examples/
├── test.bat
└── README.md
```

## 构建

### Java 编译器（维护模式，仅修 bug 不增特性）

```bash
cd java && mvn package -DskipTests
java -jar target/st2c-jar-with-dependencies.jar --input examples/test.st --output-dir output/flat/build
```

### 目标平台固件（新工作重心）

```bash
# STM32H7 裸机
cd target/stm32h7 && make

# RISC-V 向量平台
cd target/riscv && make

# EtherCAT 集成测试
cd target/soem && cmake -B build && cmake --build build
```

## 周目标

1. 找一块积灰的开发板，格式化
2. 写纯裸机（Bare-metal）或极简 RTOS 的高精度定时器
3. 把 ST 代码编译进去，点亮板子 LED
4. 用示波器测每次点亮的 **时间抖动**

## 代码约定（继承）

- 命名空间：`rt_plc`
- IEC 类型名大写：`INT`, `REAL`, `BOOL`
- 编译器类命名：`Translate` 前缀
- POU 函数签名：`void name(GVL& gvl, ProcessImage& io, TIME cycleTimeUs)`
- 目标平台 HAL：`target/<platform>/hal/`，命名风格跟随平台惯例

## 陷阱

- **Include 阴影**：`runtime-flat/` 下曾有过旧版头文件，确保 `-I` 路径只指向正确版本
- **对齐**：ARM 非对齐访问会 bus error，编译器计算 GVL 偏移时必须保证对齐
- **Windows 计时精度**：`steady_clock` ~15ms，不适合微秒级验证；抖动测试必须上示波器
- **SOEM 依赖**：需 libpcap（Linux）或 WinPcap/Npcap（Windows），见 `target/soem/README`

## TODO

### 第一阶段：裸机调度器（本周）

- [ ] STM32H7 或 RISC-V 开发板选型 + BSP 初始化
- [ ] 硬件定时器中断驱动 Cycle（TIM → IRQ → POU tick）
- [ ] 抖动测量：GPIO 翻转 + 示波器
- [ ] 从 `runtime-flat` 剥离通用 OS 依赖

### 第二阶段：架构优化编译器

- [ ] 检测批量数据移动模式（memcpy 模板匹配）
- [ ] RISC-V V 扩展 Intrinsic 代码生成
- [ ] 内联汇编注入通道
- [ ] 性能基准：相同 ST 代码 vs GCC -O3

### 第三阶段：EtherCAT 集成

- [ ] SOEM 移植到目标平台
- [ ] PDO 内存映射与 GVL 绝对地址绑定
- [ ] 驱动真实伺服电机
- [ ] 周期抖动在总线负载下验证

### 第四阶段：AI 边缘控制原型

- [ ] 定义 AI → ST 接口规范
- [ ] 端到端演示：推理 → 编译 → 执行 → 驱动

## 语法修改记录（历史存档）

见 `AGENTS.md.bak`。
