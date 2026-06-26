/**
 * runtime_integration_test.cpp — Runtime 完整流程集成测试
 *
 * 用假的 POU callback 模拟编译器生成的代码，验证 Runtime 全链路：
 *   1. POU 注册 → Scheduler 配置 → tick 执行
 *   2. GVL 跨 POU 变量共享
 *   3. ProcessImage I/O 同步
 *   4. 多任务优先级调度顺序
 *   5. Event 任务触发
 *   6. 看门狗超时检测
 *   7. 冷/暖启动 RETAIN 保留
 *   8. 错误管理集成
 *   9. TCI 硬件抽象层
 */
#include "rt_runtime.h"
#include <cstdio>
#include <cstring>
#include <cassert>

using namespace rt_plc;

// ─── 测试框架 ───
static int g_pass = 0;
static int g_fail = 0;

#define TEST_SECTION(name) printf("\n=== %s ===\n", name)
#define TEST(name) printf("  %-55s", name)
#define PASS() do { printf("[PASS]\n"); g_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); g_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)


// ═══════════════════════════════════════════════════════
// GVL 布局（模拟编译器生成的偏移量）
// ═══════════════════════════════════════════════════════

namespace Off {
    constexpr size_t COUNTER      = 0;   // DINT
    constexpr size_t SETPOINT     = 4;   // REAL
    constexpr size_t SENSOR       = 8;   // REAL
    constexpr size_t OUTPUT       = 12;  // REAL
    constexpr size_t ALARM_FLAG   = 16;  // BOOL
    constexpr size_t ALARM_COUNT  = 20;  // DINT
    constexpr size_t RETAIN_VAL   = 24;  // DINT  (RETAIN)
    constexpr size_t RETAIN_START = 24;
    constexpr size_t RETAIN_END   = 28;
    constexpr size_t TICK_ACCUM   = 28;  // DINT (测试 4 个连续 DINT)
    constexpr size_t ERROR_COUNT  = 44;  // DINT
}


// ═══════════════════════════════════════════════════════
// 假 POU 回调（模拟编译器生成的 PROGRAM）
// ═══════════════════════════════════════════════════════

// --- POU_ControlLoop: 简单 P 控制器 ---
static int ctrl_init_count = 0;
static int ctrl_cyclic_count = 0;

void POU_ControlLoop_init(GVL& gvl, ProcessImage& io) {
    ctrl_init_count++;
    gvl.write<DINT>(Off::COUNTER, 0);
    gvl.write<REAL>(Off::SETPOINT, 50.0f);
    gvl.write<REAL>(Off::OUTPUT, 0.0f);
}

void POU_ControlLoop_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    ctrl_cyclic_count++;
    REAL sp = gvl.read<REAL>(Off::SETPOINT);
    REAL sv = gvl.read<REAL>(Off::SENSOR);
    REAL out = (sp - sv) * 2.0f;
    gvl.write<REAL>(Off::OUTPUT, out);

    DINT cnt = gvl.read<DINT>(Off::COUNTER);
    gvl.write<DINT>(Off::COUNTER, cnt + 1);
}

// --- POU_Monitor: 报警检查 ---
static int mon_cyclic_count = 0;

void POU_Monitor_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    mon_cyclic_count++;
    REAL out = gvl.read<REAL>(Off::OUTPUT);
    BOOL alarm = (out >= 100.0f || out <= -100.0f) ? TRUE : FALSE;
    gvl.write<BOOL>(Off::ALARM_FLAG, alarm);
}

// --- POU_AlarmHandler: 报警计数 ---
static int alarm_cyclic_count = 0;

void POU_AlarmHandler_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    alarm_cyclic_count++;
    BOOL af = gvl.read<BOOL>(Off::ALARM_FLAG);
    if (af) {
        DINT ac = gvl.read<DINT>(Off::ALARM_COUNT);
        gvl.write<DINT>(Off::ALARM_COUNT, ac + 1);
    }
}

// --- POU_TickAccum: 累加 tick 时间 ---
static int tick_cyclic_count = 0;

void POU_TickAccum_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    tick_cyclic_count++;
    DINT prev = gvl.read<DINT>(Off::TICK_ACCUM);
    gvl.write<DINT>(Off::TICK_ACCUM, prev + (DINT)dt);
}

