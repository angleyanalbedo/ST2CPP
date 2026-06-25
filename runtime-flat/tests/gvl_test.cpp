#include "rt_plc.h"
#include "core/gvl.h"
#include <cstdio>

using namespace rt_plc;

static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

void test_gvl_read_write() {
    printf("\n─── 1. GVL 基本读写 ───\n");

    GVL gvl;
    gvl.clear();

    TEST("INT write/read");
    gvl.write<INT>(0, 42);
    CHECK(gvl.read<INT>(0) == 42, "write/read mismatch");

    TEST("REAL write/read");
    gvl.write<REAL>(4, 3.14f);
    CHECK(gvl.read<REAL>(4) == 3.14f, "REAL write/read mismatch");

    TEST("BOOL write/read");
    gvl.write<BOOL>(8, TRUE);
    CHECK(gvl.read<BOOL>(8) == TRUE, "BOOL write/read mismatch");

    TEST("DINT write/read");
    gvl.write<DINT>(12, 123456);
    CHECK(gvl.read<DINT>(12) == 123456, "DINT write/read mismatch");

    TEST("STRING write/read");
    STRING str("hello");
    gvl.write<STRING>(16, str);
    CHECK(strcmp(gvl.read<STRING>(16).data, "hello") == 0, "STRING write/read mismatch");

    TEST("multiple INTs at different offsets");
    gvl.write<INT>(0, 10);
    gvl.write<INT>(2, 20);
    gvl.write<INT>(4, 30);
    CHECK(gvl.read<INT>(0) == 10 && gvl.read<INT>(2) == 20 && gvl.read<INT>(4) == 30, "multi-offset INT mismatch");

    TEST("usedBytes tracks highWaterMark");
    CHECK(gvl.usedBytes() >= 16 + 256, "usedBytes too low");
}

void test_gvl_bounds() {
    printf("\n─── 2. GVL 越界安全 ───\n");

    GVL gvl;
    gvl.clear();

    TEST("read out of bounds returns default");
    gvl.write<INT>(0, 42);
    INT val = gvl.read<INT>(65536);
    CHECK(val == 0, "out-of-bounds read should return 0");

    TEST("write out of bounds does not crash");
    gvl.write<INT>(65536, 99);
    PASS();

    TEST("read near boundary ok");
    gvl.write<INT>(65534, 77);
    CHECK(gvl.read<INT>(65534) == 77, "boundary read failed");

    TEST("read one byte past boundary");
    val = gvl.read<INT>(65535);
    CHECK(val == 0, "one byte past boundary should return 0");

    TEST("ptr out of bounds returns null");
    CHECK(gvl.ptr<INT>(65536) == nullptr, "out-of-bounds ptr should be null");
}

void test_gvl_retain() {
    printf("\n─── 3. GVL RETAIN 区域 ───\n");

    GVL gvl;
    gvl.clear();

    TEST("setRetainRegion valid range");
    gvl.setRetainRegion(100, 200);
    CHECK(gvl.retainStart == 100, "retainStart mismatch");
    CHECK(gvl.retainEnd == 200, "retainEnd mismatch");

    TEST("clearNonRetain preserves RETAIN");
    gvl.write<INT>(50, 10);
    gvl.write<INT>(150, 20);
    gvl.clearNonRetain();
    CHECK(gvl.read<INT>(50) == 0, "non-retain should be cleared");
    CHECK(gvl.read<INT>(150) == 20, "retain should be preserved");

    TEST("clear clears everything including RETAIN");
    gvl.clear();
    CHECK(gvl.read<INT>(150) == 0, "clear should clear retain too");
}

