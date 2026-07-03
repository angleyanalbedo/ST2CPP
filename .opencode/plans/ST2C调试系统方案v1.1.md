# ST2C 调试系统方案 v1.1

## 0. 设计目标

本方案目标不是一次性实现完整 IDE 调试器，而是先建立一个不污染主编译路径、可验证、可扩展到现场运行的调试底座。

核心原则：

- 不破坏 flat memory：`gvl.memory[GVL_SIZE]` 仍是 GVL 的唯一真实存储。
- 不污染 codegen 主路径：`Translate*` 类尽量不改，调试能力通过编译器选项和统一入口接入。
- 默认 release 零侵入：`ENABLE_DEBUG=OFF` 时不编译 DebugEngine、不插 scheduler 调试钩子。
- 源码级调试和在线监视分离：GDB 用于开发仿真，DebugEngine/CLI 用于运行期 watch/read/force。
- 安全优先：ERROR/safe output 状态下，debug force 不能覆盖安全输出。

职责边界：

```text
compiler      负责 #line、debug_table.cpp、debug_map.json
runtime-flat  负责 DebugEngine、snapshot、watch、force 语义
target        负责通信方式 TCP/UART/Modbus
cli/editor    负责变量名映射、显示、交互、类型格式化
```

## 1. 两条调试路径

| 路径 | 场景 | 工具 | 目的 |
|---|---|---|---|
| 路径 A | desktop/windows 开发仿真 | GDB + `#line` | 调 codegen、调 runtime、源码级断点 |
| 路径 B | MCU/RPi/桌面运行期监控 | DebugEngine + CLI/editor | watch/read/force/diag |

两条路径互不依赖。

GDB 不替代 CLI。GDB 是给 ST2C/compiler/runtime 开发者用的，CLI 是给 PLC 程序调试和现场监视用的。

## 2. 路径 A：开发仿真 GDB

### 2.1 Java 编译器选项

新增选项：

```bash
--emit-line-directives
```

默认关闭。开启后生成 C++ 时插入 `#line` 指令。

示例：

```bash
java -jar target/st2c-jar-with-dependencies.jar \
  --input examples/test.st \
  --output-dir output/flat/build \
  --emit-line-directives
```

### 2.2 插入策略

不要在每个表达式前插 `#line`，否则生成 C++ 可读性会被破坏。

推荐只在语句级别插入：

```cpp
#line 42 "examples/test.st"
    gv.main$A = 42;
```

实现点：

- `CompilerConfig` 增加 `emitLineDirectives`
- `PLCTranslatorNew` 增加：

```java
setEmitLineDirectives(boolean enabled)
setCurrentSourceName(String sourceName)
lineDirective(ParserRuleContext ctx)
```

- `Main.processStream()` 需要传当前源文件名。
- stdlib 默认不输出 `#line`，或标为 `iec_stdlib.st`。

### 2.3 Makefile debug target

`target/desktop/Makefile` 增加：

```make
debug: OPT = -O0 -g -ggdb
debug: all
```

验收：

```bash
make debug
gdb ./plc_runtime_desktop.exe
(gdb) break examples/test.st:42
(gdb) run
```

## 3. 路径 B：运行期 DebugEngine

### 3.1 编译器调试元数据

新增选项：

```bash
--generate-debug
```

开启后额外输出：

```text
output/flat/build/debug_table.cpp
output/flat/build/debug_map.json
```

`debug_table.cpp` 链接进固件，只包含紧凑元数据，不包含变量名：

```cpp
#include "debug/debug_if.h"

namespace rt_plc {

const DebugVar st2c_debug_vars[] = {
    {1, DebugStorage::GVL,    0, 0, DebugType::INT,  2, 1, DebugAccess::READ_WRITE},
    {2, DebugStorage::GVL,    4, 0, DebugType::REAL, 4, 1, DebugAccess::READ_WRITE},
    {3, DebugStorage::INPUT,  0, 0, DebugType::BOOL, 1, 1, DebugAccess::FORCEABLE},
    {4, DebugStorage::OUTPUT, 0, 0, DebugType::BOOL, 1, 1, DebugAccess::FORCEABLE},
};

const uint32_t st2c_debug_var_count = 4;
const uint8_t st2c_build_id[16] = { /* generated */ };

} // namespace rt_plc
```

