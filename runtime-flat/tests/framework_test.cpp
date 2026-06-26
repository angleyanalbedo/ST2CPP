/**
 * framework_test.cpp — RT Runtime 框架完整性测试
 *
 * 验证 rt_plc.h + rt_runtime.h 的全部框架功能：
 *  1.  TIME 运算与比较
 *  2.  DATE / TIME_OF_DAY / DATE_AND_TIME 类型
 *  3.  位操作（AND/OR/XOR/SHL/SHR）
 *  4.  I/O 地址宏与位访问
 *  5.  错误处理（除零/越界/MOD零）
 *  6.  PROGRAM 实例生命周期（Init→Pre→FirstScan→Cyclic→Post）
 *  7.  分阶段扫描周期（ReadInputs→LogicSolve→WriteOutputs→Housekeeping）
 *  8.  冷启动 / 暖启动（RETAIN 区域保留）
 *  9.  扩展字符串操作（LEFT/RIGHT/MID/INSERT/DELETE/REPLACE/FIND）
 * 10.  互斥锁
 * 11.  系统状态机（STOP→STARTING→RUN→STOPPING→STOP, ERROR 恢复）
 * 12.  诊断统计
 * 13.  GVL 偏移量越界检查
 * 14.  ProcessImage 偏移量越界检查
 * 15.  Task 参数校验
 * 16.  错误操作数记录
 */
#include "rt_runtime.h"
#include <cstdio>
#include <cassert>

using namespace rt_plc;

// ─── 测试计数器 ───
static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

// ═══════════════════════════════════════════════════════
// 测试 PROGRAM 实例（模拟编译器生成）
// ═══════════════════════════════════════════════════════

// GVL 偏移定义
static constexpr size_t OFF_COUNTER   = 0;    // DINT
static constexpr size_t OFF_RETAIN_V  = 4;    // DINT (RETAIN)
static constexpr size_t OFF_SENSOR    = 8;    // REAL
static constexpr size_t OFF_ALARM     = 12;   // BOOL
static constexpr size_t OFF_MSG       = 16;   // STRING (256 bytes)

// 标记 RETAIN 区域：[4, 8) 即 OFF_RETAIN_V
static constexpr size_t RETAIN_START = 4;
static constexpr size_t RETAIN_END   = 8;

// PROGRAM MainProg 的全局状态追踪
static int init_called  = 0;
static int pre_called   = 0;
static int cyclic_calls = 0;
static int post_called  = 0;

void MainProg_init(GVL& gvl, ProcessImage& io) {
    init_called++;
    gvl.write<DINT>(OFF_COUNTER, 0);
}

void MainProg_pre(GVL& gvl, ProcessImage& io) {
    pre_called++;
}

void MainProg_cyclic(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    cyclic_calls++;
    DINT cnt = gvl.read<DINT>(OFF_COUNTER);
    gvl.write<DINT>(OFF_COUNTER, cnt + 1);

    // 也写 RETAIN 变量
    DINT rv = gvl.read<DINT>(OFF_RETAIN_V);
    gvl.write<DINT>(OFF_RETAIN_V, rv + 10);
}

void MainProg_post(GVL& gvl, ProcessImage& io) {
    post_called++;
}


// ═══════════════════════════════════════════════════════
// SimTCI：模拟 I/O 驱动
// ═══════════════════════════════════════════════════════

struct SimTCI : public TCI {
    int syncInCount  = 0;
    int syncOutCount = 0;

    void syncInputs(ProcessImage& img) override {
        syncInCount++;
        // 模拟传感器输入
        REAL val = 50.0f + (syncInCount % 10) * 0.5f;
        memcpy(img.inputs + PLC_IW(0), &val, sizeof(REAL));
        // 模拟数字输入位
        img.inputs[PLC_IB(10)] = (syncInCount % 2 == 0) ? 0xFF : 0x00;
    }

    void syncOutputs(ProcessImage& img) override {
        syncOutCount++;
    }
};


// ═══════════════════════════════════════════════════════
// 测试函数
// ═══════════════════════════════════════════════════════

