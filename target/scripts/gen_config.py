#!/usr/bin/env python3
"""
gen_config.py — Build-time configuration code generator.

Reads config.json + scans compiler-generated POU .cpp files,
produces runtime_config.gen.cpp with hardcoded configureRuntime().

Runtime zero parsing — everything is compiled in.

Usage:
    python gen_config.py --target rpi --input config/config.json --output build/runtime_config.gen.cpp
    python gen_config.py --target windows --input config/config.json
    python gen_config.py --target stm32f1 --input config/config.json --build-dir ../../output/flat/build

Supported targets: windows, desktop, linux, rpi, rk3588, stm32f1, bananapif3
Supported drivers: gpio, ethercat, tcp, network
"""
import json
import os
import re
import glob
import argparse
import sys


def parse_addr(addr_str):
    """Parse IEC address like 'QX0.0', 'IX0.1', 'QD0', 'IW100' → (direction, byte_offset, bit_offset)."""
    s = addr_str.strip().lstrip('%')
    direction = None
    bit_off = -1
    byte_off = 0

    if s.startswith('IX'):
        direction = 'input'
        rest = s[2:]
        if '.' in rest:
            parts = rest.split('.')
            byte_off = int(parts[0])
            bit_off = int(parts[1])
        else:
            byte_off = int(rest)
    elif s.startswith('IB'):
        direction = 'input'
        byte_off = int(s[2:])
    elif s.startswith('IW'):
        direction = 'input'
        byte_off = int(s[2:])
    elif s.startswith('ID'):
        direction = 'input'
        byte_off = int(s[2:])
    elif s.startswith('QX'):
        direction = 'output'
        rest = s[2:]
        if '.' in rest:
            parts = rest.split('.')
            byte_off = int(parts[0])
            bit_off = int(parts[1])
        else:
            byte_off = int(rest)
    elif s.startswith('QB'):
        direction = 'output'
        byte_off = int(s[2:])
    elif s.startswith('QW'):
        direction = 'output'
        byte_off = int(s[2:])
    elif s.startswith('QD'):
        direction = 'output'
        byte_off = int(s[2:])
    else:
        raise ValueError(f"Cannot parse address: {addr_str}")

    return direction, byte_off, bit_off


def scan_pou_registrations(build_dir, exclude=None):
    """Scan compiler output .cpp files for registerPOU_xxx functions."""
    if not os.path.isdir(build_dir):
        return []

    cpp_files = glob.glob(os.path.join(build_dir, "*.cpp"))
    cpp_files = [f for f in cpp_files if not os.path.basename(f).startswith("pou_registry")
                 and not os.path.basename(f).startswith("runtime_config")]
    if exclude:
        cpp_files = [f for f in cpp_files if os.path.basename(f) not in exclude]

    pattern = re.compile(r"\bvoid\s+(registerPOU_\w+)\s*\(")
    reg_funcs = set()
    for cpp in cpp_files:
        with open(cpp, "r", encoding="utf-8", errors="replace") as fh:
            for m in pattern.finditer(fh.read()):
                reg_funcs.add(m.group(1))

    return sorted(reg_funcs)


TARGET_INCLUDES = {
    'windows': [],
    'desktop': [],
    'linux': [],
    'rk3588': [],
    'rpi': ['"hal/gpio_tci.h"'],
    'bananapif3': ['"hal/gpio_tci.h"'],
    'stm32f1': [],
}

ETHERCAT_CONDITIONAL = True


def generate_gpio_code(binding, target):
    """Generate GPIO TCI setup code."""
    lines = []
    if target == 'bananapif3':
        lines.append('    static BpiGpioTCI gpio;')
    elif target == 'rpi':
        lines.append('    static RpiGpioTCI gpio;')
    else:
        lines.append(f"    // WARNING: gpio driver only supported on rpi/bananapif3 targets\n")
        return lines

    for inp in binding.get('inputs', []):
        addr = inp['addr']
        _, byte_off, bit_off = parse_addr(addr)
        pin = inp.get('bcm') or inp.get('line') or inp.get('gpio') or 0
        if bit_off >= 0:
            lines.append(f'    gpio.addInputMapping({pin}, {byte_off}, {bit_off});  // {addr}')
        else:
            lines.append(f'    gpio.addInputMapping({pin}, {byte_off}, 0);  // {addr}')

    for out in binding.get('outputs', []):
        addr = out['addr']
        _, byte_off, bit_off = parse_addr(addr)
        pin = out.get('bcm') or out.get('line') or out.get('gpio') or 0
        if bit_off >= 0:
            lines.append(f'    gpio.addOutputMapping({pin}, {byte_off}, {bit_off});  // {addr}')
        else:
            lines.append(f'    gpio.addOutputMapping({pin}, {byte_off}, 0);  // {addr}')

    lines.append('    if (gpio.init() == 0) { tci.add(&gpio); }')
    lines.append('    else { platform::logErr("GPIO TCI init failed\\n"); }')
    return lines