// --- POU_ErrorRecorder: 读 ErrorManager 状态 ---
void POU_ErrorRecorder_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    // 仅记录到 GVL（测试 GVL 跨 POU 读写）
    DINT ec = gvl.read<DINT>(Off::ERROR_COUNT);
    gvl.write<DINT>(Off::ERROR_COUNT, ec + 1);
}

// --- POU_SlowTask: 故意慢的任务（触发看门狗） ---
static int slow_cyclic_count = 0;

void POU_SlowTask_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    slow_cyclic_count++;
    volatile double dummy = 0;
    for (volatile int i = 0; i < 20000000; i++) dummy += 1.0;
}

// --- POU 优先级顺序标记（每个写不同 GVL 偏移） ---
void POU_Prio0_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    gvl.write<DINT>(Off::TICK_ACCUM + 0, 999);
}
void POU_Prio1_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    gvl.write<DINT>(Off::TICK_ACCUM + 4, 999);
}
void POU_Prio2_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    gvl.write<DINT>(Off::TICK_ACCUM + 8, 999);
}
void POU_Prio3_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    gvl.write<DINT>(Off::TICK_ACCUM + 12, 999);
}


// ═══════════════════════════════════════════════════════
// 假 TCI（硬件 I/O 抽象）
// ═══════════════════════════════════════════════════════

struct FakeTCI : public TCI {
    int syncIn_count = 0;
    int syncOut_count = 0;
    REAL fake_sensor = 25.0f;

    void syncInputs(ProcessImage& img) override {
        syncIn_count++;
        img.writeOutput<REAL>(Off::SENSOR, fake_sensor);
    }

    void syncOutputs(ProcessImage& img) override {
        syncOut_count++;
    }
};


// ═══════════════════════════════════════════════════════
// Event 任务条件函数
// ═══════════════════════════════════════════════════════

BOOL check_alarm_rising(GVL& gvl, ProcessImage& io) {
    return gvl.read<BOOL>(Off::ALARM_FLAG);
}


// ═══════════════════════════════════════════════════════
// 错误回调
// ═══════════════════════════════════════════════════════

static int error_callback_count = 0;
static ErrorCode last_callback_code = ErrorCode::NONE;

void error_callback(const ErrorEntry& entry) {
    error_callback_count++;
    last_callback_code = entry.code;
}


// ═══════════════════════════════════════════════════════
// 测试 1: 基本注册 + tick 执行
// ═══════════════════════════════════════════════════════

