# runtime-flat TODO

This TODO tracks the runtime work needed to move from a functional PLC runtime
kernel to something that can survive industrial product scrutiny.

The current strategic focus is hard real-time execution, target integration,
fieldbus behavior, persistence, diagnostics, and safety evidence. Compiler-only
items should live in compiler docs unless they directly affect the runtime ABI.

## Status Snapshot

`runtime-flat` already has:

- [x] Flat GVL memory with bounds-checked `read/write/ptr`
- [x] ProcessImage input/output buffers with bit and typed accessors
- [x] TCI abstraction plus `CompositeTCI`
- [x] PROGRAM lifecycle support
- [x] Cyclic, event, and freewheeling task types
- [x] Static task/program/event capacity limits
- [x] Software watchdog
- [x] `TaskExecutor` for unified cyclic/event/freewheeling task execution
- [x] `IoManager` wrapper for TCI/CompositeTCI sync and safe-output staging
- [x] `DiagManager` wrapper for diagnostics reset/recording and scheduler print snapshot
- [x] Runtime error ring buffer and safe math helpers
- [x] RETAIN region markers and in-memory backup/restore hooks
- [x] `RetainManager` wrapper for startup clear, in-memory save, and restore
- [x] Diagnostics counters for scan time and overruns
- [x] Desktop/Linux/bare-metal platform abstraction
- [x] Linux `timerfd` + `SCHED_FIFO` runtime entry
- [x] STM32-style timer interrupt runtime entry
- [x] Desktop functional and integration tests

## P0 — Correctness Issues Blocking Runtime Trust

- [ ] Make `platform::steadyUs()` wrap-safe on STM32/Cortex-M DWT `CYCCNT`
  - Keep a 64-bit extended counter or handle deltas with unsigned wrap logic.
  - Add a long-run test hook so diagnostics do not break after counter wrap.

- [x] Unify cyclic and event task execution paths
  - Event tasks should use the same timing, watchdog, overrun, fatal-error, and
    diagnostic accounting as cyclic tasks.
  - Avoid duplicated PROGRAM execution logic in `Scheduler::tick()` and
    `Scheduler::checkEvents()`.
  - Implemented by `TaskExecutor`; covered by the event-task watchdog regression
    test.

- [x] Make watchdog behavior independent of `ENABLE_DIAG`
  - Watchdog timing must work in release builds.
  - Diagnostics can control logging/statistics, but not safety behavior.
  - Verified with the `ENABLE_DIAG=OFF` runtime integration test.

- [ ] Define deterministic overrun semantics
  - Decide whether an overrun skips the next period, catches up, latches ERROR,
    or enters a degraded mode.
  - Document behavior and add tests for each selected policy.

- [ ] Harden GVL/ProcessImage alignment contract
  - Add compile-time or runtime checks for generated offsets.
  - Document required alignment per IEC type and target architecture.

## P1 — Hard Real-Time Evidence

- [ ] Add jitter histogram and percentiles
  - Track min/max/avg/stddev plus P95/P99/P99.9.
  - Keep fixed-size buckets to avoid heap allocation.

- [ ] Add WCET measurement per task and per scan phase
  - Record ReadInputs, LogicSolve, WriteOutputs, Housekeeping separately.
  - Record max execution time per task and per PROGRAM.

- [ ] Add long-run stability test
  - 24h/72h run mode with periodic diagnostic snapshots.
  - Detect missed ticks, timerfd overruns, counter wrap, and memory corruption.

- [ ] Add hardware GPIO jitter measurement mode
  - Toggle a pin at scan start/end for oscilloscope validation.
  - Keep this available on STM32, Raspberry Pi, and Banana Pi targets.

- [ ] Add target timing acceptance criteria
  - Example: 1 ms cycle, P99 jitter under target-specific threshold, zero missed
    watchdog-safe ticks under load.

## P1 — RETAIN and Startup Semantics

- [ ] Replace RAM-only RETAIN backup with a storage interface
  - `RetainStore::load/save/commit/invalidate`
  - Backends: file, Flash, FRAM, or target-specific NVRAM.
  - First lifecycle boundary exists as `RetainManager`; persistent backends are
    still pending.

- [ ] Add RETAIN integrity metadata
  - CRC, layout version, payload size, compiler build ID, and monotonic counter.

- [ ] Define cold/warm/hot restart behavior
  - Specify what happens to GVL, ProcessImage, tasks, diagnostics, and errors.

- [ ] Add power-loss consistency tests
  - Simulate interrupted writes.
  - Verify corrupt RETAIN data enters a known safe fallback.

## P1 — Fieldbus and I/O Productization

- [x] Add runtime I/O management boundary
  - `IoManager` owns TCI calls from the runtime core.
  - Existing `config.json -> gen_config.py -> CompositeTCI` target flow remains
    unchanged.
  - First safe-output staging API is wired into Scheduler ERROR transitions.

