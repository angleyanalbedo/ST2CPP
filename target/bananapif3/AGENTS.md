# target/bananapif3 — AI Agent 操作手册

当用户说"部署到 BPI-F3"、"在 BPI-F3 上编译"、"连 BPI-F3 试试"时，按以下流程操作。

## 连接信息

| 项 | 值 |
|---|---|
| IP | 192.168.137.239 |
| 用户 | root |
| 密码 | 1234 |
| 架构 | riscv64 (RISC-V 64-bit, Spacemit X60) |
| 编译器 | g++ 14.2.0 (Debian) |

## 操作步骤

### Step 1: 连接

```python
import paramiko
client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("192.168.137.239", 22, "root", "1234", timeout=10)
```

### Step 2: 获取 home 目录

**必须用 `pwd`，不要用 `echo $HOME`**（后者会被本地 shell 展开成 Windows 路径）。

```python
stdin, stdout, stderr = client.exec_command("pwd")
home = stdout.read().decode().strip()  # 得到 /root
remote_dir = home + "/st2c-runtime"
```

### Step 3: 生成 registry 桥接 + 打包上传

```python
import subprocess, tarfile, tempfile, os

subprocess.run(["python", "target/bananapif3/gen_registry.py",
                "--build-dir", "output/flat/build",
                "--output", "output/flat/build/pou_registry.gen.cpp"])

tar_path = os.path.join(tempfile.gettempdir(), "st2c_bpi_f3.tar.gz")
root = "ST2C-master"

with tarfile.open(tar_path, "w:gz") as tar:
    tar.add(os.path.join(root, "runtime-flat/"), arcname="runtime-flat/")
    tar.add(os.path.join(root, "target/"), arcname="target/")
    tar.add(os.path.join(root, "output/flat/build"), arcname="output/flat/build")

client.exec_command(f"mkdir -p {remote_dir}")
sftp = client.open_sftp()
sftp.put(tar_path, f"{remote_dir}/st2c_bpi_f3.tar.gz")
sftp.close()
```

### Step 4: 解压 + 编译

```python
stdin, stdout, stderr = client.exec_command(f"cd {remote_dir} && tar xzf st2c_bpi_f3.tar.gz")
rc = stdout.channel.recv_exit_status()
```

然后调用 BPI-F3 上的 make 进行编译：

```python
stdin, stdout, stderr = client.exec_command(f"cd {remote_dir}/target/bananapif3 && make 2>&1")
print(stdout.read().decode())
rc = stdout.channel.recv_exit_status()
```

编译成功标志：`rc == 0`，无报错。

### Step 5: 运行测试

```python
cmd = f"cd {remote_dir}/target/bananapif3 && timeout 3 sudo taskset -c 3 ./plc_runtime_bpi_f3 --cycle-us 1000 2>&1"
stdin, stdout, stderr = client.exec_command(cmd, timeout=15)
print(stdout.read().decode())
```

成功标志：输出 Jitter Statistics，Avg ≈ 1000us。

## 关键注意事项

1. **获取 home 用 `pwd`**，不用 `echo $HOME`
2. **SFTP 不支持 `~`**，必须完整路径
3. **编译加 `-DRT_PLATFORM_LINUX`**，启用 clock_gettime 纳秒精度
4. **运行需要 `sudo`**，设置 SCHED_FIFO 实时优先级
5. **GPIO 用 sysfs**（`/sys/class/gpio`），BPI-F3 没有 `/dev/gpiomem`
6. **推荐绑核** `taskset -c 3` 隔离到单个核心，减少抖动
7. **首次需 `apt install build-essential`**
8. **如果用户改了 runtime-flat 的代码**，重新上传再编译
9. **如果用户改了 Java 编译器**，需要先本地编译 .st → .cpp，再一起上传
