# target/windows/Makefile — Windows 平台 PLC Runtime (MinGW)
#
# 用法：
#   make                     # 编译（含 POU）
#   make stub                # 最小编译（无 POU）
#   make ethercat            # EtherCAT 编译（需要 Npcap SDK）
#   make run                 # 编译 + 运行
#   make clean               # 清理
#   make registry            # 仅生成 POU 注册桥接
#
# 注意：
#   - SOEM 在 Windows 下需要 Npcap 运行时（已安装）或 SDK
#   - SOEM 的 ec_options.h 由 CMake configure_file 生成，Makefile 不自生成；
#     若缺失则从 ec_options.h.in 手动生成（参见 target/lib/soem/CMakeLists.txt）
#   - x64 MinGW 可链接 Npcap DLL（C:\Windows\System32\Npcap\*.dll），方法：
#       gendef - wpcap.dll | Set-Content lib/wpcap.def
#       dlltool -d lib/wpcap.def -D wpcap.dll -l lib/libwpcap.a -k -m i386:x86-64
#   - 默认使用 MinGW64（g++），MSVC 请设置 CROSS=
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
            -DRT_PLATFORM_WINDOWS \
            -I$(RT_DIR)/include \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lwinmm

TARGET   = plc_runtime_windows.exe

.PHONY: all stub run clean registry

all: $(REGISTRY) $(TARGET)

STUB_SRCS = $(RT_SRCS) $(PLAT_SRC) ../pou_stub.cpp
STUB_TARGET = plc_runtime_windows_stub.exe

stub: $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

SRCS = $(RT_SRCS) $(PLAT_SRC) $(POU_SRCS) $(REGISTRY)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

CYCLE_US ?= 1000

run: all
	./$(TARGET) --cycle-us $(CYCLE_US)

stub-run: stub
	./$(STUB_TARGET) --cycle-us $(CYCLE_US)

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(ECAT_TARGET) $(REGISTRY)

# ═══ EtherCAT (SOEM) 构建 — 共享 SOEM 子模块 ═══
# Windows 需要 Npcap SDK（或 WinPcap），SOEM 自带 wpcap 库文件
SOEM_DIR  = ../lib/soem
ECAT_SRC  = ethercat_tci.cpp
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

ECAT_CPP_SRCS = $(RT_SRCS) $(PLAT_SRC) $(ECAT_SRC) ../pou_stub.cpp

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

$(ECAT_TARGET): $(ECAT_CPP_SRCS) $(SOEM_C_OBJS)
	$(CXX) $(CXXFLAGS) $(ECAT_CXXFLAGS) -o $@ $(ECAT_CPP_SRCS) $(SOEM_C_OBJS) $(LDFLAGS) -lws2_32 $(SOEM_WINPCAP_LIB)
	@echo "=== Build (ethercat): $@ ==="

ECAT_CYCLES ?= 1000
ECAT_IF     ?= eth0

ethercat-run: ethercat
	./$(ECAT_TARGET) --cycle-us $(ECAT_CYCLES) --tci ethercat --ecat-if $(ECAT_IF)
