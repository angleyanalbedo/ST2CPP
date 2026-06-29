/**
 * runtime_windows.cpp — Windows 平台 PLC 运行时
 *
 * 使用 QueryPerformanceCounter 高精度计时 + WaitableTimer 周期触发。
 * 注意：Windows 非实时 OS，1ms 周期抖动约 100-500us。
 * 建议用于开发测试，生产部署请上 PREEMPT_RT Linux 或裸机。
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cmath>
#include <algorithm>

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <mmsystem.h>

using namespace rt_plc;

// ═══ 配置 ═══
static int64_t   cycleUs       = 1000;
static int       diagIntervalS = 5;
static bool      jitterOnly    = false;
static bool      running       = true;

static Scheduler sched;
static CompositeTCI compositeTCI;
static bool      initialized   = false;

static HANDLE    timerHandle   = NULL;

// ═══ 高精度计时（QPC） ═══
static LARGE_INTEGER qpcFreq = {};

static int64_t qpcUs() {
    LARGE_INTEGER cnt;
    QueryPerformanceCounter(&cnt);
    return (int64_t)(cnt.QuadPart * 1000000LL / qpcFreq.QuadPart);
}

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
    printf("\n=== Jitter Statistics ===\n");
    printf("  Samples  : %llu\n", (unsigned long long)jitter.count);
    if (jitter.count > 0) {
        int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
        double variance = jitter.count > 1
            ? (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg
            : 0;
        printf("  Min      : %lld us\n", (long long)jitter.minJitter);
        printf("  Max      : %lld us\n", (long long)jitter.maxJitter);
        printf("  Avg      : %lld us\n", (long long)avg);
        printf("  Stddev   : %lld us\n",
            (long long)(variance > 0 ? (int64_t)std::sqrt(variance) : 0));
        printf("  Target   : %lld us\n", (long long)jitter.intervalUs);
    }
    printf("==========================\n");
}

// ═══ PLC ═══
extern void configureRuntime(Scheduler& sched, CompositeTCI& tci);

static void plcInit() {
    configureRuntime(sched, compositeTCI);
    if (cycleUs != sched.baseCycleTime)
        sched.setBaseCycle(T_us(cycleUs));
    sched.start(StartupMode::COLD);
    initialized = true;
    platform::logInfo("PLC Runtime: cycle=%lldus\n", (long long)cycleUs);
}

static void plcTick() {
    if (initialized) sched.tick();
}

// ═══ WaitableTimer 回调 ═══
static void CALLBACK timerAPC(void*, unsigned long, unsigned long) {
    int64_t now = qpcUs();
    jitterSample(now);
    plcTick();
}

// ═══ 信号处理（Ctrl+C） ═══
static int WINAPI consoleHandler(unsigned long dwType) {
    if (dwType == CTRL_C_EVENT || dwType == CTRL_BREAK_EVENT) {
        running = false;
        return 1;
    }
    return 0;
}

// ═══ 主函数 ═══
int main(int argc, char* argv[]) {
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[++i]);
        } else if (strcmp(argv[i], "--diag-interval") == 0 && i + 1 < argc) {
            diagIntervalS = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--jitter-only") == 0) {
            jitterOnly = true;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [options]\n", argv[0]);
            printf("  --cycle-us <us>       PLC cycle (us, default 1000)\n");
            printf("  --diag-interval <s>   Diagnostics interval (s, default 5)\n");
            printf("  --jitter-only         Timer jitter test only\n");
            return 0;
        }
    }

    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    QueryPerformanceFrequency(&qpcFreq);
    timeBeginPeriod(1);
    SetPriorityClass(GetCurrentProcess(), HIGH_PRIORITY_CLASS);
    SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_TIME_CRITICAL);
    SetThreadAffinityMask(GetCurrentThread(), 1);
    SetConsoleCtrlHandler(consoleHandler, TRUE);

    if (!jitterOnly) plcInit();
    else printf("Jitter-only mode\n");

    timerHandle = CreateWaitableTimer(NULL, FALSE, NULL);
    if (!timerHandle) {
        fprintf(stderr, "CreateWaitableTimer failed\n");
        return 1;
    }

    LARGE_INTEGER due;
    due.QuadPart = -(cycleUs * 10LL);
    if (!SetWaitableTimer(timerHandle, &due, (LONG)cycleUs / 1000,
                          timerAPC, NULL, FALSE)) {
        fprintf(stderr, "SetWaitableTimer failed\n");
        CloseHandle(timerHandle);
        return 1;
    }

    bool useTimer = (cycleUs >= 1000);

    if (!useTimer) {
        printf("Warning: cycleUs=%lld < 1000us, using busy loop\n",
               (long long)cycleUs);
        jitterReset(cycleUs);
    }

    printf("Timer started: %lldus, press Ctrl+C to stop\n", (long long)cycleUs);

    int64_t lastDiag = 0;
    uint64_t tickCount = 0;

    while (running) {
        if (useTimer) {
            SleepEx(100, TRUE);
        } else {
            int64_t now = qpcUs();
            jitterSample(now);
            plcTick();
            tickCount++;
            int64_t next = now + cycleUs;
            while (qpcUs() < next) {
            }
        }

        int64_t now = qpcUs();
        if (now - lastDiag > (int64_t)diagIntervalS * 1000000) {
            lastDiag = now;
            printf("[DIAG] ticks=%llu\n", (unsigned long long)tickCount);
            if (jitter.count > 0) {
                int64_t avg = jitter.sumJitter / (int64_t)jitter.count;
                double variance = jitter.count > 1
                    ? (double)jitter.sumSquareJitter / jitter.count - (double)avg * avg
                    : 0;
                printf("[JITTER] min=%lld max=%lld avg=%lld stddev=%lld (samples=%llu)\n",
                    (long long)jitter.minJitter, (long long)jitter.maxJitter,
                    (long long)avg,
                    (long long)(variance > 0 ? (int64_t)std::sqrt(variance) : 0),
                    (unsigned long long)jitter.count);
            }
        }
    }

    CancelWaitableTimer(timerHandle);
    CloseHandle(timerHandle);
    timeEndPeriod(1);
    if (!jitterOnly) sched.stop();
    printJitterStats();
    printf("Stopped. Total ticks: %llu\n", (unsigned long long)tickCount);
    return 0;
}
