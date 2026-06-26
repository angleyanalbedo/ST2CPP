#!/usr/bin/env python3
"""
gen_registry.py — 从编译器生成的 .cpp 中提取 registerPOU_xxx，
                  生成 pou_registry.gen.cpp 桥接文件。

用法：
    python gen_registry.py                           # 自动发现 output/flat/build/*.cpp
    python gen_registry.py --build-dir path/to/dir   # 指定目录
    python gen_registry.py --output path/to/file     # 指定输出文件
"""
import os
import re
import glob
import argparse

def main():
    parser = argparse.ArgumentParser(description="Generate POU registry bridge")
    parser.add_argument("--build-dir", default=None,
                        help="Compiler output directory (default: ../../output/flat/build)")
    parser.add_argument("--output", default=None,
                        help="Output file (default: <build-dir>/pou_registry.gen.cpp)")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    if args.build_dir:
        build_dir = args.build_dir
    else:
        build_dir = os.path.join(script_dir, "..", "..", "output", "flat", "build")
    build_dir = os.path.abspath(build_dir)

    if not os.path.isdir(build_dir):
        print(f"Error: build directory not found: {build_dir}")
        return 1

    # Find all .cpp files (excluding this registry file itself)
    cpp_files = glob.glob(os.path.join(build_dir, "*.cpp"))
    cpp_files = [f for f in cpp_files if not os.path.basename(f).startswith("pou_registry")]

    # Extract registerPOU_xxx function names
    pattern = re.compile(r"\bvoid\s+(registerPOU_\w+)\s*\(")
    reg_funcs = set()
    for cpp in cpp_files:
        with open(cpp, "r", encoding="utf-8", errors="replace") as fh:
            for m in pattern.finditer(fh.read()):
                reg_funcs.add(m.group(1))

    reg_funcs = sorted(reg_funcs)

    # Generate bridge
    output = args.output or os.path.join(build_dir, "pou_registry.gen.cpp")
    with open(output, "w", encoding="utf-8") as out:
        out.write("// Auto-generated: registerAllPOUs bridge\n")
        out.write("// Source: compiler-generated POU .cpp files\n")
        out.write('#include "rt_plc.h"\n')
        out.write("using namespace rt_plc;\n\n")

        for fn in reg_funcs:
            out.write(f"extern void {fn}(POURegistry& reg);\n")
        out.write("\n")

        out.write("void registerAllPOUs(POURegistry& reg) {\n")
        for fn in reg_funcs:
            out.write(f"    {fn}(reg);\n")
        out.write("}\n")

    print(f"Generated {output} with {len(reg_funcs)} registration call(s)")
    for fn in reg_funcs:
        print(f"  {fn}")
    return 0

if __name__ == "__main__":
    exit(main())
