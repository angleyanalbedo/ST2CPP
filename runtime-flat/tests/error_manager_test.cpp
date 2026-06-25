#include "rt_plc.h"
#include "core/error_manager.h"
#include <cstdio>

using namespace rt_plc;

static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

void test_error_manager_safe_div() {
    printf("\n─── 1. safeDiv ───\n");

    ErrorManager em;

    TEST("safeDiv int normal");
    CHECK(em.safeDiv(10, 2, 1, 100, 0) == 5, "10/2!=5");

    TEST("safeDiv int by zero returns 0");
    CHECK(em.safeDiv(10, 0, 1, 101, 0) == 0, "div/0 should return 0");

    TEST("safeDiv int by zero sets fatalMode");
    CHECK(em.isFatal(), "fatalMode should be true after div/0");

    em.reset();

    TEST("safeDiv float normal");
    CHECK(em.safeDiv(10.0f, 2.0f, 1, 102, 0) == 5.0f, "10.0/2.0!=5.0");

    TEST("safeDiv float by zero returns 0");
    CHECK(em.safeDiv(10.0f, 0.0f, 1, 103, 0) == 0.0f, "float div/0 should return 0");

    em.reset();

    TEST("safeDiv double normal");
    CHECK(em.safeDiv(10.0, 2.0, 1, 104, 0) == 5.0, "10.0/2.0!=5.0");

    TEST("safeDiv double by zero");
    CHECK(em.safeDiv(10.0, 0.0, 1, 105, 0) == 0.0, "double div/0 should return 0");
}

void test_error_manager_safe_mod() {
    printf("\n─── 2. safeMod ───\n");

    ErrorManager em;

    TEST("safeMod int normal");
    CHECK(em.safeMod(10, 3, 1, 200, 0) == 1, "10%3!=1");

    TEST("safeMod int by zero returns 0");
    CHECK(em.safeMod(10, 0, 1, 201, 0) == 0, "mod/0 should return 0");

    TEST("safeMod by zero sets fatalMode");
    CHECK(em.isFatal(), "fatalMode should be true after mod/0");
}

void test_error_manager_safe_arithmetic() {
    printf("\n─── 3. safeAdd/safeSub/safeMul ───\n");

    ErrorManager em;

    TEST("safeAdd normal");
    CHECK(em.safeAdd(10, 20, 1, 300, 0) == 30, "10+20!=30");

    TEST("safeAdd overflow detected");
    INT maxInt = 32767;
    em.safeAdd(maxInt, INT(1), 1, 301, 0);
    CHECK(em.count() >= 1, "overflow should be logged");

    em.reset();

    TEST("safeSub normal");
    CHECK(em.safeSub(INT(30), INT(10), 1, 302, 0) == 20, "30-10!=20");

    TEST("safeSub underflow detected");
    INT minInt = -32768;
    em.safeSub(minInt, INT(1), 1, 303, 0);
    CHECK(em.count() >= 1, "underflow should be logged");

    em.reset();

    TEST("safeMul normal");
    CHECK(em.safeMul(10, 5, 1, 304, 0) == 50, "10*5!=50");

    TEST("safeMul overflow detected");
    em.safeMul(INT(1000), INT(1000), 1, 305, 0);
    CHECK(em.count() >= 1, "mul overflow should be logged");

    TEST("safeMul by zero ok");
    CHECK(em.safeMul(100, 0, 1, 306, 0) == 0, "100*0!=0");
}

void test_error_manager_safe_array() {
    printf("\n─── 4. safeArrayAt ───\n");

    ErrorManager em;

    TEST("safeArrayAt static array ok");
    int arr[5] = {10, 20, 30, 40, 50};
    CHECK(em.safeArrayAt(arr, 0, 1, 400, 0) == 10, "arr[0]!=10");
    CHECK(em.safeArrayAt(arr, 4, 1, 401, 0) == 50, "arr[4]!=50");

    TEST("safeArrayAt static array OOB");
    CHECK(em.safeArrayAt(arr, 10, 1, 402, 0) == 50, "OOB should return last element");

    TEST("safeArrayAt pointer array ok");
    int* ptr = arr;
    CHECK(em.safeArrayAt(ptr, 2, 5, 1, 403, 0) == 30, "ptr[2]!=30");

    TEST("safeArrayAt pointer OOB");
    CHECK(em.safeArrayAt(ptr, 10, 5, 1, 404, 0) == 50, "ptr OOB should return last");
}

