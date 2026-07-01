# target/rk3588/Makefile — RK3588 PREEMPT_RT Linux PLC Runtime
#
# Typical on-board use:
#   make stub
#   sudo ./plc_runtime_rk3588_stub --cycle-us 1000 --cpu 6 --rt-prio 90
#
# From a host that can ssh to the board:
#   make deploy RK_HOST=192.168.5.130 RK_USER=root
#   make remote-run RK_HOST=192.168.5.130 RK_USER=root

CROSS   ?=
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = $(ROOT)/output/flat/build

RT_SRCS  = $(wildcard $(RT_DIR)/src/*.cpp)
MAIN_SRC = runtime_rk3588.cpp
POU_SRCS = $(filter-out $(BUILD)/pou_registry.gen.cpp, $(wildcard $(BUILD)/*.cpp))
CONFIG_JSON     = config/config.json
CONFIG_GEN      = build/runtime_config.gen.cpp
CONFIG_GEN_STUB = build/runtime_config_stub.gen.cpp
TCI_DIR  = ../lib/tci
TCI_SRCS = $(TCI_DIR)/network_tci.cpp $(TCI_DIR)/tcp_tci.cpp

OPT      ?= -O2
CXXFLAGS  = $(OPT) -std=c++17 -D_GNU_SOURCE \
            -DRT_PLATFORM_LINUX -DRT_TARGET_RK3588 \
            -I$(RT_DIR)/include \
            -I$(TCI_DIR) -I. \
            -Wall -Wextra -Wno-unused-parameter
LDFLAGS   = $(OPT) -lpthread

TARGET      = plc_runtime_rk3588
STUB_TARGET = plc_runtime_rk3588_stub

.PHONY: all stub run stub-run clean config deploy remote-run remote-jitter remote-shell stress stress-run stress-clean remote-stress remote-rt-build remote-rt-stress

all: $(CONFIG_GEN) $(TARGET)

config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target rk3588 --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target rk3588 --input $(CONFIG_JSON) --output $(CONFIG_GEN)

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | build
	python3 ../scripts/gen_config.py --target rk3588 --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target rk3588 --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB)

SRCS = $(RT_SRCS) $(MAIN_SRC) $(POU_SRCS) $(CONFIG_GEN) $(TCI_SRCS)
STUB_SRCS = $(RT_SRCS) $(MAIN_SRC) $(CONFIG_GEN_STUB) $(TCI_SRCS)

$(TARGET): $(SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build: $@ ==="

stub: $(CONFIG_GEN_STUB) $(STUB_TARGET)

$(STUB_TARGET): $(STUB_SRCS)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(LDFLAGS)
	@echo "=== Build (stub): $@ ==="

CYCLE_US ?= 1000
RT_PRIO  ?= 90
CPU      ?= 6

run: all
	sudo ./$(TARGET) --cycle-us $(CYCLE_US) --rt-prio $(RT_PRIO) --cpu $(CPU)

stub-run: stub
	sudo ./$(STUB_TARGET) --cycle-us $(CYCLE_US) --rt-prio $(RT_PRIO) --cpu $(CPU)

build:
	mkdir -p build

clean:
	-rm -f $(TARGET) $(STUB_TARGET) $(STRESS_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB) $(STRESS_CONFIG_GEN)

# ─── Xenomai 硬实时（仅远程，在 RK3588 上编译） ───

remote-rt-build:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && \
		python3 ../scripts/gen_config.py --target rk3588 --build-dir . \
			--input config/stress_config.json --output build/runtime_config_stress.gen.cpp && \
		g++ -O2 -std=c++17 -D_GNU_SOURCE -D_REENTRANT -D__COBALT__ \
			-DRT_PLATFORM_LINUX -DRT_TARGET_RK3588 -DSTRESS_LOAD=$(STRESS_LOAD) \
			-DPROCESS_IMAGE_SIZE_OVERRIDE=65536 \
			-I/usr/xenomai/include/cobalt -I/usr/xenomai/include \
			-I$(RK_DIR)/runtime-flat/include -I$(RK_DIR)/target/lib/tci -I. \
			-Wall -Wextra -Wno-unused-parameter \
			$(RK_DIR)/runtime-flat/src/*.cpp \
			runtime_rk3588_rt.cpp \
			stress_test_pou.cpp \
			build/runtime_config_stress.gen.cpp \
			$(RK_DIR)/target/lib/tci/*.cpp \
			\$$(/usr/xenomai/bin/xeno-config --ldflags --cobalt) \
			-o plc_runtime_rk3588_rt_stress 2>&1"

remote-rt-stress: remote-rt-build
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && \
		sudo ./plc_runtime_rk3588_rt_stress \
			--cycle-us $(CYCLE_US) --rt-prio $(RT_PRIO) --cpu $(CPU) --diag-interval 5"

# ─── 压力测试目标 ───

STRESS_CONFIG_JSON = config/stress_config.json
STRESS_CONFIG_GEN  = build/runtime_config_stress.gen.cpp
STRESS_POU_SRC     = stress_test_pou.cpp
STRESS_TARGET      = plc_runtime_rk3588_stress

$(STRESS_CONFIG_GEN): $(STRESS_CONFIG_JSON) $(STRESS_POU_SRC) | build
	python3 ../scripts/gen_config.py --target rk3588 --build-dir . \
		--input $(STRESS_CONFIG_JSON) --output $(STRESS_CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target rk3588 --build-dir . \
		--input $(STRESS_CONFIG_JSON) --output $(STRESS_CONFIG_GEN)

STRESS_SRCS = $(RT_SRCS) $(MAIN_SRC) $(STRESS_POU_SRC) $(STRESS_CONFIG_GEN) $(TCI_SRCS)

$(STRESS_TARGET): $(STRESS_CONFIG_GEN) $(STRESS_POU_SRC)
	$(CXX) $(CXXFLAGS) -DSTRESS_LOAD=$(STRESS_LOAD) -o $@ $(STRESS_SRCS) $(LDFLAGS)
	@echo "=== Build (stress): $@ (load=$(STRESS_LOAD)) ==="

STRESS_LOAD ?= 5

stress: $(STRESS_CONFIG_GEN) $(STRESS_TARGET)

stress-run: stress
	sudo ./$(STRESS_TARGET) --cycle-us $(CYCLE_US) --rt-prio $(RT_PRIO) --cpu $(CPU) --diag-interval 5

stress-clean:
	-rm -f $(STRESS_TARGET) $(STRESS_CONFIG_GEN)

# ─── 远程部署 ───

RK_HOST ?= 192.168.5.130
RK_USER ?= root
RK_DIR  ?= /opt/st2cpp-rk3588

deploy:
	ssh $(RK_USER)@$(RK_HOST) "mkdir -p $(RK_DIR)"
	rsync -az --delete ../../runtime-flat ../../target ../../output $(RK_USER)@$(RK_HOST):$(RK_DIR)/

remote-run:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && make stub-run CYCLE_US=$(CYCLE_US) RT_PRIO=$(RT_PRIO) CPU=$(CPU)"

remote-jitter:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && make stub && sudo ./$(STUB_TARGET) --jitter-only --cycle-us $(CYCLE_US) --rt-prio $(RT_PRIO) --cpu $(CPU)"

remote-stress:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && make stress-run CYCLE_US=$(CYCLE_US) RT_PRIO=$(RT_PRIO) CPU=$(CPU) STRESS_LOAD=$(STRESS_LOAD)"

remote-shell:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && bash"
