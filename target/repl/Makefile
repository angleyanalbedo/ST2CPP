# target/repl/Makefile — PLC REPL 调试工具
#
# 用法：
#   make                     # 编译 REPL
#   make FLAT_DIR=<path>     # 指定编译器输出目录
#   make clean               # 清理
#
# 依赖：
#   - 先运行 Java 编译器生成 POU .cpp 文件到 FLAT_DIR

ROOT     = ../..
RT_DIR   = $(ROOT)/runtime-flat
FLAT_DIR ?= $(ROOT)/output/flat/build

CXX      = g++
CXXFLAGS = -O2 -std=c++17 -g \
           -I$(RT_DIR)/include \
           -Ithird_party \
           -Wall -Wextra -Wno-unused-parameter

CC       = gcc
CFLAGS   = -O2 -std=c99 -Ithird_party

TARGET   = plc_repl

# runtime-flat 核心源文件
RT_SRCS  = \
	$(RT_DIR)/src/gvl.cpp \
	$(RT_DIR)/src/error_policy.cpp \
	$(RT_DIR)/src/io_manager.cpp \
	$(RT_DIR)/src/program.cpp \
	$(RT_DIR)/src/retain_manager.cpp \
	$(RT_DIR)/src/runtime_validator.cpp \
	$(RT_DIR)/src/task.cpp \
	$(RT_DIR)/src/task_executor.cpp \
	$(RT_DIR)/src/event.cpp \
	$(RT_DIR)/src/watchdog.cpp \
	$(RT_DIR)/src/diag.cpp \
	$(RT_DIR)/src/diag_manager.cpp \
	$(RT_DIR)/src/repl_engine.cpp

# 编译器生成的 POU + debug table
GEN_CPP  = generated.cpp debug_table.cpp debug_map.gen.cpp
GEN_SRCS = $(addprefix $(FLAT_DIR)/, $(GEN_CPP))
# 生成文件的 .o 放本地 build/，避免绝对路径冒号问题
BLD_DIR := build
GEN_OBJS = $(addprefix $(BLD_DIR)/, $(GEN_CPP:.cpp=.o))

REPL_SRC = repl_main.cpp
REPL_OBJ = $(REPL_SRC:.cpp=.o)
IC_SRC   = third_party/isocline_src/isocline.c
IC_OBJ   = third_party/isocline_src/isocline.o

RT_OBJS  = $(RT_SRCS:.cpp=.o)

.PHONY: all clean run

all: $(BLD_DIR) $(TARGET)

$(BLD_DIR):
	mkdir -p $(BLD_DIR)

$(IC_OBJ): $(IC_SRC)
	$(CC) $(CFLAGS) -c $< -o $@

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BLD_DIR)/%.o: $(FLAT_DIR)/%.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(TARGET): $(RT_OBJS) $(GEN_OBJS) $(REPL_OBJ) $(IC_OBJ)
	$(CXX) $(CXXFLAGS) -o $@ $^
	@echo "=== Build: $@ ==="

run: $(TARGET)
	./$(TARGET)

clean:
	rm -f $(TARGET) $(RT_OBJS) $(GEN_OBJS) $(REPL_OBJ) $(IC_OBJ) third_party/isocline.o
	rm -rf $(BLD_DIR)