void test_time_arithmetic() {
    printf("\n--- 1. TIME 运算与比较 ---\n");

    TEST("TIME 构造");
    TIME t1 = T_ms(100);
    TIME t2 = T_s(1);
    TIME t3 = T_min(1);
    TIME t4 = T_h(1);
    CHECK(t1 == 100000 && t2 == 1000000 && t3 == 60000000 && t4 == 3600000000LL, "构造值错误");

    TEST("TIME 加减");
    TIME sum = t1 + t2;
    TIME diff = t2 - t1;
    CHECK(sum == 1100000 && diff == 900000, "加减结果错误");

    TEST("TIME 比较");
    CHECK(T_lt(t1, t2) && T_gt(t2, t1) && T_eq(t1, t1) && T_ne(t1, t2), "比较错误");

    TEST("TIME 负差值");
    TIME neg = t1 - t2;
    CHECK(neg < 0, "负差值应小于0");
}


void test_datetime() {
    printf("\n--- 2. DATE / TOD / DT 类型 ---\n");

    TEST("DATE 构造");
    DATE d = DATE_make(2025, 1, 15);
    CHECK(d > 0, "DATE 应为正数");

    TEST("TOD 构造");
    TIME_OF_DAY tod = TOD_make(14, 30, 0);
    CHECK(tod == 14 * 3600000000LL + 30 * 60000000LL, "TOD 值错误");

    TEST("DT 构造与分解");
    DATE_AND_TIME dt = DT_make(2025, 1, 15, 14, 30, 0);
    DATE d2 = DT_to_date(dt);
    TIME_OF_DAY tod2 = DT_to_tod(dt);
    CHECK(d2 == d && tod2 == tod, "分解不一致");

    TEST("NOW() 返回合理值");
    DATE_AND_TIME now = NOW();
    CHECK(now > 0, "NOW() 应为正数");

    TEST("T_h / T_d 辅助构造");
    CHECK(T_h(2) == 7200000000LL && T_d(1) == 86400000000LL, "T_h/T_d 错误");
}


void test_bit_operations() {
    printf("\n--- 3. 位操作 ---\n");

    TEST("AND / OR / XOR / NOT");
    uint16_t a = 0xFF00, b = 0x0F0F;
    CHECK(plc_and(a, b) == 0x0F00, "AND 错误");
    CHECK(plc_or(a, b) == 0xFF0F, "OR 错误");
    CHECK(plc_xor(a, b) == 0xF00F, "XOR 错误");

    TEST("SHL / SHR");
    CHECK(SHL<uint16_t>(1, 4) == 16, "SHL 错误");
    CHECK(SHR<uint16_t>(16, 4) == 1, "SHR 错误");
}


void test_io_address() {
    printf("\n--- 4. I/O 地址宏与位访问 ---\n");

    ProcessImage img;
    img.clearInputs();
    img.clearOutputs();

    TEST("字节偏移宏");
    CHECK(PLC_IB(0) == 0 && PLC_IW(4) == 4 && PLC_ID(8) == 8, "偏移宏错误");

    TEST("位读写");
    img.writeOutputBit(PLC_QX(0, 3), TRUE);
    CHECK(img.outputs[0] == 0x08, "位写入错误");

    img.writeOutputBit(PLC_QX(0, 3), FALSE);
    CHECK(img.outputs[0] == 0x00, "位清除错误");

    TEST("位读取");
    img.inputs[5] = 0x20;  // bit 5 = 1
    BOOL bit5 = img.readInputBit(5, 5);
    BOOL bit4 = img.readInputBit(5, 4);
    CHECK(bit5 == TRUE && bit4 == FALSE, "位读取错误");

    TEST("类型化读写");
    REAL sensor = 42.5f;
    img.writeOutput(PLC_QW(10), sensor);
    REAL readback = img.readOutput<REAL>(PLC_QW(10));
    CHECK(readback == 42.5f, "类型化读写错误");
}


void test_error_handling() {
    printf("\n--- 5. 错误处理 ---\n");

    ErrorManager em;

    TEST("安全除法 (正常)");
    DINT r1 = em.safeDiv((DINT)100, (DINT)5, 0u, 0u, 0);
    CHECK(r1 == 20, "100/5 应为 20");

    TEST("安全除法 (除零)");
    DINT r2 = em.safeDiv((DINT)100, (DINT)0, 1u, 42u, 1000);
    CHECK(r2 == 0 && em.lastError == ErrorCode::DIV_BY_ZERO, "除零应返回0并记录错误");

    TEST("安全 MOD (正常)");
    DINT r3 = em.safeMod((DINT)17, (DINT)5, 0u, 0u, 0);
    CHECK(r3 == 2, "17 MOD 5 应为 2");

    TEST("安全 MOD (除零)");
    DINT r4 = em.safeMod((DINT)17, (DINT)0, 1u, 10u, 0);
    CHECK(r4 == 0 && em.totalCount == 2, "MOD零应记录错误");

    TEST("浮点安全除法");
    REAL rf = em.safeDiv(3.14f, 0.0f, 0u, 0u, 0);
    CHECK(rf == 0.0f && em.totalCount == 3, "浮点除零错误");

    TEST("致命错误标记");
    CHECK(em.fatalMode == true, "除零应为致命错误");

    TEST("错误日志环形缓冲");
    em.reset();
    for (int i = 0; i < 50; i++) {
        em.report(ErrorCode::USER_ERROR, 0, i, "test", i);
    }
    CHECK(em.totalCount == 50, "总数应为50");
}


