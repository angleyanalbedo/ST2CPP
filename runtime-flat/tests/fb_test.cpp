#include "doctest.h"
#include "rt_plc.h"

using namespace rt_plc;

TEST_CASE("TON on-delay timer") {
    TON t;

    SUBCASE("initial state") {
        CHECK(t.Q == FALSE);
        CHECK(t.ET == 0);
    }

    SUBCASE("IN=TRUE starts timing") {
        t.IN = TRUE; t.PT = T_ms(100); t.update(T_ms(1));
        CHECK(t.Q == FALSE);
        CHECK(t.ET > 0);
    }

    SUBCASE("reaches PT sets Q") {
        t.IN = TRUE; t.PT = T_ms(100);
        for (int i = 0; i < 100; i++) t.update(T_ms(1));
        CHECK(t.Q == TRUE);
        CHECK(t.ET == T_ms(100));
    }

    SUBCASE("IN=FALSE resets") {
        t.IN = TRUE; t.PT = T_ms(100);
        for (int i = 0; i < 100; i++) t.update(T_ms(1));
        t.IN = FALSE; t.update(T_ms(1));
        CHECK(t.Q == FALSE);
        CHECK(t.ET == 0);
    }
}

TEST_CASE("TOF off-delay basic") {
    TOF t;
    t.IN = TRUE; t.PT = T_ms(100); t.update(T_ms(1));
    CHECK(t.Q == TRUE);
}

TEST_CASE("TOF off-delay keep output") {
    TOF t;
    t.IN = TRUE; t.PT = T_ms(100); t.update(T_ms(1));
    t.IN = FALSE; t.update(T_ms(1));
    CHECK(t.Q == TRUE);
    CHECK(t.ET > 0);
}

TEST_CASE("TOF off-delay clears output") {
    TOF t;
    t.IN = FALSE; t.PT = T_ms(100);
    for (int i = 0; i < 100; i++) t.update(T_ms(1));
    CHECK(t.Q == FALSE);
}

TEST_CASE("TP pulse timer") {
    TP t;

    SUBCASE("rising edge triggers pulse") {
        t.IN = TRUE; t.PT = T_ms(50); t.update(T_ms(1));
        CHECK(t.Q == TRUE);
    }

    SUBCASE("pulse ends after PT") {
        t.IN = TRUE; t.PT = T_ms(50);
        for (int i = 0; i < 50; i++) t.update(T_ms(1));
        CHECK(t.Q == FALSE);
    }

    SUBCASE("no re-trigger during pulse") {
        t.IN = TRUE; t.PT = T_ms(50); t.update(T_ms(1));
        t.IN = FALSE; t.update(T_ms(1));
        t.IN = TRUE; t.update(T_ms(1));
        CHECK(t.Q == TRUE);
    }
}

TEST_CASE("CTU up counter") {
    CTU c;

    SUBCASE("initial CV is 0") {
        CHECK(c.CV == 0);
    }

    SUBCASE("counts rising edges") {
        c.CU = TRUE; c.R = FALSE; c.PV = 5; c.update(0);
        c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0);
        CHECK(c.CV == 2);
    }

    SUBCASE("Q when CV >= PV") {
        c.CU = TRUE; c.R = FALSE; c.PV = 5;
        c.update(0); c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0); c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0); c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0); c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0);
        CHECK(c.CV == 5);
        CHECK(c.Q == TRUE);
    }

    SUBCASE("RESET clears CV") {
        c.CU = TRUE; c.R = FALSE; c.PV = 5; c.update(0);
        c.CU = TRUE; c.update(0);
        c.R = TRUE; c.CU = FALSE; c.update(0);
        CHECK(c.CV == 0);
        CHECK(c.Q == FALSE);
    }
}

