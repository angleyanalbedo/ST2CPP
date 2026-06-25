#include "doctest.h"
#include "rt_plc.h"
#include "core/error_manager.h"

using namespace rt_plc;

TEST_CASE("ErrorManager safeDiv") {
    ErrorManager em;

    SUBCASE("int normal") {
        CHECK(em.safeDiv(10, 2, 1, 100, 0) == 5);
    }

    SUBCASE("int by zero returns 0") {
        CHECK(em.safeDiv(10, 0, 1, 101, 0) == 0);
    }

    SUBCASE("int by zero sets fatalMode") {
        em.safeDiv(10, 0, 1, 101, 0);
        CHECK(em.isFatal());
    }

    SUBCASE("float normal") {
        CHECK(em.safeDiv(10.0f, 2.0f, 1, 102, 0) == 5.0f);
    }

    SUBCASE("float by zero returns 0") {
        CHECK(em.safeDiv(10.0f, 0.0f, 1, 103, 0) == 0.0f);
    }

    SUBCASE("double normal") {
        CHECK(em.safeDiv(10.0, 2.0, 1, 104, 0) == 5.0);
    }

    SUBCASE("double by zero returns 0") {
        CHECK(em.safeDiv(10.0, 0.0, 1, 105, 0) == 0.0);
    }
}

TEST_CASE("ErrorManager safeMod") {
    ErrorManager em;

    SUBCASE("normal") {
        CHECK(em.safeMod(10, 3, 1, 200, 0) == 1);
    }

    SUBCASE("by zero returns 0") {
        CHECK(em.safeMod(10, 0, 1, 201, 0) == 0);
    }

    SUBCASE("by zero sets fatalMode") {
        em.safeMod(10, 0, 1, 201, 0);
        CHECK(em.isFatal());
    }
}

TEST_CASE("ErrorManager safeAdd/safeSub/safeMul") {
    ErrorManager em;

    SUBCASE("safeAdd normal") {
        CHECK(em.safeAdd(INT(10), INT(20), 1, 300, 0) == 30);
    }

    SUBCASE("safeAdd overflow detected") {
        em.safeAdd(INT(32767), INT(1), 1, 301, 0);
        CHECK(em.count() >= 1);
    }

    SUBCASE("safeSub normal") {
        CHECK(em.safeSub(INT(30), INT(10), 1, 302, 0) == 20);
    }

    SUBCASE("safeSub underflow detected") {
        em.safeSub(INT(-32768), INT(1), 1, 303, 0);
        CHECK(em.count() >= 1);
    }

    SUBCASE("safeMul normal") {
        CHECK(em.safeMul(INT(10), INT(5), 1, 304, 0) == 50);
    }

    SUBCASE("safeMul overflow detected") {
        em.safeMul(INT(1000), INT(1000), 1, 305, 0);
        CHECK(em.count() >= 1);
    }

    SUBCASE("safeMul by zero") {
        CHECK(em.safeMul(INT(100), INT(0), 1, 306, 0) == 0);
    }
}

TEST_CASE("ErrorManager safeArrayAt") {
    ErrorManager em;
    int arr[5] = {10, 20, 30, 40, 50};

    SUBCASE("static array ok") {
        CHECK(em.safeArrayAt(arr, 0, 1, 400, 0) == 10);
        CHECK(em.safeArrayAt(arr, 4, 1, 401, 0) == 50);
    }

    SUBCASE("static array OOB") {
        CHECK(em.safeArrayAt(arr, 10, 1, 402, 0) == 50);
    }

    SUBCASE("pointer array ok") {
        CHECK(em.safeArrayAt(arr, 2, 5, 1, 403, 0) == 30);
    }

    SUBCASE("pointer array OOB") {
        CHECK(em.safeArrayAt(arr, 10, 5, 1, 404, 0) == 50);
    }
}

TEST_CASE("ErrorManager report and log") {
    ErrorManager em;

    SUBCASE("report increments count") {
        em.report(ErrorCode::DIV_BY_ZERO, 1, 500, "test", 0);
        CHECK(em.count() == 1);
        em.report(ErrorCode::DIV_BY_ZERO, 1, 501, "test2", 0);
        CHECK(em.count() == 2);
    }

    SUBCASE("lastError after report") {
        em.report(ErrorCode::DIV_BY_ZERO, 1, 500, "test", 0);
        CHECK(em.lastError == ErrorCode::DIV_BY_ZERO);
    }

    SUBCASE("reset clears state") {
        em.report(ErrorCode::DIV_BY_ZERO, 1, 500, "test", 0);
        em.reset();
        CHECK(em.count() == 0);
        CHECK(em.lastError == ErrorCode::NONE);
        CHECK(!em.isFatal());
    }
}

TEST_CASE("ErrorManager fatal codes") {
    SUBCASE("DIV_BY_ZERO is fatal") {
        ErrorManager em;
        em.report(ErrorCode::DIV_BY_ZERO, 1, 600, "", 0);
        CHECK(em.isFatal());
    }

    SUBCASE("ARRAY_OUT_OF_BOUNDS is fatal") {
        ErrorManager em;
        em.report(ErrorCode::ARRAY_OUT_OF_BOUNDS, 1, 601, "", 0);
        CHECK(em.isFatal());
    }

    SUBCASE("NULL_POINTER is fatal") {
        ErrorManager em;
        em.report(ErrorCode::NULL_POINTER, 1, 602, "", 0);
        CHECK(em.isFatal());
    }

    SUBCASE("STACK_OVERFLOW is fatal") {
        ErrorManager em;
        em.report(ErrorCode::STACK_OVERFLOW, 1, 603, "", 0);
        CHECK(em.isFatal());
    }

    SUBCASE("INT_OVERFLOW is NOT fatal") {
        ErrorManager em;
        em.report(ErrorCode::INT_OVERFLOW, 1, 604, "", 0);
        CHECK(!em.isFatal());
    }
}

static bool handler_called = false;
static void test_handler(const ErrorEntry&) { handler_called = true; }

TEST_CASE("ErrorManager handler") {
    ErrorManager em;
    handler_called = false;
    em.setHandler(test_handler);

    SUBCASE("handler called on report") {
        em.report(ErrorCode::DIV_BY_ZERO, 1, 700, "test", 0);
        CHECK(handler_called);
    }
}

TEST_CASE("ErrorManager operand recording") {
    ErrorManager em;

    SUBCASE("safeDiv records operands") {
        em.safeDiv(INT(100), INT(0), 1, 800, 0);
        CHECK(em.log[0].operandA == 100);
        CHECK(em.log[0].operandB == 0);
    }

    SUBCASE("safeAdd records operands") {
        em.safeAdd(INT(32767), INT(1), 1, 801, 0);
        CHECK(em.log[0].operandA == 32767);
        CHECK(em.log[0].operandB == 1);
    }
}