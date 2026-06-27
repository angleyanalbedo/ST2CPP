#!/usr/bin/env bash
set -euo pipefail

# HVAC Controller — 集成测试编译运行脚本 (Linux/macOS)
# Usage: ./run.sh [--build-jar]

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ST2C_HOME="$(cd "$PROJECT_DIR/../../.." && pwd)"
STDLIB="$ST2C_HOME/examples/iec_stdlib.st"
JAR="$ST2C_HOME/java/target/st2c-jar-with-dependencies.jar"
OUTPUT_DIR="$ST2C_HOME/output/flat/build"
RUNTIME_DIR="$ST2C_HOME/runtime-flat"
BUILD_DIR="$ST2C_HOME/output/build"

echo "=========================================="
echo " HVAC Controller — Integration Test"
echo "=========================================="

# Step 1: Build JAR if needed
if [ "${1:-}" = "--build-jar" ]; then
    echo "[1/5] Building Java compiler JAR..."
    (cd "$ST2C_HOME/java" && mvn package -DskipTests)
fi

if [ ! -f "$JAR" ]; then
    echo "ERROR: JAR not found at $JAR"
    echo "Run with --build-jar to compile first, or build manually:"
    echo "  cd java && mvn package -DskipTests"
    exit 1
fi

echo "[1/5] JAR ready: $JAR"

# Step 2: Compile ST files
echo "[2/5] Compiling ST source files..."
mkdir -p "$OUTPUT_DIR"

java -jar "$JAR" \
    --stdlib "$STDLIB" \
    --input "$PROJECT_DIR/types.st" \
    --input "$PROJECT_DIR/io_config.st" \
    --input "$PROJECT_DIR/pid_func.st" \
    --input "$PROJECT_DIR/zone_fb.st" \
    --input "$PROJECT_DIR/main.st" \
    --output-dir "$OUTPUT_DIR" \
    --file-id hvac \
    --verbose

echo "[2/5] Compilation complete"

# Step 3: Configure CMake
echo "[3/5] Configuring CMake..."
rm -rf "$BUILD_DIR" && mkdir -p "$BUILD_DIR"

cmake -B "$BUILD_DIR" -S "$RUNTIME_DIR" \
    -DCMAKE_BUILD_TYPE=Release \
    -DGEN_CPP_DIR="$OUTPUT_DIR"

echo "[3/5] CMake configured"

# Step 4: Build runtime with POU
echo "[4/5] Building runtime..."
cmake --build "$BUILD_DIR" --parallel
echo "[4/5] Build complete"

# Step 5: Run integration test
echo "[5/5] Running HVAC integration test..."
"$BUILD_DIR/runtime"
echo "[5/5] Execution complete"

echo "=========================================="
echo " SUCCESS: HVAC integration test passed"
echo "=========================================="
