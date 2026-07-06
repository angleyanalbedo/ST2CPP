# ST2C 顶层构建 — 一条命令出 PLC REPL 调试器
#
# 用法：
#   make                                # 自动 mvn → java -jar → g++，出 plc_repl
#   make EXAMPLE=examples/foo.st        # 用指定 .st 文件
#   make FLAT_DIR=../out -C target/repl # 跳过顶层，直接编 REPL
#   make clean                          # 清理构建产物（保留 JAR）
#
# 依赖：
#   - JDK 17+（如果 JAR 不存在，自动 mvn package）
#   - g++ / cmake

JAR     := java/target/st2c-jar-with-dependencies.jar
EXAMPLE ?= examples/projects/syntax_tests/test.st
FLAT_DIR ?= output/flat/build

# ── JAR：不存在才跑 mvn ──
$(JAR):
	cd java && mvn package -DskipTests

# ── POU 生成 ──
# 用 --output 固定文件名，--file-id combined 匹配 repl_main.cpp
$(FLAT_DIR)/generated.cpp: $(JAR) $(EXAMPLE)
	mkdir -p $(FLAT_DIR)
	java -jar $(JAR) \
		--input $(EXAMPLE) \
		--output $(FLAT_DIR)/generated.cpp \
		--file-id combined \
		--generate-debug

# ── REPL ──
.PHONY: repl
repl: $(FLAT_DIR)/generated.cpp
	$(MAKE) -C target/repl FLAT_DIR=$(abspath $(FLAT_DIR))

.PHONY: all
all: repl

# ── 清理 ──
.PHONY: clean
clean:
	rm -rf $(FLAT_DIR)
	$(MAKE) -C target/repl clean
