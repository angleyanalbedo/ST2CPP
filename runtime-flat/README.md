# runtime-flat — PLC 核心运行时静态库

IEC 61131-3 硬实时运行时核心，平台无关。

## 构建

```bash
cd runtime-flat && mkdir build && cd build
cmake .. -G "MinGW Makefiles"
cmake --build .

# 启用测试
cmake .. -G "MinGW Makefiles" -DRT_BUILD_TESTS=ON
cmake --build .
```

## 用法

### 最小 PLC Runtime

```cpp
#include "rt_runtime.h"
#include "pou_registry.gen.h"

int main() {
    rt_plc::Runtime rt;
    rt.init();          // 加载 GVL + POU 注册表
    rt.start();         // 启动调度器

    while (rt.running()) {
        rt.scanOnce();  // ReadInputs → LogicSolve → WriteOutputs
    }

    rt.stop();
    return 0;
}
```

### REPL 调试

REPL 模式编译同一份 GVL-flat `.cpp` 代码为独立交互式可执行文件，用于验证 POU 逻辑，不依赖 scheduler/ProcessImage 硬件接入。

#### 编译 ST → C++

```bash
java -jar st2c.jar --input test.st --output-dir output/flat/build --generate-debug
```

#### 构建 REPL

```bash
cd runtime-flat/build
cmake .. -G "MinGW Makefiles" \
    -DRT_BUILD_REPL=ON \
    -DFLAT_OUTPUT_DIR=../../output/flat/build \
    -DREPL_GENERATED_SRC=test.cpp \
    -DREPL_REGISTER_FUNC=registerPOU_test
cmake --build . --target plc_repl
```

| CMake 参数 | 说明 |
|---|---|
| `-DFLAT_OUTPUT_DIR=<path>` | 编译器输出目录（含 generated.cpp、debug_table.cpp、debug_names.gen.cpp） |
| `-DREPL_GENERATED_SRC=xxx.cpp` | 编译器输出的 .cpp 文件名。单文件编译默认等于输入文件名（无 .st 后缀 + .cpp） |
| `-DREPL_REGISTER_FUNC=registerPOU_xxx` | 生成代码中的 POU 注册函数名。`fileId` 等于 `--file-id` 参数或输入文件名（无 `.st` 后缀） |

#### 运行

```bash
./plc_repl
```

```
plc_repl> list
MAIN$Counter = 0
MAIN$Factor  = 0
Switch = FALSE
LED    = FALSE

plc_repl> set MAIN$Counter 100
OK

plc_repl> watch MAIN$Counter MAIN$Factor
Watching MAIN$Counter (id=1)
Watching MAIN$Factor (id=2)

plc_repl> run 10
Executed 10 cycle(s). Total: 10
MAIN$Counter = 90
MAIN$Factor  = 10

plc_repl> step
Executed 1 cycle(s). Total: 11
MAIN$Counter = 85
MAIN$Factor  = 11

plc_repl> get MAIN$Counter
MAIN$Counter = 85

plc_repl> quit
Bye
```

#### 命令参考

| 命令 | 说明 |
|---|---|
| `help` | 显示帮助 |
| `list` | 列出所有变量及当前值 |
| `get <name/id>` | 读取变量 |
| `set <name/id> <val>` | 写入变量 |
| `run [N]` | 执行 N 个 Cycle（默认 1） |
| `step` | 执行 1 个 Cycle |
| `watch <name> {<name>...}` | 添加变量到监视列表（run/step 后自动显示） |
| `watch clear` | 清空监视列表 |
| `quit` | 退出 |

变量名格式：`Program$Variable`（如 `MAIN$Counter`），I/O 变量使用原始名（如 `Switch`）。输入 `list` 可查看完整变量名。

### TCI 硬件抽象

TCI (`Target Communication Interface`) 将物理 IO 与 PLC 周期解耦：

```
PLC 扫描周期
  ├── TCI::syncInputs()   ← 硬件 → ProcessImage（非阻塞）
  ├── 用户逻辑执行         ← 读写 ProcessImage
  └── TCI::syncOutputs()  → ProcessImage → 硬件（非阻塞）
```

#### 组合多个 TCI

```cpp
rt_plc::CompositeTCI composite;
composite.add(&ethercatTCI);
composite.add(&gpioTCI);
rt.setTCI(&composite);
```

## NetworkTCI

见 `target/README.md`。