`debug_map.json` 给 host/CLI/editor 使用，包含变量名：

```json
{
  "protocol_version": 1,
  "build_id": "1234abcd...",
  "variables": [
    {
      "id": 1,
      "name": "MAIN.A",
      "storage": "GVL",
      "offset": 0,
      "bit_offset": -1,
      "type": "INT",
      "size": 2,
      "count": 1,
      "access": "READ_WRITE"
    }
  ]
}
```

生成时机：

- GVL offset 是在翻译 `PROGRAM_x_init()` 时分配的。
- 因此 `debug_table.cpp/debug_map.json` 必须在所有 ST 文件翻译完成后输出。
- 放在 `Main.java` 生成 `gvl_layout.gen.h` 附近。当前 `Main.java` 已是全局 `GvlContext`，所有文件共享，ID 全局递增。

### 3.2 DebugVar 数据结构

新增：

```text
runtime-flat/include/debug/debug_if.h
runtime-flat/src/debug_engine.cpp
```

核心结构（注意 `bitOffset` 用 `uint8_t`，`0xFF` 表示非 bit，结构体 16 字节无 padding）：

```cpp
enum class DebugStorage : uint8_t {
    GVL,
    INPUT,
    OUTPUT,
    MEMORY,
    RETAIN
};

enum class DebugType : uint8_t {
    BOOL,
    SINT,
    INT,
    DINT,
    LINT,
    USINT,
    UINT,
    UDINT,
    ULINT,
    REAL,
    LREAL,
    BYTE,
    WORD,
    DWORD,
    LWORD,
    STRING,
    TIME
};

enum class DebugAccess : uint8_t {
    READ_ONLY,
    READ_WRITE,
    FORCEABLE
};

struct DebugVar {
    uint32_t     id;          // 4
    DebugStorage storage;     // 1
    uint8_t      bitOffset;   // 1, 0xFF = 非 bit 变量, 0-7 = BOOL 位
    DebugType    type;        // 1
    DebugAccess  access;      // 1
    uint32_t     offset;      // 4
    uint16_t     size;        // 2
    uint16_t     count;       // 2
};                            // 总 16 字节，无 padding
```

注意：

- `%IX0.0` / `%QX0.0` 必须使用 bit 读写。
- 不允许用 `memcpy` 覆盖整个 byte。
- 当前 `ProcessImage` 已有 `readInputBit/writeInputBit/readOutputBit/writeOutputBit`，DebugEngine 应复用。

### 3.3 DebugEngine API

```cpp
class DebugEngine {
public:
    void init(const DebugVar* vars, uint32_t count, const uint8_t* buildId);
    void bindMemory(GVL* gvl, ProcessImage* image);

    void hello(uint8_t outBuildId[16], uint32_t& outVarCount) const;
    const DebugVar* findVar(uint32_t id) const;

    bool requestSetWatchList(const uint32_t* ids, uint32_t count);
    bool requestForce(uint32_t id, const uint8_t* value, uint16_t size);
    bool requestUnforce(uint32_t id);
    void requestClearForces();

    void applyPendingCommands();
    void applyForces(GVL& gvl, ProcessImage& io, ForcePhase phase, SystemState state);

    void publishSnapshot(const GVL& gvl,
                         const ProcessImage& io,
                         uint64_t cycle,
                         TIME systemTime,
                         const DiagStats& diag,
                         SystemState state);

    const DebugSnapshot& latestSnapshot() const;
};
```

关键规则：

- transport 线程不直接改 watch list 和 force table。
- transport 线程只写 pending command queue。
- PLC tick 在周期边界调用 `applyPendingCommands()`。
- snapshot 使用双缓冲。
- 发布 active buffer 时使用 release/acquire 内存序。

