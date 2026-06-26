#!/bin/bash
# target/rpi/deploy.sh — 交叉编译 + SCP 部署到树莓派
#
# Usage:
#   ./deploy.sh                  # 编译 + 部署（默认 RPi 4）
#   ./deploy.sh rpi5             # 编译 + 部署到 RPi 5
#   ./deploy.sh jitter           # 抖动测试版本
#   ./deploy.sh run              # 编译 + 部署 + 远程启动

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RPI_IP="${RPI_IP:-192.168.5.128}"
RPI_USER="${RPI_USER:-pi}"
RPI_DIR="${RPI_DIR:-~/st2c-runtime}"
SSH_CMD="ssh ${RPI_USER}@${RPI_IP}"
SCP_CMD="scp"

# 编译
echo "=== Building ==="
case "${1,,}" in
    rpi5)     make -C "$SCRIPT_DIR" RPI_VER=5 ;;
    jitter)   make -C "$SCRIPT_DIR" jitter-test ;;
    run)      make -C "$SCRIPT_DIR" RPI_VER="${2:-4}" ;;
    *)        make -C "$SCRIPT_DIR" RPI_VER="${1:-4}" ;;
esac

# 部署
echo "=== Deploying to ${RPI_USER}@${RPI_IP} ==="
$SSH_CMD "mkdir -p ${RPI_DIR}"
$SCP_CMD "$SCRIPT_DIR/plc_runtime_rpi" "${RPI_USER}@${RPI_IP}:${RPI_DIR}/"
$SCP_CMD "$SCRIPT_DIR/config/tasks.json" "${RPI_USER}@${RPI_IP}:${RPI_DIR}/" 2>/dev/null || true
echo "=== Done ==="

# 如果指定了 run，远程启动
if [ "${1,,}" = "run" ] || [ "${2,,}" = "run" ]; then
    echo "=== Starting on RPi ==="
    $SSH_CMD "sudo chrt -f 90 ${RPI_DIR}/plc_runtime_rpi --cycle-us 1000"
fi