TEST_CASE("CTD down counter") {
    CTD c;

    SUBCASE("LOAD sets CV") {
        c.LD = TRUE; c.PV = 5; c.update(0);
        CHECK(c.CV == 5);
    }

    SUBCASE("counts down") {
        c.LD = TRUE; c.PV = 5; c.update(0);
        c.LD = FALSE; c.CD = TRUE; c.update(0);
        c.CD = FALSE; c.update(0);
        c.CD = TRUE; c.update(0);
        c.CD = FALSE; c.update(0);
        CHECK(c.CV == 3);
    }

    SUBCASE("Q when CV <= 0") {
        c.LD = TRUE; c.PV = 3; c.update(0);
        c.LD = FALSE; c.CD = TRUE; c.update(0); c.CD = FALSE; c.update(0);
        c.CD = TRUE; c.update(0); c.CD = FALSE; c.update(0);
        c.CD = TRUE; c.update(0); c.CD = FALSE; c.update(0);
        CHECK(c.CV == 0);
        CHECK(c.Q == TRUE);
    }
}

TEST_CASE("CTUD up-down counter") {
    CTUD c;

    SUBCASE("initial state") {
        CHECK(c.CV == 0);
        CHECK(c.QU == FALSE);
        CHECK(c.QD == FALSE);
    }

    SUBCASE("counts up then down") {
        c.PV = 3;
        c.CU = TRUE; c.update(0); c.CU = FALSE; c.update(0);
        c.CU = TRUE; c.update(0); c.CU = FALSE; c.update(0);
        c.CD = TRUE; c.CU = FALSE; c.update(0); c.CD = FALSE; c.update(0);
        CHECK(c.CV == 1);
    }

    SUBCASE("RESET clears") {
        c.PV = 3;
        c.CU = TRUE; c.update(0);
        c.CU = TRUE; c.update(0);
        c.R = TRUE; c.CU = FALSE; c.update(0);
        CHECK(c.CV == 0);
    }

    SUBCASE("LOAD sets PV") {
        c.LD = TRUE; c.PV = 5; c.R = FALSE; c.update(0);
        CHECK(c.CV == 5);
    }
}

TEST_CASE("R_TRIG rising edge") {
    R_TRIG rt;

    SUBCASE("no edge on FALSE") {
        rt.CLK = FALSE; rt.update(0);
        CHECK(rt.Q == FALSE);
    }

    SUBCASE("rising edge detected") {
        rt.CLK = TRUE; rt.update(0);
        CHECK(rt.Q == TRUE);
    }

    SUBCASE("no edge on sustained TRUE") {
        rt.CLK = TRUE; rt.update(0);
        rt.update(0);
        CHECK(rt.Q == FALSE);
    }

    SUBCASE("second rising edge") {
        rt.CLK = TRUE; rt.update(0);
        rt.CLK = FALSE; rt.update(0);
        rt.CLK = TRUE; rt.update(0);
        CHECK(rt.Q == TRUE);
    }
}

TEST_CASE("F_TRIG falling edge") {
    F_TRIG ft;

    SUBCASE("no edge on TRUE") {
        ft.CLK = TRUE; ft.update(0);
        CHECK(ft.Q == FALSE);
    }

    SUBCASE("falling edge detected") {
        ft.CLK = FALSE; ft.update(0);
        CHECK(ft.Q == TRUE);
    }

    SUBCASE("no edge on sustained FALSE") {
        ft.CLK = FALSE; ft.update(0);
        ft.update(0);
        CHECK(ft.Q == FALSE);
    }
}

TEST_CASE("SR set-dominant flip-flop") {
    SR sr;

    SUBCASE("initial Q1 is FALSE") {
        CHECK(sr.Q1 == FALSE);
    }

    SUBCASE("S1 sets Q1") {
        sr.S1 = TRUE; sr.R = FALSE; sr.update(0);
        CHECK(sr.Q1 == TRUE);
    }

    SUBCASE("R resets Q1") {
        sr.S1 = TRUE; sr.update(0);
        sr.S1 = FALSE; sr.R = TRUE; sr.update(0);
        CHECK(sr.Q1 == FALSE);
    }

    SUBCASE("R dominates S1") {
        sr.S1 = TRUE; sr.R = TRUE; sr.update(0);
        CHECK(sr.Q1 == FALSE);
    }
}

TEST_CASE("RS reset-dominant flip-flop") {
    RS rs;

    SUBCASE("S sets Q1") {
        rs.S = TRUE; rs.R1 = FALSE; rs.update(0);
        CHECK(rs.Q1 == TRUE);
    }
}
