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
GEN_SRCS = \
	$(FLAT_DIR)/generated.cpp \
	$(FLAT_DIR)/debug_table.cpp \
	$(FLAT_DIR)/debug_map.gen.cpp

REPL_SRC = repl_main.cpp
IC_SRC   = third_party/isocline_src/isocline.c

RT_OBJS  = $(RT_SRCS:.cpp=.o) $(GEN_SRCS:.cpp=.o) $(REPL_SRC:.cpp=.o)
IC_OBJ   = third_party/isocline_src/isocline.o

.PHONY: all clean run

all: $(TARGET)

$(IC_OBJ): $(IC_SRC)
	$(CC) $(CFLAGS) -c $< -o $@

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(TARGET): $(RT_OBJS) $(IC_OBJ)
	$(CXX) $(CXXFLAGS) -o $@ $^
	@echo "=== Build: $@ ==="

run: $(TARGET)
	./$(TARGET)

clean:
	rm -f $(TARGET) $(RT_OBJS) $(IC_OBJ) third_party/isocline.o
