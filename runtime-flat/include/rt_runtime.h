/**
 * rt_runtime.h — PLC 运行时调度框架（统一入口）
 *
 * 多任务调度器，支持：
 *   - Cyclic 任务（固定周期）
 *   - Event 任务（事件触发，上升沿/下降沿）
 *   - Freewheeling 任务（连续执行）
 *   - 全局变量表 GVL（跨 POU 变量访问，支持 RETAIN 区域）
 *   - PROGRAM 实例生命周期（Init → FirstScan → Cyclic → Post）
 *   - 分阶段扫描周期（ReadInputs → LogicSolve → WriteOutputs → Housekeeping）
 *   - 看门狗（任务级 + 全局）
 *   - 冷启动 / 暖启动
 *   - 运行时错误管理
 *   - 系统状态机（STOP → RUN → ERROR → PAUSED）
 *
 * 依赖: rt_plc.h（类型定义 + 标准功能块 + 错误处理 + 互斥锁）
 */

#pragma once

#include "core/registry.h"
#include "rt_plc.h"

#include "core/constants.h"
#include "core/types.h"
#include "core/gvl.h"
#include "core/retain_manager.h"
#include "core/runtime_validator.h"
#include "core/program.h"
#include "core/task.h"
#include "core/event.h"
#include "core/watchdog.h"
#include "core/diag.h"
#include "core/diag_manager.h"
#include "scheduler/scheduler.h"
#include "safety/safety_module.h"
