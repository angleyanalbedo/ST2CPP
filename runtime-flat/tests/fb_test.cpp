#include "doctest.h"
#include "rt_plc.h"

using namespace rt_plc;

TEST_CASE("TON on-delay timer") {
    TON t;

    SUBCASE("initial state") {
        CHECK(t.output == FALSE);
        CHECK(t.elapsed == 0);
    }

    SUBCASE("input=TRUE starts timing") {
        t.update(TRUE, T_ms(100), T_ms(1));
        CHECK(t.output == FALSE);
        CHECK(t.elapsed > 0);
    }

    SUBCASE("reaches PT sets output") {
        for (int i = 0; i < 100; i++) t.update(TRUE, T_ms(100), T_ms(1));
        CHECK(t.output == TRUE);
        CHECK(t.elapsed == T_ms(100));
    }

    SUBCASE("input=FALSE resets") {
        for (int i = 0; i < 100; i++) t.update(TRUE, T_ms(100), T_ms(1));
        t.update(FALSE, T_ms(100), T_ms(1));
        CHECK(t.output == FALSE);
        CHECK(t.elapsed == 0);
    }
}

TEST_CASE("TOF off-delay basic") {
    TOF t;
    t.update(TRUE, T_ms(100), T_ms(1));
    CHECK(t.output == TRUE);
}

TEST_CASE("TOF off-delay keep output") {
    TOF t;
    t.update(TRUE, T_ms(100), T_ms(1));
    t.update(FALSE, T_ms(100), T_ms(1));
    CHECK(t.output == TRUE);
    CHECK(t.elapsed > 0);
}

TEST_CASE("TOF off-delay clears output") {
    TOF t;
    for (int i = 0; i < 100; i++) t.update(FALSE, T_ms(100), T_ms(1));
    CHECK(t.output == FALSE);
}

TEST_CASE("TP pulse timer") {
    TP t;

    SUBCASE("rising edge triggers pulse") {
        t.update(TRUE, T_ms(50), T_ms(1));
        CHECK(t.output == TRUE);
    }

    SUBCASE("pulse ends after PT") {
        for (int i = 0; i < 50; i++) t.update(TRUE, T_ms(50), T_ms(1));
        CHECK(t.output == FALSE);
    }

    SUBCASE("no re-trigger during pulse") {
        t.update(TRUE, T_ms(50), T_ms(1));
        t.update(FALSE, T_ms(50), T_ms(1));
        t.update(TRUE, T_ms(50), T_ms(1));
        CHECK(t.output == TRUE);
    }
}

TEST_CASE("CTU up counter") {
    CTU c;

    SUBCASE("initial count is 0") {
        CHECK(c.count == 0);
    }

    SUBCASE("counts rising edges") {
        c.update(TRUE, FALSE, 5);
        c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5);
        CHECK(c.count == 2);
    }

    SUBCASE("output when count >= PV") {
        c.update(TRUE, FALSE, 5); c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5); c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5); c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5); c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5);
        CHECK(c.count == 5);
        CHECK(c.output == TRUE);
    }

    SUBCASE("RESET clears count") {
        c.update(TRUE, FALSE, 5);
        c.update(TRUE, FALSE, 5);
        c.update(FALSE, TRUE, 5);
        CHECK(c.count == 0);
        CHECK(c.output == FALSE);
    }
}

TEST_CASE("CTD down counter") {
    CTD c;

    SUBCASE("LOAD sets count") {
        c.update(FALSE, TRUE, 5);
        CHECK(c.count == 5);
    }

    SUBCASE("counts down") {
        c.update(FALSE, TRUE, 5);
        c.update(TRUE, FALSE, 5);
        c.update(FALSE, FALSE, 5);
        c.update(TRUE, FALSE, 5);
        c.update(FALSE, FALSE, 5);
        CHECK(c.count == 3);
    }

    SUBCASE("output when count <= 0") {
        c.update(FALSE, TRUE, 3);
        c.update(TRUE, FALSE, 3);
        c.update(FALSE, FALSE, 3);
        c.update(TRUE, FALSE, 3);
        c.update(FALSE, FALSE, 3);
        c.update(TRUE, FALSE, 3);
        c.update(FALSE, FALSE, 3);
        CHECK(c.count == 0);
        CHECK(c.output == TRUE);
    }
}

TEST_CASE("CTUD up-down counter") {
    CTUD c;

    SUBCASE("initial state") {
        CHECK(c.count == 0);
        CHECK(c.QU == FALSE);
        CHECK(c.QD == FALSE);
    }

    SUBCASE("counts up then down") {
        c.update(TRUE, FALSE, FALSE, FALSE, 3); c.update(FALSE, FALSE, FALSE, FALSE, 3);
        c.update(TRUE, FALSE, FALSE, FALSE, 3); c.update(FALSE, FALSE, FALSE, FALSE, 3);
        c.update(FALSE, TRUE, FALSE, FALSE, 3); c.update(FALSE, FALSE, FALSE, FALSE, 3);
        CHECK(c.count == 1);
    }

    SUBCASE("RESET clears") {
        c.update(TRUE, FALSE, FALSE, FALSE, 3);
        c.update(TRUE, FALSE, FALSE, FALSE, 3);
        c.update(FALSE, FALSE, TRUE, FALSE, 3);
        CHECK(c.count == 0);
    }

    SUBCASE("LOAD sets PV") {
        c.update(FALSE, FALSE, FALSE, TRUE, 5);
        CHECK(c.count == 5);
    }
}

TEST_CASE("R_TRIG rising edge") {
    R_TRIG rt;

    SUBCASE("no edge on FALSE") {
        CHECK(rt(FALSE) == FALSE);
    }

    SUBCASE("rising edge detected") {
        CHECK(rt(TRUE) == TRUE);
    }

    SUBCASE("no edge on sustained TRUE") {
        rt(TRUE);
        CHECK(rt(TRUE) == FALSE);
    }

    SUBCASE("second rising edge") {
        rt(TRUE);
        rt(FALSE);
        CHECK(rt(TRUE) == TRUE);
    }
}

TEST_CASE("F_TRIG falling edge") {
    F_TRIG ft;

    SUBCASE("no edge on TRUE") {
        CHECK(ft(TRUE) == FALSE);
    }

    SUBCASE("falling edge detected") {
        CHECK(ft(FALSE) == TRUE);
    }

    SUBCASE("no edge on sustained FALSE") {
        ft(FALSE);
        CHECK(ft(FALSE) == FALSE);
    }
}