void test_gvl_safe_array() {
    printf("\n─── 4. GVL safeArrayAt ───\n");

    GVL gvl;
    gvl.clear();

    TEST("safeArrayAt normal access");
    gvl.write<INT>(0, 10);
    gvl.write<INT>(2, 20);
    gvl.write<INT>(4, 30);
    CHECK(gvl.safeArrayAt<INT>(0, 0, 3) == 10, "index 0");
    CHECK(gvl.safeArrayAt<INT>(0, 1, 3) == 20, "index 1");
    CHECK(gvl.safeArrayAt<INT>(0, 2, 3) == 30, "index 2");

    TEST("safeArrayAt out of bounds returns last element");
    CHECK(gvl.safeArrayAt<INT>(0, 100, 3) == 30, "OOB should return last element");

    TEST("safeArrayAt write through reference");
    gvl.safeArrayAt<INT>(0, 1, 3) = 99;
    CHECK(gvl.read<INT>(2) == 99, "write through safeArrayAt ref failed");

    TEST("safeArrayAt with null ptr");
    CHECK(gvl.safeArrayAt<INT>(65536, 0, 1) == 0, "null ptr should return fallback");
}

void test_gvl_ptr() {
    printf("\n─── 5. GVL ptr 访问 ───\n");

    GVL gvl;
    gvl.clear();

    TEST("ptr write then read");
    INT* p = gvl.ptr<INT>(0);
    *p = 42;
    CHECK(gvl.read<INT>(0) == 42, "ptr write failed");

    TEST("const ptr read");
    const INT* cp = gvl.ptr<INT>(0);
    CHECK(*cp == 42, "const ptr read failed");

    TEST("ptr at non-zero offset");
    gvl.write<REAL>(8, 2.5f);
    REAL* rp = gvl.ptr<REAL>(8);
    CHECK(*rp == 2.5f, "ptr at non-zero offset failed");
}

void test_gvl_clear() {
    printf("\n─── 6. GVL clear 操作 ───\n");

    GVL gvl;
    gvl.clear();

    TEST("clear sets memory to zero");
    gvl.write<INT>(0, 42);
    gvl.write<REAL>(4, 3.14f);
    gvl.clear();
    CHECK(gvl.read<INT>(0) == 0, "INT not cleared");
    CHECK(gvl.read<REAL>(4) == 0.0f, "REAL not cleared");

    TEST("highWaterMark after clear");
    gvl.write<INT>(0, 1);
    gvl.clear();
    CHECK(gvl.usedBytes() == 0 || gvl.usedBytes() <= 2, "highWaterMark should be low after clear");
}

void test_gvl_multiple_types() {
    printf("\n─── 7. GVL 多类型混合读写 ───\n");

    GVL gvl;
    gvl.clear();

    gvl.write<SINT>(0, 127);
    gvl.write<USINT>(1, 255);
    gvl.write<INT>(2, 32767);
    gvl.write<UINT>(4, 65535);
    gvl.write<DINT>(8, 2147483647);
    gvl.write<UDINT>(12, 4294967295U);
    gvl.write<LINT>(16, 9223372036854775807LL);
    gvl.write<ULINT>(24, 18446744073709551615ULL);
    gvl.write<REAL>(32, 1.5f);
    gvl.write<LREAL>(40, 2.718281828);

    CHECK(gvl.read<SINT>(0) == 127, "SINT");
    CHECK(gvl.read<USINT>(1) == 255, "USINT");
    CHECK(gvl.read<INT>(2) == 32767, "INT");
    CHECK(gvl.read<UINT>(4) == 65535, "UINT");
    CHECK(gvl.read<DINT>(8) == 2147483647, "DINT");
    CHECK(gvl.read<UDINT>(12) == 4294967295U, "UDINT");
    CHECK(gvl.read<LINT>(16) == 9223372036854775807LL, "LINT");
    CHECK(gvl.read<ULINT>(24) == 18446744073709551615ULL, "ULINT");
    CHECK(gvl.read<REAL>(32) == 1.5f, "REAL");
    CHECK(gvl.read<LREAL>(40) == 2.718281828, "LREAL");
}

int main() {
    printf("=== GVL Unit Tests ===\n");
    test_gvl_read_write();
    test_gvl_bounds();
    test_gvl_retain();
    test_gvl_safe_array();
    test_gvl_ptr();
    test_gvl_clear();
    test_gvl_multiple_types();

    printf("\n─────────────────────────────\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    return test_fail > 0 ? 1 : 0;
}