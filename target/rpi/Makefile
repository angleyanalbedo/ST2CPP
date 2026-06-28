# target/rpi/Makefile — 树莓派 PLC Runtime
#
# 用法：
#   make                     # 本地编译（含 POU）
#   make stub                # 最小编译（无 POU，纯运行时 + GPIO）
#   make ethercat            # EtherCAT 编译（需要 SOEM + libpcap）
#   make CROSS=aarch64-linux-gnu-  # 交叉编译（生成 ARM 二进制）
#   make run                 # 本地编译 + 运行
#   make clean               # 清理
#   make registry            # 仅生成 POU 注册桥接
#
# 配置（环境变量或命令行覆盖）：
#   CYCLE_US=500 make run

# ═══ 编译器 ═══
CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

# ═══ 路径 ═══
ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

# ═══ 源文件 ═══
RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC = platform_rpi.cpp
MAIN_SRC = runtime_rpi.cpp
HAL_SRC  = hal/gpio_hal.cpp hal/gpio_tci.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)

# ═══ 自动生成 registerAllPOUs 桥接 ═══
REGISTRY = $(BUILD)/pou_registry.gen.cpp

registry: $(REGISTRY)

$(REGISTRY): $(POU_SRCS)
	python3 $(CURDIR)/gen_registry.py --build-dir "$(BUILD)" --output "$@" 2>/dev/null || \
	python $(CURDIR)/gen_registry.py --build-dir "$(BUILD)" --output "$@" 2>/dev/null || \
	{ echo "// Auto-generated" > $@; \
	  echo '#include "rt_plc.h"' >> $@; \
	  echo "using namespace rt_plc;" >> $@; \
	  echo "" >> $@; \
	  grep -oh 'registerPOU_[A-Za-z0-9_]*' $(POU_SRCS) | sort -u | \
	    sed 's/^/extern void /; s/$$(POURegistry& reg);$$//' >> $@; \
	  echo "" >> $@; \
	  echo "void registerAllPOUs(POURegistry& reg) {" >> $@; \
	  grep -oh 'registerPOU_[A-Za-z0-9_]*' $(POU_SRCS) | sort -u | \
	    sed 's/^/    /; s/$$(reg);$$//' >> $@; \
	  echo "}" >> $@; }

# ═══ 编译选项 ═══
OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 -D_GNU_SOURCE \
            -DRT_PLATFORM_LINUX \
            -I$(RT_DIR)/include \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lpthread

TARGET   = plc_runtime_rpi

# ═══ 构建 ═══
.PHONY: all stub run clean registry

all: $(REGISTRY) $(TARGET)

# 无 POU 模式：用 pou_stub.cpp 替代编译器输出
STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) pou_stub.cpp
STUB_TARGET = plc_runtime_stub

stub: $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) $(POU_SRCS) $(REGISTRY)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

# ═══ 运行（仅本地编译） ═══
CYCLE_US ?= 1000

run: all
ifeq ($(CROSS),)
	sudo ./$(TARGET) --cycle-us $(CYCLE_US)
else
	@echo "交叉编译模式，请手动将 $(TARGET) 上传到树莓派运行"
endif

stub-run: stub
	sudo ./$(STUB_TARGET) --cycle-us $(CYCLE_US)

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(REGISTRY)

# ═══ EtherCAT (SOEM) 构建 — 共享 SOEM 子模块 ═══
SOEM_DIR  = ../lib/soem
ECAT_SRC  = ethercat_tci.cpp
SOEM_CFLAGS = -O2 -D_GNU_SOURCE -DRT_PLATFORM_LINUX -DSOEM_LINUX \
              -I$(SOEM_DIR)/include \
              -I$(SOEM_DIR)/oshw/linux \
              -I$(SOEM_DIR)/oshw \
              -I$(SOEM_DIR)/osal/linux \
              -I$(SOEM_DIR)/osal
ECAT_CXXFLAGS = -DRT_ETHERCAT_ENABLED \
                -I$(RT_DIR)/include \
                -I$(SOEM_DIR)/include \
                -I$(SOEM_DIR)/oshw/linux \
                -I$(SOEM_DIR)/oshw \
                -I$(SOEM_DIR)/osal/linux \
                -I$(SOEM_DIR)/osal
ECAT_TARGET = plc_runtime_ecat

# SOEM C 文件（Linux 平台，用 gcc 编译）
SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,build/soem_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              build/soem_oshw.o build/soem_osal.o build/soem_nicdrv.o

# EtherCAT stub 模式（无 POU + SOEM）
ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) $(ECAT_SRC) pou_stub.cpp

# 编译 SOEM C 文件
build/soem_%.o: $(SOEM_DIR)/src/%.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_oshw.o: $(SOEM_DIR)/oshw/linux/oshw.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_osal.o: $(SOEM_DIR)/osal/linux/osal.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_nicdrv.o: $(SOEM_DIR)/oshw/linux/nicdrv.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

ethercat: $(ECAT_TARGET)

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) -lpcap
	@echo "=== Build (ethercat): $@ ==="

ECAT_CYCLES ?= 1000
ECAT_IF     ?= eth0

ethercat-run: ethercat
ifeq ($(CROSS),)
	taskset -c 3 sudo ./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci ethercat --ecat-if $(ECAT_IF)
else
	@echo "交叉编译模式，请手动将 $(ECAT_TARGET) 上传到树莓派运行"
endif

ethercat-both: ethercat
ifeq ($(CROSS),)
	taskset -c 3 sudo ./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci both --ecat-if $(ECAT_IF)
else
	@echo "交叉编译模式，请手动将 $(ECAT_TARGET) 上传到树莓派运行"
endif
