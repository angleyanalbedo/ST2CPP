#include "rt_runtime.h"
#include "rt_plc.h"

#include <time.h>
#include <signal.h>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <pthread.h>
#include <cstdio>
#include <cstdarg>
#include <cstring>
#include <cerrno>

using namespace rt_plc::platform;

extern "C" void plc_runtime_tick();

static volatile int64_t tick_start_us = 0;
static volatile uint32_t tick_count = 0;
static volatile int64_t jitter_last_us = 0;
static volatile int64_t jitter_sum = 0;
static volatile int64_t jitter_min = 999999;
static volatile int64_t jitter_max = 0;
static volatile uint64_t jitter_count = 0;
static volatile int64_t jitter_target_us = 1000;

extern "C" void plc_bpi_jitter_set_target(int64_t targetUs) {
    jitter_target_us = targetUs;
}

extern "C" void plc_bpi_jitter_init() {
    tick_start_us = steadyUs();
    jitter_last_us = tick_start_us;
    tick_count = 0;
    jitter_min = 999999;
    jitter_max = 0;
    jitter_sum = 0;
    jitter_count = 0;
}

extern "C" void plc_bpi_jitter_sample() {
    int64_t now = steadyUs();
    int64_t delta = now - jitter_last_us;
    jitter_last_us = now;

    int64_t j = delta - jitter_target_us;
    if (j < 0) j = -j;

    if (tick_count > 0 && delta > 0 && delta < 100000) {
        jitter_sum += j;
        if (j < jitter_min) jitter_min = j;
        if (j > jitter_max) jitter_max = j;
        jitter_count++;
    }
    tick_count++;
}

extern "C" int plc_bpi_set_rt_priority(int priority) {
    struct sched_param param = {};
    param.sched_priority = priority;

    if (sched_setscheduler(0, SCHED_FIFO, &param) != 0) {
        logErr("sched_setscheduler failed: %s (need root?)\n", strerror(errno));
        return -1;
    }

    mlockall(MCL_CURRENT | MCL_FUTURE);
    logInfo("RT priority set: SCHED_FIFO/%d\n", priority);
    return 0;
}

extern "C" uint32_t plc_bpi_get_tick_count() {
    return tick_count;
}

extern "C" int64_t plc_bpi_get_uptime_us() {
    return steadyUs() - tick_start_us;
}

extern "C" void plc_bpi_print_jitter_stats() {
    logInfo("\n=== Jitter Statistics (BPI-F3) ===\n");
    logInfo("  Samples    : %llu\n", (unsigned long long)jitter_count);
    if (jitter_count > 0) {
        logInfo("  Min jitter : %lld us\n", (long long)jitter_min);
        logInfo("  Max jitter : %lld us\n", (long long)jitter_max);
        logInfo("  Avg jitter : %lld us\n", (long long)(jitter_sum / jitter_count));
    }
    logInfo("=================================\n");
}
