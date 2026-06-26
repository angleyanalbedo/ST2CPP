# target/rpi/Makefile — 树莓派 PLC Runtime
#
# 两种编译方式：
#   本地编译（在树莓派上直接 make）：  CROSS 留空
#   交叉编译（在 x86 主机上编译）：   make CROSS=aarch64-linux-gnu-
#
# 用法：
#   make                    # 编译
#   make deploy             # 编译 + SCP 到树莓派
#   make run                # 编译 + 部署 + 运行
#   make clean              # 清理

# ═══ 编译器 ═══
CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++
STRIP    = $(CROSS)strip

# ═══ 路径 ═══
ROOT_DIR  = ../..
RT_DIR    = $(ROOT_DIR)/runtime-flat
INCLUDE   = $(RT_DIR)/include

# ═══ 源文件 ═══
# 核心运行时（静态库源码）
RT_SRCS = $(wildcard $(RT_DIR)/src/*.cpp)

# 平台文件（本目录下）
PLAT_SRCS = platform_rpi.cpp

# 入口文件（本目录下）
MAIN_SRCS = runtime_rpi.cpp

SRCS = $(RT_SRCS) $(PLAT_SRCS) $(MAIN_SRCS)

# ═══ 编译选项 ═══
OPT     ?= -O2
CXXFLAGS = $(OPT) -std=c++17 -D_GNU_SOURCE \
           -DRT_PLATFORM_LINUX \
           -I$(INCLUDE) \
           -Wall -Wextra -Wno-unused-parameter
LDFLAGS  = $(OPT) -lpthread

TARGET = plc_runtime_rpi

# ═══ 构建 ═══

.PHONY: all deploy run clean

all: $(TARGET)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

# ═══ 部署 ═══
RPI_IP    ?= 192.168.5.128
RPI_USER  ?= pi
RPI_DIR   ?= ~/st2c-runtime

deploy: $(TARGET)
	@echo "=== Deploy to $(RPI_USER)@$(RPI_IP) ==="
	ssh $(RPI_USER)@$(RPI_IP) "mkdir -p $(RPI_DIR)"
	scp $(TARGET) $(RPI_USER)@$(RPI_IP):$(RPI_DIR)/
	@echo "=== Done. Run: ssh $(RPI_USER)@$(RPI_IP) 'sudo $(RPI_DIR)/$(TARGET)' ==="

run: deploy
	ssh $(RPI_USER)@$(RPI_IP) "sudo $(RPI_DIR)/$(TARGET) --cycle-us 1000"

clean:
	rm -f $(TARGET)