def generate_ethercat_code(binding, target):
    """Generate EtherCAT TCI setup code."""
    lines = []
    ifname = binding.get('ifname', 'eth0')

    lines.append('#if defined(RT_ETHERCAT_ENABLED)')
    lines.append(f'    static EthercatTCI ecat;')
    lines.append(f'    if (ecat.init("{ifname}") == 0) {{')
    lines.append(f'        tci.add(&ecat);')
    lines.append(f'        platform::logInfo("EtherCAT TCI registered on {ifname}\\n");')
    lines.append(f'    }} else {{')
    lines.append(f'        platform::logErr("EtherCAT TCI init failed on {ifname}\\n");')
    lines.append(f'    }}')

    pdo_inputs = binding.get('inputs', [])
    pdo_outputs = binding.get('outputs', [])
    if pdo_inputs or pdo_outputs:
        lines.append('    // TODO: PDO mapping is hardcoded in pdo_config.h for now')
        lines.append('    // Future: generate pdo_config.h from config.json bindings')

    lines.append('#else')
    lines.append('    platform::logErr("EtherCAT not enabled at compile time\\n");')
    lines.append('#endif')
    return lines


def generate_tcp_code(binding, target):
    """Generate TcpTCI setup code."""
    lines = []
    host = binding.get('host', '127.0.0.1')
    port = binding.get('port', 5000)
    reconnect = binding.get('reconnect_ms', 3000)

    lines.append(f'    static rt_plc::TcpTCI tcp("{host}", {port}, {reconnect});')

    for m in binding.get('map', []):
        plc_off = m['plc_offset']
        net_off = m.get('net_offset', 0)
        length = m['length']
        lines.append(f'    tcp.map({plc_off}, {net_off}, {length});')

    lines.append('    tci.add(&tcp);')
    return lines


def generate_network_code(binding, target):
    """Generate NetworkTCI setup code."""
    lines = []
    host = binding.get('host', '127.0.0.1')
    port = binding.get('port', 5000)
    reconnect = binding.get('reconnect_ms', 3000)
    mode = binding.get('mode', 'client')

    lines.append('    static rt_plc::NetworkTCI net;')
    if mode == 'server':
        lines.append(f'    net.begin({port}, {reconnect});')
    else:
        lines.append(f'    net.begin("{host}", {port}, {reconnect});')

    for m in binding.get('input_maps', []):
        plc_off = m['plc_offset']
        net_off = m.get('net_offset', 0)
        length = m['length']
        lines.append(f'    net.mapInput({plc_off}, {net_off}, {length});')

    for m in binding.get('output_maps', []):
        plc_off = m['plc_offset']
        net_off = m.get('net_offset', 0)
        length = m['length']
        lines.append(f'    net.mapOutput({plc_off}, {net_off}, {length});')

    lines.append('    tci.add(&net);')
    return lines


DRIVER_GENERATORS = {
    'gpio': generate_gpio_code,
    'ethercat': generate_ethercat_code,
    'tcp': generate_tcp_code,
    'network': generate_network_code,
}


