# target/desktop/Makefile — 桌面开发测试 PLC Runtime
#
# 用法：
#   make                     # 编译（含 POU）
#   make stub                # 最小编译（无 POU）
#   make ethercat            # EtherCAT 编译（需要 SOEM，自动检测 OS）
#   make run                 # 编译 + 运行
#   make clean               # 清理
#   make registry            # 仅生成 POU 注册桥接
#
# 特点：
#   - 通用跨平台，使用 runtime_main.cpp（读取 tasks.json）
#   - 没有 GPIO 或平台特定 HAL
#   - EtherCAT 自动根据 OS 选择 Linux/Windows SOEM 驱动

CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC = runtime_main.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)

REGISTRY = $(BUILD)/pou_registry.gen.cpp

registry: $(REGISTRY)

$(REGISTRY): $(POU_SRCS)
	python3 $(CURDIR)/../rpi/gen_registry.py --build-dir "$(BUILD)" --output "$@" 2>/dev/null || \
	python $(CURDIR)/../rpi/gen_registry.py --build-dir "$(BUILD)" --output "$@" 2>/dev/null || \
	{ echo "// Auto-generated" > $@; \
	  echo '#include "rt_plc.h"' >> $@; \
	  echo "using namespace rt_plc;" >> $@; \
	  echo "" >> $@; \
	  grep -oh 'registerPOU_[A-Za-z0-9_]*' $(POU_SRCS) | sort -u | \
	    sed 's/^/extern void /; s/$$/(POURegistry\& reg);/' >> $@; \
	  echo "" >> $@; \
	  echo "void registerAllPOUs(POURegistry& reg) {" >> $@; \
	  grep -oh 'registerPOU_[A-Za-z0-9_]*' $(POU_SRCS) | sort -u | \
	    sed 's/^/    /; s/$$/(reg);/' >> $@; \
	  echo "}" >> $@; }

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 \
            -I$(RT_DIR)/include \
            -Wall -Wextra -Wno-unused-parameter

# OS 检测
UNAME    := $(shell uname -s 2>/dev/null || echo "Windows")
ifeq ($(UNAME),Linux)
LDFLAGS   = $(OPT) -lpthread
EXE       =
else
LDFLAGS   = $(OPT) -lwinmm
EXE       = .exe
endif

TARGET   = plc_runtime_desktop$(EXE)

.PHONY: all stub run clean registry

all: $(REGISTRY) $(TARGET)

STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) ../pou_stub.cpp
STUB_TARGET = plc_runtime_desktop_stub$(EXE)

stub: $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(POU_SRCS) $(REGISTRY)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

run: all
	./$(TARGET)

stub-run: stub
	./$(STUB_TARGET)

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(REGISTRY)

# ═══ EtherCAT (SOEM) 构建 — 共享 SOEM 子模块 ═══
SOEM_DIR  = ../lib/soem
ECAT_SRC  = ../rpi/ethercat_tci.cpp

ifeq ($(UNAME),Linux)
SOEM_OSHWDIR = linux
SOEM_CFLAGS  = -O2 -D_GNU_SOURCE -DRT_PLATFORM_LINUX -DSOEM_LINUX \
               -I$(SOEM_DIR)/include \
               -I$(SOEM_DIR)/oshw/linux \
               -I$(SOEM_DIR)/oshw \
               -I$(SOEM_DIR)/osal/linux \
               -I$(SOEM_DIR)/osal
ECAT_LIBS    = -lpcap
SOEM_OBJ_OSHW = /tmp/soem_desk_oshw.o
SOEM_OBJ_OSAL = /tmp/soem_desk_osal.o
SOEM_OBJ_NIC  = /tmp/soem_desk_nicdrv.o
OSHW_SRC      = $(SOEM_DIR)/oshw/linux/oshw.c
OSAL_SRC      = $(SOEM_DIR)/osal/linux/osal.c
NIC_SRC       = $(SOEM_DIR)/oshw/linux/nicdrv.c
else
SOEM_OSHWDIR = win32
SOEM_CFLAGS  = -O2 -DRT_PLATFORM_WINDOWS -DSOEM_WIN32 \
               -I$(SOEM_DIR)/include \
               -I$(SOEM_DIR)/oshw/win32 \
               -I$(SOEM_DIR)/oshw \
               -I$(SOEM_DIR)/osal/win32 \
               -I$(SOEM_DIR)/osal \
               -I$(SOEM_DIR)/oshw/win32/wpcap/Include
ECAT_LIBS    = -L$(SOEM_DIR)/oshw/win32/wpcap/Lib -lwpcap -lpacket
SOEM_OBJ_OSHW = /tmp/soem_desk_oshw.o
SOEM_OBJ_OSAL = /tmp/soem_desk_osal.o
SOEM_OBJ_NIC  = /tmp/soem_desk_nicdrv.o
OSHW_SRC      = $(SOEM_DIR)/oshw/win32/oshw.c
OSAL_SRC      = $(SOEM_DIR)/osal/win32/osal.c
NIC_SRC       = $(SOEM_DIR)/oshw/win32/nicdrv.c
endif

ECAT_CXXFLAGS = -DRT_ETHERCAT_ENABLED \
                -I$(RT_DIR)/include \
                -I$(SOEM_DIR)/include \
                -I$(SOEM_DIR)/oshw/$(SOEM_OSHWDIR) \
                -I$(SOEM_DIR)/oshw \
                -I$(SOEM_DIR)/osal/$(SOEM_OSHWDIR) \
                -I$(SOEM_DIR)/osal
ECAT_TARGET = plc_runtime_desktop_ecat$(EXE)

SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,/tmp/soem_desk_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              $(SOEM_OBJ_OSHW) $(SOEM_OBJ_OSAL) $(SOEM_OBJ_NIC)

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(ECAT_SRC) ../pou_stub.cpp

/tmp/soem_desk_%.o: $(SOEM_DIR)/src/%.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_OSHW): $(OSHW_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_OSAL): $(OSAL_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_NIC): $(NIC_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

ethercat: $(ECAT_TARGET)

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) $(ECAT_LIBS)
	@echo "=== Build (ethercat): $@ ==="

ECAT_CYCLES ?= 1000
ECAT_IF     ?= eth0

ethercat-run: ethercat
	./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci ethercat --ecat-if $(ECAT_IF)