void test_error_manager_report() {
    printf("\n─── 5. report 和日志 ───\n");

    ErrorManager em;

    TEST("report increments count");
    em.report(ErrorCode::DIV_BY_ZERO, 1, 500, "test error", 0);
    CHECK(em.count() == 1, "count should be 1");

    em.report(ErrorCode::DIV_BY_ZERO, 1, 501, "test error 2", 0);
    CHECK(em.count() == 2, "count should be 2");

    TEST("lastError after report");
    CHECK(em.lastError == ErrorCode::DIV_BY_ZERO, "lastError mismatch");

    TEST("reset clears state");
    em.reset();
    CHECK(em.count() == 0, "count should be 0 after reset");
    CHECK(em.lastError == ErrorCode::NONE, "lastError should be NONE");
    CHECK(!em.isFatal(), "fatalMode should be false");
}

void test_error_manager_fatal_codes() {
    printf("\n─── 6. 致命错误码 ───\n");

    ErrorManager em;

    TEST("DIV_BY_ZERO is fatal");
    em.report(ErrorCode::DIV_BY_ZERO, 1, 600, "div0", 0);
    CHECK(em.isFatal(), "DIV_BY_ZERO should be fatal");

    em.reset();

    TEST("ARRAY_OUT_OF_BOUNDS is fatal");
    em.report(ErrorCode::ARRAY_OUT_OF_BOUNDS, 1, 601, "oob", 0);
    CHECK(em.isFatal(), "ARRAY_OUT_OF_BOUNDS should be fatal");

    em.reset();

    TEST("NULL_POINTER is fatal");
    em.report(ErrorCode::NULL_POINTER, 1, 602, "null", 0);
    CHECK(em.isFatal(), "NULL_POINTER should be fatal");

    em.reset();

    TEST("STACK_OVERFLOW is fatal");
    em.report(ErrorCode::STACK_OVERFLOW, 1, 603, "so", 0);
    CHECK(em.isFatal(), "STACK_OVERFLOW should be fatal");

    em.reset();

    TEST("INT_OVERFLOW is NOT fatal");
    em.report(ErrorCode::INT_OVERFLOW, 1, 604, "overflow", 0);
    CHECK(!em.isFatal(), "INT_OVERFLOW should NOT be fatal");
}

static bool handlerCalled = false;
static void testHandler(const ErrorEntry&) { handlerCalled = true; }

void test_error_manager_handler() {
    printf("\n─── 7. 自定义 handler ───\n");

    ErrorManager em;
    handlerCalled = false;

    em.setHandler(testHandler);

    TEST("handler is called on report");
    em.report(ErrorCode::DIV_BY_ZERO, 1, 700, "handler test", 0);
    CHECK(handlerCalled, "handler should be called");
}

void test_error_manager_operand_recording() {
    printf("\n─── 8. 操作数记录 ───\n");

    ErrorManager em;

    TEST("safeDiv records operands");
    em.safeDiv(INT(100), INT(0), 1, 800, 0);
    CHECK(em.log[0].operandA == 100, "operandA mismatch");
    CHECK(em.log[0].operandB == 0, "operandB mismatch");

    em.reset();

    TEST("safeAdd records operands");
    em.safeAdd(INT(32767), INT(1), 1, 801, 0);
    CHECK(em.log[0].operandA == 32767, "operandA mismatch");
    CHECK(em.log[0].operandB == 1, "operandB mismatch");
}

int main() {
    printf("=== ErrorManager Unit Tests ===\n");
    test_error_manager_safe_div();
    test_error_manager_safe_mod();
    test_error_manager_safe_arithmetic();
    test_error_manager_safe_array();
    test_error_manager_report();
    test_error_manager_fatal_codes();
    test_error_manager_handler();
    test_error_manager_operand_recording();

    printf("\n─────────────────────────────\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    return test_fail > 0 ? 1 : 0;
}