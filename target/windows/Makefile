# target/windows/Makefile — Windows 平台 PLC Runtime (MinGW)
#
# 用法：
#   make                     # 编译（含 POU）
#   make stub                # 最小编译（无 POU，无配置）
#   make ethercat            # EtherCAT 编译（需要 Npcap SDK）
#   make run                 # 编译 + 运行
#   make clean               # 清理
#   make config              # 仅生成运行时配置代码
#
# 注意：
#   - SOEM 在 Windows 下需要 Npcap 运行时（已安装）或 SDK
#   - Windows 非实时 OS，1ms 周期抖动约 100-500us

CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC = runtime_windows.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)
CONFIG_JSON = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp

# ─── 生成运行时配置代码（绑定 + 任务 + POU 注册） ───
config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target windows --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target windows --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target windows --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target windows --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	echo "Warning: gen_config.py --stub failed" 1>&2

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 \
            -DRT_PLATFORM_WINDOWS \
            -I$(RT_DIR)/include \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lwinmm -lws2_32

TARGET   = plc_runtime_windows.exe

.PHONY: all stub run clean config

all: $(CONFIG_GEN) $(TARGET)

# Stub 模式：无 POU（gen_config --stub 生成空注册）
STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) ../network_tci.cpp ../tcp_tci.cpp $(CONFIG_GEN_STUB)
STUB_TARGET = plc_runtime_windows_stub.exe

stub: $(CONFIG_GEN_STUB) $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(POU_SRCS) $(CONFIG_GEN) ../network_tci.cpp ../tcp_tci.cpp

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

CYCLE_US ?= 1000

run: all
	./$(TARGET) --cycle-us $(CYCLE_US)

stub-run: stub
	./$(STUB_TARGET) --cycle-us $(CYCLE_US)

build:
	mkdir -p build

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB)

# ═══ EtherCAT (SOEM) 构建 — 共享 SOEM 子模块 ═══
SOEM_DIR     = ../lib/soem
SOEM_OPTIONS = $(SOEM_DIR)/include/soem/ec_options.h
ECAT_SRC     = ethercat_tci.cpp
SOEM_CFLAGS = -O2 -DRT_PLATFORM_WINDOWS -DSOEM_WIN32 \
              -I$(SOEM_DIR)/include \
              -I$(SOEM_DIR)/oshw/win32 \
              -I$(SOEM_DIR)/oshw \
              -I$(SOEM_DIR)/osal/win32 \
              -I$(SOEM_DIR)/osal \
              -I$(SOEM_DIR)/oshw/win32/wpcap/Include
ECAT_CXXFLAGS = -DRT_ETHERCAT_ENABLED \
                -I$(RT_DIR)/include \
                -I$(SOEM_DIR)/include \
                -I$(SOEM_DIR)/oshw/win32 \
                -I$(SOEM_DIR)/oshw \
                -I$(SOEM_DIR)/osal/win32 \
                -I$(SOEM_DIR)/osal \
                -I$(SOEM_DIR)/oshw/win32/wpcap/Include
ECAT_TARGET = plc_runtime_windows_ecat.exe

SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,build/soem_win_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              build/soem_win_oshw.o build/soem_win_osal.o build/soem_win_nicdrv.o

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(ECAT_SRC) $(CONFIG_GEN) ../network_tci.cpp ../tcp_tci.cpp

SOEM_WINPCAP_LIB = -Llib -lwpcap -lpacket

build/soem_win_%.o: $(SOEM_DIR)/src/%.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_win_oshw.o: $(SOEM_DIR)/oshw/win32/oshw.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_win_osal.o: $(SOEM_DIR)/osal/win32/osal.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_win_nicdrv.o: $(SOEM_DIR)/oshw/win32/nicdrv.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

ethercat: $(ECAT_TARGET)

$(SOEM_OPTIONS): $(SOEM_OPTIONS).in
	python3 $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS) 2>/dev/null || \
	python  $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS)

$(SOEM_C_OBJS): $(SOEM_OPTIONS)

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) -lws2_32 $(SOEM_WINPCAP_LIB)
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
