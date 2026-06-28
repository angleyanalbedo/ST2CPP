# ST2C 编译器测试指南

## 测试体系概览

```
tests/
├── JUnit (95+ 单元测试)  ← mvn test
│   ├── FlatCodeGeneratorTest     — 代码生成器方法测试 (61)
│   ├── PLCSymbolTableTest        — 符号表操作 (11)
│   ├── PLCScopeTest              — 作用域栈 (12)
│   ├── IDGeneratorTest           — ID 生成器 (8)
│   └── SnapshotTest              — 快照测试 (7, 持续增加)
└── C++ runtime (doctest)  ← cmake --build . && ctest
    ├── framework_test            — 框架完整性 (112)
    ├── gvl_test                  — GVL 读写
    └── ...
```

## 快照测试（Snapshot Test）

### 原理

每个 `.st` 输入文件对应一个 **golden 文件**（预期 C++ 输出），存放在 `src/test/resources/snapshots/` 下：

```
snapshots/
├── test.st              ← 输入
├── test.cpp             ← golden（预期编译器输出）
├── test_struct.st
├── test_struct.cpp
└── ...
```

`SnapshotTest` 用 `ProcessBuilder` 启动独立 JVM 运行 `Main.main()`，将输出与 golden 文件逐字符比较。

### 运行

```bash
# 验证所有 snapshot
mvn test -Dtest=SnapshotTest

# 首次创建 / 更新 golden 文件（编译器输出有意图变更时）
mvn test -Dtest=SnapshotTest -DupdateSnapshots=true
```

### 添加新快照

```
1. 把 test_xxx.st 放入 src/test/resources/snapshots/
2. 运行 mvn test -Dtest=SnapshotTest -DupdateSnapshots=true
3. 审查生成的 test_xxx.cpp 内容是否正确
4. 提交 test_xxx.st + test_xxx.cpp
```

### 更新 golden 文件

编译器输出有 `意图的变更`（如修复 bug、改善格式）时，用 `-DupdateSnapshots=true` 重新生成，然后 `git diff` 审查差异：

```bash
mvn test -Dtest=SnapshotTest -DupdateSnapshots=true
git diff src/test/resources/snapshots/
```

确认变更合理后提交。

## 单元测试

```bash
# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=FlatCodeGeneratorTest

# 跳过测试（打包时）
mvn package -DskipTests
```

### 测试编写约定

- 使用 JUnit 4（`@Test`, `assertEquals` 等）
- 测试类放在 `src/test/java/`，包名与被测类一致
- 方法名用 `testXxx` 命名，清晰表达测试意图
- 避免依赖测试执行顺序

## Runtime C++ 测试

见 `runtime-flat/tests/`。使用 [doctest](https://github.com/doctest/doctest) 框架。

```bash
cd runtime-flat/build
cmake .. -G "MinGW Makefiles"
cmake --build .
.\tests\framework_test.exe
```

## 完整 CI 流程

```bash
# 1. Java 测试（编译器）
cd java
mvn test

# 2. 编译所有 examples 并验证快照
mvn test -Dtest=SnapshotTest

# 3. C++ runtime 测试
cd ../runtime-flat/build
cmake .. -G "MinGW Makefiles"
cmake --build .
ctest
```
