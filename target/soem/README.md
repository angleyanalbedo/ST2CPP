# target/soem — EtherCAT (SOEM) 集成

## 概述

通过 SOEM（Simple Open EtherCAT Master）将 EtherCAT 从站设备接入 PLC 运行时。
PDO 数据自动映射到 ProcessImage，ST 代码通过 `%IW`/`%QW` 地址直接读写 EtherCAT 从站。

## 依赖

```bash
# Raspberry Pi 4 / Linux
sudo apt install libpcap-dev
```

## 构建

```bash
# 在 target/rpi/ 目录下
make ethercat            # 编译（含 SOEM + EtherCAT TCI）
make ethercat-run        # 编译 + 运行
```

## SOEM 源码

首次使用需下载 SOEM：

```bash
cd target/soem
git clone https://github.com/OpenEtherCATsociety/soem.git soem
```

## PDO 配置

编辑 `pdo_config.h`，修改 `ECAT_INPUT_MAP` 和 `ECAT_OUTPUT_MAP` 数组。
每个条目定义一个 PDO 字段到 ProcessImage 字节/位的映射。

## 运行

```bash
# 仅 EtherCAT
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --ecat-if eth0

# EtherCAT + GPIO 同时
taskset -c 3 sudo ./plc_runtime_ecat --cycle-us 1000 --tci both --ecat-if eth0
```

## 命令行参数

| 参数 | 说明 |
|------|------|
| `--cycle-us N` | PLC 周期（微秒），默认 1000 |
| `--tci MODE` | I/O 模式：`gpio`（默认）、`ethercat`、`both` |
| `--ecat-if NAME` | EtherCAT 网口名，默认 `eth0` |

## 目录结构

```
target/soem/
├── README.md              # 本文件
├── pdo_config.h           # PDO ↔ ProcessImage 映射表
├── ethercat_tci.h         # EthercatTCI 类声明
├── ethercat_tci.cpp       # EthercatTCI 实现
├── config/                # 配置文件
└── soem/                  # SOEM 源码（git clone）
```