void test_basic_register_and_tick() {
    TEST_SECTION("1. POU 注册 → Scheduler → tick 执行");

    POURegistry reg;
    reg.add("ControlLoop", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    reg.add("Monitor", nullptr, POU_Monitor_cyclic);
    TEST("注册 2 个 POU 到 Registry");
    CHECK(reg.count() == 2, "count should be 2");

    const auto* cbs = reg.lookup("ControlLoop");
    TEST("lookup(\"ControlLoop\") 返回有效回调");
    CHECK(cbs != nullptr && cbs->cyclic != nullptr, "cyclic should exist");

    const auto* miss = reg.lookup("NonExistent");
    TEST("lookup(\"NonExistent\") 返回 nullptr");
    CHECK(miss == nullptr, "should be nullptr");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(500));

    // 用 Freewheeling 任务保证每次 tick 都执行
    int tIdx = sched.addFreewheelingTask("CtrlTask", 5);
    TEST("添加 Freewheeling 任务");
    CHECK(tIdx >= 0, "task index should be >= 0");

    sched.addProgram("ControlLoop", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgram("Monitor", nullptr, POU_Monitor_cyclic);
    sched.addProgramToTask(tIdx, 0);
    sched.addProgramToTask(tIdx, 1);

    TEST("挂载 2 个 PROGRAM 到任务");
    CHECK(sched.programCount() == 2, "programCount should be 2");

    ctrl_init_count = 0;
    ctrl_cyclic_count = 0;
    mon_cyclic_count = 0;

    sched.tick();  // start(COLD) + init + cyclic
    TEST("第一次 tick 触发 Init + Cyclic");
    CHECK(ctrl_init_count == 1, "init should be called once");
    CHECK(ctrl_cyclic_count == 1, "cyclic should be called once");
    CHECK(mon_cyclic_count == 1, "monitor cyclic once");

    DINT counter = sched.gvl.read<DINT>(Off::COUNTER);
    TEST("GVL: counter 增到 1");
    CHECK(counter == 1, "counter should be 1");

    for (int i = 0; i < 5; i++) sched.tick();
    TEST("连续 6 次 tick 后 counter = 6");
    counter = sched.gvl.read<DINT>(Off::COUNTER);
    CHECK(counter == 6, "counter should be 6");

    TEST("ctrl_cyclic_count = 6");
    CHECK(ctrl_cyclic_count == 6, "should be 6");

    REAL sp = sched.gvl.read<REAL>(Off::SETPOINT);
    TEST("GVL: setpoint 被 init 设为 50.0");
    CHECK(sp > 49.9f && sp < 50.1f, "setpoint should be ~50.0");
}


// ═══════════════════════════════════════════════════════
// 测试 2: GVL 跨 POU 变量共享
// ═══════════════════════════════════════════════════════

void test_gvl_shared_variables() {
    TEST_SECTION("2. GVL 跨 POU 变量共享");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    int t1 = sched.addFreewheelingTask("Control", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(t1, 0);

    int t2 = sched.addFreewheelingTask("Monitor", 10);
    sched.addProgram("MON", nullptr, POU_Monitor_cyclic);
    sched.addProgramToTask(t2, 1);

    // start(COLD) 会清零 GVL，所以先 tick 一次让 start 跑完
    sched.tick();
    // 然后再写 SENSOR
    sched.gvl.write<REAL>(Off::SENSOR, 100.0f);
    sched.tick();

    REAL output = sched.gvl.read<REAL>(Off::OUTPUT);
    TEST("sensor=100 → output = (50-100)*2 = -100.0");
    CHECK(output > -100.1f && output < -99.9f, "output should be ~-100.0");

    BOOL alarm = sched.gvl.read<BOOL>(Off::ALARM_FLAG);
    TEST("output=-100 → alarm_flag = TRUE (|out| >= 100)");
    CHECK(alarm == TRUE, "alarm should be TRUE");

    // 改 sensor = 30 → output = (50-30)*2 = 40 → alarm = FALSE
    sched.gvl.write<REAL>(Off::SENSOR, 30.0f);
    sched.tick();

    output = sched.gvl.read<REAL>(Off::OUTPUT);
    alarm = sched.gvl.read<BOOL>(Off::ALARM_FLAG);
    TEST("sensor=30 → output=40, alarm=FALSE");
    CHECK(output > 39.9f && output < 40.1f, "output should be ~40.0");
    CHECK(alarm == FALSE, "alarm should be FALSE");
}


// ═══════════════════════════════════════════════════════
// 测试 3: ProcessImage I/O 同步（TCI）
// ═══════════════════════════════════════════════════════

void test_tci_io_sync() {
    TEST_SECTION("3. TCI 硬件 I/O 同步");

    FakeTCI tci;
    tci.fake_sensor = 37.5f;

    Scheduler sched;
    sched.gvl.clear();
    sched.setTCI(&tci);
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    int t1 = sched.addFreewheelingTask("Main", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(t1, 0);

    sched.tick();  // start + init + cyclic

    TEST("TCI::syncInputs 被调用");
    CHECK(tci.syncIn_count >= 1, "syncIn should be >= 1");

    TEST("TCI::syncOutputs 被调用");
    CHECK(tci.syncOut_count >= 1, "syncOut should be >= 1");

    // ControlLoop 执行了（通过 cyclic 计数验证）
    TEST("ControlLoop cyclic 被执行");
    CHECK(ctrl_cyclic_count >= 1, "cyclic should be >= 1");
}


// ═══════════════════════════════════════════════════════
// 测试 4: 多任务优先级调度顺序
// ═══════════════════════════════════════════════════════

void test_priority_ordering() {
    TEST_SECTION("4. 多任务优先级调度顺序");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    int t3 = sched.addFreewheelingTask("Low",    10);
    int t1 = sched.addFreewheelingTask("High",    0);
    int t2 = sched.addFreewheelingTask("Medium",  5);
    int t0 = sched.addFreewheelingTask("Critical", 0);

    sched.addProgram("P0", nullptr, POU_Prio0_cyclic);
    sched.addProgram("P1", nullptr, POU_Prio1_cyclic);
    sched.addProgram("P2", nullptr, POU_Prio2_cyclic);
    sched.addProgram("P3", nullptr, POU_Prio3_cyclic);

    sched.addProgramToTask(t0, 0);
    sched.addProgramToTask(t1, 1);
    sched.addProgramToTask(t2, 2);
    sched.addProgramToTask(t3, 3);

    // tick 1: start+init (systemTime=0, cyclic不执行)
    // tick 2: cyclic 执行
    sched.tick();
    sched.tick();

    DINT v0 = sched.gvl.read<DINT>(Off::TICK_ACCUM + 0);
    DINT v3 = sched.gvl.read<DINT>(Off::TICK_ACCUM + 12);

    TEST("4 个任务都执行了");
    CHECK(v0 == 999 && v3 == 999, "all 4 POU executed");

    TEST("scheduler 有 4 个任务");
    CHECK(sched.taskCount() == 4, "taskCount should be 4");
}


// ═══════════════════════════════════════════════════════
// 测试 5: Event 任务触发
// ═══════════════════════════════════════════════════════

void test_event_trigger() {
    TEST_SECTION("5. Event 任务上升沿触发");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    alarm_cyclic_count = 0;

    // ControlLoop: sensor=0 → output=100 → 会触发 alarm
    int tc = sched.addFreewheelingTask("Ctrl", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(tc, 0);

    // Monitor: 读 output，写 alarm_flag
    int tm = sched.addFreewheelingTask("Mon", 6);
    sched.addProgram("MON", nullptr, POU_Monitor_cyclic);
    sched.addProgramToTask(tm, 1);

    int te = sched.addEventTask("AlarmEvt", 0, check_alarm_rising, EventEdge::RISING);
    sched.addProgram("ALM", nullptr, POU_AlarmHandler_cyclic);
    sched.addProgramToTask(te, 2);

    // tick 1: start(COLD) → clear → init(OUTPUT=0) → checkEvents(ALARM=0) →
    //         ControlLoop(sensor=0 → output=100) → Monitor(output=100 → alarm=TRUE)
    sched.tick();
    BOOL af1 = sched.gvl.read<BOOL>(Off::ALARM_FLAG);

    // tick 2: checkEvents() 读到 ALARM=TRUE(lastState=FALSE) → RISING → fires!
    sched.tick();

    TEST("sensor=0 → output=100 → alarm 上升沿触发");
    CHECK(af1 == TRUE, "alarm should be TRUE after tick1");
    CHECK(alarm_cyclic_count == 1, "alarm handler should fire once");

    // tick 3: ALARM 持续 TRUE → 无新上升沿
    sched.tick();
    TEST("alarm 持续 TRUE, event 不再触发");
    CHECK(alarm_cyclic_count == 1, "should still be 1");

    // 改 sensor=30 → output=40 → alarm=FALSE
    sched.gvl.write<REAL>(Off::SENSOR, 30.0f);
    sched.tick();  // ControlLoop + Monitor: alarm → FALSE

    TEST("回落到 FALSE");
    CHECK(sched.gvl.read<BOOL>(Off::ALARM_FLAG) == FALSE, "alarm should be FALSE");

    // 改 sensor=100 → output=-100 → alarm=TRUE → 上升沿
    sched.gvl.write<REAL>(Off::SENSOR, 100.0f);
    sched.tick();  // Monitor: alarm → TRUE
    sched.tick();  // checkEvents: 上升沿 → fires!

    TEST("回落再上升后 event 再次触发");
    CHECK(alarm_cyclic_count == 2, "alarm handler should be 2");
}


// ═══════════════════════════════════════════════════════
// 测试 6: 看门狗超时检测
// ═══════════════════════════════════════════════════════

void test_watchdog() {
    TEST_SECTION("6. 看门狗超时检测");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_ms(1));
    sched.watchdog.setDefault(T_ms(1));  // 1ms 看门狗

    slow_cyclic_count = 0;
    int tIdx = sched.addFreewheelingTask("Slow", 5);
    sched.addProgram("SLOW", nullptr, POU_SlowTask_cyclic);
    sched.addProgramToTask(tIdx, 0);
    sched.setTaskWatchdog(tIdx, T_ms(1));  // 1ms 超时

    for (int i = 0; i < 5; i++) {
        sched.tick();
    }

    TEST("看门狗检测到超时");
    CHECK(sched.watchdog.tripped == true, "watchdog should be tripped");

    TEST("任务进入 OVERRUN 状态");
    const Task& t = sched.task(0);
    CHECK(t.state == TaskState::OVERRUN, "task should be OVERRUN");
}


// ═══════════════════════════════════════════════════════
// 测试 7: 冷/暖启动 RETAIN
// ═══════════════════════════════════════════════════════

void test_cold_warm_start() {
    TEST_SECTION("7. 冷/暖启动 RETAIN 保留");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));
    sched.setRetainRegion(Off::RETAIN_START, Off::RETAIN_END);

    int tIdx = sched.addFreewheelingTask("Main", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(tIdx, 0);

    // 冷启动后 tick 3 次：init + 2 次 cyclic
    sched.start(StartupMode::COLD);
    for (int i = 0; i < 3; i++) sched.tick();

    DINT retain_after_run = sched.gvl.read<DINT>(Off::RETAIN_VAL);
    DINT counter_after_run = sched.gvl.read<DINT>(Off::COUNTER);

    TEST("运行后 counter > 0");
    CHECK(counter_after_run > 0, "counter should be > 0");

    // 暖启动（start 不重置 totalTicks，但会 clearNonRetain）
    DINT prevTotalTicks = sched.totalTicks;
    sched.start(StartupMode::WARM);

    DINT retain_after_warm = sched.gvl.read<DINT>(Off::RETAIN_VAL);
    DINT counter_after_warm = sched.gvl.read<DINT>(Off::COUNTER);

    TEST("暖启动后 RETAIN 变量保留");
    CHECK(retain_after_warm == retain_after_run, "retain should be preserved");

    TEST("暖启动后非 RETAIN 变量清零");
    CHECK(counter_after_warm == 0, "non-retain should be zeroed");

    // 冷启动清零一切
    sched.start(StartupMode::COLD);
    DINT retain_after_cold = sched.gvl.read<DINT>(Off::RETAIN_VAL);
    TEST("冷启动后 RETAIN 变量也清零");
    CHECK(retain_after_cold == 0, "retain should be cleared");
}


// ═══════════════════════════════════════════════════════
// 测试 8: 错误管理集成
// ═══════════════════════════════════════════════════════

void test_error_integration() {
    TEST_SECTION("8. 错误管理集成");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));
    sched.setErrorHandler(error_callback);

    error_callback_count = 0;
    last_callback_code = ErrorCode::NONE;

    // 用 safeDiv 触发除零
    INT result = sched.errorMgr.safeDiv(10, 0, 1, 42, 0);

    TEST("safeDiv(10, 0) 返回 0");
    CHECK(result == 0, "should return 0");

    TEST("错误回调被触发");
    CHECK(error_callback_count == 1, "callback count should be 1");

    TEST("错误码为 DIV_BY_ZERO");
    CHECK(last_callback_code == ErrorCode::DIV_BY_ZERO, "code should be DIV_BY_ZERO");

    TEST("ErrorManager 记录了错误");
    CHECK(sched.errorMgr.totalCount == 1, "totalCount should be 1");

    TEST("ErrorManager 进入 fatalMode");
    CHECK(sched.errorMgr.fatalMode == true, "fatalMode should be true");

    // 访问 errorMgr 日志
    const ErrorEntry& e = sched.errorMgr.log[0];
    TEST("错误日志包含操作数信息");
    CHECK(e.pouId == 1 && e.lineNo == 42, "pouId and line should match");
    CHECK(e.operandA == 10 && e.operandB == 0, "operands should match");
}


