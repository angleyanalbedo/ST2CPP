#include "doctest.h"
#include "rt_plc.h"
#include <cstring>

using namespace rt_plc;

TEST_CASE("Type conversion") {
    SUBCASE("TO_INT from REAL") { CHECK(TO_INT(REAL(3.7f)) == 3); }
    SUBCASE("TO_INT from DINT") { CHECK(TO_INT(DINT(123)) == 123); }
    SUBCASE("TO_REAL from INT") { CHECK(TO_REAL(INT(42)) == 42.0f); }
    SUBCASE("TO_BOOL non-zero") { CHECK(TO_BOOL(INT(1)) == TRUE); }
    SUBCASE("TO_BOOL zero") { CHECK(TO_BOOL(INT(0)) == FALSE); }
    SUBCASE("TO_BOOL REAL non-zero") { CHECK(TO_BOOL(REAL(3.14f)) == TRUE); }
    SUBCASE("TO_BOOL REAL zero") { CHECK(TO_BOOL(REAL(0.0f)) == FALSE); }
    SUBCASE("TO_LREAL from REAL") { CHECK(TO_LREAL(REAL(1.5f)) == 1.5); }

    SUBCASE("TO_STRING from INT") {
        STRING s = TO_STRING(INT(42));
        CHECK(std::strcmp(s.data, "42") == 0);
    }

    SUBCASE("TO_STRING from BOOL") {
        STRING s = TO_STRING(BOOL(TRUE));
        CHECK(std::strcmp(s.data, "TRUE") == 0);
        s = TO_STRING(BOOL(FALSE));
        CHECK(std::strcmp(s.data, "FALSE") == 0);
    }
}

TEST_CASE("Math functions") {
    SUBCASE("ABS") {
        CHECK(ABS(INT(5)) == 5);
        CHECK(ABS(INT(-5)) == 5);
        CHECK(ABS(REAL(-3.14f)) == 3.14f);
    }
    SUBCASE("SQRT") { CHECK(SQRT(REAL(4.0f)) == 2.0f); }
    SUBCASE("MIN") { CHECK(MIN(INT(3), INT(5)) == 3); }
    SUBCASE("MAX") { CHECK(MAX(INT(3), INT(5)) == 5); }
    SUBCASE("MOD") {
        CHECK(MOD(INT(10), INT(3)) == 1);
        CHECK(MOD(INT(10), INT(0)) == 0);
    }
    SUBCASE("SEL") {
        CHECK(SEL(TRUE, INT(1), INT(2)) == 2);
        CHECK(SEL(FALSE, INT(1), INT(2)) == 1);
    }
    SUBCASE("LIMIT") {
        CHECK(LIMIT(INT(0), INT(5), INT(10)) == 5);
        CHECK(LIMIT(INT(0), INT(-5), INT(10)) == 0);
        CHECK(LIMIT(INT(0), INT(15), INT(10)) == 10);
    }
}

TEST_CASE("String operations") {
    SUBCASE("constructor") {
        STRING s("hello");
        CHECK(std::strcmp(s.data, "hello") == 0);
    }

    SUBCASE("default empty") {
        STRING empty;
        CHECK(empty.data[0] == '\0');
    }

    SUBCASE("equality") {
        CHECK(STRING("abc") == STRING("abc"));
        CHECK(STRING("abc") != STRING("xyz"));
    }

    SUBCASE("CONCAT") {
        STRING r = CONCAT(STRING("hello "), STRING("world"));
        CHECK(std::strcmp(r.data, "hello world") == 0);
    }

    SUBCASE("LEN") {
        CHECK(LEN(STRING("hello")) == 5);
        CHECK(LEN(STRING("")) == 0);
    }

    SUBCASE("STR_EQ/NE") {
        CHECK(STR_EQ(STRING("a"), STRING("a")) == TRUE);
        CHECK(STR_NE(STRING("a"), STRING("b")) == TRUE);
    }

    SUBCASE("LEFT") {
        CHECK(std::strcmp(LEFT(STRING("hello"), 3).data, "hel") == 0);
    }

    SUBCASE("RIGHT") {
        CHECK(std::strcmp(RIGHT(STRING("hello"), 3).data, "llo") == 0);
    }

    SUBCASE("MID") {
        CHECK(std::strcmp(MID(STRING("hello"), 3, 2).data, "ell") == 0);
    }
}

TEST_CASE("TIME helpers") {
    CHECK(T_ms(100) == 100000);
    CHECK(T_us(500) == 500);
    CHECK(T_s(1) == 1000000);
    CHECK(T_eq(T_ms(10), T_us(10000)) == TRUE);
    CHECK(T_lt(T_ms(1), T_ms(2)) == TRUE);
    CHECK(T_gt(T_ms(2), T_ms(1)) == TRUE);
}

TEST_CASE("PLC_Array") {
    PLC_Array<INT, 5> arr;

    SUBCASE("size") { CHECK(arr.size() == 5); }
    SUBCASE("default init") { CHECK(arr[0] == 0); }

    SUBCASE("write and read") {
        arr[0] = 10;
        arr[1] = 20;
        CHECK(arr[0] == 10);
        CHECK(arr[1] == 20);
    }

    SUBCASE("at() normal") {
        arr[0] = 10;
        CHECK(arr.at(0) == 10);
    }

    SUBCASE("at() OOB") {
        CHECK(arr.at(10) == arr[4]);
    }

    SUBCASE("fill") {
        arr.fill(42);
        CHECK(arr[0] == 42);
        CHECK(arr[4] == 42);
    }

    SUBCASE("equality") {
        PLC_Array<INT, 5> arr2;
        arr2.fill(42);
        arr.fill(42);
        CHECK(arr == arr2);
    }
}

TEST_CASE("PLC_Subrange") {
    PLC_Subrange<INT, 0, 100> range;

    SUBCASE("default to low") { CHECK((INT)range == 0); }
    SUBCASE("in range") { range = 50; CHECK((INT)range == 50); }
    SUBCASE("clamp low") { range = -10; CHECK((INT)range == 0); }
    SUBCASE("clamp high") { range = 200; CHECK((INT)range == 100); }
    SUBCASE("add") {
        range = 30;
        auto r2 = range + 20;
        CHECK((INT)r2 == 50);
    }
}