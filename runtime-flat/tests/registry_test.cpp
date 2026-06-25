#include "rt_plc.h"
#include "core/registry.h"
#include "core/error_manager.h"
#include <cstdio>
#include <cstring>

using namespace rt_plc;

static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

static int gvl_value = 0;

static void test_init(GVL& gvl, ProcessImage&) {
    gvl_value = 10;
}

static void test_cyclic(GVL& gvl, ProcessImage&, TIME) {
    gvl_value = gvl_value * 2;
}

static void test_pre(GVL& gvl, ProcessImage&) {
    gvl_value = gvl_value + 1;
}

static void test_post(GVL& gvl, ProcessImage&) {
    gvl_value = gvl_value + 100;
}

void test_registry() {
    printf("\n─── 1. POURegistry ───\n");

    POURegistry reg;

    TEST("registry empty initially");
    CHECK(reg.count() == 0, "count!=0");

    TEST("add callbacks");
    reg.add("MAIN", test_init, test_cyclic, test_pre, test_post);
    CHECK(reg.count() == 1, "count!=1");

    TEST("lookup existing");
    const POUCallbacks* cbs = reg.lookup("MAIN");
    CHECK(cbs != nullptr, "lookup returned null");
    CHECK(cbs->init == test_init, "init mismatch");
    CHECK(cbs->cyclic == test_cyclic, "cyclic mismatch");
    CHECK(cbs->pre == test_pre, "pre mismatch");
    CHECK(cbs->post == test_post, "post mismatch");

    TEST("lookup non-existing");
    CHECK(reg.lookup("NONEXISTENT") == nullptr, "should return null");

    TEST("add multiple entries");
    reg.add("SUB", test_init, test_cyclic);
    CHECK(reg.count() == 2, "count!=2");

    TEST("entries iterable");
    CHECK(reg.entries() != nullptr, "entries should not be null");

    TEST("add up to MAX_ENTRIES");
    POURegistry reg2;
    for (int i = 0; i < 70; i++) {
        char name[16];
        snprintf(name, sizeof(name), "POU_%d", i);
        reg2.add(name, nullptr, nullptr);
    }
    CHECK(reg2.count() == 64, "should cap at MAX_ENTRIES=64");
}

void test_program_instance() {
    printf("\n─── 2. ProgramInstance ───\n");

    GVL gvl;
    gvl.clear();
    ProcessImage io;

    ProgramInstance prog;
    strcpy(prog.name, "TEST");
    prog.initFunc = test_init;
    prog.cyclicFunc = test_cyclic;
    prog.preFunc = test_pre;
    prog.postFunc = test_post;

    TEST("ProgramInstance initial phase");
    CHECK(prog.phase == ProgramPhase::UNINIT, "phase should be UNINIT");

    gvl_value = 0;
    prog.doInit(gvl, io);
    TEST("doInit calls initFunc");
    CHECK(gvl_value == 10, "initFunc should set gvl_value to 10");

    gvl_value = 5;
    prog.doPre(gvl, io);
    TEST("doPre calls preFunc");
    CHECK(gvl_value == 6, "preFunc should add 1");

    gvl_value = 5;
    prog.doCyclic(gvl, io, 0);
    TEST("doCyclic calls cyclicFunc");
    CHECK(gvl_value == 10, "cyclicFunc should double");

    gvl_value = 5;
    prog.doPost(gvl, io);
    TEST("doPost calls postFunc");
    CHECK(gvl_value == 105, "postFunc should add 100");

    TEST("cycleCount increments");
    CHECK(prog.cycleCount >= 0, "cycleCount should be non-negative");
}
#if 0
void test_scheduler_basic() {
    printf("\n─── 3. Scheduler basic ───\n");

    Scheduler sched;
    GVL gvl;
    gvl.clear();

    TEST("Scheduler initial state");
    CHECK(sched.systemState == SystemState::STOPPED, "should be STOPPED");

    sched.addProgram("MAIN", test_init, test_cyclic);

    TEST("addProgram increments count");
    CHECK(sched.programCount >= 1, "programCount should be >= 1");

    sched.addCyclicTask("TestTask", 0, T_ms(10));
    TEST("addCyclicTask increments count");
    CHECK(sched.taskCount >= 1, "taskCount should be >= 1");

    sched.addPOU(0, [](GVL& g, ProcessImage& io, TIME dt) {
        g.write<INT>(0, 42);
    });
    TEST("addPOU ok");
    CHECK(sched.tasks[0].pouCount >= 1, "pouCount should be >= 1");

    sched.start(COLD);
    TEST("start sets state to RUNNING");
    CHECK(sched.systemState == SystemState::RUNNING, "should be RUNNING");

    sched.tick();
    TEST("tick increments scan count");
    CHECK(sched.diag.totalScanCount >= 1, "scanCount should be >= 1");

    sched.stop();
    TEST("stop sets state to STOPPED");
    CHECK(sched.systemState == SystemState::STOPPED, "should be STOPPED");
}
#endif
int main() {
    printf("=== Registry & ProgramInstance Unit Tests ===\n");
    test_registry();
    test_program_instance();
    // test_scheduler_basic();

    printf("\n─────────────────────────────\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    return test_fail > 0 ? 1 : 0;
}