void test_string_ops() {
    printf("\n--- 9. 扩展字符串操作 ---\n");

    TEST("CONCAT");
    STRING s1("Hello");
    STRING s2(" World");
    STRING s3 = CONCAT(s1, s2);
    CHECK(strcmp(s3.data, "Hello World") == 0, "拼接错误");

    TEST("LEN");
    CHECK(LEN(s3) == 11, "长度应为11");

    TEST("LEFT");
    STRING l = LEFT(s3, 5);
    CHECK(strcmp(l.data, "Hello") == 0, "LEFT 错误");

    TEST("RIGHT");
    STRING r = RIGHT(s3, 5);
    CHECK(strcmp(r.data, "World") == 0, "RIGHT 错误");

    TEST("MID");
    STRING m = MID(s3, 3, 7);  // 从位置7取3字符 → "Wor"
    CHECK(strcmp(m.data, "Wor") == 0, "MID 错误");

    TEST("INSERT");
    STRING ins("Beautiful ");
    STRING inserted = INSERT(s3, ins, 6);  // 0-based: 拷贝前6字符后插入
    CHECK(strcmp(inserted.data, "Hello Beautiful World") == 0, "INSERT 错误");

    TEST("DELETE");
    STRING del = DELETE(s3, 6, 6);  // 从位置6删6字符
    CHECK(strcmp(del.data, "Hello") == 0, "DELETE 错误");

    TEST("REPLACE");
    STRING rep("Earth");
    STRING replaced = REPLACE(s3, rep, 5, 7);  // 位置7替换5字符
    CHECK(strcmp(replaced.data, "Hello Earth") == 0, "REPLACE 错误");

    TEST("FIND");
    DINT pos = FIND(s3, "World");
    CHECK(pos == 7, "FIND 应返回7");

    TEST("FIND 未找到");
    DINT pos2 = FIND(s3, "xyz");
    CHECK(pos2 == 0, "未找到应返回0");

    TEST("字符串比较");
    STRING a("abc"), b("abd");
    CHECK(STR_LT(a, b) && STR_GT(b, a) && STR_EQ(a, a) && STR_NE(a, b), "比较错误");
}


void test_mutex() {
    printf("\n--- 10. 互斥锁 ---\n");

    TEST("基本加锁解锁");
    plc_lock lk;
    lk.lock();
    CHECK(lk.tryLock() == false, "已锁定时 tryLock 应为 false");
    lk.unlock();
    CHECK(lk.tryLock() == true, "解锁后 tryLock 应为 true");
    lk.unlock();

    TEST("RAII guard");
    {
        plc_lock_guard g(lk);
        CHECK(lk.tryLock() == false, "guard 持锁时 tryLock 应为 false");
    }
    CHECK(lk.tryLock() == true, "guard 释放后应可获取");
    lk.unlock();
}


