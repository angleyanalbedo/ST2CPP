# tools / .env — 工具集

## .env — 本地编译工具链

`.env/` 存放 ARM 交叉编译器 + OpenOCD，由 `tools_download.ps1` 自动下载。

### 下载

```powershell
powershell -ExecutionPolicy Bypass -File tools_download.ps1
```

脚本操作：
1. 下载 ARM GCC 15.2.rel1（固定地址，已验证）
2. 通过 GitHub API 获取最新版 OpenOCD（xpack 发行版）
3. 解压到 `.env/arm-gcc/` 和 `.env/opencoded/`

### 验证

```powershell
.env\arm-gcc\bin\arm-none-eabi-gcc.exe --version
.env\opencoded\bin\openocd.exe --version
```

### 使用

Makefile 会自动检测 `.env/` 下的工具链并优先使用，不存在时回退到系统 PATH。

---

## debug_cli — 调试客户端

`tools/debug_cli.cpp` — 通过 TCP 连接 DebugEngine，提供两种调试模式：交互式一问一答和实时监视面板。

## 快速开始

```bash
# 终端 1：启动 debug server
cd target/desktop && make debug-server
./build/plc_runtime_desktop_dbg.exe --cycle-us 3000

# 终端 2：连接调试
cd tools && ./debug_cli.exe 127.0.0.1 9090 ../output/flat/build/debug_map.json monitor
```

## 命令行参数

```bash
./debug_cli.exe <host> <port> [debug_map.json] [monitor]

  host           调试服务器地址 (默认 127.0.0.1)
  port           调试端口 (默认 9090)
  debug_map.json 变量清单（自动加载变量名和类型）
  monitor        直接进入实时面板模式（不设则进入一问一答模式）
```

示例：

```bash
# 一问一答模式
./debug_cli.exe 127.0.0.1 9090 output/flat/build/debug_map.json

# 实时面板模式
./debug_cli.exe 127.0.0.1 9090 output/flat/build/debug_map.json monitor
```

## 模式一：一问一答

适合单次查询、强制操作。

```bash
> hello
  OK HELLO protocol=1 build_id=... var_count=5
> watch Counter Factor
  watching 2 variables
> read
  [1] Counter = 138  (INT)
  [2] Factor  = 2.500 (REAL)
> force Switch 1
  OK FORCE
> unforce Switch
  OK UNFORCE
> quit
```

### 命令一览

| 命令 | 作用 |
|------|------|
| `hello` | 查看服务器信息（协议版本、变量数） |
| `list` | 从服务器获取变量列表 |
| `watch <name>...` | 设置监视变量（空格分隔多个） |
| `read` | 读取所有监视变量的当前值 |
| `force <name> <value>` | 强制变量为指定值 |
| `unforce <name>` | 取消变量强制 |
| `diag` | 查看诊断信息（周期数、抖动等） |
| `monitor` | 切换到实时面板模式 |
| `quit` | 退出 |

### 变量名匹配

支持**短名匹配**：

```bash
# 全名：test_debug_full$MAIN$Counter
# 也可用：
watch Counter           # 自动匹配短名
```

## 模式二：实时面板

适合持续监视变量变化。进入后自动添加所有标量变量到监视列表。

```
┌─ ST2C Debug Monitor ──────────────────────────┐
│ cycle=1234  ticks=1234  refresh=500ms          │
├───────────────────────────────────────────────┤
│ Variable           Type    Value     Forced    │
│ Counter            INT     1234               │
│ Factor             REAL    2.500              │
│ Switch             BOOL    TRUE      ► FORCED │
│ LED                BOOL    FALSE              │
├───────────────────────────────────────────────┤
│ > force Counter 99                             │
└───────────────────────────────────────────────┘
```

面板内命令直接在底部输入：

| 命令 | 作用 |
|------|------|
| `add <name>` | 添加变量到监视列表 |
| `remove <name>` | 从监视列表移除 |
| `force <name> <value>` | 强制变量 |
| `unforce <name>` | 取消强制 |
| `clearforces` | 清除所有强制 |
| `interval <ms>` | 修改刷新间隔（最小 50ms） |
| `pause` | 暂停自动刷新 |
| `resume` | 恢复自动刷新 |
| `help` | 显示命令帮助 |
| `quit` | 退出面板 |

### 快捷键

| 按键 | 作用 |
|------|------|
| `Enter` | 执行命令 |
| `Backspace` | 删除上一个字符 |
| `Ctrl-C` | 强制退出 |

## 运行前准备

1. 编译 ST 源码（需带 `--generate-debug`）
2. 用 `make debug-server` 构建 debug 版运行时
3. 启动运行时（`.exe` 会自动监听 `:9090`）

```bash
# 编译 ST
java -jar st2c.jar --input your.st --output-dir output/flat/build --generate-debug

# 构建 + 启动
cd target/desktop && make debug-server
./build/plc_runtime_desktop_dbg.exe --cycle-us 3000
```

## 调试协议

`debug_cli` 通过 TCP 文本协议与 DebugEngine 通信。协议支持的命令见下表：

| 命令 | 格式 |
|------|------|
| HELLO | `HELLO` → `OK HELLO protocol=1 build_id=... var_count=N` |
| LIST | `LIST` → 多行 `var id=N ...` + `OK LIST` |
| WATCH | `WATCH <id>...` → `OK WATCH` |
| READ | `READ` → `OK READ cycle=N ...` + 多行 `sample ...` |
| FORCE | `FORCE <id> <hex>` → `OK FORCE` |
| UNFORCE | `UNFORCE <id>` → `OK UNFORCE` |
| CLEARFORCES | `CLEARFORCES` → `OK CLEARFORCES` |
| RANGE | `RANGE GVL <offset> <size>` → `OK RANGE ... hex=...` |
| DIAG | `DIAG` → `OK DIAG cycle=N ...` |
| QUIT | `QUIT` → `OK BYE` |

不依赖任何第三方库，纯 C++17 + Winsock/POSIX socket。
