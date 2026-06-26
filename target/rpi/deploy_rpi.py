"""
deploy_rpi.py — 通过 paramiko SSH 连接树莓派，自动编译+部署 PLC 运行时

用法：
    python deploy_rpi.py                                # 远程编译 + 上传
    python deploy_rpi.py --cross                        # 本地交叉编译 + 上传
    python deploy_rpi.py --cross --jitter-test          # 抖动测试版
    python deploy_rpi.py --run                          # 编译 + 上传 + 运行
    python deploy_rpi.py --build-only                   # 仅编译（不上传）
    python deploy_rpi.py --shell                        # 交互式 SSH shell

--cross 选项使用 target/rpi/Makefile 进行本地交叉编译（需要 ARM 交叉编译器）。
默认（不带 --cross）在树莓派上远程编译。
"""

import paramiko
import os
import sys
import time
import argparse
import tarfile
import tempfile

# ═══ 配置 ═══
RPI_IP     = "192.168.5.128"
RPI_PORT   = 22
RPI_USER   = "pi"
RPI_PASS   = "123456"
REMOTE_DIR = "~/st2c-runtime"

# 本机 runtime-flat 目录
LOCAL_DIR = os.path.dirname(os.path.abspath(__file__))


def log(msg):
    print(f"\033[36m[deploy]\033[0m {msg}")


def err(msg):
    print(f"\033[31m[error]\033[0m {msg}", file=sys.stderr)