void test_program_lifecycle() {
    printf("\n--- 6. PROGRAM 实例生命周期 ---\n");

    init_called = pre_called = cyclic_calls = post_called = 0;

    // 直接测试 ProgramInstance 生命周期（绕过调度器计时）
    ProgramInstance prog;
    strncpy(prog.name, "TestProg", sizeof(prog.name));
    prog.initFunc   = MainProg_init;
    prog.cyclicFunc = MainProg_cyclic;
    prog.preFunc    = MainProg_pre;
    prog.postFunc   = MainProg_post;

    GVL gvl;
    ProcessImage io;
    gvl.clear();
    io.clearInputs();
    io.clearOutputs();

    TEST("初始状态 UNINIT");
    CHECK(prog.phase == ProgramPhase::UNINIT, "应为 UNINIT");

    prog.doInit(gvl, io);
    TEST("Init 被调用");
    CHECK(init_called == 1, "应调用 init");

    TEST("状态变为 INIT");
    CHECK(prog.phase == ProgramPhase::INIT, "应为 INIT");

    prog.doPre(gvl, io);
    TEST("Pre 被调用");
    CHECK(pre_called == 1, "应调用 pre");

    TEST("状态变为 FIRST_SCAN");
    CHECK(prog.phase == ProgramPhase::FIRST_SCAN, "应为 FIRST_SCAN");

    // 执行 3 次 cyclic
    prog.doCyclic(gvl, io, T_ms(10));
    prog.doCyclic(gvl, io, T_ms(10));
    prog.doCyclic(gvl, io, T_ms(10));

    TEST("Cyclic 被调用 3 次");
    CHECK(cyclic_calls == 3, "应执行3次 cyclic");

    TEST("首次 cyclic 后进入 CYCLIC");
    CHECK(prog.phase == ProgramPhase::CYCLIC, "应为 CYCLIC");

    TEST("cycleCount = 3");
    CHECK(prog.cycleCount == 3, "cycleCount 应为 3");

    TEST("GVL counter = 3");
    DINT counter = gvl.read<DINT>(OFF_COUNTER);
    CHECK(counter == 3, "counter 应为 3");

    prog.doPost(gvl, io);
    TEST("Post 被调用");
    CHECK(post_called == 1, "应调用 post");

    TEST("状态变为 STOPPED");
    CHECK(prog.phase == ProgramPhase::STOPPED, "应为 STOPPED");
}


void test_scan_phases() {
    printf("\n--- 7. 分阶段扫描周期 ---\n");

    // 测试 ScanPhase 枚举值完整性
    TEST("ScanPhase 枚举完整");
    CHECK((int)ScanPhase::IDLE == 0 &&
          (int)ScanPhase::READ_INPUTS == 1 &&
          (int)ScanPhase::LOGIC_SOLVE == 2 &&
          (int)ScanPhase::WRITE_OUTPUTS == 3 &&
          (int)ScanPhase::HOUSEKEEPING == 4,
          "枚举值应完整");

    // 使用 Runtime::scanOnce 验证基本扫描流程（不依赖计时）
    Runtime rt;
    SimTCI tci;
    rt.setTCI(&tci);
    rt.setCycleTime(1000);

    static int pouCalls = 0;
    pouCalls = 0;
    int cycles = 0;
    rt.run([&]() {
        pouCalls++;
        cycles++;
        if (cycles >= 3) rt.stop();
    });

    TEST("scanOnce 执行 3 次");
    CHECK(pouCalls == 3, "应执行 3 次");

    TEST("I/O 同步被调用");
    CHECK(tci.syncInCount >= 3 && tci.syncOutCount >= 3, "syncIn/syncOut 应各 >= 3");

    TEST("cycleCount 更新");
    CHECK(rt.cycleCount >= 3, "cycleCount 应 >= 3");

    // 验证 Scheduler 的 currentPhase 字段存在且初始为 IDLE
    Scheduler sched;
    TEST("Scheduler currentPhase 初始 IDLE");
    CHECK(sched.currentPhase == ScanPhase::IDLE, "应为 IDLE");

    // 验证 DiagStats
    DiagStats stats;
    stats.recordScan(100);
    stats.recordScan(200);
    stats.recordScan(300);
    TEST("DiagStats 统计正确");
    CHECK(stats.totalScanCount == 3 && stats.minScanTime == 100 &&
          stats.maxScanTime == 300 && stats.avgScanTime() == 200, "统计值错误");
}


