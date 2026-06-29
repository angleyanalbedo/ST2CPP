# target/bananapif3/Makefile — Banana Pi BPI-F3 PLC Runtime
#
# 用法：
#   make                     # 本地编译（含 POU）
#   make stub                # 最小编译（无 POU，纯运行时 + GPIO）
#   make run                 # 本地编译 + 运行
#   make clean               # 清理
#   make config              # 仅生成运行时配置代码
#
# 配置（环境变量或命令行覆盖）：
#   CYCLE_US=500 make run

CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC = platform_bpi_f3.cpp
MAIN_SRC = runtime_bpi_f3.cpp
HAL_SRC  = hal/gpio_hal.cpp hal/gpio_tci.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)
CONFIG_JSON     = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp
TCI_DIR  = ../lib/tci
TCI_SRCS = $(TCI_DIR)/network_tci.cpp $(TCI_DIR)/tcp_tci.cpp

config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target bananapif3 --input $(CONFIG_JSON) --output $(CONFIG_GEN) 2>/dev/null || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target bananapif3 --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) 2>/dev/null || \
	echo "Warning: gen_config.py --stub failed" 1>&2

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 -D_GNU_SOURCE \
            -DRT_PLATFORM_LINUX \
            -I$(RT_DIR)/include \
            -I$(TCI_DIR) -I. \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lpthread

TARGET   = plc_runtime_bpi_f3

.PHONY: all stub run clean config

all: $(CONFIG_GEN) $(TARGET)

STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) $(CONFIG_GEN_STUB) $(TCI_SRCS)
STUB_TARGET = plc_runtime_stub

stub: $(CONFIG_GEN_STUB) $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) $(POU_SRCS) $(CONFIG_GEN) $(TCI_SRCS)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

CYCLE_US ?= 1000

run: all
ifeq ($(CROSS),)
	sudo taskset -c 3 ./$(TARGET) --cycle-us $(CYCLE_US)
else
	@echo "交叉编译模式，请手动将 $(TARGET) 上传到 BPI-F3 运行"
endif

stub-run: stub
	sudo taskset -c 3 ./$(STUB_TARGET) --cycle-us $(CYCLE_US)

build:
	mkdir -p build

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB)

# EtherCAT 支持（预留）：需 SOEM 库
# SOEM_DIR  = ../lib/soem
# ECAT_SRC  = ethercat_tci.cpp
# include ../rpi/ethercat.mk