### 3.4 Watch/Force 并发模型

不推荐：

```text
TCP thread 直接修改 forceTable_
PLC tick 同时遍历 forceTable_
```

推荐：

```text
TCP/UART/CLI thread
    -> pending command queue
PLC tick 边界
    -> applyPendingCommands()
    -> 更新 watch list / force table
```

这样可以避免 transport 线程和硬实时 tick 抢同一份数据。

### 3.5 Force 时序

当前 scheduler 已经有四阶段：

```text
READ_INPUTS
LOGIC_SOLVE
WRITE_OUTPUTS
HOUSEKEEPING
```

调试钩子插入为：

```text
syncInputs()
debug.applyPendingCommands()
debug.applyForces(PRE_LOGIC)       // 只处理 INPUT force

checkEvents()
execute cyclic/event tasks

debug.applyForces(POST_LOGIC)      // 只处理 OUTPUT + GVL force
syncOutputs()

debug.publishSnapshot()
totalTicks++
```

语义：

- INPUT force：硬件输入同步后、逻辑执行前覆盖。
- OUTPUT force：逻辑执行后、输出同步前覆盖。
- GVL force：逻辑执行后覆盖，下一周期逻辑可读到强制值。
- ERROR/safe output 状态：OUTPUT force 不允许覆盖 safe output。

### 3.6 安全优先级

安全规则：

```text
SystemState != RUN 时，默认不应用 OUTPUT force。
SystemState == ERROR 时，safe output 永远高于 debug force。
```

原因：

- 当前 `Scheduler::enterErrorState()` 会调用 `io.applySafeOutputs(image)` 并同步输出。
- 如果 debug force 在 ERROR 状态继续覆盖输出，会破坏安全语义。

## 4. 构建隔离

### 4.1 runtime-flat CMake

增加：

```cmake
option(ENABLE_DEBUG "Enable runtime debug engine" OFF)

if(ENABLE_DEBUG)
    add_compile_definitions(ENABLE_DEBUG)
    list(APPEND RT_CORE_SOURCES ${RT_SRC_DIR}/debug_engine.cpp)
endif()
```

### 4.2 Scheduler 头文件和实现

必须用编译期隔离：

```cpp
#ifdef ENABLE_DEBUG
DebugEngine* debug_ = nullptr;
void setDebugEngine(DebugEngine* debug) { debug_ = debug; }
#endif
```

tick 中：

```cpp
#ifdef ENABLE_DEBUG
if (debug_) debug_->applyPendingCommands();
#endif
```

`ENABLE_DEBUG=OFF` 时，普通 runtime 不应包含 DebugEngine 类型和调用。

### 4.3 desktop Makefile

当前 Makefile 使用：

```make
RT_SRCS = $(wildcard $(RT_DIR)/src/*.cpp)
```

这会导致新增 `debug_engine.cpp` 后普通 build 自动编译 debug 代码。

v1.1 要改为显式源文件列表，或排除 debug 文件：

```make
RT_SRCS = \
  $(RT_DIR)/src/gvl.cpp \
  $(RT_DIR)/src/error_policy.cpp \
  ...

DEBUG_SRCS = $(RT_DIR)/src/debug_engine.cpp target/desktop/debug_tcp_server.cpp

debug-server: CXXFLAGS += -DENABLE_DEBUG -O0 -g -ggdb
debug-server: SRCS += $(DEBUG_SRCS)
debug-server: $(TARGET)
```

## 5. Transport：桌面 TCP Server

第一版只做 desktop TCP，后续再做 UART/Modbus。

新增：

```text
target/desktop/debug_tcp_server.h
target/desktop/debug_tcp_server.cpp
```

文本协议：

```text
HELLO
LIST
WATCH <id1> <id2> ...
READ
FORCE <id> <hex>
UNFORCE <id>
CLEARFORCES
DIAG
QUIT
```

返回格式必须包含 OK/ERR：

