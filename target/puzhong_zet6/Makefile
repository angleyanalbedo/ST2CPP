ROOT     = ../..
TOOLS_DIR = $(ROOT)/.env
ifneq ("$(wildcard $(TOOLS_DIR)/arm-gcc/bin/arm-none-eabi-gcc.exe)","")
    CROSS   := $(TOOLS_DIR)/arm-gcc/bin/arm-none-eabi-
else
    CROSS   ?= arm-none-eabi-
endif
CC       = $(CROSS)gcc
CXX      = $(CROSS)g++
LD       = $(CROSS)g++
OBJCOPY  = $(CROSS)objcopy
SIZE     = $(CROSS)size

RT_DIR   = $(ROOT)/runtime-flat
BUILD    = build
TCI_DIR  = ../lib/tci

HAL_DIR ?= ../lib/STM32CubeF1
CMSIS_DIR = $(HAL_DIR)/Drivers/CMSIS
HAL_DRV   = $(HAL_DIR)/Drivers/STM32F1xx_HAL_Driver

MCU       = STM32F103ZE
LD_SCRIPT = STM32F103ZET6.ld

OPT      ?= -Os
CSTANDARD = -std=c11
CXXSTANDARD = -std=c++17

MCUFLAGS  = -mcpu=cortex-m3 -mthumb -msoft-float

DEFINES   = -DRT_PLATFORM_BARE_METAL \
            -DSTM32F1xx \
            -DUSE_HAL_DRIVER \
            -DHSE_VALUE=8000000 \
            -DSTM32F103xE \
            -DPROCESS_IMAGE_SIZE_OVERRIDE=1024 \
            -DGVL_SIZE_OVERRIDE=8192 \
            -DGVL_RETAIN_SIZE_OVERRIDE=2048 \
            -DMAX_TASKS=8 \
            -DMAX_PROGRAMS=16 \
            -DMAX_POUS_PER_TASK=8

CFLAGS    = $(OPT) $(MCUFLAGS) $(CSTANDARD) $(DEFINES) \
            -I$(RT_DIR)/include \
            -I. \
            -I$(TCI_DIR) \
            -I$(CMSIS_DIR)/Include \
            -I$(CMSIS_DIR)/Device/ST/STM32F1xx/Include \
            -I$(HAL_DRV)/Inc \
            -Wall -Wextra -Wno-unused-parameter \
            -fdata-sections -ffunction-sections

CXXFLAGS  = $(OPT) $(MCUFLAGS) $(CXXSTANDARD) $(DEFINES) \
            -I$(RT_DIR)/include \
            -I. \
            -I$(TCI_DIR) \
            -I$(CMSIS_DIR)/Include \
            -I$(CMSIS_DIR)/Device/ST/STM32F1xx/Include \
            -I$(HAL_DRV)/Inc \
            -Wall -Wextra -Wno-unused-parameter \
            -fdata-sections -ffunction-sections \
            -fno-exceptions -fno-rtti -fno-use-cxa-atexit

LDFLAGS   = $(OPT) $(MCUFLAGS) \
            -T$(LD_SCRIPT) \
            -Wl,--gc-sections \
            -Wl,-Map=$(BUILD)/plc_puzhong.map \
            -specs=nano.specs -specs=nosys.specs \
            -lc -lm -lnosys

RT_SRCS   = $(wildcard $(RT_DIR)/src/*.cpp)
PLAT_SRC  = platform_puzhong.cpp runtime_puzhong.cpp
CONFIG_JSON     = config/config.json
CONFIG_GEN      = $(BUILD)/runtime_config.gen.cpp
CONFIG_GEN_STUB = $(BUILD)/runtime_config_stub.gen.cpp

HAL_SRCS  = \
    $(HAL_DRV)/Src/stm32f1xx_hal.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_rcc.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_gpio.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_uart.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_tim.c \
    $(HAL_DRV)/Src/stm32f1xx_hal_cortex.c

config: $(CONFIG_GEN)

$(CONFIG_GEN): $(CONFIG_JSON) | $(BUILD)
	python3 ../scripts/gen_config.py --target stm32f1 --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	python  ../scripts/gen_config.py --target stm32f1 --input $(CONFIG_JSON) --output $(CONFIG_GEN) || \
	echo "Warning: gen_config.py failed" 1>&2

SRCS      = $(PLAT_SRC) $(HAL_SRCS) startup_stm32f103xe.s system_stm32f1xx.c $(CONFIG_GEN) pou_stub.cpp
RT_OBJS   = $(addprefix $(BUILD)/,diag.o diag_manager.o error_policy.o event.o gvl.o io_manager.o program.o retain_manager.o runtime_validator.o scheduler.o task.o task_executor.o watchdog.o)
OBJS      = $(RT_OBJS) \
            $(patsubst %.cpp,$(BUILD)/%.o,$(filter %.cpp,$(notdir $(SRCS)))) \
            $(patsubst %.c,$(BUILD)/%.o,$(filter %.c,$(notdir $(SRCS)))) \
            $(patsubst %.s,$(BUILD)/%.o,$(filter %.s,$(notdir $(SRCS))))

TARGET    = plc_puzhong

.PHONY: all flash clean config

all: $(CONFIG_GEN) $(BUILD)/$(TARGET).elf $(BUILD)/$(TARGET).bin $(BUILD)/$(TARGET).hex

vpath %.c $(HAL_DRV)/Src
STARTUP_DIR = $(CMSIS_DIR)/Device/ST/STM32F1xx/Source/Templates
vpath %.s $(STARTUP_DIR)/gcc
vpath %.c $(STARTUP_DIR)

$(BUILD)/%.o: %.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/%.o: %.c | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/%.o: %.s | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/runtime_config.gen.o: $(CONFIG_GEN) | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $(CONFIG_GEN) -o $@

$(BUILD)/%.o: $(RT_DIR)/src/%.cpp | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/$(TARGET).elf: $(OBJS)
	$(LD) $(LDFLAGS) -o $@ $^
	$(SIZE) $@

$(BUILD)/$(TARGET).bin: $(BUILD)/$(TARGET).elf
	$(OBJCOPY) -O binary $< $@

$(BUILD)/$(TARGET).hex: $(BUILD)/$(TARGET).elf
	$(OBJCOPY) -O ihex $< $@

OPENOCD ?= openocd
ifneq ("$(wildcard $(TOOLS_DIR)/opencoded/bin/openocd.exe)","")
    OPENOCD := $(TOOLS_DIR)/opencoded/bin/openocd.exe
endif

flash: $(BUILD)/$(TARGET).bin
	$(OPENOCD) -f interface/stlink.cfg -f target/stm32f1x.cfg \
		-c "program $(BUILD)/$(TARGET).bin 0x08000000 reset exit"

flash-jtag: $(BUILD)/$(TARGET).bin
	$(OPENOCD) -c "transport select jtag" -f interface/stlink.cfg -f target/stm32f1x.cfg \
		-c "program $(BUILD)/$(TARGET).bin 0x08000000 reset exit"

openocd:
	$(OPENOCD) -f interface/stlink.cfg -f target/stm32f1x.cfg

clean:
	rm -rf $(BUILD)

$(BUILD):
	mkdir -p $(BUILD)
