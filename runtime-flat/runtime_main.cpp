/**
 * runtime_main.cpp — ST2C++ Flat 运行时主程序
 *
 * 编译器生成的代码（generated_pou.cpp）只包含 POU 函数：
 *   - void PROGRAM_P(GVL& gvl, ProcessImage& io, TIME dt)
 *   - INT ADD_TEN(INT X) 等辅助函数
 *
 * 本文件负责：
 *   1. 创建 Scheduler 调度器
 *   2. 注册编译器生成的 POU 到任务
 *   3. 运行调度循环
 *   4. 输出诊断信息
 */
#include "rt_runtime.h"
#include <cstdio>
#include <thread>
#include <chrono>

using namespace rt_plc;

// 前向声明：编译器生成的 POU 函数（在 generated_pou.cpp 中定义）
extern void PROGRAM_P(GVL& gvl, ProcessImage& io, TIME dt);

// GVL 偏移量（由编译器在 generated_pou.cpp 中定义）
// 这里用 extern 声明，实际值在编译器生成的代码中初始化

int main() {
    printf("=== ST2C++ Flat Runtime ===\n\n");

    Scheduler sched;
    sched.setBaseCycle(T_ms(1));  // 1ms 基础节拍
    sched.watchdog.setDefault(T_ms(10));  // 全局看门狗 10ms

    // 初始化 GVL（编译器生成的代码会在 PROGRAM_P 中进一步初始化变量）
    sched.gvl.clear();

    // 注册编译器生成的 PROGRAM 为 Cyclic 任务
    // 任务名 "MainTask"，优先级 5，周期 10ms
    int mainTask = sched.addCyclicTask("MainTask", 5, T_ms(10));
    sched.addPOU(mainTask, PROGRAM_P);
    sched.setTaskWatchdog(mainTask, T_ms(5));

    printf("Configuration:\n");
    printf("  Task: MainTask  priority=5  cyclic=10ms\n");
    printf("  POU:  PROGRAM_P\n");
    printf("  Base cycle: 1ms\n\n");

    // 运行 100 个 tick（约 100ms）
    printf("--- Running 100 ticks ---\n\n");
    for (int i = 0; i < 100; i++) {
        sched.tick();
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    // 诊断报告
    printf("\n");
    sched.printDiag();

    printf("\n=== Runtime Complete ===\n");
    return 0;
}