```text
OK HELLO protocol=1 build_id=1234abcd var_count=42
ERR BAD_ID id=99
ERR BUILD_ID_MISMATCH
```

协议规则：

- CLI 连接后先 `HELLO`。
- CLI 的 `debug_map.json` build_id 必须匹配 runtime build_id。
- `FORCE` 必须校验变量存在、size 匹配、access 允许。
- `READ` 返回 raw hex，由 CLI 按 `debug_map.json` 格式化。

## 6. CLI 工具

新增：

```text
tools/debug_cli.cpp
```

用途：

- 验证 DebugEngine 协议。
- 提供无 IDE 的 PLC 在线监视。
- 作为后续 editor/UI 的协议参考实现。

命令：

```text
connect 127.0.0.1:9090
hello
list
watch MAIN.A MAIN.B
read
force MAIN.A 99
unforce MAIN.A
diag
quit
```

CLI 负责：

- 读取 `debug_map.json`
- name 到 id 映射
- raw hex 到 typed value 格式化
- BOOL/INT/REAL/LREAL/TIME 显示
- 数组展开
- forced 状态显示

## 7. `%M` / RETAIN / 数组 / 结构体语义

### 7.1 `%M`

当前 Java 编译器里 `%M` 地址解析为 `IODirection.MEMORY`，但 `registerIOVariable()` 直接跳过。

v1.1 决策：

- 第一阶段不承诺 `%M` force。
- 如果要支持 `%M`，需要明确映射到 GVL 还是独立 memory area。

### 7.2 RETAIN

RETAIN 本质仍在 GVL 中，只是有保留区间。

v1.1 决策：

- debug storage 可先标为 `GVL`。
- `debug_map.json` 额外加 `"retain": true`。
- 后续如需按 RETAIN 独立筛选，再扩展 `DebugStorage::RETAIN`。

### 7.3 数组

第一阶段：

- `debug_table.cpp` 记录数组基地址、元素类型、元素数量。
- CLI 可整段读取并展开显示。

后续阶段：

- 支持数组元素独立 watch，例如 `MAIN.ARR[3]`。

### 7.4 结构体

第一阶段：

- `debug_map.json` 记录 struct 字段 layout。
- CLI 可显示整体 raw 或字段展开。

后续阶段：

- 支持字段级 watch，例如 `MAIN.Axis.Status`.

## 8. 实施顺序

### M1：`#line` 可选输出

改动：

- `CompilerConfig.java`
- `Main.java`
- `PLCTranslatorNew.java`
- 语句级翻译入口
- `target/desktop/Makefile debug`

验收：

```bash
java -jar st2c.jar --input examples/test.st --output-dir output/flat/build --emit-line-directives
make debug
gdb ./plc_runtime_desktop.exe
```

### M2：debug metadata 生成

改动：

- `CompilerConfig.java` 增加 `--generate-debug`
- `GvlContext.java` 增加 `dumpDebugTable()` / `dumpDebugMap()`
- `Main.java` 输出 `debug_table.cpp` / `debug_map.json`

验收：

```bash
java -jar st2c.jar --input examples/test.st --output-dir output/flat/build --generate-debug
```

### M3：DebugEngine 单测

改动：

- `debug_if.h`
- `debug_engine.cpp`
- `debug_engine_test.cpp`

验收：

- `findVar`
- `setWatchList`
- `publishSnapshot`
- `readVar`
- `requestForce`
- `applyForces`
- bit input/output force
- invalid id/size/access

### M4：Scheduler 集成

改动：

- `scheduler.h`
- `scheduler.cpp`
- `constants.h`
- `CMakeLists.txt`
- `target/desktop/Makefile`

验收：

- 普通 build 不包含 debug 代码。
- `ENABLE_DEBUG` build 可运行。
- 原有 runtime tests 不回归。

### M5：Desktop TCP Server

改动：

- `target/desktop/debug_tcp_server.h`
- `target/desktop/debug_tcp_server.cpp`
- `target/desktop/runtime_main.cpp`
- `target/desktop/Makefile`

验收：

