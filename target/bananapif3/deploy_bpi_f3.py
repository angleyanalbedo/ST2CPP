#!/usr/bin/env python3
"""
deploy_bpi_f3.py — 打包 ST2C 运行时并上传到 Banana Pi BPI-F3。

用法：
    python deploy_bpi_f3.py                                    # 默认打包上传
    python deploy_bpi_f3.py --build                            # 先编译再上传
    python deploy_bpi_f3.py --ip 192.168.137.239 --password 1234
"""
import os, sys, subprocess, tarfile, tempfile, argparse, paramiko, time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
TARGET_NAME = "bananapif3"

def parse_args():
    parser = argparse.ArgumentParser(description="Deploy to Banana Pi BPI-F3")
    parser.add_argument("--ip", default="192.168.137.239")
    parser.add_argument("--port", type=int, default=22)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="1234")
    parser.add_argument("--build", action="store_true", help="Run Java compiler first")
    parser.add_argument("--cycle-us", type=int, default=1000, help="PLC cycle in us")
    parser.add_argument("--stub", action="store_true", help="Build stub (no POU)")
    parser.add_argument("--jitter-test", action="store_true", help="Run jitter test only")
    return parser.parse_args()

def build_java():
    print("[*] Building Java compiler...")
    java_dir = os.path.join(PROJECT_ROOT, "java")
    result = subprocess.run(["mvn", "package", "-DskipTests", "-q"], cwd=java_dir,
                          capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Java build failed:\n{result.stderr}")
        return False
    print("[+] Java compiler built")
    return True

def generate_registry():
    gen_py = os.path.join(SCRIPT_DIR, "gen_registry.py")
    if not os.path.exists(gen_py):
        gen_py = os.path.join(PROJECT_ROOT, "target", "rpi", "gen_registry.py")
    build_dir = os.path.join(PROJECT_ROOT, "output", "flat", "build")
    result = subprocess.run(["python", gen_py,
                           "--build-dir", build_dir,
                           "--output", os.path.join(build_dir, "pou_registry.gen.cpp")],
                          capture_output=True, text=True)
    if result.returncode == 0:
        print(f"[+] Registry generated")
    else:
        print(f"[-] Registry generation failed: {result.stderr}")

def package(include_pou=True):
    print("[*] Packaging runtime...")
    tar_path = os.path.join(tempfile.gettempdir(), "st2c_bpi_f3.tar.gz")
    with tarfile.open(tar_path, "w:gz") as tar:
        runtime_dir = os.path.join(PROJECT_ROOT, "runtime-flat")
        tar.add(runtime_dir, arcname="runtime-flat/")
        target_dir = os.path.join(PROJECT_ROOT, "target")
        tar.add(target_dir, arcname="target/")
        if include_pou:
            build_dir = os.path.join(PROJECT_ROOT, "output", "flat", "build")
            if os.path.isdir(build_dir):
                tar.add(build_dir, arcname="output/flat/build")
    print(f"[+] Packaged: {tar_path}")
    return tar_path

def upload(client, local_path, remote_path):
    print(f"[*] Uploading to {remote_path}...")
    sftp = client.open_sftp()
    sftp.put(local_path, remote_path)
    sftp.close()
    print("[+] Upload done")

def run_remote(client, cmd, timeout=30):
    print(f"[*] Remote: {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    exit_status = stdout.channel.recv_exit_status()
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    if out: print(out)
    if err: print(f"[stderr] {err}", file=sys.stderr)
    return exit_status, out, err

def main():
    args = parse_args()

    if args.build:
        if not build_java():
            sys.exit(1)

    generate_registry()

    include_pou = not args.stub
    tar_path = package(include_pou)

    print(f"[*] Connecting to {args.ip}:{args.port}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.ip, args.port, args.user, args.password, timeout=10)
    print("[+] Connected")

    stdin, stdout, stderr = client.exec_command("pwd")
    home = stdout.read().decode().strip()
    remote_dir = f"{home}/st2c-runtime"
    client.exec_command(f"mkdir -p {remote_dir}")

    upload(client, tar_path, f"{remote_dir}/st2c_bpi_f3.tar.gz")
    run_remote(client, f"cd {remote_dir} && tar xzf st2c_bpi_f3.tar.gz")

    if args.jitter_test:
        target_dir = f"{remote_dir}/target/{TARGET_NAME}"
        cmd = f"cd {target_dir} && g++ -O2 -DRT_PLATFORM_LINUX jitter_test.cpp -lpthread -o jitter_test && timeout 10 taskset -c 3 sudo ./jitter_test --cycle-us {args.cycle_us} 2>&1"
    elif args.stub:
        cmd = f"cd {remote_dir}/target/{TARGET_NAME} && make stub && sudo taskset -c 3 ./plc_runtime_stub --cycle-us {args.cycle_us}"
    else:
        cmd = f"cd {remote_dir}/target/{TARGET_NAME} && make && sudo taskset -c 3 ./plc_runtime_bpi_f3 --cycle-us {args.cycle_us}"

    run_remote(client, "apt-get install -y build-essential" if args.jitter_test else "", timeout=60)
    run_remote(client, cmd, timeout=30)

    client.close()
    print("[+] Done")

if __name__ == "__main__":
    main()