void test_cold_warm_start() {
    printf("\n--- 8. 冷启动 / 暖启动 ---\n");

    GVL gvl;
    gvl.setRetainRegion(RETAIN_START, RETAIN_END);
    ProcessImage io;

    // 模拟冷启动：全部清零
    gvl.clear();
    TEST("冷启动后 GVL 全零");
    CHECK(gvl.read<DINT>(OFF_COUNTER) == 0 && gvl.read<DINT>(OFF_RETAIN_V) == 0,
          "冷启动后应全零");

    // 模拟 3 个扫描周期（直接写 GVL 模拟 PROGRAM 执行）
    for (int i = 0; i < 3; i++) {
        DINT cnt = gvl.read<DINT>(OFF_COUNTER);
        gvl.write<DINT>(OFF_COUNTER, cnt + 1);
        DINT rv = gvl.read<DINT>(OFF_RETAIN_V);
        gvl.write<DINT>(OFF_RETAIN_V, rv + 10);
    }

    TEST("运行后 counter=3, retain_v=30");
    CHECK(gvl.read<DINT>(OFF_COUNTER) == 3, "counter 应为 3");
    CHECK(gvl.read<DINT>(OFF_RETAIN_V) == 30, "retain_v 应为 30");

    // 暖启动：RETAIN 区域保留，非 RETAIN 清零
    gvl.clearNonRetain();
    TEST("暖启动后非 RETAIN 变量清零");
    CHECK(gvl.read<DINT>(OFF_COUNTER) == 0, "counter 应清零为 0");

    TEST("暖启动后 RETAIN 变量保留");
    CHECK(gvl.read<DINT>(OFF_RETAIN_V) == 30, "retain_v 应保留为 30");

    // 暖启动后继续运行
    DINT rv = gvl.read<DINT>(OFF_RETAIN_V);
    gvl.write<DINT>(OFF_RETAIN_V, rv + 10);
    TEST("暖启动后 RETAIN 继续累加");
    CHECK(gvl.read<DINT>(OFF_RETAIN_V) == 40, "retain_v 应递增到 40");

    // 冷启动：全部清零
    gvl.clear();
    TEST("再次冷启动后 RETAIN 也清零");
    CHECK(gvl.read<DINT>(OFF_RETAIN_V) == 0, "retain_v 应清零");

    // 测试 Scheduler 集成的 start(mode)
    Scheduler sched;
    SimTCI tci;
    sched.setTCI(&tci);
    sched.setRetainRegion(RETAIN_START, RETAIN_END);

    init_called = 0;
    int pIdx = sched.addProgram("RetainProg",
                                MainProg_init, MainProg_cyclic,
                                nullptr, nullptr);
    CHECK(pIdx >= 0, "addProgram 应成功");

    sched.start(StartupMode::COLD);
    TEST("Scheduler.start(COLD) 调用 init");
    CHECK(init_called == 1, "应调用 init");

    TEST("Scheduler.start(COLD) 进入 RUN");
    CHECK(sched.systemState == SystemState::RUN, "应为 RUN");

    sched.stop();
    TEST("Scheduler.stop() 进入 STOP");
    CHECK(sched.systemState == SystemState::STOP, "应为 STOP");
}


void test_system_state_machine() {
    printf("\n--- 11. 系统状态机 ---\n");

    Scheduler sched;
    SimTCI tci;
    sched.setTCI(&tci);

    int pIdx = sched.addProgram("SM_Prog",
                                MainProg_init, MainProg_cyclic,
                                nullptr, nullptr);
    int tIdx = sched.addCyclicTask("SM_Task", 1, T_ms(10));
    sched.addProgramToTask(tIdx, pIdx);

    TEST("初始状态 STOP");
    CHECK(sched.systemState == SystemState::STOP, "应为 STOP");

    sched.start(StartupMode::COLD);
    TEST("start() → RUN");
    CHECK(sched.systemState == SystemState::RUN, "应为 RUN");

    sched.pause();
    TEST("pause() → PAUSED");
    CHECK(sched.systemState == SystemState::PAUSED, "应为 PAUSED");

    sched.resume();
    TEST("resume() → RUN");
    CHECK(sched.systemState == SystemState::RUN, "应为 RUN");

    sched.error();
    TEST("error() → ERROR");
    CHECK(sched.systemState == SystemState::ERROR, "应为 ERROR");

    sched.resetError();
    TEST("resetError() → STOP");
    CHECK(sched.systemState == SystemState::STOP, "应为 STOP");

    // 再次启动
    init_called = 0;
    sched.start(StartupMode::COLD);
    sched.tick();
    sched.stop();
    TEST("STOP → RUN → STOP 完整流程");
    CHECK(sched.systemState == SystemState::STOP, "应为 STOP");
}


void test_watchdog_and_diag() {
    printf("\n--- 12. 看门狗与诊断 ---\n");

    Scheduler sched;
    SimTCI tci;
    sched.setTCI(&tci);
    sched.watchdog.setDefault(T_us(1));  // 极短的看门狗（1us）

    // 一个会"耗时"的 POU（大量运算确保超过 1us）
    POUFunc slowPOU = [](GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
        volatile int sum = 0;
        for (int i = 0; i < 1000000; i++) sum += i;
    };

    int tIdx = sched.addCyclicTask("SlowTask", 1, T_us(1));
    sched.addPOU(tIdx, slowPOU);

    sched.start(StartupMode::COLD);

    // 多次 tick 让看门狗触发
    for (int i = 0; i < 20; i++) {
        if (sched.systemState != SystemState::RUN) break;
        sched.tick();
    }

    TEST("看门狗触发");
    CHECK(sched.watchdog.tripped || sched.diag.totalOverruns > 0,
          "应检测到超时");

    sched.stop();

    TEST("诊断报告");
    sched.printDiag();
    CHECK(sched.diag.totalScanCount > 0, "扫描计数应 > 0");
}


