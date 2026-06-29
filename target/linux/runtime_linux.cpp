/**
 * runtime_linux.cpp — Linux 通用 PLC 运行时
 *
 * 不依赖树莓派 GPIO，纯 Linux 用户态高精度周期调度。
 * 通用 x86_64 / aarch64，服务器或工控机。
 *
 * 精度指标（PREEMPT_RT 内核）：
 *   - timerfd + SCHED_FIFO：抖动 < 20us
 *   - 标准内核：抖动 < 500us
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cmath>
#include <algorithm>

#include <time.h>
#include <signal.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/timerfd.h>
#include <poll.h>

using namespace rt_plc;

// ═══ 配置 ═══
static int64_t   cycleUs       = 1000;
static int       rtPriority    = 90;
static int       diagIntervalS = 5;
static bool      jitterOnly    = false;
static bool      running       = true;

static Scheduler sched;
static CompositeTCI compositeTCI;
static bool      initialized   = false;

// ═══ 抖动统计 ═══
static struct {
    int64_t intervalUs;
    int64_t prevUs;
    int64_t minJitter, maxJitter;
    int64_t sumJitter;
    int64_t sumSquareJitter;
    uint64_t count;
    bool ready;
} jitter = {1000, 0, 999999, -999999, 0, 0, 0, false};

static void jitterReset(int64_t us) {
    jitter.intervalUs = us;
    jitter.prevUs = 0; jitter.minJitter = 999999; jitter.maxJitter = -999999;
    jitter.sumJitter = 0; jitter.sumSquareJitter = 0; jitter.count = 0; jitter.ready = false;
}

static void jitterSample(int64_t now) {
    if (!jitter.ready) { jitter.prevUs = now; jitter.ready = true; return; }
    int64_t actual = now - jitter.prevUs;
    int64_t jit = actual - jitter.intervalUs;
    jitter.prevUs = now;
    jitter.minJitter = std::min(jitter.minJitter, jit);
    jitter.maxJitter = std::max(jitter.maxJitter, jit);
    jitter.sumJitter += jit;
    jitter.sumSquareJitter += jit * jit;
    jitter.count++;
}

static void printJitterStats() {
    platform::logInfo("\n=== Jitter Statistics ===\n");
    platform::logInfo("  Samples  : %llu\n", (unsigned long long)jitter.count);
    if (jitter.count > 0) {
        int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
        double variance = jitter.count > 1
            ? (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg
            : 0;
        platform::logInfo("  Min      : %lld us\n", (long long)jitter.minJitter);
        platform::logInfo("  Max      : %lld us\n", (long long)jitter.maxJitter);
        platform::logInfo("  Avg      : %lld us\n", (long long)avg);
        platform::logInfo("  Stddev   : %lld us\n",
            (long long)(variance > 0 ? (int64_t)std::sqrt(variance) : 0));
        platform::logInfo("  Target   : %lld us\n", (long long)jitter.intervalUs);
    }
    platform::logInfo("==========================\n");
}

// ═── PLC ═──
extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

static void plcInit() {
    configureRuntime(sched, compositeTCI);
    if (cycleUs != sched.baseCycleTime)
        sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;
    sched.start(StartupMode::COLD);
    initialized = true;
    platform::logInfo("PLC Runtime: cycle=%lldus\n", (long long)cycleUs);
}

static void plcTick() {
    if (initialized) sched.tick();
}

// ═══ 信号处理 ═══
static void sigHandler(int) { running = false; }

// ═══ 主函数 ═══
int main(int argc, char* argv[]) {
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[++i]);
        } else if (strcmp(argv[i], "--rt-prio") == 0 && i + 1 < argc) {
            rtPriority = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--diag-interval") == 0 && i + 1 < argc) {
            diagIntervalS = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--jitter-only") == 0) {
            jitterOnly = true;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [options]\n", argv[0]);
            printf("  --cycle-us <us>        PLC cycle (us, default 1000)\n");
            printf("  --rt-prio <1-99>       SCHED_FIFO priority (default 90)\n");
            printf("  --diag-interval <s>    Diagnostics interval (s, default 5)\n");
            printf("  --jitter-only          Timer jitter test only\n");
            printf("\nNote: Linux realtime requires PREEMPT_RT kernel\n");
            return 0;
        }
    }

    signal(SIGINT, sigHandler);
    signal(SIGTERM, sigHandler);

    struct sched_param sp = {};
    sp.sched_priority = rtPriority;
    if (sched_setscheduler(0, SCHED_FIFO, &sp) == 0) {
        mlockall(MCL_CURRENT | MCL_FUTURE);
        platform::logInfo("RT priority: SCHED_FIFO/%d\n", rtPriority);
    } else {
        platform::logInfo("Warning: sched_setscheduler failed, run as root?\n");
    }

    if (!jitterOnly) plcInit();
    else platform::logInfo("Jitter-only mode\n");

    int tfd = timerfd_create(CLOCK_MONOTONIC, TFD_NONBLOCK);
    if (tfd < 0) {
        platform::logErr("timerfd_create failed: %s\n", strerror(errno));
        return 1;
    }
    struct itimerspec its = {};
    its.it_interval.tv_sec  = cycleUs / 1000000;
    its.it_interval.tv_nsec = (cycleUs % 1000000) * 1000;
    its.it_value = its.it_interval;
    timerfd_settime(tfd, 0, &its, nullptr);
    jitterReset(cycleUs);

    platform::logInfo("Timer started: %lldus, press Ctrl+C to stop\n", (long long)cycleUs);

    struct pollfd pfd = {tfd, POLLIN, 0};
    int64_t lastDiag = 0;
    uint64_t tickCount = 0;

    while (running) {
        int ret = poll(&pfd, 1, 100);
        if (ret > 0 && (pfd.revents & POLLIN)) {
            uint64_t exp;
            read(tfd, &exp, sizeof(exp));

            int64_t now = platform::steadyUs();
            jitterSample(now);
            plcTick();
            tickCount += exp;

            if (now - lastDiag > (int64_t)diagIntervalS * 1000000) {
                lastDiag = now;
                platform::logInfo("[DIAG] ticks=%llu  uptime=%lldms\n",
                    (unsigned long long)tickCount,
                    (long long)((now - jitter.prevUs + jitter.intervalUs) / 1000));
                if (jitter.count > 0) {
                    int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
                    platform::logInfo("[JITTER] min=%lld max=%lld avg=%lld stddev=%lld (samples=%llu)\n",
                        (long long)jitter.minJitter, (long long)jitter.maxJitter,
                        (long long)avg,
                        (long long)(jitter.count > 1 ? (int64_t)std::sqrt(
                            (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg) : 0),
                        (unsigned long long)jitter.count);
                }
            }
        } else if (ret < 0 && errno != EINTR) {
            platform::logErr("poll error: %s\n", strerror(errno));
            break;
        }
    }

    close(tfd);
    if (!jitterOnly) sched.stop();
    printJitterStats();
    platform::logInfo("Stopped. Total ticks: %llu\n", (unsigned long long)tickCount);
    return 0;
}
