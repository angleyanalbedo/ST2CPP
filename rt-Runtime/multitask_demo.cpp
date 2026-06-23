/**
 * multitask_demo.cpp — 多任务调度演示
 *
 * 模拟一个真实的 PLC 程序结构：
 *
 * GVL（全局变量表）:
 *   sensor_value  : REAL    @ offset 0    // 传感器读数
 *   setpoint      : REAL    @ offset 4    // 设定值
 *   output_value  : REAL    @ offset 8    // 控制输出
 *   alarm_flag    : BOOL    @ offset 12   // 报警标志
 *   cycle_counter : DINT    @ offset 16   // 总周期计数
 *   alarm_count   : DINT    @ offset 20   // 报警次数
 *   status_msg    : STRING  @ offset 24   // 状态消息
 *
 * Task[0] "FastControl"  priority=1, cyclic 10ms
 *   → POU_ControlLoop: PID 简化控制，读写 GVL sensor/setpoint/output
 *
 * Task[1] "Monitoring"    priority=5, cyclic 50ms
 *   → POU_Monitor: 检查报警条件，更新 GVL alarm_flag
 *   → POU_Logger: 跨 POU 读取 GVL 打印状态
 *
 * Task[2] "AlarmHandler"  priority=0, event on alarm_flag rising edge
 *   → POU_AlarmHandler: 处理报警，递增 alarm_count
 *
 * Task[3] "Diagnostics"   priority=10, cyclic 100ms
 *   → POU_Diag: 统计信息
 */
#include "rt_runtime.h"
#include <cstdio>
#include <cmath>
#include <thread>
#include <chrono>

using namespace rt_plc;

// ═══════════════════════════════════════
// GVL 偏移定义（编译器生成）
// ═══════════════════════════════════════

namespace GVL_Offsets {
    constexpr size_t SENSOR_VALUE  = 0;   // REAL
    constexpr size_t SETPOINT      = 4;   // REAL
    constexpr size_t OUTPUT_VALUE  = 8;   // REAL
    constexpr size_t ALARM_FLAG    = 12;  // BOOL
    constexpr size_t CYCLE_COUNTER = 16;  // DINT
    constexpr size_t ALARM_COUNT   = 20;  // DINT
    // STRING status_msg at offset 24 (256 bytes)
}


// ═══════════════════════════════════════
// POU 函数（编译器为每个 PROGRAM/FB 生成）
// ═══════════════════════════════════════

// --- POU_ControlLoop: 简化 P 控制器 ---
// ST:
//   PROGRAM ControlLoop
//     GVL.sensor_value := <from I/O>;
//     GVL.output_value := (GVL.setpoint - GVL.sensor_value) * 2.0;
//     GVL.cycle_counter := GVL.cycle_counter + 1;
//   END_PROGRAM
static int controlCallCount = 0;
void POU_ControlLoop(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    REAL sensor   = gvl.read<REAL>(GVL_Offsets::SENSOR_VALUE);
    REAL setpoint = gvl.read<REAL>(GVL_Offsets::SETPOINT);

    // 模拟传感器噪声：在 setpoint 附近波动（幅度足以触发报警）
    float noise = ((controlCallCount % 5) - 2) * 1.2f;
    sensor = setpoint + noise;
    gvl.write<REAL>(GVL_Offsets::SENSOR_VALUE, sensor);

    // P 控制
    REAL error = setpoint - sensor;
    REAL output = error * 2.0f;
    gvl.write<REAL>(GVL_Offsets::OUTPUT_VALUE, output);

    // 递增周期计数（跨 POU 可访问）
    DINT cnt = gvl.read<DINT>(GVL_Offsets::CYCLE_COUNTER);
    gvl.write<DINT>(GVL_Offsets::CYCLE_COUNTER, cnt + 1);

    controlCallCount++;
}


