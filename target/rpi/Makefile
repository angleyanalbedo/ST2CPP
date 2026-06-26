# target/rpi/Makefile — 树莓派 PLC Runtime（统一编译+部署）
#
# 用法：
#   make                     # 本地编译
#   make CROSS=aarch64-linux-gnu-  # 交叉编译
#   make deploy              # 编译 + 上传到 Pi
#   make run                 # 编译 + 上传 + 运行
#   make clean               # 清理
#   make registry            # 仅生成 POU 注册桥接
#
# 配置（环境变量或命令行覆盖）：
#   RPI_IP=192.168.1.100 make deploy
#   RPI_USER=root make deploy
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
.PHONY: all deploy run clean registry

all: $(REGISTRY) $(TARGET)

SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(POU_SRCS) $(REGISTRY)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

# ═══ 部署 ═══
RPI_IP   ?= 192.168.5.128
RPI_USER ?= pi
RPI_DIR  ?= ~/st2c-runtime
CYCLE_US ?= 1000

deploy: $(TARGET)
	@echo "=== Deploy to $(RPI_USER)@$(RPI_IP) ==="
	ssh $(RPI_USER)@$(RPI_IP) "mkdir -p $(RPI_DIR)"
	scp $(TARGET) $(RPI_USER)@$(RPI_IP):$(RPI_DIR)/
	@echo "=== Done. Run: ssh $(RPI_USER)@$(RPI_IP) 'sudo $(RPI_DIR)/$(TARGET)' ==="

run: deploy
	ssh $(RPI_USER)@$(RPI_IP) "sudo $(RPI_DIR)/$(TARGET) --cycle-us $(CYCLE_US)"

clean:
	rm -f $(TARGET) $(REGISTRY)
