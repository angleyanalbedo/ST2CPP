/**
 * runtime_windows.cpp — Windows 平台 PLC 运行时
 *
 * 使用 QueryPerformanceCounter 高精度计时 + WaitableTimer 周期触发。
 * 注意：Windows 非实时 OS，1ms 周期抖动约 100-500us。
 * 建议用于开发测试，生产部署请上 PREEMPT_RT Linux 或裸机。
 *
 * 精度指标：
 *   - QPC 分辨率：< 1us（硬件依赖）
 *   - WaitableTimer 周期：抖动 ~100-500us（1ms 周期）
 *   - timeBeginPeriod(1)：可降至 ~15ms 调度的 ~100us 抖动
 *
 * 编译（MinGW 或 MSVC）：
 *   g++ -O2 -std=c++17 -I../../runtime-flat/include
 *       ../../runtime-flat/src/ *.cpp runtime_windows.cpp
 *       -lwinmm -o plc_runtime_windows.exe
 *
 * 运行：
 *   plc_runtime_windows.exe              # 1ms 周期
 *   plc_runtime_windows.exe --cycle-us 5000  # 5ms（Windows 更稳）
 *
 * 注意：types.h（经 rt_plc.h 引入）必须在 <windows.h> 之前 include。
 * 避免 <windows.h> 的 ERROR/TRUE/FALSE 宏污染 enum 和 typedef。
 * using namespace rt_plc; 虽会引入 BOOL/DWORD/INT/UINT，但本文件
 * 未直接使用这些类型名，不产生歧义。
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#if defined(RT_ETHERCAT_ENABLED)
#include "ethercat_tci.h"
#endif

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

// TCI 配置
enum class TciMode { NONE, ETHERCAT };
static TciMode tciMode = TciMode::NONE;
static char ecatIfname[32] = "eth0";

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
extern void registerAllPOUs(POURegistry& reg);

static void plcInit() {
    POURegistry reg;
    registerAllPOUs(reg);

    sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;

    // ─── 初始化 TCI ───
#if defined(RT_ETHERCAT_ENABLED)
    if (tciMode == TciMode::ETHERCAT) {
        static EthercatTCI ecatTCI;
        if (ecatTCI.init(ecatIfname) == 0) {
            compositeTCI.add(&ecatTCI);
            platform::logInfo("EtherCAT TCI registered on %s\n", ecatIfname);
        } else {
            platform::logErr("EtherCAT TCI init failed on %s\n", ecatIfname);
        }
    }
#endif

    if (compositeTCI.count() > 0) {
        sched.setTCI(&compositeTCI);
        platform::logInfo("TCI: %d backend(s) active\n", compositeTCI.count());
    }

    int mainTask = sched.addCyclicTask("Main", 5, T_us(cycleUs));
    if (mainTask < 0) { platform::logErr("FATAL: addCyclicTask failed\n"); return; }
    for (int i = 0; i < reg.count(); i++) {
        const auto& e = reg.entries()[i];
        if (e.cbs.cyclic) {
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic,
                                           e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) sched.addProgramToTask(mainTask, progIdx);
        }
    }
    sched.setTaskWatchdog(mainTask, T_us(cycleUs * 5));
    sched.start(StartupMode::COLD);
    initialized = true;
    platform::logInfo("PLC Runtime: %d POU(s), cycle=%lldus\n",
                      reg.count(), (long long)cycleUs);
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
        } else if (strcmp(argv[i], "--tci") == 0 && i + 1 < argc) {
            i++;
            if (strcmp(argv[i], "ethercat") == 0) tciMode = TciMode::ETHERCAT;
            else fprintf(stderr, "Unknown TCI mode: %s (use ethercat)\n", argv[i]);
        } else if (strcmp(argv[i], "--ecat-if") == 0 && i + 1 < argc) {
            strncpy(ecatIfname, argv[i + 1], sizeof(ecatIfname) - 1);
            i++;
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [options]\n", argv[0]);
            printf("  --cycle-us <us>       PLC 周期（微秒，默认 1000）\n");
            printf("  --diag-interval <s>   诊断间隔（秒，默认 5）\n");
            printf("  --jitter-only         纯定时器抖动测试\n");
            printf("  --tci <mode>          I/O 模式: ethercat\n");
            printf("  --ecat-if <name>      EtherCAT 网卡接口 (默认 eth0)\n");
            printf("\n注意：Windows 非实时 OS，1ms 周期抖动 ~100-500us\n");
            return 0;
        }
    }

    // 高精度计时初始化
    QueryPerformanceFrequency(&qpcFreq);

    // 提高定时器精度
    timeBeginPeriod(1);

    // Ctrl+C 处理
    SetConsoleCtrlHandler(consoleHandler, TRUE);

    if (!jitterOnly) plcInit();
    else printf("Jitter-only mode\n");

    // 创建 WaitableTimer
    timerHandle = CreateWaitableTimer(NULL, FALSE, NULL);
    if (!timerHandle) {
        fprintf(stderr, "CreateWaitableTimer failed\n");
        return 1;
    }

    // 转换微秒到 100ns 间隔
    LARGE_INTEGER due;
    due.QuadPart = -(cycleUs * 10LL);  // 负值 = 相对时间
    if (!SetWaitableTimer(timerHandle, &due, (LONG)cycleUs / 1000,
                          timerAPC, NULL, FALSE)) {
        fprintf(stderr, "SetWaitableTimer failed\n");
        CloseHandle(timerHandle);
        return 1;
    }

    // Windows 的 WaitableTimer 最小精度是毫秒
    // 如果 cycleUs < 1000，降级为忙等模式
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
            // APCs 只在可告警等待中执行
            SleepEx(100, TRUE);
        } else {
            // 忙等模式（<1ms 周期）
            int64_t now = qpcUs();
            jitterSample(now);
            plcTick();
            tickCount++;
            // 自旋等待到下一个周期
            int64_t next = now + cycleUs;
            while (qpcUs() < next) {
                // busy wait
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
