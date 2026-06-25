#include "doctest.h"
#include "rt_plc.h"
#include "core/registry.h"
#include "core/error_manager.h"
#include <cstdio>
#include <cstring>

using namespace rt_plc;

static int gvl_value = 0;

static void test_init(GVL&, ProcessImage&) { gvl_value = 10; }
static void test_cyclic(GVL&, ProcessImage&, TIME) { gvl_value = gvl_value * 2; }
static void test_pre(GVL&, ProcessImage&) { gvl_value = gvl_value + 1; }
static void test_post(GVL&, ProcessImage&) { gvl_value = gvl_value + 100; }

TEST_CASE("POURegistry") {
    POURegistry reg;

    SUBCASE("empty initially") {
        CHECK(reg.count() == 0);
    }

    SUBCASE("add callbacks") {
        reg.add("MAIN", test_init, test_cyclic, test_pre, test_post);
        CHECK(reg.count() == 1);
    }

    SUBCASE("lookup existing") {
        reg.add("MAIN", test_init, test_cyclic, test_pre, test_post);
        const POUCallbacks* cbs = reg.lookup("MAIN");
        CHECK(cbs != nullptr);
        CHECK(cbs->init == test_init);
        CHECK(cbs->cyclic == test_cyclic);
        CHECK(cbs->pre == test_pre);
        CHECK(cbs->post == test_post);
    }

    SUBCASE("lookup non-existing") {
        CHECK(reg.lookup("NOEXIST") == nullptr);
    }

    SUBCASE("add multiple") {
        reg.add("MAIN", test_init, test_cyclic);
        reg.add("SUB", test_init, test_cyclic);
        CHECK(reg.count() == 2);
    }

    SUBCASE("cap at MAX_ENTRIES=64") {
        for (int i = 0; i < 70; i++) {
            char name[16];
            snprintf(name, sizeof(name), "POU_%d", i);
            reg.add(name, nullptr, nullptr);
        }
        CHECK(reg.count() == 64);
    }
}

TEST_CASE("ProgramInstance") {
    GVL gvl;
    gvl.clear();
    ProcessImage io;

    ProgramInstance prog;
    std::strcpy(prog.name, "TEST");
    prog.initFunc = test_init;
    prog.cyclicFunc = test_cyclic;
    prog.preFunc = test_pre;
    prog.postFunc = test_post;

    SUBCASE("initial phase") {
        CHECK(prog.phase == ProgramPhase::UNINIT);
    }

    SUBCASE("doInit") {
        gvl_value = 0;
        prog.doInit(gvl, io);
        CHECK(gvl_value == 10);
    }

    SUBCASE("doPre") {
        gvl_value = 5;
        prog.doPre(gvl, io);
        CHECK(gvl_value == 6);
    }

    SUBCASE("doCyclic") {
        gvl_value = 5;
        prog.doCyclic(gvl, io, 0);
        CHECK(gvl_value == 10);
    }

    SUBCASE("doPost") {
        gvl_value = 5;
        prog.doPost(gvl, io);
        CHECK(gvl_value == 105);
    }
}