```bash
make debug-server
./plc_runtime_desktop.exe
telnet 127.0.0.1 9090
HELLO
```

### M6：CLI 工具

改动：

- `tools/debug_cli.cpp`

验收：

```bash
debug_cli 127.0.0.1 9090 output/flat/build/debug_map.json
> hello
> watch MAIN.A
> read
> force MAIN.A 99
> unforce MAIN.A
```

### M7：端到端验证

完整链路：

```text
ST
-> Java compiler --generate-debug --emit-line-directives
-> generated C++ + debug_table.cpp + debug_map.json
-> desktop debug-server
-> CLI watch/read/force
```

必须覆盖：

- GVL INT force
- GVL REAL watch
- INPUT `%IX0.0` force
- OUTPUT `%QX0.0` force
- 数组 watch
- build_id mismatch
- invalid id
- ERROR safe output 优先级
- release build 无 debug 代码

## 9. 后续扩展

不放入第一轮：

- UART transport for STM32H7
- Modbus FC `0x41-0x45`
- openplc-editor 接入
- VS Code UI 接入
- ring-buffer trace
- breakpoint/single-step
- 在线变量写入权限系统
- EtherCAT PDO 自动映射显示

## 10. 最终结论

v1.1 的核心路线：

```text
先做源码定位 + 变量表 + 周期边界 snapshot/force，
再做 TCP/CLI，
最后接 editor 和现场 transport。
```

这可以避免调试系统变成旁路 demo，而是直接长在 ST2C 的硬实时 runtime 骨架上。

---

## 附录 A：设计细节确认 (2026-07-03)

以下 5 个问题在 v1.1 方案评审时确认：

### A1: `bindMemory(GVL* gvl, ProcessImage* image)` 与参数传递的关系

**结论：** 不是冗余，两条路径各取所需。

- `bindMemory` 存的指针供 transport 线程的 `readMemory()` 使用——transport 线程不知道 Scheduler 的存在。
- `applyForces` / `publishSnapshot` 接收引用供 Scheduler tick 线程调用——tick 里 `gvl` 和 `image` 就在手边。

两者最终操作同一对象（`&sched.gvl` / `&sched.image`）。

### A2: `bitOffset` 类型

**结论：** 用 `uint8_t`，`0xFF` 表示非 bit 变量。

```cpp
struct DebugVar {
    uint32_t     id;          // 4
    DebugStorage storage;     // 1
    uint8_t      bitOffset;   // 1, 0xFF = 非 bit
    DebugType    type;        // 1
    DebugAccess  access;      // 1
    uint32_t     offset;      // 4
    uint16_t     size;        // 2
    uint16_t     count;       // 2
};                            // 16 字节，无 padding
```

判断方式：`if (v.bitOffset != 0xFF) { /* BOOL 变量，bit 读写 */ }`

### A3: pending command queue 容量与溢出

**结论：** 固定容量 16 条，满时丢弃最旧命令 + `droppedCommands_` 计数器，不阻塞 transport 线程。

```cpp
#ifndef DEBUG_MAX_PENDING_COMMANDS
#define DEBUG_MAX_PENDING_COMMANDS 16
#endif
```

MCU 目标可通过 CMake 覆写为 8。

### A4: 数组变量 watch

**结论：** snapshot 只存标量（`v->count == 1` 且 `v->size <= 8`），数组/大结构体跳过。CLI 发现数组变量时不走 `READ` 命令，自动发送 `RANGE GVL <offset> <total_size>` 直接读活内存，按 `debug_map.json` 的 `count`/`size` 拆开显示。

理由：runtime 周期内零额外压力，复杂度全部在 CLI 侧。

### A5: `debug_table.cpp` 生成时机

**结论：** 全局合并。当前 `Main.java` 已是单一全局 `GvlContext`，所有文件共享。`dumpDebugTable()` / `dumpDebugMap()` 在 `main()` 末尾（`emitGVLLayoutHeader()` 旁边）统一输出。ID 全局递增，无重复风险。
