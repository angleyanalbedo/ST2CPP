/**
 * runtime_rk3588_rt.cpp — RK3588 Xenomai 3 Cobalt 硬实时 PLC Runtime
 *
 * 与 runtime_rk3588.cpp 功能相同，但用 Xenomai Cobalt POSIX 包装替代 Linux timerfd：
 *   timerfd + poll()  ->  clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME)
 *   sched_setscheduler -> pthread_setschedparam(SCHED_FIFO)
 *
 * 抖动测量：clock_gettime(CLOCK_MONOTONIC) 取实际唤醒时间
 *
 * 编译：
 *   g++ -I/usr/xenomai/include/cobalt -I/usr/xenomai/include -D_GNU_SOURCE \
 *       -D_REENTRANT -D__COBALT__ -DRT_PLATFORM_LINUX -DRT_TARGET_RK3588 \
 *       ... \
 *       $(/usr/xenomai/bin/xeno-config --ldflags --cobalt)
 */

#include "rt_runtime.h"
#include "rt_plc.h"

#include <algorithm>
#include <cerrno>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <pthread.h>
#include <sched.h>
#include <signal.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

using namespace rt_plc;

extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

static int64_t cycleUs = 1000;
static int rtPriority = 90;
static int cpuAffinity = 6;
static int diagIntervalS = 5;
static bool jitterOnly = false;
static bool warmStart = false;
static volatile sig_atomic_t running = 1;

static Scheduler sched;
static CompositeTCI compositeTCI;
static bool initialized = false;

struct JitterStats {
    int64_t intervalNs = 1000000;
    int64_t min = INT64_MAX;
    int64_t max = INT64_MIN;
    int64_t sum = 0;
    int64_t sumSq = 0;
    uint64_t count = 0;

    void reset(int64_t iv) { intervalNs = iv; min = INT64_MAX; max = INT64_MIN; sum = sumSq = 0; count = 0; }
    void sample(int64_t actualNs, int64_t expectedNs) {
        int64_t delta = actualNs - expectedNs;
        if (delta < min) min = delta;
        if (delta > max) max = delta;
        sum += delta;
        sumSq += delta * delta;
        count++;
    }
};

static JitterStats jitter;
static int64_t firstActualNs = 0;

static void resetJitter(int64_t cycleNs) { jitter.reset(cycleNs); }

static void sampleJitter(int64_t actualNs) {
    if (jitter.count == 0) {
        firstActualNs = actualNs;
        jitter.sample(actualNs, actualNs);
        return;
    }
    int64_t expectedNs = firstActualNs + (int64_t)(jitter.count) * jitter.intervalNs;
    int64_t delta = actualNs - expectedNs;
    if (delta < jitter.min) jitter.min = delta;
    if (delta > jitter.max) jitter.max = delta;
    jitter.sum += delta;
    jitter.sumSq += delta * delta;
    jitter.count++;
}

static void printJitter() {
    if (jitter.count == 0) return;
    double avg = (double)jitter.sum / jitter.count;
    double variance = (double)jitter.sumSq / jitter.count - avg * avg;
    if (variance < 0) variance = 0;
    double std = sqrt(variance);
    platform::logInfo("[RK3588_RT][JITTER] samples=%llu min=%ldns max=%ldns avg=%.0fns std=%.0fns target=%lldus\n",
        (unsigned long long)jitter.count,
        (long)jitter.min, (long)jitter.max,
        avg, std,
        (long long)(jitter.intervalNs / 1000));
}

static void sigHandler(int) { running = 0; }

static bool setCpuAffinity(int cpu) {
    if (cpu < 0) return false;
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(cpu, &cpuset);
    pthread_t self = pthread_self();
    if (pthread_setaffinity_np(self, sizeof(cpuset), &cpuset) != 0) {
        platform::logErr("[RK3588_RT] pthread_setaffinity_np(cpu=%d) failed: %s\n", cpu, strerror(errno));
        return false;
    }
    platform::logInfo("[RK3588_RT] CPU affinity: cpu%d\n", cpu);
    return true;
}

static void printSnapshot() {
    if (!initialized) return;
    DiagSnapshot s = sched.snapshotDiag();
    platform::logInfo("[RK3588_RT][PLC] state=%u phase=%u ticks=%llu cycle=%lldus scans=%llu overruns=%llu watchdog=%d errors=%d fatal=%d tasks=%d tci=%d safeout=%d\n",
        (unsigned)s.systemState,
        (unsigned)s.currentPhase,
        (unsigned long long)s.totalTicks,
        (long long)s.baseCycleTime,
        (unsigned long long)s.totalScanCount,
        (unsigned long long)s.totalOverruns,
        s.watchdogTripped ? 1 : 0,
        s.errorCount,
        s.fatalMode ? 1 : 0,
        s.taskCount,
        s.tciBound ? 1 : 0,
        s.safeOutputsEnabled ? 1 : 0);
}

