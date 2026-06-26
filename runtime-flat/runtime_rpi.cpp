/**
 * runtime_rpi.cpp — Raspberry Pi Linux PLC 运行时入口
 *
 * 与 runtime_stm32.cpp 对应，但运行在 Linux 用户态。
 * 使用 POSIX timer 实现高精度周期 tick，不依赖 RTOS。
 *
 * 启动方式：
 *   sudo ./plc_runtime_rpi                    # 默认 1ms 周期
 *   sudo ./plc_runtime_rpi --cycle-us 500     # 500us 周期
 *   sudo chrt -f 99 ./plc_runtime_rpi         # SCHED_FIFO 最高优先级
 *
 * 精度要求：
 *   - PREEMPT_RT 内核：抖动 < 30us（推荐）
 *   - 标准内核：抖动 < 500us（10ms+ 周期可接受）
 *   - 建议用 chrt 或代码内 plc_rpi_set_rt_priority() 提升优先级
 *
 * 编译：
 *   g++ -O2 -I include/ src/*.cpp platform_rpi.cpp runtime_rpi.cpp \
 *       -lpthread -o plc_runtime_rpi
 */
#include "rt_runtime.h"
#include "rt_plc.h"

using namespace rt_plc;

// ═══════════════════════════════════════════════════════
// 外部声明
// ═══════════════════════════════════════════════════════

extern void registerAllPOUs(POURegistry& reg);
extern "C" int  plc_rpi_create_timer(int64_t intervalUs);
extern "C" void plc_rpi_destroy_timer();
extern "C" int  plc_rpi_set_rt_priority(int priority);
extern "C" uint32_t plc_rpi_get_tick_count();
extern "C" int64_t  plc_rpi_get_uptime_us();
extern "C" int  plc_rpi_gpio_init();
extern "C" void plc_rpi_gpio_set(int pin, int value);


// ═══════════════════════════════════════════════════════
// 调度器
// ═══════════════════════════════════════════════════════

static Scheduler sched;
static bool initialized = false;
static int64_t cycleUs = 1000;  // 默认 1ms


// ═══════════════════════════════════════════════════════
// PLC 初始化
// ═══════════════════════════════════════════════════════

static void plc_init() {
    POURegistry reg;
    registerAllPOUs(reg);

    sched.setBaseCycle(T_us(cycleUs));
    sched.gvl.errorMgr = &sched.errorMgr;

    int mainTask = sched.addCyclicTask("Main", 5, T_us(cycleUs));
    if (mainTask < 0) {
        platform::logErr("FATAL: Failed to create main task\n");
        return;
    }

    for (int i = 0; i < reg.count(); i++) {
        const auto& e = reg.entries()[i];
        if (e.cbs.cyclic) {
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic,
                                           e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) {
                sched.addProgramToTask(mainTask, progIdx);
            }
        }
    }

    sched.setTaskWatchdog(mainTask, T_us(cycleUs * 5));
    sched.start(StartupMode::COLD);
    initialized = true;

    platform::logInfo("PLC Runtime: %d POU(s), cycle=%lldus\n",
                      reg.count(), (long long)cycleUs);
}

extern "C" void plc_runtime_tick() {
    if (initialized) {
        sched.tick();
    }
}


// ═══════════════════════════════════════════════════════
// 信号处理（优雅退出）
// ═══════════════════════════════════════════════════════

static volatile bool running = true;

static void signal_handler(int /*sig*/) {
    running = false;
}


// ═══════════════════════════════════════════════════════
// 主函数
// ═══════════════════════════════════════════════════════

int main(int argc, char* argv[]) {
    // 解析参数
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--cycle-us") == 0 && i + 1 < argc) {
            cycleUs = atol(argv[i + 1]);
            i++;
        }
        if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            printf("Usage: %s [--cycle-us <microseconds>]\n", argv[0]);
            printf("  --cycle-us  PLC 周期（微秒，默认 1000 = 1ms）\n");
            return 0;
        }
    }

    // 1. 信号处理
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);

    // 2. 尝试提升实时优先级（需要 root）
    plc_rpi_set_rt_priority(90);

    // 3. 初始化 GPIO（LED 心跳）
    plc_rpi_gpio_init();

    // 4. 初始化 PLC
    plc_init();

    // 5. 创建高精度定时器
    if (plc_rpi_create_timer(cycleUs) != 0) {
        platform::logErr("Failed to create timer, falling back to busy loop\n");
        // 降级：忙等模式
        while (running) {
            plc_runtime_tick();
            platform::sleepUs(cycleUs);
        }
    } else {
        platform::logInfo("Timer started: %lldus period\n", (long long)cycleUs);
        platform::logInfo("Press Ctrl+C to stop\n");

        // 主循环：等待退出信号
        while (running) {
            usleep(100000);  // 100ms 检查一次退出信号

            // 每 5 秒打印一次诊断
            static int64_t lastDiagUs = 0;
            int64_t now = platform::steadyUs();
            if (now - lastDiagUs > 5000000) {
                lastDiagUs = now;
                platform::logInfo("[DIAG] ticks=%u uptime=%lldms\n",
                                 plc_rpi_get_tick_count(),
                                 plc_rpi_get_uptime_us() / 1000);
            }
        }
    }

    // 6. 清理
    plc_rpi_destroy_timer();
    sched.stop();

    platform::logInfo("PLC Runtime stopped. Total ticks: %u\n",
                      plc_rpi_get_tick_count());
    return 0;
}