def generate(config, target, build_dir, stub=False, exclude=None):
    """Generate runtime_config.gen.cpp content."""
    reg_funcs = [] if stub else scan_pou_registrations(build_dir, exclude)

    base_cycle = config.get('base_cycle_us', 1000)
    tasks = config.get('tasks', [])
    bindings = config.get('bindings', [])

    includes_needed = set()
    includes_needed.add('"rt_plc.h"')
    includes_needed.add('"rt_runtime.h"')

    target_incs = TARGET_INCLUDES.get(target, [])
    for inc in target_incs:
        includes_needed.add(inc)

    has_ethercat = any(b.get('driver') == 'ethercat' for b in bindings)
    has_tcp = any(b.get('driver') == 'tcp' for b in bindings)
    has_network = any(b.get('driver') == 'network' for b in bindings)
    has_gpio = any(b.get('driver') == 'gpio' for b in bindings)

    if has_ethercat:
        includes_needed.add('"ethercat_tci.h"')
    if has_tcp:
        includes_needed.add('"tcp_tci.h"')
    if has_network:
        includes_needed.add('"network_tci.h"')

    lines = []
    lines.append("// Auto-generated by gen_config.py — DO NOT EDIT")
    lines.append(f"// Target: {target}")
    lines.append(f"// Config: config.json")
    lines.append("")

    for inc in sorted(includes_needed):
        lines.append(f"#include {inc}")

    lines.append("")
    lines.append("using namespace rt_plc;")
    lines.append("")

    # POU registration
    for fn in reg_funcs:
        lines.append(f"extern void {fn}(POURegistry& reg);")
    lines.append("")

    # registerAllPOUs
    lines.append("void registerAllPOUs(POURegistry& reg) {")
    for fn in reg_funcs:
        lines.append(f"    {fn}(reg);")
    lines.append("}")
    lines.append("")

    # configureRuntime
    lines.append("void configureRuntime(Scheduler& sched, CompositeTCI& tci) {")
    lines.append("")
    lines.append("    // ─── POU Registration ───")
    lines.append("    POURegistry reg;")
    lines.append("    registerAllPOUs(reg);")
    lines.append("")

    # TCI bindings
    lines.append("    // ─── TCI Bindings ───")
    if not bindings:
        lines.append("    // No bindings configured")
    else:
        for binding in bindings:
            driver = binding.get('driver', '')
            gen_func = DRIVER_GENERATORS.get(driver)
            if gen_func:
                lines.append(f"    // --- {driver} ---")
                code_lines = gen_func(binding, target)
                lines.extend(code_lines)
                lines.append("")

    lines.append("    if (tci.count() > 0) {")
    lines.append("        sched.setTCI(&tci);")
    lines.append("        platform::logInfo(\"TCI: %d backend(s) active\\n\", tci.count());")
    lines.append("    }")
    lines.append("")

    # Task configuration
    lines.append("    // ─── Task Configuration ───")
    lines.append(f"    sched.setBaseCycle(T_us({base_cycle}));")
    lines.append("")

    if not tasks:
        # Fallback: auto-register each POU in its own task
        lines.append("    // No tasks configured — auto-register each POU")
        lines.append("    for (int i = 0; i < reg.count(); i++) {")
        lines.append("        const auto& e = reg.entries()[i];")
        lines.append("        if (e.cbs.cyclic) {")
        lines.append("            int taskIdx = sched.addCyclicTask(e.name, 5, T_ms(10));")
        lines.append("            int progIdx = sched.addProgram(e.name, e.cbs.init, e.cbs.cyclic, e.cbs.pre, e.cbs.post);")
        lines.append("            if (progIdx >= 0) sched.addProgramToTask(taskIdx, progIdx);")
        lines.append("            sched.setTaskWatchdog(taskIdx, T_ms(5));")
        lines.append("        }")
        lines.append("    }")
    else:
        for ti, task in enumerate(tasks):
            name = task.get('name', f'Task{ti}')
            priority = task.get('priority', 5)
            interval = task.get('interval_us', 10000)
            watchdog = task.get('watchdog_us', 5000)
            pous = task.get('pous', [])

            lines.append(f'    int task{ti} = sched.addCyclicTask("{name}", {priority}, T_us({interval}));')

            for pou_name in pous:
                lines.append(f'    {{')
                lines.append(f'        const auto* cbs = reg.lookup("{pou_name}");')
                lines.append(f'        if (cbs && cbs->cyclic) {{')
                lines.append(f'            int progIdx = sched.addProgram("{pou_name}", cbs->init, cbs->cyclic, cbs->pre, cbs->post);')
                lines.append(f'            if (progIdx >= 0) sched.addProgramToTask(task{ti}, progIdx);')
                lines.append(f'        }} else {{')
                lines.append(f'            platform::logErr("POU \\"{pou_name}\\" not found in registry\\n");')
                lines.append(f'        }}')
                lines.append(f'    }}')

            lines.append(f'    sched.setTaskWatchdog(task{ti}, T_us({watchdog}));')
            lines.append("")

    lines.append("    sched.gvl.errorMgr = &sched.errorMgr;")
    lines.append("}")
    lines.append("")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Generate runtime_config.gen.cpp from config.json")
    parser.add_argument("--target", required=True,
                        choices=['windows', 'desktop', 'linux', 'rpi', 'rk3588', 'stm32f1', 'bananapif3'],
                        help="Target platform")
    parser.add_argument("--input", default=None,
                        help="Path to config.json (default: config/config.json relative to target dir)")
    parser.add_argument("--output", default=None,
                        help="Output .cpp file (default: build/runtime_config.gen.cpp)")
    parser.add_argument("--build-dir", default=None,
                        help="Compiler output directory for POU scanning (default: ../../output/flat/build)")
    parser.add_argument("--stub", action="store_true",
                        help="Generate empty POU registration (stub mode, no POU scanning)")
    parser.add_argument("--exclude", nargs="*", default=[],
                        help="Basenames to exclude from POU scanning (e.g. test_tricky.cpp)")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    target_dir = os.path.join(script_dir, "..", args.target)

    # Defaults
    if args.input:
        config_path = args.input
    else:
        config_path = os.path.join(target_dir, "config", "config.json")

    if args.build_dir:
        build_dir = args.build_dir
    else:
        build_dir = os.path.join(script_dir, "..", "..", "output", "flat", "build")
    build_dir = os.path.abspath(build_dir)

    if args.output:
        output_path = args.output
    else:
        output_path = os.path.join(target_dir, "build", "runtime_config.gen.cpp")

    # Read config
    config = {}
    if os.path.isfile(config_path):
        with open(config_path, "r", encoding="utf-8") as f:
            config = json.load(f)
        print(f"Read config: {config_path}")
    else:
        print(f"Config not found: {config_path}, using defaults")

    # Generate
    code = generate(config, args.target, build_dir, args.stub, args.exclude)

    # Write
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(code)

    print(f"Generated: {output_path}")
    pou_count = 0 if args.stub else len(scan_pou_registrations(build_dir, args.exclude))
    print(f"  POU registrations: {pou_count}")
    print(f"  Tasks: {len(config.get('tasks', []))}")
    print(f"  Bindings: {len(config.get('bindings', []))}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
