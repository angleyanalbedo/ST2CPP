# target/rpi — AI Agent 操作手册

当用户说"部署到树莓派"、"在 Pi 上编译"、"连 Pi 试试"时，按以下流程操作。

## 连接信息

| 项 | 值 |
|---|---|
| IP | 看真实情况 |
| 用户 | pi |
| 密码 |  |
| 架构 | aarch64 (ARM 64-bit) |

## 操作步骤

### Step 1: 连接

```python
import paramiko
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("ip", 22, "pi", "passwd", timeout=10)
```

### Step 2: 获取 home 目录

**必须用 `pwd`，不要用 `echo $HOME`**（后者会被本地 shell 展开成 Windows 路径）。

```python
stdin, stdout, stderr = client.exec_command("pwd")
home = stdout.read().decode().strip()  # 得到 /home/pi
remote_dir = home + "/st2c-runtime"
```

### Step 3: 生成 registry 桥接 + 打包上传

运行 `gen_registry.py` 自动生成 `pou_registry.gen.cpp`，然后打包上传。

```python
import subprocess, tarfile, tempfile, os

# 生成 registry 桥接
subprocess.run(["python", "target/rpi/gen_registry.py",
                "--build-dir", "output/flat/build",
                "--output", "output/flat/build/pou_registry.gen.cpp"])

# 打包上传
tar_path = os.path.join(tempfile.gettempdir(), "st2c.tar.gz")
root = "ST2C-master"

with tarfile.open(tar_path, "w:gz") as tar:
    tar.add(os.path.join(root, "runtime-flat/include"), arcname="runtime-flat/include")
    tar.add(os.path.join(root, "runtime-flat/src"), arcname="runtime-flat/src")
    tar.add(os.path.join(root, "target/rpi/platform_rpi.cpp"), arcname="target/rpi/platform_rpi.cpp")
    tar.add(os.path.join(root, "target/rpi/runtime_rpi.cpp"), arcname="target/rpi/runtime_rpi.cpp")
    # 上传编译器生成的 POU 代码
    tar.add(os.path.join(root, "output/flat/build"), arcname="output/flat/build")

client.exec_command(f"mkdir -p {remote_dir}")
sftp = client.open_sftp()
sftp.put(tar_path, f"{remote_dir}/st2c.tar.gz")
sftp.close()
```

### Step 4: 解压 + 编译

```python
client.exec_command(f"cd {remote_dir} && tar xzf st2c.tar.gz")

cmd = f"""cd {remote_dir} && g++ -O2 -std=c++17 -DRT_PLATFORM_LINUX \
    -I runtime-flat/include \
    runtime-flat/src/*.cpp \
    target/rpi/platform_rpi.cpp target/rpi/runtime_rpi.cpp \
    output/flat/build/*.cpp \
    -lpthread -o plc_runtime_rpi 2>&1"""
stdin, stdout, stderr = client.exec_command(cmd, timeout=120)
print(stdout.read().decode())
rc = stdout.channel.recv_exit_status()
```

编译成功标志：`rc == 0`，无报错。

### Step 5: 运行测试

```python
cmd = f"cd {remote_dir} && timeout 3 sudo ./plc_runtime_rpi --cycle-us 1000 2>&1"
stdin, stdout, stderr = client.exec_command(cmd, timeout=15)
print(stdout.read().decode())
```

成功标志：输出 Jitter Statistics，Avg ≈ 1000us。

## 关键注意事项

1. **获取 home 用 `pwd`**，不用 `echo $HOME`
2. **SFTP 不支持 `~`**，必须完整路径
3. **编译加 `-DRT_PLATFORM_LINUX`**，启用 clock_gettime 纳秒精度
4. **运行需要 `sudo`**，设置 SCHED_FIFO 实时优先级
5. **首次需 `sudo apt install build-essential`**
6. **如果用户改了 runtime-flat 的代码**，重新上传再编译
7. **如果用户改了 Java 编译器**，需要先本地编译 .st → .cpp，再一起上传