static void printHelp(const char* argv0) {
    printf("Usage: %s [options]\n", argv0);
    printf("  --cycle-us N          PLC cycle in microseconds (default 1000)\n");
    printf("  --rt-prio N           SCHED_FIFO priority 1..99 (default 90)\n");
    printf("  --cpu N               CPU affinity, use -1 to disable (default 6)\n");
    printf("  --diag-interval N     diagnostics interval seconds (default 5)\n");
    printf("  --warm                start with WARM startup mode\n");
    printf("  --jitter-only         do not initialize PLC, only measure timer jitter\n");
    printf("  -h, --help            show this help\n");
}

int main(int argc, char** argv) {
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atoll(argv[++i]);
        } else if (strcmp(argv[i], "--rt-prio") == 0 && i + 1 < argc) {
            rtPriority = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--cpu") == 0 && i + 1 < argc) {
            cpuAffinity = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--diag-interval") == 0 && i + 1 < argc) {
            diagIntervalS = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--warm") == 0) {
            warmStart = true;
        } else if (strcmp(argv[i], "--jitter-only") == 0) {
            jitterOnly = true;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printHelp(argv[0]);
            return 0;
        }
    }

    if (cycleUs <= 0) {
        platform::logErr("[RK3588_RT] invalid cycle-us: %lld\n", (long long)cycleUs);
        return 2;
    }

    signal(SIGINT, sigHandler);
    signal(SIGTERM, sigHandler);

    // 锁内存 + 核绑定
    if (mlockall(MCL_CURRENT | MCL_FUTURE) != 0) {
        platform::logErr("[RK3588_RT] mlockall failed: %s\n", strerror(errno));
    }
    setCpuAffinity(cpuAffinity);

    // 设置 SCHED_FIFO 优先级（在 Xenomai Cobalt 下，这设置 RTDM 任务优先级）
    {
        pthread_t self = pthread_self();
        sched_param sp = {};
        sp.sched_priority = rtPriority;
        int policy = SCHED_FIFO;
        if (pthread_setschedparam(self, policy, &sp) != 0) {
            platform::logErr("[RK3588_RT] pthread_setschedparam(SCHED_FIFO/%d) failed: %s\n",
                             rtPriority, strerror(errno));
        } else {
            platform::logInfo("[RK3588_RT] realtime policy: SCHED_FIFO/%d\n", rtPriority);
        }
    }

    if (!jitterOnly) {
        configureRuntime(sched, compositeTCI);
        sched.setBaseCycle(T_us(cycleUs));
        sched.gvl.errorMgr = &sched.errorMgr;
        sched.start(warmStart ? StartupMode::WARM : StartupMode::COLD);
        initialized = true;
        platform::logInfo("[RK3588_RT] PLC initialized\n");
    } else {
        platform::logInfo("[RK3588_RT] jitter-only mode\n");
    }

    // --- Xenomai 硬实时主循环 ---
    int64_t cycleNs = cycleUs * 1000;
    resetJitter(cycleNs);

    struct timespec next;
    clock_gettime(CLOCK_MONOTONIC, &next);

    platform::logInfo("[RK3588_RT] runtime started: cycle=%lldus cpu=%d diag=%ds\n",
        (long long)cycleUs, cpuAffinity, diagIntervalS);

    uint64_t ticks = 0;
    int64_t lastDiagUs = platform::steadyUs();

    while (running) {
        // 等待下个周期 — Xenomai Cobalt POSIX 包装层
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, nullptr);

        // 读取实际唤醒时间（非 next — 那只是我们请求的时间）
        struct timespec actual;
        clock_gettime(CLOCK_MONOTONIC, &actual);
        int64_t actualNs = (int64_t)actual.tv_sec * 1000000000LL + actual.tv_nsec;
        sampleJitter(actualNs);

        // 执行 PLC tick
        if (initialized) sched.tick();
        ticks++;

        // 推进下一个周期
        next.tv_nsec += cycleNs;
        while (next.tv_nsec >= 1000000000L) {
            next.tv_sec++;
            next.tv_nsec -= 1000000000L;
        }

        // 周期性诊断输出
        int64_t nowUs = actualNs / 1000;
        if (nowUs - lastDiagUs >= (int64_t)diagIntervalS * 1000000) {
            lastDiagUs = nowUs;
            platform::logInfo("[RK3588_RT] timer ticks=%llu\n", (unsigned long long)ticks);
            printJitter();
            printSnapshot();
        }
    }

    if (initialized) sched.stop();
    printJitter();
    platform::logInfo("[RK3588_RT] stopped. total timer ticks=%llu\n", (unsigned long long)ticks);
    return 0;
}
