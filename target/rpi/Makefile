# target/rpi/Makefile — 树莓派 PLC Runtime
#
# 用法：
#   make                     # 本地编译（含 POU）
#   make stub                # 最小编译（无 POU，纯运行时 + GPIO）
#   make ethercat            # EtherCAT 编译（需要 SOEM + libpcap）
#   make CROSS=aarch64-linux-gnu-  # 交叉编译（生成 ARM 二进制）
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
PLAT_SRC = platform_rpi.cpp
MAIN_SRC = runtime_rpi.cpp
HAL_SRC  = hal/gpio_hal.cpp hal/gpio_tci.cpp
POU_SRCS = $(wildcard $(BUILD)/*.cpp)
CONFIG_JSON     = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp
TCI_DIR  = ../lib/tci
TCI_SRCS = $(TCI_DIR)/network_tci.cpp $(TCI_DIR)/tcp_tci.cpp

config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target rpi --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target rpi --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target rpi --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target rpi --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	echo "Warning: gen_config.py --stub failed" 1>&2

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 -D_GNU_SOURCE \
            -DRT_PLATFORM_LINUX \
            -I$(RT_DIR)/include \
            -I$(TCI_DIR) -I. \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lpthread

TARGET   = plc_runtime_rpi

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
	sudo ./$(TARGET) --cycle-us $(CYCLE_US)
else
	@echo "交叉编译模式，请手动将 $(TARGET) 上传到树莓派运行"
endif

stub-run: stub
	sudo ./$(STUB_TARGET) --cycle-us $(CYCLE_US)

build:
	mkdir -p build

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB)

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

SOEM_C_OBJS = $(patsubst $(SOEM_DIR)/src/%.c,build/soem_%.o,$(wildcard $(SOEM_DIR)/src/*.c)) \
              build/soem_oshw.o build/soem_osal.o build/soem_nicdrv.o

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(MAIN_SRC) $(HAL_SRC) $(ECAT_SRC) $(CONFIG_GEN) $(TCI_SRCS)

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
