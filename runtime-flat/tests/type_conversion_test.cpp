#include "rt_plc.h"
#include <cstdio>
#include <cstring>

using namespace rt_plc;

static int test_pass = 0;
static int test_fail = 0;

#define TEST(name) printf("  %-50s", name)
#define PASS() do { printf("[PASS]\n"); test_pass++; } while(0)
#define FAIL(msg) do { printf("[FAIL] %s\n", msg); test_fail++; } while(0)
#define CHECK(cond, msg) do { if (cond) PASS(); else FAIL(msg); } while(0)

void test_type_conversion() {
    printf("\n─── 1. 类型转换 ───\n");

    TEST("TO_INT from REAL");
    CHECK(TO_INT(REAL(3.7f)) == 3, "3.7 -> 3");

    TEST("TO_INT from DINT");
    CHECK(TO_INT(DINT(123)) == 123, "123 -> 123");

    TEST("TO_REAL from INT");
    CHECK(TO_REAL(INT(42)) == 42.0f, "42 -> 42.0");

    TEST("TO_BOOL from INT non-zero");
    CHECK(TO_BOOL(INT(1)) == TRUE, "1 -> TRUE");

    TEST("TO_BOOL from INT zero");
    CHECK(TO_BOOL(INT(0)) == FALSE, "0 -> FALSE");

    TEST("TO_BOOL from REAL non-zero");
    CHECK(TO_BOOL(REAL(3.14f)) == TRUE, "3.14 -> TRUE");

    TEST("TO_BOOL from REAL zero");
    CHECK(TO_BOOL(REAL(0.0f)) == FALSE, "0.0 -> FALSE");

    TEST("TO_LREAL from REAL");
    CHECK(TO_LREAL(REAL(1.5f)) == 1.5, "1.5f -> 1.5");

    TEST("TO_STRING from INT");
    STRING s = TO_STRING(INT(42));
    CHECK(strcmp(s.data, "42") == 0, "TO_STRING(42) failed");

    TEST("TO_STRING from BOOL TRUE");
    s = TO_STRING(BOOL(TRUE));
    CHECK(strcmp(s.data, "TRUE") == 0, "TO_STRING(TRUE) failed");

    TEST("TO_STRING from BOOL FALSE");
    s = TO_STRING(BOOL(FALSE));
    CHECK(strcmp(s.data, "FALSE") == 0, "TO_STRING(FALSE) failed");
}

void test_math_functions() {
    printf("\n─── 2. 数学函数 ───\n");

    TEST("ABS positive");
    CHECK(ABS(INT(5)) == 5, "ABS(5)!=5");

    TEST("ABS negative");
    CHECK(ABS(INT(-5)) == 5, "ABS(-5)!=5");

    TEST("ABS REAL");
    CHECK(ABS(REAL(-3.14f)) == 3.14f, "ABS(-3.14)!=3.14");

    TEST("SQRT");
    CHECK(SQRT(REAL(4.0f)) == 2.0f, "SQRT(4)!=2");

    TEST("MIN int");
    CHECK(MIN(INT(3), INT(5)) == 3, "MIN(3,5)!=3");

    TEST("MAX int");
    CHECK(MAX(INT(3), INT(5)) == 5, "MAX(3,5)!=5");

    TEST("MOD int");
    CHECK(MOD(INT(10), INT(3)) == 1, "MOD(10,3)!=1");

    TEST("MOD by zero");
    CHECK(MOD(INT(10), INT(0)) == 0, "MOD(10,0)!=0");

    TEST("SEL true");
    CHECK(SEL(TRUE, INT(1), INT(2)) == 2, "SEL(TRUE,1,2)!=2");

    TEST("SEL false");
    CHECK(SEL(FALSE, INT(1), INT(2)) == 1, "SEL(FALSE,1,2)!=1");

    TEST("LIMIT in range");
    CHECK(LIMIT(INT(0), INT(5), INT(10)) == 5, "LIMIT(0,5,10)!=5");

    TEST("LIMIT below min");
    CHECK(LIMIT(INT(0), INT(-5), INT(10)) == 0, "LIMIT(0,-5,10)!=0");

    TEST("LIMIT above max");
    CHECK(LIMIT(INT(0), INT(15), INT(10)) == 10, "LIMIT(0,15,10)!=10");
}

