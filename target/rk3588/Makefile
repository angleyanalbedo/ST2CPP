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

.PHONY: all stub run stub-run clean config deploy remote-run remote-jitter remote-shell

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
	-rm -f $(TARGET) $(STUB_TARGET) $(CONFIG_GEN) $(CONFIG_GEN_STUB)

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

remote-shell:
	ssh -t $(RK_USER)@$(RK_HOST) "cd $(RK_DIR)/target/rk3588 && bash"
