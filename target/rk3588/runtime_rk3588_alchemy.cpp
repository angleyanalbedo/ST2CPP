/**
 * runtime_rk3588_alchemy.cpp — RK3588 Xenomai 3 Alchemy 原生硬实时 PLC Runtime
 *
 * 与 runtime_rk3588_rt.cpp 不同，此版本直接使用 Xenomai Alchemy API：
 *   rt_task_set_periodic() + rt_task_wait_period() 替代 clock_nanosleep()
 *   rt_timer_read() 替代 clock_gettime() 获取高精度时间戳
 *
 * 编译：
 *   g++ -O2 -std=c++17 -D_GNU_SOURCE -D_REENTRANT -D__COBALT__ \
 *       -DRT_PLATFORM_LINUX -DRT_TARGET_RK3588 \
 *       -I/usr/xenomai/include/cobalt -I/usr/xenomai/include \
 *       ... \
 *       $(/usr/xenomai/bin/xeno-config --ldflags --alchemy)
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
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <unistd.h>

#include <alchemy/task.h>
#include <alchemy/timer.h>

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
    int64_t firstActualNs = 0;

    void reset(int64_t iv) { intervalNs = iv; min = INT64_MAX; max = INT64_MIN; sum = sumSq = 0; count = 0; firstActualNs = 0; }
};

static JitterStats jitter;

static void resetJitter(int64_t cycleNs) { jitter.reset(cycleNs); }

static void sampleJitter(int64_t actualNs) {
    if (jitter.count == 0) {
        jitter.firstActualNs = actualNs;
        jitter.count++;
        return;
    }
    int64_t expectedNs = jitter.firstActualNs + (int64_t)(jitter.count) * jitter.intervalNs;
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
    platform::logInfo("[RK3588_ALCHEMY][JITTER] samples=%llu min=%ldns max=%ldns avg=%.0fns std=%.0fns target=%lldus\n",
        (unsigned long long)jitter.count,
        (long)jitter.min, (long)jitter.max,
        avg, std,
        (long long)(jitter.intervalNs / 1000));
}

static void sigHandler(int) { running = 0; }

static void unbindGpioKeys() {
    int fd = open("/sys/bus/platform/drivers/gpio-keys/unbind", O_WRONLY);
    if (fd >= 0) {
        if (write(fd, "gpio-keys", 9) > 0)
            platform::logInfo("[RK3588_ALCHEMY] unbound gpio-keys driver\n");
        else
            platform::logInfo("[RK3588_ALCHEMY] unbind gpio-keys: %s\n", strerror(errno));
        close(fd);
    }
}

static bool setCpuAffinity(int cpu) {
    if (cpu < 0) return false;
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(cpu, &cpuset);
    pthread_t self = pthread_self();
    if (pthread_setaffinity_np(self, sizeof(cpuset), &cpuset) != 0) {
        platform::logErr("[RK3588_ALCHEMY] pthread_setaffinity_np(cpu=%d) failed: %s\n", cpu, strerror(errno));
        return false;
    }
    platform::logInfo("[RK3588_ALCHEMY] CPU affinity: cpu%d\n", cpu);
    return true;
}

static void printSnapshot() {
    if (!initialized) return;
    DiagSnapshot s = sched.snapshotDiag();
    platform::logInfo("[RK3588_ALCHEMY][PLC] state=%u phase=%u ticks=%llu cycle=%lldus scans=%llu overruns=%llu watchdog=%d errors=%d fatal=%d tasks=%d tci=%d safeout=%d\n",
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
    printf("  --rt-prio N           Alchemy task priority 1..99 (default 90)\n");
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
        platform::logErr("[RK3588_ALCHEMY] invalid cycle-us: %lld\n", (long long)cycleUs);
        return 2;
    }

    signal(SIGINT, sigHandler);
    signal(SIGTERM, sigHandler);

    // 锁内存
    if (mlockall(MCL_CURRENT | MCL_FUTURE) != 0) {
        platform::logErr("[RK3588_ALCHEMY] mlockall failed: %s\n", strerror(errno));
    }

    // 解绑 gpio-keys，释放 GPIO 引脚供扫描线程使用
    unbindGpioKeys();

    // 用原生 pthread 设置 CPU 亲和性（Alchemy task 继承主线程的 affinity）
    setCpuAffinity(cpuAffinity);

    // --- 初始化 Alchemy task ---
    // 主线程在 Cobalt wrap 下虽已是 Xenomai 域，但 Alchemy 需要显式 shadow
    RT_TASK mainTask;
    int ret = rt_task_shadow(&mainTask, "PLC_Main", rtPriority, 0);
    if (ret) {
        platform::logErr("[RK3588_ALCHEMY] rt_task_shadow failed: %s\n", strerror(-ret));
        return 1;
    }
    platform::logInfo("[RK3588_ALCHEMY] Alchemy task shadowed: priority=%d\n", rtPriority);

    if (!jitterOnly) {
        configureRuntime(sched, compositeTCI);
        sched.setBaseCycle(T_us(cycleUs));
        sched.gvl.errorMgr = &sched.errorMgr;
        sched.start(warmStart ? StartupMode::WARM : StartupMode::COLD);
        initialized = true;
        platform::logInfo("[RK3588_ALCHEMY] PLC initialized\n");
    } else {
        platform::logInfo("[RK3588_ALCHEMY] jitter-only mode\n");
    }

    // --- Alchemy 周期性调度 ---
    int64_t cycleNs = cycleUs * 1000;
    resetJitter(cycleNs);

    // 设置周期性定时器：立即开始 (TM_NOW)，周期为 cycleNs
    ret = rt_task_set_periodic(&mainTask, TM_NOW, cycleNs);
    if (ret) {
        platform::logErr("[RK3588_ALCHEMY] rt_task_set_periodic failed: %s\n", strerror(-ret));
        return 1;
    }

    platform::logInfo("[RK3588_ALCHEMY] runtime started: cycle=%lldus cpu=%d diag=%ds\n",
        (long long)cycleUs, cpuAffinity, diagIntervalS);

    uint64_t ticks = 0;
    int64_t lastDiagUs = platform::steadyUs();
    unsigned long overruns = 0;

    // 预热第一个周期
    ret = rt_task_wait_period(&overruns);

    while (running) {
        // 等待下一个周期 — Alchemy 原生定时器，由 Cobalt 硬件定时器驱动
        ret = rt_task_wait_period(&overruns);

        // 用 Alchemy 高精度时钟读取实际唤醒时间
        RTIME actualRt = rt_timer_read();
        int64_t actualNs = (int64_t)actualRt;
        sampleJitter(actualNs);

        // 执行 PLC tick
        if (initialized) sched.tick();
        ticks++;

        // 周期性诊断输出
        int64_t nowUs = actualNs / 1000;
        if (nowUs - lastDiagUs >= (int64_t)diagIntervalS * 1000000) {
            lastDiagUs = nowUs;
            platform::logInfo("[RK3588_ALCHEMY] timer ticks=%llu overruns=%lu\n",
                (unsigned long long)ticks, overruns);
            printJitter();
            printSnapshot();
        }
    }

    if (initialized) sched.stop();
    printJitter();
    platform::logInfo("[RK3588_ALCHEMY] stopped. total timer ticks=%llu\n", (unsigned long long)ticks);
    return 0;
}
