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
