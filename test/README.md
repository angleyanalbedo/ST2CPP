# ST2C Debug System Tests

## E2E Test (`e2e_test.ps1`)

端到端自动化测试 — 启动 debug server，通过 TCP 协议逐项验证 HELLO / LIST / WATCH+READ / FORCE / RANGE / DIAG / Release。

### 用法

```bash
pwsh -File test/e2e_test.ps1 \
  -Binary target/desktop/plc_runtime_desktop_dbg.exe \
  -Map output/flat/build/debug_map.json \
  -CycleUs 2000
```

### 参数

| 参数 | 必填 | 默认 | 说明 |
|------|------|------|------|
| `-Binary` | 是 | — | debug server 可执行文件路径 |
| `-Map` | 是 | — | debug_map.json 路径（自动从中解析变量列表） |
| `-Host` | 否 | `127.0.0.1` | 服务器 IP |
| `-Port` | 否 | `9090` | 服务器端口 |
| `-CycleUs` | 否 | `5000` | PLC 周期（微秒），越小测试越快 |

### 测试覆盖

| # | 测试 | 说明 |
|---|---|---|
| 1 | HELLO | 验证 var_count 与 debug_map.json 一致 |
| 2 | LIST | 验证所有变量 id 均被服务器枚举 |
| 3 | GVL watch/read | 自动遍历所有 GVL 标量，watch 后 read，验证值非空 |
| 4 | INPUT force | 对所有 INPUT FORCEABLE 变量逐一 force/unforce |
| 5 | OUTPUT force | 对所有 OUTPUT FORCEABLE 变量逐一 force/unforce |
| 6 | Array RANGE | 对所有 count>1 的数组变量发 RANGE 读整段 |
| 7 | Invalid ID | 发送不存在的变量 ID，验证服务器不崩溃 |
| 8 | DIAG | 获取诊断信息 |
| 9 | Release | 启动 release binary (stub)，验证调试端口不开放 |

### 运行前准备

```bash
# 1. 编译 ST 源码（带调试元数据）
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input your_project.st --output-dir output/flat/build \
  --generate-debug --no-stdlib

# 2. 构建 debug server
cd target/desktop && make debug-server

# 3. 运行测试
cd ../..
pwsh -File test/e2e_test.ps1 \
  -Binary target/desktop/plc_runtime_desktop_dbg.exe \
  -Map output/flat/build/debug_map.json
```

### 前置条件

- `debug_map.json` 必须通过 `--generate-debug` 编译选项生成
- debug server 二进制必须用 `make debug-server` 构建（带 `ENABLE_DEBUG`）
- 测试结束自动停止服务器进程，无需手动清理

## Unit Tests (`runtime-flat/tests/`)

### debug_engine_test.cpp

DebugEngine 核心逻辑单元测试（40 项），覆盖 findVar / hello / publishSnapshot / readVar / readMemory / requestForce / applyForces / bit force / 越界 / 无效 ID / ERROR 安全输出 / pending queue 溢出。

```bash
cd runtime-flat
g++ -std=c++17 -DENABLE_DEBUG -I include -O0 -g \
  tests/debug_engine_test.cpp src/debug_engine.cpp \
  -o debug_engine_test && ./debug_engine_test
```

### framework_test / runtime_integration_test (CMake)

```bash
cd runtime-flat/build
cmake .. -G "MinGW Makefiles" -DRT_BUILD_TESTS=ON
cmake --build .
./tests/framework_test.exe
./tests/runtime_integration_test.exe
```
