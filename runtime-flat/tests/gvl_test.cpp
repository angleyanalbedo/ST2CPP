#include "doctest.h"
#include "rt_plc.h"
#include "core/gvl.h"

using namespace rt_plc;

TEST_CASE("GVL basic read/write") {
    GVL gvl;
    gvl.clear();

    SUBCASE("INT write/read") {
        gvl.write<INT>(0, 42);
        CHECK(gvl.read<INT>(0) == 42);
    }

    SUBCASE("REAL write/read") {
        gvl.write<REAL>(4, 3.14f);
        CHECK(gvl.read<REAL>(4) == 3.14f);
    }

    SUBCASE("BOOL write/read") {
        gvl.write<BOOL>(8, TRUE);
        CHECK(gvl.read<BOOL>(8) == TRUE);
    }

    SUBCASE("DINT write/read") {
        gvl.write<DINT>(12, 123456);
        CHECK(gvl.read<DINT>(12) == 123456);
    }

    SUBCASE("STRING write/read") {
        STRING str("hello");
        gvl.write<STRING>(16, str);
        CHECK(std::strcmp(gvl.read<STRING>(16).data, "hello") == 0);
    }

    SUBCASE("multiple INTs at different offsets") {
        gvl.write<INT>(0, 10);
        gvl.write<INT>(2, 20);
        gvl.write<INT>(4, 30);
        CHECK(gvl.read<INT>(0) == 10);
        CHECK(gvl.read<INT>(2) == 20);
        CHECK(gvl.read<INT>(4) == 30);
    }

    SUBCASE("usedBytes tracks highWaterMark") {
        CHECK(gvl.usedBytes() >= 0);
    }
}

TEST_CASE("GVL bounds safety") {
    GVL gvl;
    gvl.clear();

    SUBCASE("read out of bounds returns default") {
        gvl.write<INT>(0, 42);
        CHECK(gvl.read<INT>(65536) == 0);
    }

    SUBCASE("write out of bounds does not crash") {
        gvl.write<INT>(65536, 99);
        CHECK(true);
    }

    SUBCASE("read near boundary ok") {
        gvl.write<INT>(65534, 77);
        CHECK(gvl.read<INT>(65534) == 77);
    }

    SUBCASE("read one byte past boundary") {
        CHECK(gvl.read<INT>(65535) == 0);
    }

    SUBCASE("ptr out of bounds returns null") {
        CHECK(gvl.ptr<INT>(65536) == nullptr);
    }
}

TEST_CASE("GVL RETAIN region") {
    GVL gvl;
    gvl.clear();

    SUBCASE("setRetainRegion valid range") {
        gvl.setRetainRegion(100, 200);
        CHECK(gvl.retainStart == 100);
        CHECK(gvl.retainEnd == 200);
    }

    SUBCASE("clearNonRetain preserves RETAIN") {
        gvl.setRetainRegion(100, 200);
        gvl.write<INT>(50, 10);
        gvl.write<INT>(150, 20);
        gvl.clearNonRetain();
        CHECK(gvl.read<INT>(50) == 0);
        CHECK(gvl.read<INT>(150) == 20);
    }

    SUBCASE("clear clears everything including RETAIN") {
        gvl.setRetainRegion(100, 200);
        gvl.write<INT>(150, 20);
        gvl.clear();
        CHECK(gvl.read<INT>(150) == 0);
    }
}

TEST_CASE("GVL safeArrayAt") {
    GVL gvl;
    gvl.clear();

    SUBCASE("normal access") {
        gvl.write<INT>(0, 10);
        gvl.write<INT>(2, 20);
        gvl.write<INT>(4, 30);
        CHECK(gvl.safeArrayAt<INT>(0, 0, 3) == 10);
        CHECK(gvl.safeArrayAt<INT>(0, 1, 3) == 20);
        CHECK(gvl.safeArrayAt<INT>(0, 2, 3) == 30);
    }

    SUBCASE("out of bounds returns last element") {
        gvl.write<INT>(0, 10);
        gvl.write<INT>(2, 20);
        gvl.write<INT>(4, 30);
        CHECK(gvl.safeArrayAt<INT>(0, 100, 3) == 30);
    }

    SUBCASE("write through reference") {
        gvl.write<INT>(0, 10);
        gvl.write<INT>(2, 20);
        gvl.safeArrayAt<INT>(0, 1, 3) = 99;
        CHECK(gvl.read<INT>(2) == 99);
    }

    SUBCASE("with null ptr") {
        CHECK(gvl.safeArrayAt<INT>(65536, 0, 1) == 0);
    }
}

TEST_CASE("GVL ptr access") {
    GVL gvl;
    gvl.clear();

    SUBCASE("ptr write then read") {
        INT* p = gvl.ptr<INT>(0);
        *p = 42;
        CHECK(gvl.read<INT>(0) == 42);
    }

    SUBCASE("const ptr read") {
        gvl.write<INT>(0, 42);
        const INT* cp = gvl.ptr<INT>(0);
        CHECK(*cp == 42);
    }

    SUBCASE("ptr at non-zero offset") {
        gvl.write<REAL>(8, 2.5f);
        REAL* rp = gvl.ptr<REAL>(8);
        CHECK(*rp == 2.5f);
    }
}

TEST_CASE("GVL clear") {
    GVL gvl;
    gvl.clear();

    SUBCASE("clear sets memory to zero") {
        gvl.write<INT>(0, 42);
        gvl.write<REAL>(4, 3.14f);
        gvl.clear();
        CHECK(gvl.read<INT>(0) == 0);
        CHECK(gvl.read<REAL>(4) == 0.0f);
    }
}

TEST_CASE("GVL multi-type read/write") {
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

    CHECK(gvl.read<SINT>(0) == 127);
    CHECK(gvl.read<USINT>(1) == 255);
    CHECK(gvl.read<INT>(2) == 32767);
    CHECK(gvl.read<UINT>(4) == 65535);
    CHECK(gvl.read<DINT>(8) == 2147483647);
    CHECK(gvl.read<UDINT>(12) == 4294967295U);
    CHECK(gvl.read<LINT>(16) == 9223372036854775807LL);
    CHECK(gvl.read<ULINT>(24) == 18446744073709551615ULL);
    CHECK(gvl.read<REAL>(32) == 1.5f);
    CHECK(gvl.read<LREAL>(40) == 2.718281828);
}