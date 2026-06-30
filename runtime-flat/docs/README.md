# runtime-flat — hard real-time flat-memory runtime

`runtime-flat` is the ST2C PLC runtime core. It is intentionally small,
static, and predictable: generated POU code runs against a flat GVL memory
area, a process image, and a deterministic scan scheduler.

The current goal is not to imitate a full desktop PLC engineering suite. The
goal is to build a runtime kernel that can eventually run on bare metal or a
minimal RTOS with microsecond-level timing behavior.

## Current Position

`runtime-flat` is an industrial-runtime kernel alpha:

- It has a usable PLC execution skeleton: GVL, ProcessImage, PROGRAM lifecycle,
  task scheduler, event tasks, watchdog, diagnostics, and runtime errors.
- It is static by design: fixed-size arrays, no heap requirement in the core
  path, and compile-time capacity limits.
- It already has target bindings for desktop/Linux and bare-metal style STM32
  entry points.
- It is not yet a commercial PLC runtime. The missing work is mostly proof,
  fieldbus behavior, persistence, diagnostics, and online engineering features.

## Directory Layout

```text
runtime-flat/
├── include/
│   ├── rt_plc.h                  # ProcessImage, TCI, safe-operation macros, legacy Runtime
│   ├── rt_runtime.h              # unified public runtime include
│   ├── core/
│   │   ├── constants.h           # static limits and configurable sizes
│   │   ├── types.h               # IEC scalar types, time/date helpers, conversions
│   │   ├── gvl.h                 # flat global variable memory and RETAIN region
│   │   ├── error_manager.h       # runtime error ring buffer and safe math helpers
│   │   ├── task.h                # task definition and POU attachment
│   │   ├── program.h             # PROGRAM instance lifecycle
│   │   ├── event.h               # event-triggered task conditions
│   │   ├── watchdog.h            # software watchdog
│   │   ├── diag.h                # scan and overrun statistics
│   │   ├── platform.h            # desktop/Linux/bare-metal platform abstraction
│   │   └── registry.h            # generated POU callback registry support
│   ├── scheduler/
│   │   └── scheduler.h           # Scheduler API
│   └── safety/
│       └── safety_module.h       # safety extension interface placeholder
├── src/                          # runtime implementation
├── tests/                        # desktop tests and demos
├── CMakeLists.txt                # static libraries and optional tests
└── docs/
    ├── README.md
    └── TODO.md
```

Platform-specific runtime entry points live outside this directory under
`target/<platform>/`.

## Build

```bash
cd runtime-flat
mkdir build
cd build
cmake .. -G "MinGW Makefiles"
cmake --build .
```

Enable tests:

```bash
cmake .. -G "MinGW Makefiles" -DRT_BUILD_TESTS=ON
cmake --build .
```

Expected test/demo targets:

- `framework_test`
- `runtime_integration_test`
- `unit_test`
- `fibonacci`
- `multitask_demo`

The core library is C++17 and is built as static libraries:

- `rt_core`
- `rt_scheduler`

## Core Model

### Flat GVL Memory

All global variables are stored in `GVL.memory`. The compiler owns layout:
offsets, alignment, RETAIN placement, and generated access code.

Runtime access uses:

```cpp
gvl.read<T>(offset);
gvl.write<T>(offset, value);
gvl.ptr<T>(offset);
```

Important constraints:

- Generated offsets must respect target alignment requirements.
- ARM targets can fault on unaligned access, so compiler layout is part of the
  runtime safety contract.
- The default GVL size is 64 KB and can be overridden at compile time.

### Process Image and TCI

`ProcessImage` owns input and output byte arrays. A `TCI` implementation copies
real hardware state into and out of the image:

```text
syncInputs()
  -> generated PLC logic reads ProcessImage/GVL
syncOutputs()
  -> target driver pushes outputs to hardware or fieldbus
```

`CompositeTCI` can combine several backends, such as GPIO and EtherCAT.

### Scheduler

The scheduler executes a scan in four phases:

```text
ReadInputs -> LogicSolve -> WriteOutputs -> Housekeeping
```

Supported task types:

- Cyclic tasks
- Event tasks
- Freewheeling tasks

Current scheduler semantics are cooperative and single-threaded. Task priority
controls execution order inside a scan; it is not preemptive priority scheduling.
That is suitable for small MCU deployments, but it must be documented and tested
as a product constraint.

### PROGRAM Lifecycle

Generated PROGRAM instances follow this lifecycle:

```text
Init -> Pre -> FirstScan -> Cyclic -> Post
```

The generated cyclic function signature is:

```cpp
void name(rt_plc::GVL& gvl, rt_plc::ProcessImage& io, rt_plc::TIME cycleTimeUs);
```

### Errors and Watchdog

`ErrorManager` records runtime errors in a fixed ring buffer and exposes safe
helpers for division, modulo, arithmetic overflow checks, and array bounds.

The software watchdog checks task execution time when diagnostics timing is
available. Product-grade behavior still needs a single consistent path for
cyclic and event tasks, plus target-level hardware watchdog integration.

## Commercial Gap Summary

The runtime needs four major upgrades before it can be honestly presented as a
commercial PLC runtime:

1. Determinism evidence: WCET, jitter, long-run stability, interrupt latency,
   and overrun behavior on real hardware.
2. Fieldbus behavior: EtherCAT/PDO mapping, fault recovery, distributed clock
   handling, bus-load jitter validation, and real servo/IO tests.
3. Persistence and diagnostics: true RETAIN storage, CRC/versioning, trace,
   online variable monitoring, force, alarms, and remote diagnostics.
4. Safety and certification path: safe state, hardware watchdog, fault
   injection, FMEA/FTA, requirements traceability, and eventually SIL evidence.

## Development Rules

- Keep the runtime core allocation-free and static where practical.
- Keep platform-specific timing and hardware code under `target/<platform>/`.
- Add tests with every behavioral change.
- Treat generated compiler layout as part of the ABI.
- Keep event-task, cyclic-task, and watchdog semantics consistent.
- Avoid adding engineering-station features directly into the hard real-time
  inner loop; expose stable hooks instead.

## Known Risks

- STM32 DWT `CYCCNT` is 32-bit on common Cortex-M parts and wraps quickly at
  high clock rates. Long-run timing needs wrap-safe accounting.
- Event tasks currently need the same watchdog/error path as cyclic tasks.
- RETAIN is a memory model feature today, not yet a real non-volatile storage
  system.
- Linux timing quality depends on kernel configuration, privileges, CPU
  isolation, and scheduler noise.
- Windows desktop timing is useful for functional tests only, not microsecond
  jitter validation.