void test_string_ops() {
    printf("\n─── 3. 字符串操作 ───\n");

    TEST("STRING constructor from literal");
    STRING s("hello");
    CHECK(strcmp(s.data, "hello") == 0, "constructor failed");

    TEST("STRING default constructor");
    STRING empty;
    CHECK(empty.data[0] == '\0', "default should be empty");

    TEST("STRING equality");
    STRING a("abc");
    STRING b("abc");
    CHECK(a == b, "'abc' != 'abc'");

    TEST("STRING inequality");
    STRING c("xyz");
    CHECK(a != c, "'abc' == 'xyz'");

    TEST("CONCAT");
    STRING result = CONCAT(STRING("hello "), STRING("world"));
    CHECK(strcmp(result.data, "hello world") == 0, "CONCAT failed");

    TEST("LEN");
    CHECK(LEN(STRING("hello")) == 5, "LEN('hello')!=5");

    TEST("LEN empty");
    CHECK(LEN(STRING("")) == 0, "LEN('')!=0");

    TEST("STR_EQ");
    CHECK(STR_EQ(STRING("a"), STRING("a")) == TRUE, "STR_EQ failed");

    TEST("STR_NE");
    CHECK(STR_NE(STRING("a"), STRING("b")) == TRUE, "STR_NE failed");

    TEST("LEFT");
    STRING left = LEFT(STRING("hello"), 3);
    CHECK(strcmp(left.data, "hel") == 0, "LEFT('hello',3)!=hel");

    TEST("RIGHT");
    STRING right = RIGHT(STRING("hello"), 3);
    CHECK(strcmp(right.data, "llo") == 0, "RIGHT('hello',3)!=llo");

    TEST("MID");
    STRING mid = MID(STRING("hello"), 1, 3);
    CHECK(strcmp(mid.data, "ell") == 0, "MID('hello',1,3)!=ell");
}

void test_time_helpers() {
    printf("\n─── 4. TIME 辅助函数 ───\n");

    TEST("T_ms");
    CHECK(T_ms(100) == 100000, "T_ms(100)!=100000us");

    TEST("T_us");
    CHECK(T_us(500) == 500, "T_us(500)!=500");

    TEST("T_s");
    CHECK(T_s(1) == 1000000, "T_s(1)!=1000000us");

    TEST("T_eq");
    CHECK(T_eq(T_ms(10), T_us(10000)) == TRUE, "T_eq failed");

    TEST("T_lt");
    CHECK(T_lt(T_ms(1), T_ms(2)) == TRUE, "T_lt failed");

    TEST("T_gt");
    CHECK(T_gt(T_ms(2), T_ms(1)) == TRUE, "T_gt failed");
}

void test_plc_array() {
    printf("\n─── 5. PLC_Array ───\n");

    PLC_Array<INT, 5> arr;

    TEST("PLC_Array size");
    CHECK(arr.size() == 5, "size!=5");

    TEST("PLC_Array default init to 0");
    CHECK(arr[0] == 0, "arr[0]!=0");

    TEST("PLC_Array write and read");
    arr[0] = 10;
    arr[1] = 20;
    CHECK(arr[0] == 10 && arr[1] == 20, "write/read failed");

    TEST("PLC_Array at() normal");
    CHECK(arr.at(0) == 10 && arr.at(1) == 20, "at() failed");

    TEST("PLC_Array at() OOB");
    INT val = arr.at(10);
    CHECK(val == arr[4], "at() OOB should return last element");

    TEST("PLC_Array fill");
    arr.fill(42);
    CHECK(arr[0] == 42 && arr[4] == 42, "fill failed");

    TEST("PLC_Array equality");
    PLC_Array<INT, 5> arr2;
    arr2.fill(42);
    CHECK(arr == arr2, "arrays should be equal");
}

void test_plc_subrange() {
    printf("\n─── 6. PLC_Subrange ───\n");

    PLC_Subrange<INT, 0, 100> range;

    TEST("PLC_Subrange default to low");
    CHECK((INT)range == 0, "default!=0");

    TEST("PLC_Subrange in range");
    range = 50;
    CHECK((INT)range == 50, "50!=50");

    TEST("PLC_Subrange clamp to low");
    range = -10;
    CHECK((INT)range == 0, "not clamped to low");

    TEST("PLC_Subrange clamp to high");
    range = 200;
    CHECK((INT)range == 100, "not clamped to high");

    TEST("PLC_Subrange add");
    range = 30;
    PLC_Subrange<INT, 0, 100> r2 = range + 20;
    CHECK((INT)r2 == 50, "add failed");
}

int main() {
    printf("=== Type Conversion & Operations Unit Tests ===\n");
    test_type_conversion();
    test_math_functions();
    test_string_ops();
    test_time_helpers();
    test_plc_array();
    test_plc_subrange();

    printf("\n─────────────────────────────\n");
    printf("通过: %d  失败: %d  总计: %d\n", test_pass, test_fail, test_pass + test_fail);
    return test_fail > 0 ? 1 : 0;
}