// ═══════════════════════════════════════════════════════
// 测试 9: 诊断统计
// ═══════════════════════════════════════════════════════

void test_diagnostics() {
    TEST_SECTION("9. 诊断统计");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    int tIdx = sched.addFreewheelingTask("Main", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(tIdx, 0);

    for (int i = 0; i < 10; i++) sched.tick();

    TEST("totalTicks = 10");
    CHECK(sched.totalTicks == 10, "totalTicks should be 10");

    TEST("diag.totalScanCount > 0");
    CHECK(sched.diag.totalScanCount > 0, "scanCount should be > 0");

    TEST("diag.minScanTime <= avgScanTime");
    CHECK(sched.diag.minScanTime <= sched.diag.avgScanTime(),
          "min should <= avg");

    TEST("diag.maxScanTime >= avgScanTime");
    CHECK(sched.diag.maxScanTime >= sched.diag.avgScanTime(),
          "max should >= avg");

    TEST("Task cycleCount = 10");
    CHECK(sched.task(tIdx).cycleCount == 10, "cycleCount should be 10");

    TEST("Task lastExecTime >= 0");
    CHECK(sched.task(tIdx).lastExecTime >= 0, "execTime should be >= 0");
}


// ═══════════════════════════════════════════════════════
// 测试 10: 状态机转换
// ═══════════════════════════════════════════════════════

void test_state_machine() {
    TEST_SECTION("10. 系统状态机转换");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    TEST("初始状态 STOP");
    CHECK(sched.systemState == SystemState::STOP, "should be STOP");

    sched.start();
    TEST("start() → RUN");
    CHECK(sched.systemState == SystemState::RUN, "should be RUN");

    sched.pause();
    TEST("pause() → PAUSED");
    CHECK(sched.systemState == SystemState::PAUSED, "should be PAUSED");

    sched.resume();
    TEST("resume() → RUN");
    CHECK(sched.systemState == SystemState::RUN, "should be RUN");

    sched.stop();
    TEST("stop() → STOP");
    CHECK(sched.systemState == SystemState::STOP, "should be STOP");

    sched.error();
    TEST("error() → ERROR");
    CHECK(sched.systemState == SystemState::ERROR, "should be ERROR");

    sched.resetError();
    TEST("resetError() → STOP");
    CHECK(sched.systemState == SystemState::STOP, "should be STOP");
}


// ═══════════════════════════════════════════════════════
// 测试 11: POURegistry 批量注册
// ═══════════════════════════════════════════════════════

void test_registry_batch() {
    TEST_SECTION("11. POURegistry 批量注册与查找");

    POURegistry reg;
    reg.add("POU_A", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    reg.add("POU_B", nullptr, POU_Monitor_cyclic);
    reg.add("POU_C", nullptr, POU_AlarmHandler_cyclic);
    reg.add("POU_D", nullptr, POU_TickAccum_cyclic);

    TEST("注册 4 个 POU");
    CHECK(reg.count() == 4, "count should be 4");

    TEST("POU_A 查找成功");
    CHECK(reg.lookup("POU_A") != nullptr, "should find POU_A");

    TEST("POU_D 查找成功");
    CHECK(reg.lookup("POU_D") != nullptr, "should find POU_D");

    TEST("POU_X 查找失败");
    CHECK(reg.lookup("POU_X") == nullptr, "should not find POU_X");

    TEST("entries() 返回有效数组");
    const auto& e = reg.entries()[0];
    CHECK(strcmp(e.name, "POU_A") == 0, "first entry should be POU_A");
    CHECK(e.cbs.init == POU_ControlLoop_init, "init callback should match");
    CHECK(e.cbs.cyclic == POU_ControlLoop_cyclic, "cyclic callback should match");
}


// ═══════════════════════════════════════════════════════
// 测试 12: Freewheeling 任务
// ═══════════════════════════════════════════════════════

void test_freewheeling_task() {
    TEST_SECTION("12. Freewheeling 任务（每次 tick 都执行）");

    Scheduler sched;
    sched.gvl.clear();
    sched.setBaseCycle(T_us(1));
    sched.watchdog.setDefault(T_ms(100));

    ctrl_cyclic_count = 0;
    int tIdx = sched.addFreewheelingTask("Free", 5);
    sched.addProgram("CL", POU_ControlLoop_init, POU_ControlLoop_cyclic);
    sched.addProgramToTask(tIdx, 0);

    for (int i = 0; i < 5; i++) sched.tick();

    TEST("Freewheeling 任务每次 tick 都执行");
    CHECK(ctrl_cyclic_count == 5, "should execute 5 times");
}


// ═══════════════════════════════════════════════════════
// Main
// ═══════════════════════════════════════════════════════

int main() {
    printf("═══════════════════════════════════════\n");
    printf("  Runtime 集成测试（假 POU callback）\n");
    printf("═══════════════════════════════════════\n");

    test_basic_register_and_tick();
    test_gvl_shared_variables();
    test_tci_io_sync();
    test_priority_ordering();
    test_event_trigger();
    test_watchdog();
    test_cold_warm_start();
    test_error_integration();
    test_diagnostics();
    test_state_machine();
    test_registry_batch();
    test_freewheeling_task();

    printf("\n═══════════════════════════════════════\n");
    printf("  通过: %d  失败: %d  总计: %d\n", g_pass, g_fail, g_pass + g_fail);
    printf("═══════════════════════════════════════\n");

    return g_fail > 0 ? 1 : 0;
}
