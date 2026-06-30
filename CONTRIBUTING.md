# 开发指南

> 建议先阅读 [架构详解](architecture.md) 了解类图、序列图和依赖关系。

## 环境搭建

### 1. 安装工具

| 工具 | 版本要求 | 安装方式 |
|------|---------|---------|
| JDK | 8+ | [Adoptium](https://adoptium.net/) 或系统自带 |
| Maven | 3.x | `winget install Apache.Maven` |
| MinGW (GCC) | 7+ | [MSYS2](https://www.msys2.org/) 或 `winget install MSYS2.MSYS2` 后 `pacman -S mingw-w64-x86_64-gcc` |
| CMake | 3.10+ | `winget install Kitware.CMake` |
| ANTLR4 | 4.10.1 | 已包含在 `java/lib/` 中（无需单独安装） |

### 2. 验证安装

```bash
java -version          # Java 8+
mvn -version           # Maven 3.x
g++ --version          # GCC 7+
cmake --version        # CMake 3.10+
```

### 3. 构建项目

```bash
# 手动分步
cd java && mvn compile
cd runtime-flat/build && cmake .. -G "MinGW Makefiles" && cmake --build .
```

---

## 编译器开发


### 添加新的静态检查

在 `staticCheckVisitor/strategy/` 下按语法类别组织：

| 目录 | 用途 |
|------|------|
| `pou_decl/` | PROGRAM, FUNCTION, FB 声明 |
| `var_decls/` | 变量声明（VAR, VAR_INPUT 等） |
| `constant_expr/` | 表达式、字面量 |
| `type_access/` | 类型引用 |
| `variable_access/` | 变量引用 |
| `type_decl/` | TYPE...END_TYPE 声明 |
| `fb_body/` | 语句体（赋值、控制流、PRINT） |

---

## 运行时开发

### 添加新功能块

1. 在 `runtime-flat/include/rt_plc.h` 的「标准功能块」区域添加 struct：
   ```cpp
   struct MY_FB {
       // 内部状态
       bool internalState = false;

       void update(BOOL input, TIME delay, TIME cycleTimeUs) {
           // 实现逻辑
       }
   };
   ```

2. 在 `runtime-flat/tests/framework_test.cpp` 中添加测试

3. 运行 `framework_test.exe` 确认全部 PASS

### 添加新 IEC 类型

1. 在 `rt_plc.h` 中添加 typedef：
   ```cpp
   using MY_TYPE = int32_t;
   ```

2. 如需类型转换，添加 `TO_MY_TYPE()` 函数

3. 在 `GvlContext.SIZE_MAP` 中注册大小：
   ```java
   SIZE_MAP.put("MY_TYPE", 4);
   ```

4. 在 `GvlContext.toNativeType()` 中注册映射：
   ```java
   case "MY_TYPE": return "MY_TYPE";
   ```

### 运行时测试

```bash
cd runtime-flat/build
cmake .. -G "MinGW Makefiles" && cmake --build .
.\tests\framework_test.exe      # 124+ 项全部 PASS
.\fibonacci.exe            # 基础类型验证
.\multitask_demo.exe       # 多任务调度演示
```

---

## 编码规范

### Java 编译器

| 规范 | 示例 |
|------|------|
| 包名驼峰 | `PLCTranslator`, `staticCheckVisitor` |
| Visitor 方法大写开头 | `VisitFunc_decl`, `VisitProg_decl` |
| 翻译器类 `Translate` 前缀 | `TranslateFunc_decl`, `TranslateCallFunc` |
| 工厂方法在 `packageFactory` 中 | — |
| `GvlContext` 管理偏移量 + 表达式转换 | `Translate*` 类只读 `GvlContext` |
| 策略类用 `@StrategyForVisit` 注解 | `ruleIndex` + `branch` |

### C++ 运行时

| 规范 | 示例 |
|------|------|
| 命名空间 | `rt_plc` |
| IEC 类型名大写 | `INT`, `REAL`, `BOOL`, `STRING` |
| 函数名遵循 IEC 标准 | `TON`, `CTU`, `ABS`, `SEL` |
| 模板类 `PLC_` 前缀 | `PLC_Array`, `PLC_Subrange` |
| 内部辅助 `plc_` 前缀 | `plc_lock`, `plc_deref` |
| 偏移常量 `Offsets::` 命名空间 | — |
| POU 函数签名 | `void name(GVL& gvl, ProcessImage& io, TIME cycleTimeUs)` |

### 提交信息格式

```
<type>: <subject>

<type>:
  feat     新功能
  fix      Bug 修复
  refactor 重构
  docs     文档
  test     测试
  build    构建系统
```

---

## 调试技巧

### 编译器调试

```bash
# 查看 GVL 偏移量映射
java -cp "target/classes;lib/*" Main --input test.st --output out.cpp --verbose
# stdout 会打印每个变量的偏移量

# 查看生成的 C++ 代码
cat output/flat/main.cpp
```

### 运行时调试

```bash
# framework_test 输出详细信息
.\framework_test.exe

# 查看调度诊断
# runtime_main.cpp 最后会调用 sched.printDiag()
```

### 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 标识符解析失败 | 变量名含小写 | ST 要求大写 `[A-Z][A-Z0-9$_]*` |
| `ARRAY` 不识别 | 关键字顺序错误 | 确保 `ARRAY_KW` 在 `Identifier` 之前 |
| GVL 偏移对齐错误 | REAL/LREAL 未对齐 | 检查 `allocateOffset()` 对齐逻辑 |
| 运行时 bus error | ARM 非对齐访问 | 确保偏移量按类型大小对齐 |
| `POURegistry` not found | 头文件阴影 | 删除旧版 `rt_runtime.h` |
