# target/stm32f1/Makefile — STM32F103C8T6 裸机 PLC 运行时
#
# 依赖：
#   - arm-none-eabi-gcc (gcc-arm-none-eabi)
#   - STM32CubeF1 HAL（或手动管理的 CMSIS + HAL 源文件）
#   - OpenOCD 或 ST-Link 工具（用于烧录）
#
# 用法：
#   make               # 编译 + 链接
#   make flash         # 烧录到目标板
#   make clean         # 清理
#   make config        # 仅生成运行时配置代码

CROSS   ?= arm-none-eabi-
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++
LD       = $(CROSS)g++
OBJCOPY  = $(CROSS)objcopy
SIZE     = $(CROSS)size

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
BUILD    = build

# STM32 HAL 路径
HAL_DIR ?= ../lib/STM32CubeF1
CMSIS_DIR = $(HAL_DIR)/Drivers/CMSIS
HAL_DRV   = $(HAL_DIR)/Drivers/STM32F1xx_HAL_Driver

# 芯片型号
MCU       = STM32F103C8
LD_SCRIPT = STM32F103C8T6.ld

OPT      ?= -Os
CSTANDARD = -std=c11
CXXSTANDARD = -std=c++17

# 芯片特定标志
MCUFLAGS  = -mcpu=cortex-m3 -mthumb -msoft-float

DEFINES   = -DRT_PLATFORM_BARE_METAL \
            -DSTM32F1xx \
            -DUSE_HAL_DRIVER \
            -DHSE_VALUE=8000000 \
            -DSTM32F103xB \
            -DPROCESS_IMAGE_SIZE_OVERRIDE=1024 \
            -DGVL_SIZE_OVERRIDE=4096 \
            -DGVL_RETAIN_SIZE_OVERRIDE=1024

CFLAGS    = $(OPT) $(MCUFLAGS) $(CSTANDARD) $(DEFINES) \
            -I$(RT_DIR)/include \
            -I. \
            -I$(CMSIS_DIR)/Include \
            -I$(CMSIS_DIR)/Device/ST/STM32F1xx/Include \
            -I$(HAL_DRV)/Inc \
            -Wall -Wextra -Wno-unused-parameter \
            -fdata-sections -ffunction-sections

CXXFLAGS  = $(OPT) $(MCUFLAGS) $(CXXSTANDARD) $(DEFINES) \
            -I$(RT_DIR)/include \
            -I. \
            -I$(CMSIS_DIR)/Include \
            -I$(CMSIS_DIR)/Device/ST/STM32F1xx/Include \
            -I$(HAL_DRV)/Inc \
            -Wall -Wextra -Wno-unused-parameter \
            -fdata-sections -ffunction-sections \
            -fno-exceptions -fno-rtti -fno-use-cxa-atexit

LDFLAGS   = $(OPT) $(MCUFLAGS) \
            -T$(LD_SCRIPT) \
            -Wl,--gc-sections \
            -Wl,-Map=$(BUILD)/plc_f1.map \
            -specs=nano.specs -specs=nosys.specs \
            -lc -lm -lnosys

# 源文件
RT_SRCS   = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC  = platform_stm32f1.cpp runtime_stm32f1.cpp
CONFIG_JSON     = config/config.json
CONFIG_GEN      = $(BUILD)/runtime_config.gen.cpp
CONFIG_GEN_STUB = $(BUILD)/runtime_config_stub.gen.cpp

# HAL 源文件（从 CubeF1 包中添加需要的 HAL 驱动）
HAL_SRCS  = \
    $(HAL_DRV)/Src/stm32f1xx_hal.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_rcc.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_gpio.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_uart.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_tim.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_cortex.c

# 如果 HAL 不存在，使用 stub 占位
HAL_STUB :=
ifneq ("$(wildcard $(HAL_DRV)/Src/stm32f1xx_hal.c)","")
    HAL_STUB :=
else
    HAL_STUB := hal_stub.cpp
endif

# ─── 生成运行时配置代码（绑定 + 任务 + POU 注册） ───
config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | $(BUILD)
	python3 ../scripts/gen_config.py --target stm32f1 --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target stm32f1 --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

$(CONFIG_GEN_STUB): $(CONFIG_JSON) | $(BUILD)
	python3 ../scripts/gen_config.py --target stm32f1 --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	python  ../scripts/gen_config.py --target stm32f1 --stub --input $(CONFIG_JSON) --output $(CONFIG_GEN_STUB) || \
	echo "Warning: gen_config.py --stub failed" 1>&2

# 替换 pou_stub.cpp 为 runtime_config.gen.cpp
SRCS      = $(PLAT_SRC) $(HAL_SRCS) $(HAL_STUB) startup_stm32f103xb.s system_stm32f1xx.c $(CONFIG_GEN)
RT_OBJS   = $(addprefix $(BUILD)/,diag.o event.o gvl.o program.o scheduler.o task.o watchdog.o)
OBJS      = $(RT_OBJS) \
            $(patsubst %.cpp,$(BUILD)/%.o,$(filter %.cpp,$(notdir $(SRCS)))) \
            $(patsubst %.c,$(BUILD)/%.o,$(filter %.c,$(notdir $(SRCS)))) \
            $(patsubst %.s,$(BUILD)/%.o,$(filter %.s,$(notdir $(SRCS))))

TARGET    = plc_f1

.PHONY: all flash clean config

all: $(CONFIG_GEN) $(BUILD)/$(TARGET).elf $(BUILD)/$(TARGET).bin $(BUILD)/$(TARGET).hex

vpath %.c $(HAL_DRV)/Src

# CMSIS 启动/系统文件路径
STARTUP_DIR = $(CMSIS_DIR)/Device/ST/STM32F1xx/Source/Templates
vpath %.s $(STARTUP_DIR)/gcc
vpath %.c $(STARTUP_DIR)

$(BUILD)/%.o: %.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/%.o: %.c | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/%.o: %.s | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

# runtime_config.gen.cpp 编译（在 build/ 下生成，从 build/ 编译）
$(BUILD)/runtime_config.gen.o: $(CONFIG_GEN) | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $(CONFIG_GEN) -o $@

# 从 RT_DIR/src/ 编译
$(BUILD)/diag.o: $(RT_DIR)/src/diag.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/event.o: $(RT_DIR)/src/event.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/gvl.o: $(RT_DIR)/src/gvl.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/program.o: $(RT_DIR)/src/program.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/scheduler.o: $(RT_DIR)/src/scheduler.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/task.o: $(RT_DIR)/src/task.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@
$(BUILD)/watchdog.o: $(RT_DIR)/src/watchdog.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/$(TARGET).elf: $(OBJS)
	$(LD) $(LDFLAGS) -o $@ $^
	$(SIZE) $@

$(BUILD)/$(TARGET).bin: $(BUILD)/$(TARGET).elf
	$(OBJCOPY) -O binary $< $@

$(BUILD)/$(TARGET).hex: $(BUILD)/$(TARGET).elf
	$(OBJCOPY) -O ihex $< $@

flash: $(BUILD)/$(TARGET).bin
	openocd -f interface/stlink.cfg -f target/stm32f1x.cfg \
		-c "program $(BUILD)/$(TARGET).bin 0x08000000 reset exit"

openocd:
	openocd -f interface/stlink.cfg -f target/stm32f1x.cfg

clean:
	rm -rf $(BUILD)

$(BUILD):
	mkdir -p $(BUILD)
