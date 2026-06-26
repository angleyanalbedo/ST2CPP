---
name: st2c-full-build
description: Full ST2C build pipeline - Java compile → ST→C++ transpile → CMake build → test execution
---

# ST2C Full Build Pipeline

Execute the complete ST2C build-test cycle. This skill encodes the exact workflow used repeatedly across sessions with all discovered nuances.

## When to Use

- After modifying Java compiler code (any file under `java/src/`)
- After modifying C++ runtime headers (`runtime-flat/include/`)
- After modifying ST example files (`examples/*.st`)
- Before committing changes
- When verifying a feature implementation

## Pipeline Steps

### Step 1: Java Compilation

```bash
# Quick compile check (catches syntax errors fast)
cd java && mvn compile -q 2>&1

# Full package (needed before ST transpilation)
cd java && mvn package -DskipTests -q 2>&1
```

**Timeout**: 120s for compile, 180s for package.

### Step 2: ST → C++ Transpilation

```bash
# Single file
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/<file>.st \
  --output output/flat/build/<file>.cpp

# Multiple files (run separately for each)
java -jar java/target/st2c-jar-with-dependencies.jar \
  --input examples/test_fb_simple.st \
  --output output/flat/build/test_fb_simple.cpp
```

**Important**: Remove old `.cpp` files from `output/flat/build/` before transpiling to avoid multiple definition errors from stale outputs.

### Step 3: CMake Build

```bash
# From project root
cd runtime-flat/build && cmake --build . 2>&1

# Build specific target
cd runtime-flat/build && cmake --build . --target framework_test 2>&1
cd runtime-flat/build && cmake --build . --target runtime_integration_test 2>&1
```

**First-time CMake setup** (if `build/` doesn't exist):
```bash
cd runtime-flat/build && cmake .. -G "MinGW Makefiles"
```

### Step 4: Test Execution

```bash
# Framework tests (should all pass: 124+ items)
cd runtime-flat/build && ./framework_test.exe 2>&1

# Integration tests (假 POU callback)
cd runtime-flat/build && ./runtime_integration_test.exe 2>&1

# Unit tests (doctest)
cd runtime-flat/build && ./unit_test.exe 2>&1

# Individual tests
cd runtime-flat/build && ./fibonacci.exe 2>&1
cd runtime-flat/build && ./multitask_demo.exe 2>&1
```

## Quick One-Shot (All Steps)

```bash
cd java && mvn package -DskipTests -q && \
java -jar target/st2c-jar-with-dependencies.jar --input ../examples/test.st --output ../output/flat/build/test.cpp && \
cd ../runtime-flat/build && cmake --build . && \
./framework_test.exe 2>&1
```

## Common Failure Patterns

| Error | Cause | Fix |
|-------|-------|-----|
| `multiple definition of PROGRAM_*` | Stale `.cpp` files in `output/flat/build/` | Delete old files, retranspile |
| `registerAllPOUs undefined` | `pou_registry.gen.cpp` not linked | CMake auto-generates; re-run cmake |
| `isDue() never triggers` in tests | Windows ~15ms clock precision | Use Freewheeling tasks, not Cyclic |
| Maven `antlr4-maven-plugin` not found | Plugin only in `<pluginManagement>` | Move to `<plugins>` section |
| `CLASSCastException` in Java | Unsafe `(PLCVariable)` cast | Use `instanceof` check |

## Platform Notes

- **Windows/MinGW**: Use `MinGW Makefiles` generator
- **Clock precision**: `std::chrono::steady_clock` on Windows ≈15ms; tight test loops won't trigger Cyclic tasks
- **Build type**: Release with `-O3 -march=native`