def ssh_exec(client, cmd, timeout=60):
    """执行远程命令，实时输出"""
    log(f"  $ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    exit_code = stdout.channel.recv_exit_status()

    for line in iter(stdout.readline, ""):
        print(f"    {line.rstrip()}")
    for line in iter(stderr.readline, ""):
        print(f"    \033[33m{line.rstrip()}\033[0m")

    return exit_code


def connect():
    """建立 SSH 连接"""
    log(f"Connecting to {RPI_USER}@{RPI_IP}:{RPI_PORT}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(RPI_IP, RPI_PORT, RPI_USER, RPI_PASS, timeout=10)
    log("Connected!")

    # 获取实际 home 目录（~ 在 SFTP 中不展开）
    stdin, stdout, stderr = client.exec_command("echo $HOME")
    home = stdout.read().decode().strip()
    global REMOTE_DIR
    REMOTE_DIR = f"{home}/st2c-runtime"
    log(f"Remote dir: {REMOTE_DIR}")

    return client


def upload_source(client):
    """打包并上传源码"""
    log("Packaging source files...")

    # 要上传的文件
    includes = [
        "include/",
        "src/",
        "platform_rpi.cpp",
        "runtime_rpi.cpp",
        "CMakeLists.txt",
        "deploy_rpi.sh",
    ]

    # 创建临时 tar.gz
    tar_path = os.path.join(tempfile.gettempdir(), "st2c-runtime.tar.gz")
    with tarfile.open(tar_path, "w:gz") as tar:
        for name in includes:
            path = os.path.join(LOCAL_DIR, name)
            if os.path.isdir(path):
                for root, dirs, files in os.walk(path):
                    # 跳过 build 目录
                    dirs[:] = [d for d in dirs if d != "build"]
                    for f in files:
                        full = os.path.join(root, f)
                        arcname = os.path.relpath(full, LOCAL_DIR)
                        tar.add(full, arcname=arcname)
            elif os.path.isfile(path):
                tar.add(path, arcname=name)

    tar_size = os.path.getsize(tar_path) / 1024
    log(f"Package: {tar_size:.0f} KB")

    # 确保远程目录存在
    ssh_exec(client, f"mkdir -p {REMOTE_DIR}")

    # SCP 上传
    log("Uploading...")
    sftp = client.open_sftp()
    remote_tar = f"{REMOTE_DIR}/st2c-runtime.tar.gz"
    sftp.put(tar_path, remote_tar)
    sftp.close()

    os.remove(tar_path)
    log("Upload complete!")


def build_remote(client):
    """远程编译"""
    log("Installing build tools...")
    ssh_exec(client, "sudo apt-get update -qq", timeout=120)
    ssh_exec(client, "sudo apt-get install -y -qq build-essential cmake", timeout=120)

    log("Extracting source...")
    ssh_exec(client, f"cd {REMOTE_DIR} && tar xzf st2c-runtime.tar.gz")

    log("Compiling...")
    compile_cmd = f"""cd {REMOTE_DIR} && \
g++ -O2 -std=c++17 \
    -I include/ \
    -DRT_PLATFORM_LINUX \
    src/gvl.cpp src/program.cpp src/task.cpp src/event.cpp \
    src/watchdog.cpp src/diag.cpp src/scheduler.cpp \
    platform_rpi.cpp runtime_rpi.cpp \
    -lpthread \
    -o plc_runtime_rpi 2>&1"""

    rc = ssh_exec(client, compile_cmd, timeout=120)
    if rc != 0:
        err("Build failed!")
        return False

    log("Build successful: plc_runtime_rpi")
    return True


def install_service(client):
    """安装 systemd 服务"""
    log("Installing systemd service...")
    service_cmd = f"""sudo tee /etc/systemd/system/plc-runtime.service > /dev/null << 'EOF'
[Unit]
Description=ST2C PLC Runtime
After=network.target

[Service]
Type=simple
ExecStart={REMOTE_DIR}/plc_runtime_rpi --cycle-us 1000
Restart=always
RestartSec=1
CPUSchedulingPolicy=fifo
CPUSchedulingPriority=90
LimitMEMLOCK=infinity
LimitCORE=0

[Install]
WantedBy=multi-user.target
EOF"""
    ssh_exec(client, service_cmd)
    ssh_exec(client, "sudo systemctl daemon-reload")
    ssh_exec(client, "sudo systemctl enable plc-runtime")
    log("Service installed!")


def run_remote(client):
    """运行 PLC 运行时"""
    log("Starting PLC runtime (Ctrl+C to stop)...")
    log("Output:")
    print("-" * 60)

    # 使用 exec_command 获取交互式输出
    transport = client.get_transport()
    channel = transport.open_session()
    channel.get_pty()
    channel.exec_command(f"cd {REMOTE_DIR} && sudo ./plc_runtime_rpi --cycle-us 1000")

    try:
        while True:
            if channel.recv_ready():
                data = channel.recv(4096).decode("utf-8", errors="replace")
                print(data, end="", flush=True)
            if channel.recv_stderr_ready():
                data = channel.recv_stderr(4096).decode("utf-8", errors="replace")
                print(f"\033[33m{data}\033[0m", end="", flush=True)
            if channel.exit_status_ready():
                break
            time.sleep(0.1)
    except KeyboardInterrupt:
        log("Stopping...")
        channel.send("\x03")  # Ctrl+C
        time.sleep(1)
        channel.close()


# ═══ 新增：本地交叉编译 ═══

SCRIPT_DIR = os.path.abspath(os.path.join(LOCAL_DIR, ".."))


def cross_build(jitter_test=False):
    """使用 target/rpi/Makefile 进行本地交叉编译"""
    make_dir = os.path.join(SCRIPT_DIR, "target", "rpi")
    if not os.path.exists(os.path.join(make_dir, "Makefile")):
        err(f"Makefile not found in {make_dir}")
        sys.exit(1)

    log("Cross-compiling with target/rpi/Makefile...")
    target = "jitter-test" if jitter_test else ""
    cmd = f"make -C {make_dir} {target}".strip()
    log(f"  $ {cmd}")
    rc = os.system(cmd)
    if rc != 0:
        err("Cross-compilation failed!")
        sys.exit(1)
    log("Cross-compilation successful!")


def upload_binary(client, binary_path):
    """仅上传编译好的二进制文件（不重新编译）"""
    log(f"Uploading binary: {binary_path}")
    ssh_exec(client, f"mkdir -p {REMOTE_DIR}")
    sftp = client.open_sftp()
    sftp.put(binary_path, f"{REMOTE_DIR}/plc_runtime_rpi")
    sftp.close()
    ssh_exec(client, f"chmod +x {REMOTE_DIR}/plc_runtime_rpi")
    log("Upload complete!")


# ═══ 交互式 Shell ═══
    """交互式 SSH shell"""
    log("Interactive shell (type 'exit' to quit)")
    transport = client.get_transport()
    channel = transport.open_session()
    channel.get_pty()
    channel.invoke_shell()

    import select
    import tty
    import termios

    old_settings = termios.tcgetattr(sys.stdin)
    try:
        tty.setraw(sys.stdin.fileno())
        while True:
            r, _, _ = select.select([channel, sys.stdin], [], [], 0.1)
            if channel in r:
                data = channel.recv(4096)
                if not data:
                    break
                sys.stdout.write(data.decode("utf-8", errors="replace"))
                sys.stdout.flush()
            if sys.stdin in r:
                data = os.read(sys.stdin.fileno(), 4096)
                if not data:
                    break
                channel.send(data)
    finally:
        termios.tcsetattr(sys.stdin, termios.TCSADRAIN, old_settings)
        channel.close()


def main():
    parser = argparse.ArgumentParser(description="Deploy ST2C PLC Runtime to RPi")
    parser.add_argument("--run", action="store_true", help="Compile + upload + run")
    parser.add_argument("--build-only", action="store_true", help="Compile only (no upload)")
    parser.add_argument("--shell", action="store_true", help="Interactive SSH shell")
    parser.add_argument("--cross", action="store_true", help="Use local cross-compiler instead of remote build")
    parser.add_argument("--jitter-test", action="store_true", help="Build jitter test version")
    parser.add_argument("--no-service", action="store_true", help="Skip systemd service install")
    args = parser.parse_args()

    client = connect()

    try:
        if args.shell:
            interactive_shell(client)
            return

        if args.build_only:
            if args.cross:
                cross_build(args.jitter_test)
            else:
                build_remote(client)
            return

        # 完整部署流程
        if args.cross:
            cross_build(args.jitter_test)
            # 上传二进制
            binary = os.path.join(SCRIPT_DIR, "target", "rpi", "plc_runtime_rpi")
            if not os.path.exists(binary):
                # 也尝试在 runtime-flat 下找
                binary = os.path.join(LOCAL_DIR, "plc_runtime_rpi")
            if not os.path.exists(binary):
                err("Cross-compiled binary not found. Run with --build-only first.")
                return
            upload_binary(client, binary)
        else:
            upload_source(client)
            if not build_remote(client):
                err("Build failed, skipping run.")
                return

        if not args.no_service:
            install_service(client)
        if args.run:
            run_remote(client)
        else:
            log(f"\nTo run:  ssh {RPI_USER}@{RPI_IP} \"cd {REMOTE_DIR} && sudo ./plc_runtime_rpi\"")
            log(f"Or:      sudo systemctl start plc-runtime")
    finally:
        client.close()
        log("Disconnected.")


if __name__ == "__main__":
    main()
