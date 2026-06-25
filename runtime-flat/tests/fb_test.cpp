#include "rt_plc.h"
#include <cstdio>

using namespace rt_plc;

static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

void test_ton() {
    printf("\n─── 1. TON 接通延时 ───\n");

    TON t;

    TEST("TON initial state");
    CHECK(t.output == FALSE, "initial output should be FALSE");
    CHECK(t.elapsed == 0, "initial elapsed should be 0");

    TEST("TON input=TRUE starts timing");
    t.update(TRUE, T_ms(100), T_ms(1));
    CHECK(t.output == FALSE, "output should be FALSE before PT");
    CHECK(t.elapsed > 0, "elapsed should accumulate");

    TEST("TON reaches PT sets output");
    for (int i = 0; i < 100; i++) t.update(TRUE, T_ms(100), T_ms(1));
    CHECK(t.output == TRUE, "output should be TRUE after PT");
    CHECK(t.elapsed == T_ms(100), "elapsed should equal PT");

    TEST("TON input=FALSE resets");
    t.update(FALSE, T_ms(100), T_ms(1));
    CHECK(t.output == FALSE, "output should be FALSE");
    CHECK(t.elapsed == 0, "elapsed should reset to 0");
}

void test_tof() {
    printf("\n─── 2. TOF 断开延时 ───\n");

    TOF t;

    TEST("TOF input=TRUE sets output immediately");
    t.update(TRUE, T_ms(100), T_ms(1));
    CHECK(t.output == TRUE, "output should be TRUE");

    TEST("TOF input=FALSE starts timing");
    t.update(FALSE, T_ms(100), T_ms(1));
    CHECK(t.output == TRUE, "output should remain TRUE before PT");

    TEST("TOF reaches PT clears output");
    for (int i = 0; i < 100; i++) t.update(FALSE, T_ms(100), T_ms(1));
    CHECK(t.output == FALSE, "output should be FALSE after PT");
}

void test_tp() {
    printf("\n─── 3. TP 脉冲定时器 ───\n");

    TP t;

    TEST("TP rising edge triggers pulse");
    t.update(TRUE, T_ms(50), T_ms(1));
    CHECK(t.output == TRUE, "output should be TRUE on rising edge");

    TEST("TP pulse ends after PT");
    for (int i = 0; i < 50; i++) t.update(TRUE, T_ms(50), T_ms(1));
    CHECK(t.output == FALSE, "output should be FALSE after PT");

    TEST("TP no re-trigger during pulse");
    t.update(TRUE, T_ms(50), T_ms(1));
    CHECK(t.output == TRUE, "first edge");
    t.update(FALSE, T_ms(50), T_ms(1));
    t.update(TRUE, T_ms(50), T_ms(1));
    CHECK(t.output == TRUE, "output should still be TRUE during pulse");
}

void test_ctu() {
    printf("\n─── 4. CTU 递增计数器 ───\n");

    CTU c;

    TEST("CTU initial count is 0");
    CHECK(c.count == 0, "initial count!=0");

    TEST("CTU counts rising edges");
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    CHECK(c.count == 2, "count should be 2");

    TEST("CTU output when count >= PV");
    c.update(TRUE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    CHECK(c.count == 5, "count should be 5");
    CHECK(c.output == TRUE, "output should be TRUE");

    TEST("CTU RESET clears count");
    c.update(FALSE, TRUE, 5);
    CHECK(c.count == 0, "count should be 0 after RESET");
    CHECK(c.output == FALSE, "output should be FALSE after RESET");
}

void test_ctd() {
    printf("\n─── 5. CTD 递减计数器 ───\n");

    CTD c;

    TEST("CTD LOAD sets count to PV");
    c.update(FALSE, TRUE, 5);
    CHECK(c.count == 5, "count should be 5 after LOAD");

    TEST("CTD counts down on falling edges");
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    CHECK(c.count == 3, "count should be 3");

    TEST("CTD output when count <= 0");
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    c.update(TRUE, FALSE, 5);
    c.update(FALSE, FALSE, 5);
    CHECK(c.count == 0, "count should be 0");
    CHECK(c.output == TRUE, "output should be TRUE");
}

void test_ctud() {
    printf("\n─── 6. CTUD 双向计数器 ───\n");

    CTUD c;

    TEST("CTUD initial state");
    CHECK(c.count == 0, "initial count!=0");
    CHECK(c.QU == FALSE, "QU should be FALSE");
    CHECK(c.QD == FALSE, "QD should be FALSE");

    TEST("CTUD counts up then down");
    c.update(TRUE, FALSE, FALSE, FALSE, 3);
    c.update(TRUE, FALSE, FALSE, FALSE, 3);
    c.update(FALSE, TRUE, FALSE, FALSE, 3);
    CHECK(c.count == 1, "count should be 1");

    TEST("CTUD RESET clears");
    c.update(FALSE, FALSE, TRUE, FALSE, 3);
    CHECK(c.count == 0, "count should be 0 after RESET");

    TEST("CTUD LOAD sets PV");
    c.update(FALSE, FALSE, FALSE, TRUE, 5);
    CHECK(c.count == 5, "count should be 5 after LOAD");
}

void test_r_trig() {
    printf("\n─── 7. R_TRIG 上升沿检测 ───\n");

    R_TRIG rt;

    TEST("R_TRIG no edge on FALSE");
    CHECK(rt(FALSE) == FALSE, "no edge");

    TEST("R_TRIG rising edge detected");
    CHECK(rt(TRUE) == TRUE, "rising edge");

    TEST("R_TRIG no edge on sustained TRUE");
    CHECK(rt(TRUE) == FALSE, "no edge on sustained");

    TEST("R_TRIG second rising edge");
    CHECK(rt(FALSE) == FALSE, "reset");
    CHECK(rt(TRUE) == TRUE, "second rising edge");
}

void test_f_trig() {
    printf("\n─── 8. F_TRIG 下降沿检测 ───\n");

    F_TRIG ft;

    TEST("F_TRIG no edge on TRUE");
    CHECK(ft(TRUE) == FALSE, "no edge");

    TEST("F_TRIG falling edge detected");
    CHECK(ft(FALSE) == TRUE, "falling edge");

    TEST("F_TRIG no edge on sustained FALSE");
    CHECK(ft(FALSE) == FALSE, "no edge on sustained");
}

int main() {
    printf("=== Standard Function Block Unit Tests ===\n");
    test_ton();
    test_tof();
    test_tp();
    test_ctu();
    test_ctd();
    test_ctud();
    test_r_trig();
    test_f_trig();

    printf("\n─────────────────────────────\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    return test_fail > 0 ? 1 : 0;
}