# target/desktop/Makefile — 桌面开发测试 PLC Runtime
#
# 用法：
#   make                     # 编译（含 POU）
#   make stub                # 最小编译（无 POU，无配置）
#   make ethercat            # EtherCAT 编译（需要 SOEM，自动检测 OS）
#   make run                 # 编译 + 运行
#   make clean               # 清理
#   make config              # 仅生成运行时配置代码
#
# 特点：
#   - 通用跨平台
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
CONFIG_JSON = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp

# ─── 生成运行时配置代码（绑定 + 任务 + POU 注册） ───
config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target desktop --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target desktop --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target desktop --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target desktop --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	echo "Warning: gen_config.py --stub failed" 1>&2

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
LDFLAGS   = $(OPT) -lwinmm -lws2_32
EXE       = .exe
endif

TARGET   = plc_runtime_desktop$(EXE)

.PHONY: all stub run clean config

all: $(CONFIG_GEN) $(TARGET)

STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) $(CONFIG_GEN_STUB) ../network_tci.cpp ../tcp_tci.cpp
STUB_TARGET = plc_runtime_desktop_stub$(EXE)

stub: $(CONFIG_GEN_STUB) $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(POU_SRCS) $(CONFIG_GEN) ../network_tci.cpp ../tcp_tci.cpp

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

run: all
	./$(TARGET)

stub-run: stub
	./$(STUB_TARGET)

build:
	mkdir -p build

clean:
	rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB) 2>/dev/null || del /Q $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB) 2>NUL

# ═══ EtherCAT (SOEM) 构建 — 共享 SOEM 子模块 ═══
SOEM_DIR     = ../lib/soem
SOEM_OPTIONS = $(SOEM_DIR)/include/soem/ec_options.h
ECAT_SRC     = ethercat_tci.cpp

ifeq ($(UNAME),Linux)
SOEM_OSHWDIR = linux
SOEM_CFLAGS  = -O2 -D_GNU_SOURCE -DRT_PLATFORM_LINUX -DSOEM_LINUX \
               -I$(SOEM_DIR)/include \
               -I$(SOEM_DIR)/oshw/linux \
               -I$(SOEM_DIR)/oshw \
               -I$(SOEM_DIR)/osal/linux \
               -I$(SOEM_DIR)/osal
ECAT_LIBS    = -lpcap
SOEM_OBJ_OSHW = build/soem_desk_oshw.o
SOEM_OBJ_OSAL = build/soem_desk_osal.o
SOEM_OBJ_NIC  = build/soem_desk_nicdrv.o
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
SOEM_OBJ_OSHW = build/soem_desk_oshw.o
SOEM_OBJ_OSAL = build/soem_desk_osal.o
SOEM_OBJ_NIC  = build/soem_desk_nicdrv.o
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
ifeq ($(SOEM_OSHWDIR),win32)
ECAT_CXXFLAGS += -I$(SOEM_DIR)/oshw/win32/wpcap/Include
endif
ECAT_TARGET = plc_runtime_desktop_ecat$(EXE)

SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,build/soem_desk_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              $(SOEM_OBJ_OSHW) $(SOEM_OBJ_OSAL) $(SOEM_OBJ_NIC)

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(ECAT_SRC) $(CONFIG_GEN) ../network_tci.cpp ../tcp_tci.cpp

build/soem_desk_%.o: $(SOEM_DIR)/src/%.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_OSHW): $(OSHW_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_OSAL): $(OSAL_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

$(SOEM_OBJ_NIC): $(NIC_SRC)
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

ethercat: $(ECAT_TARGET)

$(SOEM_OPTIONS): $(SOEM_OPTIONS).in
	python3 $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS) 2>/dev/null || \
	python  $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS)

$(SOEM_C_OBJS): $(SOEM_OPTIONS)

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) $(ECAT_LIBS)
	@echo "=== Build (ethercat): $@ ==="

ECAT_CYCLES ?= 1000
ECAT_IF     ?= eth0

ethercat-run: ethercat
	./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci ethercat --ecat-if $(ECAT_IF)

# ═══ NetworkTCI 测试 ═══
NET_TEST_TARGET = network_test$(EXE)
NET_TEST_SRCS   = ../network_test.cpp ../network_tci.cpp ../tcp_tci.cpp

$(NET_TEST_TARGET): $(NET_TEST_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

network-test: $(NET_TEST_TARGET)
	./$(NET_TEST_TARGET)

.PHONY: network-test