void test_error_integration() {
    printf("\n--- 错误处理集成测试 ---\n");

    Scheduler sched;
    SimTCI tci;
    sched.setTCI(&tci);

    // 一个会触发除零的 POU
    POUFunc divZeroPOU = [](GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
        // 模拟编译器生成的安全除法调用
        // 这里直接用 runtime 的 errorMgr
        // 注意：实际代码中 errorMgr 需要从 Scheduler 传入
        // 这里简化为直接测试
    };

    int tIdx = sched.addCyclicTask("ErrTask", 1, T_us(1));
    sched.addPOU(tIdx, divZeroPOU);

    // 先启动（start 会 reset errorMgr）
    sched.start(StartupMode::COLD);

    // 然后手动报告致命错误
    sched.errorMgr.report(ErrorCode::DIV_BY_ZERO, 1, 42, "a / b", 100);
    TEST("错误记录到 ErrorManager");
    CHECK(sched.errorMgr.totalCount == 1, "应有1条错误");

    TEST("致命错误 → fatalMode");
    CHECK(sched.errorMgr.fatalMode == true, "除零应为致命");

    // tick 检测到 fatalMode → 进入 ERROR
    sched.tick();

    TEST("fatalMode 导致系统进入 ERROR");
    CHECK(sched.systemState == SystemState::ERROR, "应为 ERROR");

    sched.resetError();
    TEST("resetError 清除错误状态");
    CHECK(sched.errorMgr.totalCount == 0, "应清零");
}


void test_gvl_bounds() {
    printf("\n--- 13. GVL 偏移量越界检查 ---\n");

    GVL gvl;
    gvl.clear();

    TEST("GVL 正常读写");
    gvl.write<DINT>(0, 42);
    DINT v = gvl.read<DINT>(0);
    CHECK(v == 42, "正常读写应正确");

    TEST("GVL read 越界返回默认值");
    DINT outOfBounds = gvl.read<DINT>(GVL_SIZE - 1);
    CHECK(outOfBounds == 0, "越界读应返回 0");

    TEST("GVL write 越界不崩溃");
    gvl.write<DINT>(GVL_SIZE - 1, 999);
    DINT afterOOB = gvl.read<DINT>(0);
    CHECK(afterOOB == 42, "越界写不应影响已有数据");

    TEST("GVL ptr 越界返回 nullptr");
    DINT* p = gvl.ptr<DINT>(GVL_SIZE - 1);
    CHECK(p == nullptr, "越界 ptr 应返回 nullptr");

    TEST("GVL ptr 正常返回有效指针");
    DINT* pOk = gvl.ptr<DINT>(0);
    CHECK(pOk != nullptr, "正常 ptr 应非空");

    TEST("GVL 边界精确检查 (offset + sizeof(T) == GVL_SIZE)");
    // offset=65532, sizeof(DINT)=4, total=65536 = GVL_SIZE → 应允许
    gvl.write<DINT>(GVL_SIZE - sizeof(DINT), 123);
    DINT edgeVal = gvl.read<DINT>(GVL_SIZE - sizeof(DINT));
    CHECK(edgeVal == 123, "恰好在边界应允许");
}


