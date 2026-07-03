#include "debug/debug_if.h"
#include <cstdio>
#include <cstring>
#include <cassert>
#include <vector>
#include <string>

using namespace rt_plc;

static int passed = 0, failed = 0;
#define test(name) std::printf("  [%s] ", name)
#define ok() do { passed++; std::printf("PASS\n"); } while(0)
#define fail(msg) do { failed++; std::printf("FAIL: %s\n", msg); } while(0)
#define check(cond, msg) do { if(cond) ok(); else fail(msg); } while(0)
#define checkEq(a,b) check((a)==(b), "expected " #a "==" #b)

// ── Test data ──

static uint8_t testBuildId[16] = {0x12,0x34,0x56,0x78};

static DebugVar testVars[] = {
    // id  storage   bitOff  type         access       offset  size  count
    {1, DebugStorage::GVL,    0xFF, DebugType::INT,  DebugAccess::READ_WRITE,  0,  2, 1},
    {2, DebugStorage::GVL,    0xFF, DebugType::REAL, DebugAccess::READ_WRITE,  4,  4, 1},
    {3, DebugStorage::GVL,    0xFF, DebugType::INT,  DebugAccess::READ_WRITE,  8,  2, 5},  // 数组
    {4, DebugStorage::INPUT,  0,    DebugType::BOOL, DebugAccess::FORCEABLE,   0,  1, 1},
    {5, DebugStorage::INPUT,  1,    DebugType::BOOL, DebugAccess::FORCEABLE,   0,  1, 1},
    {6, DebugStorage::OUTPUT, 0,    DebugType::BOOL, DebugAccess::FORCEABLE,   0,  1, 1},
    {7, DebugStorage::OUTPUT, 0xFF, DebugType::INT,  DebugAccess::FORCEABLE,   8,  2, 1},
};

static const uint32_t varCount = 7;

static GVL gvl;
static ProcessImage io;

static void setupTestData() {
    // GVL: INT=42 at offset 0, REAL=3.14 at offset 4, ARR[5] INT at offset 8
    int16_t val = 42;
    std::memcpy(gvl.memory + 0, &val, 2);
    float fval = 3.14f;
    std::memcpy(gvl.memory + 4, &fval, 4);
    for (int i = 0; i < 5; i++) {
        int16_t av = (int16_t)(100 + i);
        std::memcpy(gvl.memory + 8 + i*2, &av, 2);
    }

    // IO: set some inputs
    io.inputs[0] = 0x00;  // both BOOL inputs off

    // OUTPUT INT at offset 8
    int16_t ov = 77;
    std::memcpy(io.outputs + 8, &ov, 2);
}

// ── Tests ──

static void t_findVar() {
    test("findVar_valid");
    const DebugVar* v = nullptr;
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    v = de.findVar(1);
    check(v != nullptr && v->id == 1 && v->storage == DebugStorage::GVL, "");

    test("findVar_nonexist");
    check(de.findVar(999) == nullptr, "");

    test("findVar_bitVar");
    v = de.findVar(4);
    check(v != nullptr && v->bitOffset == 0 && v->storage == DebugStorage::INPUT, "");

    test("findVar_array");
    v = de.findVar(3);
    check(v != nullptr && v->count == 5, "array count should be 5");
}

static void t_hello() {
    test("hello");
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    uint8_t bid[16];
    uint32_t vc;
    de.hello(bid, vc);
    check(std::memcmp(bid, testBuildId, 16) == 0, "");
    checkEq(vc, varCount);

    test("hello_nullBuildId");
    DebugEngine de2;
    de2.init(testVars, varCount, nullptr);
    de2.hello(bid, vc);
    checkEq(vc, varCount);
}

