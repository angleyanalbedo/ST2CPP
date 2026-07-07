# RK3588 GPIO Scan Daemon Plan

## Goal

The RK3588 Alchemy PLC task must not call Linux GPIO cdev `ioctl()` from the
real-time cycle. Direct cdev access from the Alchemy primary domain causes
primary-to-secondary domain switches and can produce millisecond-level jitter.

The GPIO path is split into two parts:

```text
Linux non-real-time GPIO scan daemon
  -> uses /dev/gpiochipN and ioctl()
  -> updates shared input/output snapshots

Xenomai Alchemy PLC task
  -> calls TCI::syncInputs()/syncOutputs()
  -> only reads/writes shared memory snapshots
  -> does not call open/read/write/ioctl/close
```

This trades a small amount of I/O freshness for deterministic PLC cycle timing.
The worst-case I/O age is approximately:

```text
gpio_scan_period + plc_cycle_period
```

For a 100 us GPIO scan period and a 1000 us PLC cycle, the worst-case input age
is about 1.1 ms.

## Files

Use the existing RK3588 HAL layout:

```text
target/rk3588/hal/gpio_hal.h
target/rk3588/hal/gpio_hal.cpp
target/rk3588/hal/gpio_scan.h
target/rk3588/hal/gpio_scan.cpp
target/rk3588/hal/gpio_tci.h
target/rk3588/hal/gpio_tci.cpp
```

Responsibilities:

| File | Responsibility |
| --- | --- |
| `gpio_hal.*` | Linux GPIO cdev access, including `GPIOHANDLE_*_IOCTL` |
| `gpio_scan.*` | Non-real-time scan daemon thread and shared snapshots |
| `gpio_tci.*` | Real-time-safe TCI adapter for `ProcessImage` |

## GpioScanDaemon API

Prefer `GpioScanDaemon` as the class name. It is a daemon-like service, not part
of the real-time PLC scan.

Recommended interface:

```cpp
class GpioScanDaemon {
public:
    int start(const rk_gpio_pin_t* pins, int count, int scanPeriodUs = 100);
    void stop();

    int readInput(int halIndex) const;
    void writeOutput(int halIndex, int value);

    int pinCount() const;

private:
    std::atomic<int> inputValues[RK_GPIO_SCAN_MAX_PINS];
    std::atomic<int> outputValues[RK_GPIO_SCAN_MAX_PINS];
    std::atomic<bool> running;
    pthread_t thread;
};
```

Use `std::atomic<int>` instead of `volatile int`. `volatile` does not provide
cross-thread synchronization semantics. `std::memory_order_relaxed` is enough
because GPIO state is a sampled snapshot; no multi-field ordering contract is
required.

Example:

```cpp
int GpioScanDaemon::readInput(int halIndex) const {
    return inputValues[halIndex].load(std::memory_order_relaxed);
}

void GpioScanDaemon::writeOutput(int halIndex, int value) {
    outputValues[halIndex].store(value, std::memory_order_relaxed);
}
```

## Scan Loop

The daemon loop runs in Linux context and may call cdev `ioctl()`.

Recommended loop:

```cpp
while (running.load(std::memory_order_relaxed)) {
    rk_gpio_read_all(localInput);

    for each pin:
        inputValues[i].store(localInput[i], std::memory_order_relaxed);

    for each pin:
        localOutput[i] = outputValues[i].load(std::memory_order_relaxed);

    rk_gpio_write_all(localOutput);

    sleep_until_next_scan_period();
}
```

Do not use a pure spin loop by default. Start with a 100 us or 200 us scan
period, then measure jitter and I/O latency. A spin loop can consume a core and
may interfere with Linux services or remote access.

## Thread Priority And CPU Affinity

Recommended initial configuration:

```text
PLC Alchemy task:
  priority 90
  isolated CPU, for example cpu6

GPIO scan daemon:
  SCHED_OTHER, or SCHED_FIFO priority 1 only after measurement
  not pinned to the same isolated CPU initially
```

Do not let the GPIO scan daemon compete with the PLC task on the isolated PLC
core until measurements show it is safe.

## Real-Time TCI Rules

`Rk3588GpioTCI::syncInputs()` and `Rk3588GpioTCI::syncOutputs()` must be
real-time safe.

Allowed:

