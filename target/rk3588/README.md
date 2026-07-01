# target/rk3588 — RK3588 PREEMPT_RT PLC Runtime

Target for an RK3588 PLC running a realtime Linux kernel.

## Quick Test On The Board

```bash
cd target/rk3588
make stub
sudo ./plc_runtime_rk3588_stub --cycle-us 1000 --cpu 6 --rt-prio 90
```

Jitter-only mode:

```bash
sudo ./plc_runtime_rk3588_stub --jitter-only --cycle-us 1000 --cpu 6 --rt-prio 90
```

## Remote Workflow

Default board address is `192.168.5.130`.

```bash
cd target/rk3588
make deploy RK_HOST=192.168.5.130 RK_USER=root
make remote-jitter RK_HOST=192.168.5.130 RK_USER=root
make remote-run RK_HOST=192.168.5.130 RK_USER=root
```

Override user/path if needed:

```bash
make deploy RK_HOST=192.168.5.130 RK_USER=linaro RK_DIR=/home/linaro/st2cpp
```

## Runtime Options

| Option | Default | Meaning |
| --- | --- | --- |
| `--cycle-us N` | `1000` | PLC cycle period |
| `--rt-prio N` | `90` | `SCHED_FIFO` realtime priority |
| `--cpu N` | `6` | CPU affinity, use `-1` to disable |
| `--diag-interval N` | `5` | periodic diagnostics interval |
| `--warm` | off | start with `StartupMode::WARM` |
| `--jitter-only` | off | measure timer jitter without PLC init |

## Realtime Linux Checklist

```bash
uname -a
grep PREEMPT_RT /boot/config-$(uname -r)
sudo sysctl kernel.sched_rt_runtime_us=-1
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor
sudo systemctl stop irqbalance
```

For best results, reserve one big core for PLC runtime in your boot args, then
run with the matching `--cpu` value.

Example kernel args:

```text
isolcpus=6 nohz_full=6 rcu_nocbs=6
```

## I/O Binding

The default `config/config.json` has no physical bindings so the first test is
only scheduler/runtime timing. Add bindings later, for example EtherCAT:

```json
"bindings": [
  {
    "driver": "ethercat",
    "ifname": "eth1"
  }
]
```

Then add SOEM/libpcap support in the same style as `target/linux`.