void test_process_image_bounds() {
    printf("\n--- 14. ProcessImage 偏移量越界检查 ---\n");

    ProcessImage img;
    img.clearInputs();
    img.clearOutputs();

    TEST("ProcessImage 正常读写");
    img.writeOutput<INT>(10, 5678);
    INT readback = img.readOutput<INT>(10);
    CHECK(readback == 5678, "正常读写应正确");

    TEST("ProcessImage readInput 越界返回默认值");
    INT oobIn = img.readInput<INT>(PROCESS_IMAGE_SIZE - 1);
    CHECK(oobIn == 0, "越界读应返回 0");

    TEST("ProcessImage writeOutput 越界不崩溃");
    img.writeOutput<INT>(PROCESS_IMAGE_SIZE - 1, 9999);
    INT afterOOB = img.readOutput<INT>(10);
    CHECK(afterOOB == 5678, "越界写不应影响已有数据");

    TEST("ProcessImage readInputBit 越界返回 FALSE");
    BOOL bitOOB = img.readInputBit(PROCESS_IMAGE_SIZE, 0);
    CHECK(bitOOB == FALSE, "越界位读应返回 FALSE");

    TEST("ProcessImage readInputBit 位偏移越界");
    BOOL bitOOB2 = img.readInputBit(0, 8);
    CHECK(bitOOB2 == FALSE, "位偏移>7应返回 FALSE");

    TEST("ProcessImage writeOutputBit 越界不崩溃");
    img.writeOutputBit(PROCESS_IMAGE_SIZE, 0, TRUE);
    CHECK(img.outputs[0] == 0x00, "越界位写不应影响数据");

    TEST("ProcessImage 位读写正常");
    img.writeOutputBit(PLC_QX(5, 3), TRUE);
    BOOL b = img.readInputBit(5, 3);
    CHECK(b == FALSE, "输入位应为初始值");
    CHECK((img.outputs[5] & (1 << 3)) != 0, "输出位应已设置");

    // ─── writeInput / writeInputBit / readOutputBit 新增测试 ───

    TEST("writeInput 正常写入 + readInput 回读");
    img.writeInput<DINT>(20, 12345);
    DINT inVal = img.readInput<DINT>(20);
    CHECK(inVal == 12345, "writeInput/readInput 应配对");

    TEST("writeInput 越界不崩溃");
    img.writeInput<INT>(PROCESS_IMAGE_SIZE - 1, 999);
    DINT inAfterOOB = img.readInput<DINT>(20);
    CHECK(inAfterOOB == 12345, "越界写不应影响已有数据");

    TEST("writeInputBit 正常写入 + readInputBit 回读");
    img.writeInputBit(10, 5, TRUE);
    BOOL inBit = img.readInputBit(10, 5);
    CHECK(inBit == TRUE, "writeInputBit/readInputBit 应配对");
    img.writeInputBit(10, 5, FALSE);
    inBit = img.readInputBit(10, 5);
    CHECK(inBit == FALSE, "writeInputBit(FALSE) 应清除位");

    TEST("writeInputBit 越界不崩溃");
    img.writeInputBit(PROCESS_IMAGE_SIZE, 0, TRUE);
    CHECK(img.inputs[0] == 0x00, "越界位写不应影响数据");

    TEST("readOutputBit 正常读取");
    img.writeOutputBit(7, 2, TRUE);
    BOOL outBit = img.readOutputBit(7, 2);
    CHECK(outBit == TRUE, "readOutputBit 应返回 TRUE");
    outBit = img.readOutputBit(7, 3);
    CHECK(outBit == FALSE, "未设置的位应返回 FALSE");

    TEST("readOutputBit 越界返回 FALSE");
    BOOL outBitOOB = img.readOutputBit(PROCESS_IMAGE_SIZE, 0);
    CHECK(outBitOOB == FALSE, "越界位读应返回 FALSE");
    BOOL outBitOOB2 = img.readOutputBit(0, 8);
    CHECK(outBitOOB2 == FALSE, "位偏移>7应返回 FALSE");

    TEST("PI_WRITE_INPUT / PI_READ_INPUT 宏正常");
    PI_WRITE_INPUT(img, REAL, 100, 3.14f);
    REAL macroVal = PI_READ_INPUT(img, REAL, 100);
    CHECK(macroVal > 3.13f && macroVal < 3.15f, "宏读写应配对");

    TEST("PI_WRITE_INPUT_BIT / PI_READ_INPUT_BIT 宏正常");
    PI_WRITE_INPUT_BIT(img, 15, 4, TRUE);
    BOOL macroBit = PI_READ_INPUT_BIT(img, 15, 4);
    CHECK(macroBit == TRUE, "宏位读写应配对");

    TEST("PI_READ_OUTPUT_BIT / PI_WRITE_OUTPUT_BIT 宏正常");
    PI_WRITE_OUTPUT_BIT(img, 20, 6, TRUE);
    BOOL macroOutBit = PI_READ_OUTPUT_BIT(img, 20, 6);
    CHECK(macroOutBit == TRUE, "宏输出位读写应配对");
}


