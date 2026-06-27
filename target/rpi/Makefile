# target/rpi/Makefile — 树莓派 PLC Runtime
#
# 用法：
#   make                     # 本地编译
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
.PHONY: all run clean registry

all: $(REGISTRY) $(TARGET)

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

clean:
	rm -f $(TARGET) $(REGISTRY)
