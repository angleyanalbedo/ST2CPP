---
name: st2c-add-test
description: Pattern for adding new tests to ST2C runtime test suite (framework_test or integration_test)
---

# ST2C Test Development Pattern

Add new tests to the ST2C runtime test suite. This skill encodes the consistent pattern observed across 48+ test additions.

## Test Files

| File | Purpose | Test Count |
|------|---------|------------|
| `runtime-flat/tests/framework_test.cpp` | rt_plc.h + rt_runtime.h 框架完整性 | 124+ items |
| `runtime-flat/tests/runtime_integration_test.cpp` | 假 POU callback 全链路集成 | 60+ items |
| `runtime-flat/tests/unit_test.cpp` | doctest 单元测试合集 | varies |

## Adding to framework_test.cpp

### Pattern

```cpp
// Find the next test number (look at existing TEST() calls)
// Add after the last test in the relevant section

TEST("描述测试内容的名称");
CHECK(condition, "失败时的错误信息");

// For multiple related checks:
TEST("检查项 1");
CHECK(value == expected, "value should be expected");
TEST("检查项 2");
CHECK(ptr != nullptr, "pointer should not be null");
```

### Section Headers

Use `printf("\n═══ Section Name ═══\n")` to separate test groups.

### Common Patterns

**GVL read/write test:**
```cpp
GVL gvl;
gvl.write<INT>(offset, 42);
CHECK(gvl.read<INT>(offset) == 42, "GVL write/read roundtrip");
```

**Bounds check test:**
```cpp
GVL gvl;
INT val = gvl.read<INT>(GVL_SIZE);  // Out of bounds
CHECK(val == 0, "out-of-bounds read returns zero");
```

**ProcessImage test:**
```cpp
ProcessImage pi;
pi.writeOutput<REAL>(0, 3.14f);
CHECK(pi.readOutput<REAL>(0) == 3.14f, "PI write/read");
```

**Macro test:**
```cpp
ProcessImage pi;
PI_WRITE_OUTPUT(pi, 0, 42);
CHECK(PI_READ_OUTPUT(pi, 0) == 42, "PI macro");
```

## Adding to runtime_integration_test.cpp

### Pattern

```cpp
// 1. Add static variables for tracking
static int my_test_count = 0;

// 2. Add POU callback functions
void POU_MyTest_init(GVL& gvl, ProcessImage& io) {
    my_test_count = 0;
    gvl.write<INT>(Off::MY_OFFSET, 0);
}

void POU_MyTest_cyclic(GVL& gvl, ProcessImage& io, TIME dt) {
    my_test_count++;
    INT val = gvl.read<INT>(Off::MY_OFFSET);
    gvl.write<INT>(Off::MY_OFFSET, val + 1);
}

// 3. Add test function
void test_my_feature() {
    TEST_SECTION("N. 测试描述");

    Scheduler s;
    s.start(COLD);

    my_test_count = 0;
    int tIdx = s.addFreewheelingTask("Test", 5);
    s.addProgram("MyTest", POU_MyTest_init, POU_MyTest_cyclic);
    s.addProgramToTask(tIdx, 0);

    for (int i = 0; i < 5; i++) s.tick();

    TEST("描述");
    CHECK(condition, "error message");
}

// 4. Register in main()
int main() {
    // ... existing tests ...
    test_my_feature();  // Add here
    // ... summary ...
}
```

### Critical Timing Rules

1. **Use Freewheeling tasks** for deterministic tests (Cyclic tasks depend on real-time `isDue()`)
2. **`start(COLD)` zeroes GVL** — set values AFTER first tick, not before
3. **Event detection has 1-tick latency** — `checkEvents()` runs before task loop
4. **Busy loop for watchdog**: ~20M volatile double iterations ≈ 47ms

### GVL Offset Convention

Define offsets in the `Off` namespace:
```cpp
namespace Off {
    constexpr size_t MY_VAR = 28;  // Next available offset
}
```

## After Adding Tests

1. Build: `cd runtime-flat/build && cmake --build .`
2. Run: `./framework_test.exe` or `./runtime_integration_test.exe`
3. Verify: All existing tests still pass, new tests pass
4. Update count in file header comment if significantly changed
