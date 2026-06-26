/**
 * runtime_stm32.cpp — STM32 裸机 PLC 运行时完整入口
 *
 * 本文件是 PLC 运行时在 STM32 上的主程序。
 * 与 platform_stm32.cpp 配合使用：
 *   - platform_stm32.cpp：硬件抽象（DWT、TIM2、UART、LED）
 *   - runtime_stm32.cpp：PLC 调度逻辑（注册、配置、tick）
 *
 * 启动流程：
 *   Reset → SystemClock_Config() → plc_platform_init() → plc_runtime_init() → while(1) {}
 *
 * 所有 PLC 逻辑在 TIM2 中断中执行（1ms 周期），主循环为空。
 *
 * 编译方式：
 *   与 runtime-flat 的 include/ 和 src/ 一起编译，
 *   定义 -DRT_PLATFORM_BARE_METAL -DSTM32F4xx
 *   链接 STM32 HAL 库
 */
#include "rt_runtime.h"
#include "rt_plc.h"

#if defined(RT_PLATFORM_BARE_METAL)

using namespace rt_plc;

// ═══════════════════════════════════════════════════════
// POU 注册（由编译器生成的代码提供）
// ═══════════════════════════════════════════════════════

extern void registerAllPOUs(POURegistry& reg);


// ═══════════════════════════════════════════════════════
// 调度器实例
// ═══════════════════════════════════════════════════════

static Scheduler sched;
static bool initialized = false;

// 配置参数（可根据实际需求修改）
static constexpr TIME PLC_BASE_CYCLE_US = 1000;   // 1ms 基础周期
static constexpr TIME PLC_TASK_INTERVAL = 1000;   // 主任务 1ms 执行一次
static constexpr int  PLC_TASK_PRIORITY = 5;      // 中等优先级
static constexpr TIME PLC_WATCHDOG_US   = 5000;   // 5ms 看门狗


// ═══════════════════════════════════════════════════════
// 初始化
// ═══════════════════════════════════════════════════════

/**
 * 初始化 PLC 运行时
 * 在 plc_platform_init() 之后调用
 */
extern "C" void plc_runtime_init() {
    // 1. 注册所有编译好的 POU
    POURegistry reg;
    registerAllPOUs(reg);

    // 2. 配置调度器
    sched.setBaseCycle(T_us(PLC_BASE_CYCLE_US));
    sched.gvl.errorMgr = &sched.errorMgr;

    // 3. 创建主任务，挂载所有 POU
    int mainTask = sched.addCyclicTask("Main", PLC_TASK_PRIORITY,
                                       T_us(PLC_TASK_INTERVAL));
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

    // 4. 设置看门狗
    sched.setTaskWatchdog(mainTask, T_us(PLC_WATCHDOG_US));

    // 5. 冷启动
    sched.start(StartupMode::COLD);
    initialized = true;

    // 6. 输出启动信息
    platform::logInfo("PLC Runtime: %d POU(s), cycle=%lldus, watchdog=%lldus\n",
                      reg.count(), (long long)PLC_BASE_CYCLE_US,
                      (long long)PLC_WATCHDOG_US);
}


// ═══════════════════════════════════════════════════════
// Tick（由 TIM2 中断调用）
// ═══════════════════════════════════════════════════════

extern "C" void plc_runtime_tick() {
    if (initialized) {
        sched.tick();
    }
}


// ═══════════════════════════════════════════════════════
// 诊断接口
// ═══════════════════════════════════════════════════════

extern "C" Scheduler* plc_runtime_get_scheduler() {
    return initialized ? &sched : nullptr;
}


// ═══════════════════════════════════════════════════════
// 主函数（空循环，所有逻辑在中断中）
// ═══════════════════════════════════════════════════════

int main() {
    // 1. 系统时钟配置（由 STM32CubeMX 生成或手动配置）
    // SystemClock_Config();

    // 2. 初始化硬件抽象层（DWT + TIM2 + UART + LED）
    plc_platform_init();

    // 3. 初始化 PLC 运行时（注册 POU + 配置调度器）
    plc_runtime_init();

    platform::logInfo("PLC System Ready. Entering main loop.\n");
    platform::logInfo("LED heartbeat: 100ms period\n");
    platform::logInfo("TIM2 interrupt: 1ms period\n");

    // 4. 主循环（空，所有逻辑在 TIM2 中断中执行）
    while (1) {
        __WFI();  // 等待中断，降低功耗
    }
}

#else

// 桌面端：不编译此文件
int main() {
    return 0;
}

#endif // RT_PLATFORM_BARE_METAL
