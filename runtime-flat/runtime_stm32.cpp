/**
 * runtime_stm32.cpp — STM32 裸机运行时入口示例
 *
 * 编译时定义 RT_PLATFORM_BARE_METAL，用户提供：
 *   - platform::nowUs()      — 系统时钟微秒
 *   - platform::steadyUs()   — 单调时钟微秒
 *   - platform::sleepUs()    — 延时（可为 no-op）
 *   - platform::logErr()     — 错误输出（重定向到 UART/ITM）
 *   - platform::logInfo()    — 信息输出
 *
 * 用法（在 STM32 项目中）：
 *   1. 将 runtime-flat/include/ 和 runtime-flat/src/ 加入编译
 *   2. 实现 platform 命名空间中的函数
 *   3. 在 SystemClock_Config() 之后调用 plc_runtime_init()
 *   4. 在 SysTick 中断或 TIM 中断中调用 plc_runtime_tick()
 */
#include "rt_runtime.h"
#include "rt_plc.h"

using namespace rt_plc;

// ═══════════════════════════════════════════════════════
// 平台函数实现（用户替换为自己的硬件驱动）
// ═══════════════════════════════════════════════════════

#if defined(RT_PLATFORM_BARE_METAL)

// 示例：基于 HAL_GetTick() 的实现
// 需要 #include "stm32f4xx_hal.h" 或对应芯片头文件
//
// int64_t rt_plc::platform::nowUs() {
//     return (int64_t)HAL_GetTick() * 1000;
// }
//
// int64_t rt_plc::platform::steadyUs() {
//     // 使用 DWT cycle counter 获取更高精度
//     return (int64_t)DWT->CYCCNT / (SystemCoreClock / 1000000);
// }
//
// void rt_plc::platform::sleepUs(int64_t us) {
//     // 裸机环境下通常由调度器控制时序，sleep 为空操作
//     (void)us;
// }
//
// void rt_plc::platform::logErr(const char* fmt, ...) {
//     va_list args;
//     va_start(args, fmt);
//     vprintf(fmt, args);  // 重定向到 UART
//     va_end(args);
// }
//
// void rt_plc::platform::logInfo(const char* fmt, ...) {
//     va_list args;
//     va_start(args, fmt);
//     vprintf(fmt, args);
//     va_end(args);
// }

#endif // RT_PLATFORM_BARE_METAL


// ═══════════════════════════════════════════════════════
// POU 注册（由编译器生成的代码提供）
// ═══════════════════════════════════════════════════════

extern void registerAllPOUs(POURegistry& reg);


// ═══════════════════════════════════════════════════════
// 静态调度配置（替代 tasks.json）
// ═══════════════════════════════════════════════════════

static Scheduler sched;
static bool initialized = false;


/**
 * 初始化 PLC 运行时
 * 在 SystemClock_Config() 之后、主循环之前调用
 */
extern "C" void plc_runtime_init() {
    // 1. 注册所有编译好的 POU
    POURegistry reg;
    registerAllPOUs(reg);

    // 2. 配置调度器
    sched.setBaseCycle(T_ms(10));  // 10ms 基础周期
    sched.gvl.errorMgr = &sched.errorMgr;

    // 3. 静态配置：创建一个主任务，挂载所有 POU
    int taskIdx = sched.addCyclicTask("Main", 5, T_ms(10));
    for (int i = 0; i < reg.count(); i++) {
        const auto& e = reg.entries()[i];
        if (e.cbs.cyclic) {
            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic,
                                           e.cbs.pre, e.cbs.post);
            if (progIdx >= 0) {
                sched.addProgramToTask(taskIdx, progIdx);
            }
        }
    }
    sched.setTaskWatchdog(taskIdx, T_ms(5));

    // 4. 启动（冷启动）
    sched.start(StartupMode::COLD);
    initialized = true;
}


/**
 * PLC 周期调用
 * 在 SysTick 中断（1ms）或定时器中断中调用
 * 建议每 1ms 调用一次，调度器内部会根据任务 interval 跳过不需要执行的任务
 */
extern "C" void plc_runtime_tick() {
    if (initialized) {
        sched.tick();
    }
}


/**
 * 获取调度器引用（用于诊断/监控）
 */
extern "C" Scheduler* plc_runtime_get_scheduler() {
    return initialized ? &sched : nullptr;
}