void test_task_parameter_validation() {
    printf("\n--- 15. Task 参数校验 ---\n");

    Scheduler sched;

    TEST("addCyclicTask priority 超上限 clamp");
    int t1 = sched.addCyclicTask("HighPri", 100, T_ms(10));
    CHECK(t1 >= 0, "应成功创建");
    CHECK(sched.task(t1).priority == MAX_PRIORITY, "priority 应被 clamp 到 MAX");

    TEST("addCyclicTask priority 低于下限 clamp");
    int t2 = sched.addCyclicTask("LowPri", -5, T_ms(10));
    CHECK(t2 >= 0, "应成功创建");
    CHECK(sched.task(t2).priority == MIN_PRIORITY, "priority 应被 clamp 到 MIN");

    TEST("addCyclicTask interval 为 0 使用默认值");
    int t3 = sched.addCyclicTask("ZeroInt", 1, 0);
    CHECK(t3 >= 0, "应成功创建");
    CHECK(sched.task(t3).interval == sched.baseCycleTime, "interval 应为 baseCycleTime");

    TEST("addCyclicTask name 为 nullptr 不崩溃");
    int t4 = sched.addCyclicTask(nullptr, 1, T_ms(5));
    CHECK(t4 >= 0, "应成功创建");
    CHECK(strlen(sched.task(t4).name) > 0, "name 应有默认值");

    TEST("addFreewheelingTask priority clamp");
    int t5 = sched.addFreewheelingTask("FWHigh", 50);
    CHECK(t5 >= 0 && sched.task(t5).priority == MAX_PRIORITY, "priority 应 clamp");

    TEST("addEventTask priority clamp");
    int t6 = sched.addEventTask("EvtHigh", -10, nullptr);
    CHECK(t6 >= 0 && sched.task(t6).priority == MIN_PRIORITY, "priority 应 clamp");
}


void test_error_operand_recording() {
    printf("\n--- 16. 错误操作数记录 ---\n");

    ErrorManager em;

    TEST("safeDiv 记录操作数");
    em.safeDiv((DINT)100, (DINT)0, 1u, 10u, 1000);
    CHECK(em.log[0].operandA == 100 && em.log[0].operandB == 0,
          "应记录 a=100, b=0");

    TEST("safeMod 记录操作数");
    em.safeMod((DINT)42, (DINT)0, 2u, 20u, 2000);
    CHECK(em.log[1].operandA == 42 && em.log[1].operandB == 0,
          "MOD 应记录操作数");

    TEST("safeAdd 溢出检测");
    DINT maxVal = 2147483647;  // DINT max
    DINT result = em.safeAdd(maxVal, (DINT)1, 3u, 30u, 3000);
    CHECK(em.lastError == ErrorCode::INT_OVERFLOW, "应检测到溢出");

    TEST("safeSub 溢出检测");
    DINT minVal = (-2147483647 - 1);  // DINT min
    DINT result2 = em.safeSub(minVal, (DINT)1, 4u, 40u, 4000);
    CHECK(em.lastError == ErrorCode::INT_OVERFLOW, "应检测到下溢");

    TEST("safeMul 溢出检测");
    DINT big = 100000;
    DINT result3 = em.safeMul(big, big, 5u, 50u, 5000);
    CHECK(em.lastError == ErrorCode::INT_OVERFLOW, "应检测到乘法溢出");

    TEST("正常运算不触发错误");
    em.reset();
    DINT ok = em.safeAdd((DINT)10, (DINT)20, 0u, 0u, 0);
    CHECK(ok == 30 && em.totalCount == 0, "正常加法不应记录错误");
}


// ═══════════════════════════════════════════════════════
// main
// ═══════════════════════════════════════════════════════

int main() {
    printf("=== RT Runtime 框架完整性测试 ===\n");

    test_time_arithmetic();
    test_datetime();
    test_bit_operations();
    test_io_address();
    test_error_handling();
    test_string_ops();
    test_mutex();
    test_program_lifecycle();
    test_scan_phases();
    test_cold_warm_start();
    test_system_state_machine();
    test_watchdog_and_diag();
    test_error_integration();
    test_gvl_bounds();
    test_process_image_bounds();
    test_task_parameter_validation();
    test_error_operand_recording();

    printf("\n═══════════════════════════════\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    printf("═══════════════════════════════\n");

    if (test_fail == 0) {
        printf("全部通过!\n");
    } else {
        printf("有 %d 项失败，请检查!\n", test_fail);
    }

    return test_fail > 0 ? 1 : 0;
}