// --- POU_Monitor: 报警监测 ---
// ST:
//   PROGRAM Monitor
//     IF ABS(GVL.sensor_value - GVL.setpoint) > 2.0 THEN
//       GVL.alarm_flag := TRUE;
//     ELSE
//       GVL.alarm_flag := FALSE;
//     END_IF;
//   END_PROGRAM
static int monitorCallCount = 0;
void POU_Monitor(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    REAL sensor   = gvl.read<REAL>(GVL_Offsets::SENSOR_VALUE);
    REAL setpoint = gvl.read<REAL>(GVL_Offsets::SETPOINT);

    REAL diff = sensor - setpoint;
    if (diff < 0) diff = -diff;  // ABS

    BOOL alarm = (diff > 2.0f) ? TRUE : FALSE;
    gvl.write<BOOL>(GVL_Offsets::ALARM_FLAG, alarm);

    // 每 10 次调用打印一次状态（跨 POU 读取 sensor 和 output）
    monitorCallCount++;
    if (monitorCallCount % 5 == 0) {
        REAL output = gvl.read<REAL>(GVL_Offsets::OUTPUT_VALUE);
        DINT cycles = gvl.read<DINT>(GVL_Offsets::CYCLE_COUNTER);
        printf("[Monitor] sensor=%.2f setpoint=%.2f output=%.2f alarm=%d cycles=%d\n",
               sensor, setpoint, output, alarm, cycles);
    }
}


// --- POU_Logger: 日志（同一个 Task 里挂多个 POU） ---
// ST:
//   PROGRAM Logger
//     // 跨 POU 访问 GVL 里 Monitor 写入的 alarm_flag
//     IF GVL.alarm_flag THEN
//       str_assign(GVL.status_msg, 'ALARM ACTIVE');
//     ELSE
//       str_assign(GVL.status_msg, 'NORMAL');
//     END_IF;
//   END_PROGRAM
void POU_Logger(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    BOOL alarm = gvl.read<BOOL>(GVL_Offsets::ALARM_FLAG);
    STRING* msg = gvl.ptr<STRING>(24);
    if (alarm) {
        str_assign(*msg, "ALARM ACTIVE");
    } else {
        str_assign(*msg, "NORMAL");
    }
}


// --- POU_AlarmHandler: 报警处理（事件触发） ---
// ST:
//   PROGRAM AlarmHandler
//     GVL.alarm_count := GVL.alarm_count + 1;
//     // 处理报警...
//   END_PROGRAM
void POU_AlarmHandler(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    DINT count = gvl.read<DINT>(GVL_Offsets::ALARM_COUNT);
    gvl.write<DINT>(GVL_Offsets::ALARM_COUNT, count + 1);

    REAL sensor = gvl.read<REAL>(GVL_Offsets::SENSOR_VALUE);
    printf("[ALARM #%d] sensor=%.2f out of range!\n", count + 1, sensor);
}


// --- POU_Diag: 诊断（低频任务） ---
void POU_Diag(GVL& gvl, ProcessImage& io, TIME cycleTimeUs) {
    DINT cycles = gvl.read<DINT>(GVL_Offsets::CYCLE_COUNTER);
    DINT alarms = gvl.read<DINT>(GVL_Offsets::ALARM_COUNT);
    STRING* msg = gvl.ptr<STRING>(24);
    printf("[Diag] total_cycles=%d alarms=%d status='%s'\n",
           cycles, alarms, msg->data);
}


// --- Event 触发条件函数 ---
// 检查 GVL.alarm_flag 的值
BOOL CheckAlarmCondition(GVL& gvl, ProcessImage& io) {
    return gvl.read<BOOL>(GVL_Offsets::ALARM_FLAG);
}


// ═══════════════════════════════════════
// 模拟 I/O 驱动
// ═══════════════════════════════════════

struct SimTCI : public TCI {
    REAL simulatedSetpoint = 50.0f;

    void syncInputs(ProcessImage& img) override {
        // 模拟：把设定值写入输入映像
        img.writeOutput<REAL>(0, simulatedSetpoint);
    }

    void syncOutputs(ProcessImage& img) override {
        // 模拟：读取控制输出（实际场景中写到执行器）
        // REAL output = img.readOutput<REAL>(4);
        // printf("  -> Actuator output: %.2f\n", output);
    }
};


// ═══════════════════════════════════════
// 主程序
// ═══════════════════════════════════════