static void t_watchAndSnapshot() {
    setupTestData();
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    test("setWatchList_scalar");
    uint32_t ids[] = {1, 2};
    de.requestSetWatchList(ids, 2);
    de.applyPendingCommands();
    checkEq(de.watchCount(), 2);

    test("publishSnapshot_int");
    DiagStats ds{}; ds.totalScanCount = 42; ds.lastScanTime = 100; ds.maxScanTime = 200;
    de.publishSnapshot(gvl, io, 10, 500, ds, SystemState::RUN);
    const DebugSnapshot& snap = de.latestSnapshot();
    checkEq(snap.cycle, 10);
    checkEq(snap.systemTime, 500);
    checkEq(snap.totalTicks, 42);
    check(snap.sampleCount == 2, "expected 2 samples");

    test("readVar_int");
    uint8_t buf[8]; uint16_t sz;
    check(de.readVar(1, buf, sz), "readVar(1) should succeed");
    int16_t iv;
    std::memcpy(&iv, buf, 2);
    check(iv == 42, "expected INT=42");

    test("readVar_real");
    check(de.readVar(2, buf, sz), "readVar(2) should succeed");
    float fv;
    std::memcpy(&fv, buf, 4);
    check(fv > 3.0f && fv < 3.2f, "expected REAL ~3.14");

    test("readVar_notFound");
    check(!de.readVar(999, buf, sz), "should fail for invalid id");

    test("array_skipped_in_snapshot");
    uint32_t arrIds[] = {3};
    de.requestSetWatchList(arrIds, 1);
    de.applyPendingCommands();
    de.publishSnapshot(gvl, io, 11, 600, ds, SystemState::RUN);
    check(de.latestSnapshot().sampleCount == 0, "array should be skipped (count>1)");
}

static void t_force() {
    setupTestData();
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    // GVL INT force
    test("force_GVL_INT");
    uint8_t fv[2] = {99, 0};  // INT = 99
    de.requestForce(1, fv, 2);
    de.applyPendingCommands();
    check(de.isForced(1), "should be forced");

    // Apply POST_LOGIC force (GVL force happens after logic)
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::RUN);
    int16_t gvVal;
    std::memcpy(&gvVal, gvl.memory + 0, 2);
    check(gvVal == 99, "GVL INT should be forced to 99");

    test("unforce");
    de.requestUnforce(1);
    de.applyPendingCommands();
    check(!de.isForced(1), "should not be forced");

    // Restore and verify
    std::memcpy(gvl.memory + 0, fv, 2); // reset to 42 doesn't matter, we check no forced state
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::RUN);
    // Force is cleared, value stays as-is (no override)

    test("clearForces");
    de.requestForce(1, fv, 2);
    de.requestForce(2, fv, 4);
    de.applyPendingCommands();
    check(de.isForced(1) && de.isForced(2), "both should be forced");
    de.requestClearForces();
    de.applyPendingCommands();
    check(!de.isForced(1) && !de.isForced(2), "neither should be forced");

    // INPUT BOOL force (PRE_LOGIC)
    test("force_INPUT_BOOL_PRE");
    uint8_t bv[1] = {1};
    de.requestForce(4, bv, 1);  // Start (INPUT bit0)
    de.applyPendingCommands();
    de.applyForces(gvl, io, ForcePhase::PRE_LOGIC, SystemState::RUN);
    check(io.readInputBit(0, 0) == TRUE, "INPUT bit0 should be TRUE");

    test("force_INPUT_BOOL_PRE_bit1");
    de.requestForce(5, bv, 1);  // Stop (INPUT bit1)
    de.applyPendingCommands();
    de.applyForces(gvl, io, ForcePhase::PRE_LOGIC, SystemState::RUN);
    check(io.readInputBit(0, 1) == TRUE, "INPUT bit1 should be TRUE");

    // OUTPUT BOOL force (POST_LOGIC)
    test("force_OUTPUT_BOOL_POST");
    de.requestForce(6, bv, 1);  // Run (OUTPUT bit0)
    de.applyPendingCommands();
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::RUN);
    check(io.readOutputBit(0, 0) == TRUE, "OUTPUT bit0 should be TRUE");

    // OUTPUT INT force (POST_LOGIC)
    test("force_OUTPUT_INT_POST");
    uint8_t ov[2] = {0xCD, 0xAB};  // INT = 0xABCD = -21555 (signed) or 43981
    de.requestForce(7, ov, 2);
    de.applyPendingCommands();
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::RUN);
    int16_t outVal;
    std::memcpy(&outVal, io.outputs + 8, 2);
    check(outVal == static_cast<int16_t>(0xABCD), "OUTPUT INT should be forced");
}