```cpp
daemon.readInput(halIndex);
daemon.writeOutput(halIndex, rawValue);
img.writeInputBit(byteOffset, bitOffset, logicalValue);
img.readOutputBit(byteOffset, bitOffset);
```

Forbidden in `syncInputs()` / `syncOutputs()`:

```text
open()
close()
read()
write()
ioctl()
malloc()
free()
printf()
fprintf()
JSON parsing
filesystem access
```

`syncInputs()`:

```cpp
int raw = daemon.readInput(halIndex);
bool logical = activeLow ? !raw : raw;
img.writeInputBit(byteOffset, bitOffset, logical);
```

`syncOutputs()`:

```cpp
bool logical = img.readOutputBit(byteOffset, bitOffset);
int raw = activeLow ? !logical : logical;
daemon.writeOutput(halIndex, raw);
```

## Configuration

Keep JSON parsing out of the RK3588 runtime. `target/scripts/gen_config.py`
should generate C++ mapping calls at build time.

Input:

```json
{
  "driver": "gpio",
  "inputs": [
    {"addr": "%IX0.0", "gpio": 140, "active_low": true},
    {"addr": "%IX0.1", "gpio": 141, "active_low": true}
  ],
  "outputs": [
    {"addr": "%QX0.0", "gpio": 139}
  ]
}
```

Generated C++:

```cpp
static Rk3588GpioTCI gpio;
gpio.addInputMapping(140, 0, 0, true);
gpio.addInputMapping(141, 0, 1, true);
gpio.addOutputMapping(139, 0, 0, false);
if (gpio.init() == 0) {
    tci.add(&gpio);
}
```

`Rk3588GpioTCI::loadMapping()` can remain as a developer helper, but production
Alchemy builds should use generated mapping calls.

## Build Requirements

All RK3588 builds that use GPIO TCI must link all three HAL components:

```text
hal/gpio_hal.cpp
hal/gpio_tci.cpp
hal/gpio_scan.cpp
```

Check these Makefile targets:

```text
remote-rt-build
remote-alchemy-build
stress
stub
```

If `gpio_tci.cpp` owns a `GpioScanDaemon` member, missing `gpio_scan.cpp` will
either fail the link or silently prevent the intended GPIO isolation path from
being tested.

## MatriBox GPIO Notes

Known bindings:

```text
gpio140 -> %IX0.0, plc_run, active low
gpio141 -> %IX0.1, plc_stop, active low
gpio139 -> %QX0.0
```

`gpio140` and `gpio141` may be held by the `gpio-keys` driver. Release them
before running GPIO tests:

```bash
echo "gpio-keys" | sudo tee /sys/bus/platform/drivers/gpio-keys/unbind
```

## Test Plan

### 1. Build Only

Confirm the Alchemy target links `gpio_scan.cpp`.

```bash
cd target/rk3588
make alchemy STRESS_LOAD=5
```

### 2. GPIO Functional Test

Run a non-real-time or non-PLC GPIO check first. The goal is only to verify
mapping direction:

```text
%IX0.0 / %IX0.1 read physical input state
%QX0.0 writes physical output state
active_low handling is correct
```

### 3. Alchemy With GPIO, Light Load

```bash
sudo ./plc_runtime_rk3588_alchemy \
  --cycle-us 1000 \
  --cpu 6 \
  --rt-prio 90 \
  --diag-interval 5
```

Use `STRESS_LOAD=5`.

Pass criteria:

```text
max jitter < +30 us
std < 5 us
overruns = 0
watchdog = 0
errors = 0
fatal = 0
```

### 4. Alchemy With GPIO, Heavy Load

Repeat with `STRESS_LOAD=20` after the light-load run passes.

Pass criteria remain the same. If jitter increases but overrun remains zero,
record the I/O scan period and CPU affinity before changing code.

## Decision Criteria

This design is accepted if:

```text
GPIO-enabled Alchemy run has no domain-switch jitter spikes
1 ms cycle keeps overrun = 0 under STRESS_LOAD=5 and STRESS_LOAD=20
GPIO input/output mapping remains correct
SSH/system responsiveness is preserved during the test
```

If this design still causes large jitter spikes, the next step is an RTDM GPIO
driver or a proven direct-register path that can run entirely in Xenomai primary
mode.