int main() {
    printf("=== ST2C 多任务调度演示 ===\n\n");

    Scheduler sched;
    SimTCI simIO;
    sched.setTCI(&simIO);
    sched.setBaseCycle(T_ms(1));  // 1ms 基础节拍
    sched.watchdog.setDefault(T_ms(5));  // 全局看门狗 5ms

    // ─── 初始化 GVL ───
    sched.gvl.clear();
    sched.gvl.write<REAL>(GVL_Offsets::SETPOINT, 50.0f);
    sched.gvl.write<DINT>(GVL_Offsets::CYCLE_COUNTER, 0);
    sched.gvl.write<DINT>(GVL_Offsets::ALARM_COUNT, 0);
    str_assign(*sched.gvl.ptr<STRING>(24), "INIT");

    // ─── 配置任务 ───

    // Task 0: FastControl, priority=1, cyclic 10ms
    int t0 = sched.addCyclicTask("FastControl", 1, T_ms(10));
    sched.addPOU(t0, POU_ControlLoop);
    sched.setTaskWatchdog(t0, T_ms(2));  // 控制任务超时 2ms

    // Task 1: Monitoring, priority=5, cyclic 50ms, 挂载两个 POU
    int t1 = sched.addCyclicTask("Monitoring", 5, T_ms(50));
    sched.addPOU(t1, POU_Monitor);
    sched.addPOU(t1, POU_Logger);

    // Task 2: AlarmHandler, priority=0 (最高), event on alarm_flag
    int t2 = sched.addEventTask("AlarmHandler", 0, CheckAlarmCondition, EventEdge::RISING);
    sched.addPOU(t2, POU_AlarmHandler);

    // Task 3: Diagnostics, priority=10, cyclic 200ms
    int t3 = sched.addCyclicTask("Diagnostics", 10, T_ms(200));
    sched.addPOU(t3, POU_Diag);

    printf("配置完成: %d 任务, %d 事件\n", sched.taskCount(), sched.eventCount());
    printf("Task[0] FastControl  pri=1  cyclic 10ms  watchdog=2ms\n");
    printf("Task[1] Monitoring   pri=5  cyclic 50ms  (Monitor+Logger)\n");
    printf("Task[2] AlarmHandler pri=0  event on alarm_flag rising\n");
    printf("Task[3] Diagnostics  pri=10 cyclic 200ms\n\n");

    // ─── 模拟运行 ───
    printf("--- 模拟运行 200 ticks ---\n\n");

    // 手动跑 200 个 tick，中间注入一些变化
    for (int tick = 0; tick < 200; tick++) {
        // 在 tick 50 时改变设定值（制造偏差触发报警）
        if (tick == 50) {
            printf(">> tick 50: setpoint changed to 60.0\n");
            sched.gvl.write<REAL>(GVL_Offsets::SETPOINT, 60.0f);
            simIO.simulatedSetpoint = 60.0f;
        }
        // 在 tick 120 时恢复正常
        if (tick == 120) {
            printf(">> tick 120: setpoint restored to 50.0\n");
            sched.gvl.write<REAL>(GVL_Offsets::SETPOINT, 50.0f);
            simIO.simulatedSetpoint = 50.0f;
        }

        sched.tick();
        // 模拟 1ms 节拍（与 baseCycleTime 一致）
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    // ─── 诊断报告 ───
    printf("\n");
    sched.printDiag();

    // ─── 验证跨 POU 访问 ───
    printf("\n--- 跨 POU 变量访问验证 ---\n");
    printf("GVL.sensor_value  = %.2f\n",
           sched.gvl.read<REAL>(GVL_Offsets::SENSOR_VALUE));
    printf("GVL.setpoint      = %.2f\n",
           sched.gvl.read<REAL>(GVL_Offsets::SETPOINT));
    printf("GVL.output_value  = %.2f\n",
           sched.gvl.read<REAL>(GVL_Offsets::OUTPUT_VALUE));
    printf("GVL.alarm_flag    = %d\n",
           sched.gvl.read<BOOL>(GVL_Offsets::ALARM_FLAG));
    printf("GVL.cycle_counter = %d\n",
           sched.gvl.read<DINT>(GVL_Offsets::CYCLE_COUNTER));
    printf("GVL.alarm_count   = %d\n",
           sched.gvl.read<DINT>(GVL_Offsets::ALARM_COUNT));
    printf("GVL.status_msg    = '%s'\n",
           sched.gvl.ptr<STRING>(24)->data);

    printf("\n=== 演示完成 ===\n");
    return 0;
}
