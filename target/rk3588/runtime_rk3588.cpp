/**
 * runtime_rk3588.cpp — RK3588 PREEMPT_RT Linux PLC runtime entry.
 *
 * Focus:
 *   - timerfd periodic scan
 *   - SCHED_FIFO realtime priority
 *   - mlockall memory locking
 *   - CPU affinity for isolcpus/nohz_full style setups
 *   - structured DiagSnapshot printout for runtime-flat validation
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#include <algorithm>
#include <cerrno>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <poll.h>
#include <sched.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/timerfd.h>
#include <time.h>
#include <unistd.h>

using namespace rt_plc;

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
    int64_t intervalUs = 1000;
    int64_t prevUs = 0;
    int64_t minJitter = 999999999;
    int64_t maxJitter = -999999999;
    int64_t sumJitter = 0;
    long double sumSquareJitter = 0;
    uint64_t samples = 0;
    uint64_t missedExpirations = 0;
    bool ready = false;
};

static JitterStats jitter;

extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

static void sigHandler(int) {
    running = 0;
}

static void resetJitter(int64_t intervalUs) {
    jitter = JitterStats{};
    jitter.intervalUs = intervalUs;
}

static void sampleJitter(int64_t nowUs, uint64_t expirations) {
    if (expirations > 1) {
        jitter.missedExpirations += expirations - 1;
    }

    if (!jitter.ready) {
        jitter.prevUs = nowUs;
        jitter.ready = true;
        return;
    }

    int64_t actual = nowUs - jitter.prevUs;
    int64_t expected = jitter.intervalUs * (int64_t)std::max<uint64_t>(1, expirations);
    int64_t sample = actual - expected;
    jitter.prevUs = nowUs;

    jitter.minJitter = std::min(jitter.minJitter, sample);
    jitter.maxJitter = std::max(jitter.maxJitter, sample);
    jitter.sumJitter += sample;
    jitter.sumSquareJitter += (long double)sample * (long double)sample;
    jitter.samples++;
}

static int64_t avgJitter() {
    return jitter.samples > 0 ? jitter.sumJitter / (int64_t)jitter.samples : 0;
}

static int64_t stddevJitter() {
    if (jitter.samples <= 1) return 0;
    long double avg = (long double)avgJitter();
    long double variance = jitter.sumSquareJitter / (long double)jitter.samples - avg * avg;
    return variance > 0 ? (int64_t)std::sqrt((double)variance) : 0;
}

static void printJitter() {
    platform::logInfo("[RK3588][JITTER] samples=%llu missed=%llu min=%lldus max=%lldus avg=%lldus std=%lldus target=%lldus\n",
        (unsigned long long)jitter.samples,
        (unsigned long long)jitter.missedExpirations,
        (long long)(jitter.samples ? jitter.minJitter : 0),
        (long long)(jitter.samples ? jitter.maxJitter : 0),
        (long long)avgJitter(),
        (long long)stddevJitter(),
        (long long)jitter.intervalUs);
}

static bool setCpuAffinity(int cpu) {
    if (cpu < 0) return true;
    cpu_set_t set;
    CPU_ZERO(&set);
    CPU_SET(cpu, &set);
    if (sched_setaffinity(0, sizeof(set), &set) != 0) {
        platform::logErr("[RK3588] sched_setaffinity(cpu=%d) failed: %s\n", cpu, strerror(errno));
        return false;
    }
    platform::logInfo("[RK3588] CPU affinity: cpu%d\n", cpu);
    return true;
}

static bool enableRealtime(int priority) {
    if (mlockall(MCL_CURRENT | MCL_FUTURE) != 0) {
        platform::logErr("[RK3588] mlockall failed: %s\n", strerror(errno));
    }

    sched_param sp = {};
    sp.sched_priority = priority;
    if (sched_setscheduler(0, SCHED_FIFO, &sp) != 0) {
        platform::logErr("[RK3588] sched_setscheduler(SCHED_FIFO/%d) failed: %s\n",
                         priority, strerror(errno));
        return false;
    }

    platform::logInfo("[RK3588] realtime policy: SCHED_FIFO/%d\n", priority);
    return true;
}

static int createPeriodicTimer(int64_t intervalUs) {
    int fd = timerfd_create(CLOCK_MONOTONIC, TFD_CLOEXEC | TFD_NONBLOCK);
    if (fd < 0) {
        platform::logErr("[RK3588] timerfd_create failed: %s\n", strerror(errno));
        return -1;
    }

    itimerspec spec = {};
    spec.it_interval.tv_sec = intervalUs / 1000000;
    spec.it_interval.tv_nsec = (intervalUs % 1000000) * 1000;
    spec.it_value = spec.it_interval;

    if (timerfd_settime(fd, 0, &spec, nullptr) != 0) {
        platform::logErr("[RK3588] timerfd_settime failed: %s\n", strerror(errno));
        close(fd);
        return -1;
    }

    return fd;
}

static void initPlc() {
    configureRuntime(sched, compositeTCI);
    sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;
    sched.start(warmStart ? StartupMode::WARM : StartupMode::COLD);
    initialized = true;
}

static void tickPlc() {
    if (!initialized) return;
    sched.tick();
}

static void printSnapshot() {
    if (!initialized) return;
    DiagSnapshot s = sched.snapshotDiag();
    platform::logInfo("[RK3588][PLC] state=%u phase=%u ticks=%llu cycle=%lldus scans=%llu overruns=%llu watchdog=%d errors=%d fatal=%d tasks=%d tci=%d safeout=%d\n",
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
        platform::logErr("[RK3588] invalid cycle-us: %lld\n", (long long)cycleUs);
        return 2;
    }

    signal(SIGINT, sigHandler);
    signal(SIGTERM, sigHandler);

    setCpuAffinity(cpuAffinity);
    enableRealtime(rtPriority);

    if (!jitterOnly) {
        initPlc();
    } else {
        platform::logInfo("[RK3588] jitter-only mode\n");
    }

    int timerFd = createPeriodicTimer(cycleUs);
    if (timerFd < 0) return 1;

    resetJitter(cycleUs);
    platform::logInfo("[RK3588] runtime started: cycle=%lldus cpu=%d diag=%ds\n",
        (long long)cycleUs, cpuAffinity, diagIntervalS);

    pollfd pfd = {timerFd, POLLIN, 0};
    int64_t lastDiagUs = platform::steadyUs();
    uint64_t ticks = 0;

    while (running) {
        int ret = poll(&pfd, 1, 100);
        if (ret > 0 && (pfd.revents & POLLIN)) {
            uint64_t expirations = 0;
            ssize_t n = read(timerFd, &expirations, sizeof(expirations));
            if (n != (ssize_t)sizeof(expirations)) continue;

            int64_t now = platform::steadyUs();
            sampleJitter(now, expirations);
            tickPlc();
            ticks += expirations;

            if (now - lastDiagUs >= (int64_t)diagIntervalS * 1000000) {
                lastDiagUs = now;
                platform::logInfo("[RK3588] timer ticks=%llu\n", (unsigned long long)ticks);
                printJitter();
                printSnapshot();
            }
        } else if (ret < 0 && errno != EINTR) {
            platform::logErr("[RK3588] poll failed: %s\n", strerror(errno));
            break;
        }
    }

    close(timerFd);
    if (initialized) sched.stop();
    printJitter();
    platform::logInfo("[RK3588] stopped. total timer ticks=%llu\n", (unsigned long long)ticks);
    return 0;
}