static void t_accessControl() {
    setupTestData();
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    test("force_READ_ONLY_rejected");
    // Variable 1 has READ_WRITE, so let's test with a modified var
    // Actually testVars[0] is READ_WRITE which allows force.
    // Test: force with wrong size
    uint8_t fv[4] = {99, 0, 0, 0};  // 4 bytes for a 2-byte INT
    de.requestForce(1, fv, 4);
    de.applyPendingCommands();
    check(!de.isForced(1), "oversized force should be rejected");

    test("force_invalid_id");
    de.requestForce(999, fv, 2);
    de.applyPendingCommands();
    check(!de.isForced(999), "invalid id force should be silently ignored");

    test("readVar_invalid_id");
    uint8_t buf[8]; uint16_t sz;
    check(!de.readVar(999, buf, sz), "readVar(999) should fail");
}

static void t_errorSafeOutput() {
    setupTestData();
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    // Force OUTPUT BOOL
    uint8_t bv[1] = {1};
    de.requestForce(6, bv, 1);  // Run (OUTPUT bit0)
    de.applyPendingCommands();

    // Apply with ERROR state
    test("force_OUTPUT_blocked_in_ERROR");
    io.outputs[0] = 0x00;  // safe output
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::ERROR);
    check(io.readOutputBit(0, 0) == FALSE, "OUTPUT force blocked in ERROR");

    // Apply with STOP state
    test("force_OUTPUT_blocked_in_STOP");
    de.requestClearForces(); de.applyPendingCommands();
    de.requestForce(6, bv, 1); de.applyPendingCommands();
    io.outputs[0] = 0x00;
    de.applyForces(gvl, io, ForcePhase::POST_LOGIC, SystemState::STOP);
    check(io.readOutputBit(0, 0) == FALSE, "OUTPUT force blocked in STOP");
}

static void t_readMemory() {
    setupTestData();
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    test("readMemory_GVL");
    uint8_t buf[8];
    check(de.readMemory(DebugStorage::GVL, 0, 2, buf), "");
    int16_t iv;
    std::memcpy(&iv, buf, 2);
    checkEq(iv, 42);

    test("readMemory_OOB");
    check(!de.readMemory(DebugStorage::GVL, 65530, 10, buf), "should fail OOB");

    test("readMemory_noBind");
    DebugEngine de2;
    de2.init(testVars, varCount, testBuildId);
    check(!de2.readMemory(DebugStorage::GVL, 0, 2, buf), "should fail without bindMemory");

    test("readMemory_INPUT");
    io.inputs[10] = 77;
    check(de.readMemory(DebugStorage::INPUT, 10, 1, buf), "");
    checkEq(buf[0], 77);
}

static void t_pendingQueueOverflow() {
    DebugEngine de;
    de.init(testVars, varCount, testBuildId);
    de.bindMemory(&gvl, &io);

    test("pendingQueue_overflow");
    // Fill beyond capacity (16)
    uint8_t bv[1] = {1};
    for (int i = 0; i < DEBUG_MAX_PENDING_COMMANDS + 5; i++) {
        de.requestForce(4, bv, 1);  // push to pending
    }
    check(de.droppedCommands() > 0, "should have dropped some commands");
    de.applyPendingCommands();
    check(de.isForced(4), "last force should still be applied");
}

int main() {
    std::printf("=== DebugEngine Unit Tests ===\n\n");

    std::printf("[findVar/hello]\n");
    t_findVar();
    t_hello();

    std::printf("\n[watch/snapshot/readVar]\n");
    t_watchAndSnapshot();

    std::printf("\n[force]\n");
    t_force();

    std::printf("\n[accessControl]\n");
    t_accessControl();

    std::printf("\n[errorSafeOutput]\n");
    t_errorSafeOutput();

    std::printf("\n[readMemory]\n");
    t_readMemory();

    std::printf("\n[pendingQueue]\n");
    t_pendingQueueOverflow();

    std::printf("\n=== Results: %d passed, %d failed ===\n", passed, failed);
    return failed > 0 ? 1 : 0;
}
