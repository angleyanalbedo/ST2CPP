# target/linux/Makefile — Linux 通用 PLC Runtime
#
# 用法：
#   make                     # 本地编译（含 POU）
#   make stub                # 最小编译（无 POU，无配置）
#   make ethercat            # EtherCAT 编译（需要 SOEM + libpcap）
#   make run                 # 编译 + 运行
#   make clean               # 清理
#   make config              # 仅生成运行时配置代码
#
# 配置：
#   CYCLE_US=500 make run

CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC = runtime_linux.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)
CONFIG_JSON     = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp
TCI_DIR  = ../lib/tci
TCI_SRCS = $(TCI_DIR)/network_tci.cpp $(TCI_DIR)/tcp_tci.cpp

config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target linux --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target linux --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target linux --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target linux --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	echo "Warning: gen_config.py --stub failed" 1>&2

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 -D_GNU_SOURCE \
            -DRT_PLATFORM_LINUX \
            -I$(RT_DIR)/include \
            -I$(TCI_DIR) -I. \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lpthread

TARGET   = plc_runtime_linux

.PHONY: all stub run clean config

all: $(CONFIG_GEN) $(TARGET)

STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) $(CONFIG_GEN_STUB) $(TCI_SRCS)
STUB_TARGET = plc_runtime_linux_stub

stub: $(CONFIG_GEN_STUB) $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(POU_SRCS) $(CONFIG_GEN) $(TCI_SRCS)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

CYCLE_US ?= 1000

run: all
	sudo ./$(TARGET) --cycle-us $(CYCLE_US)

stub-run: stub
	sudo ./$(STUB_TARGET) --cycle-us $(CYCLE_US)

build:
	mkdir -p build

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB)

SOEM_DIR     = ../lib/soem
SOEM_OPTIONS = $(SOEM_DIR)/include/soem/ec_options.h
ECAT_SRC     = ethercat_tci.cpp
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
ECAT_TARGET = plc_runtime_linux_ecat

SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,build/soem_linux_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              build/soem_linux_oshw.o build/soem_linux_osal.o build/soem_linux_nicdrv.o

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(ECAT_SRC) $(CONFIG_GEN) $(TCI_SRCS)

build/soem_linux_%.o: $(SOEM_DIR)/src/%.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_linux_oshw.o: $(SOEM_DIR)/oshw/linux/oshw.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_linux_osal.o: $(SOEM_DIR)/osal/linux/osal.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

build/soem_linux_nicdrv.o: $(SOEM_DIR)/oshw/linux/nicdrv.c
	$(CC) $(SOEM_CFLAGS) -c $< -o $@

ethercat: $(ECAT_TARGET)

$(SOEM_OPTIONS): $(SOEM_OPTIONS).in
	python3 $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS) 2>/dev/null || \
	python  $(SOEM_DIR)/gen_options.py $(SOEM_OPTIONS).in $(SOEM_OPTIONS)

$(SOEM_C_OBJS): $(SOEM_OPTIONS)

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) -lpcap
	@echo "=== Build (ethercat): $@ ==="

ECAT_CYCLES ?= 1000
ECAT_IF     ?= eth0

ethercat-run: ethercat
	sudo ./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci ethercat --ecat-if $(ECAT_IF)

NET_TEST_TARGET = network_test
NET_TEST_SRCS   = ../network_test.cpp $(TCI_SRCS)

$(NET_TEST_TARGET): $(NET_TEST_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

network-test: $(NET_TEST_TARGET)
	./$(NET_TEST_TARGET)

.PHONY: network-test