- [x] Wire safe outputs into fatal/error transitions
  - `Scheduler::enterErrorState()` applies configured safe outputs and performs
    final `syncOutputs()` when safe outputs are enabled.
  - Covered by direct `Scheduler::error()` and event-watchdog ERROR tests.

- [ ] Promote EtherCAT TCI to a first-class runtime backend
  - PDO mapping generated from config/compiler layout.
  - Input/output domain copy must be deterministic and non-blocking inside scan.

- [ ] Add EtherCAT fault state handling
  - Link down, slave missing, PDO watchdog timeout, working-counter mismatch,
    and safe output fallback.

- [ ] Validate under bus load
  - Measure scan jitter with EtherCAT traffic enabled.
  - Record bus cycle time, WKC, slave state, and dropped/error frames.

- [ ] Add real hardware demo
  - ST code -> generated C++ -> runtime -> EtherCAT PDO -> real servo or IO.

- [ ] Define absolute address mapping contract
  - `%IX/%QX/%IB/%QB/%IW/%QW/...` must map predictably to ProcessImage/PDO.

## P2 — Diagnostics and Online Engineering Hooks

- [x] Add structured diagnostics API
  - Machine-readable snapshot instead of only `printDiag()`.
  - Include system state, task stats, scan stats, errors, watchdog, and IO state.
  - Implemented as fixed-capacity `DiagSnapshot` via `Scheduler::snapshotDiag()`.

- [ ] Add variable watch/trace hooks
  - Read GVL/ProcessImage by symbol metadata and offset.
  - Keep the hard real-time path free from blocking transport code.

- [ ] Add force/override mechanism
  - Force inputs/outputs/internal variables with clear ownership and expiry.
  - Log force activity for audit and safety review.
  - Route I/O forcing through `IoManager`, while symbol/GVL forcing can remain
    a separate diagnostics feature.

- [ ] Add alarm/event history
  - Fixed-size ring buffer.
  - Include timestamp, source, severity, acknowledge state, and payload.

- [ ] Add remote diagnostic transport adapters
  - OPC UA, MQTT, or a small custom binary protocol can sit outside the core.

## P2 — Runtime API and ABI Hardening

- [ ] Version the generated-code/runtime ABI
  - Include ABI version, GVL layout hash, ProcessImage size, and compiler ID.

- [ ] Add config validation at startup
  - Task intervals, priorities, watchdog limits, RETAIN ranges, and TCI mappings.

- [ ] Document capacity limits
  - `MAX_TASKS`, `MAX_PROGRAMS`, `MAX_EVENTS`, `MAX_POUS_PER_TASK`, GVL size,
    ProcessImage size, and expected RAM footprint.

- [ ] Add graceful safe-state transition
  - On fatal runtime error, explicitly drive outputs to configured safe values.

- [ ] Add hardware watchdog interface
  - Software watchdog detects logical overruns.
  - Hardware watchdog resets the target if the runtime itself stalls.

## P3 — Safety Path

- [ ] Expand `SafetyModule` beyond placeholder interface
  - Safe inputs, safe outputs, safe state, cyclic safety check, and fault latch.

- [ ] Add fault-injection tests
  - Divide by zero, array OOB, task overrun, corrupted RETAIN, IO timeout,
    fieldbus disconnect, and watchdog failure.

- [ ] Prepare certification evidence structure
  - Requirements traceability matrix
  - FMEA/FTA
  - Test evidence index
  - Coding standard checklist

- [ ] Decide safety scope honestly
  - Non-safety controller first, safety-rated runtime later.
  - Do not market as SIL-capable before the evidence exists.

## P3 — IEC Library and Ecosystem

- [ ] Complete and verify standard IEC functions and function blocks
  - Timers, counters, triggers, string functions, math functions, conversion
    edge cases, and time/date operations.

- [ ] Add PLCopen-style motion-control foundation
  - Axis object model, enable/disable, homing, move absolute/relative, stop,
    fault reset, and per-axis timeout.

- [ ] Add conformance-oriented tests
  - Golden ST programs with expected scan-by-scan behavior.

## Completed Historical Items

These were previously tracked here and are kept as context:

- [x] TaskExecutor extraction from Scheduler
- [x] IoManager extraction around TCI/CompositeTCI
- [x] DiagManager extraction around diagnostic stats and print snapshot
- [x] RetainManager extraction around RETAIN startup/save/restore lifecycle
- [x] GVL offset bounds checks
- [x] ProcessImage offset bounds checks
- [x] Task interval/priority validation
- [x] Safe division/modulo with operand logging
- [x] Safe array access support
- [x] STRUCT and ARRAY generated-code support
- [x] RETAIN region marker generation
- [x] ErrorManager extraction to independent header
- [x] Runtime submodule split under `include/core`
