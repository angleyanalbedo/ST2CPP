/**
 * fibonacci.cpp — 手写测试程序
 *
 * 模拟"编译器生成"的 C++ 代码，验证新 Runtime 的接口设计。
 * 对比旧 Runtime 的 main.cpp（ST2C-master/java/src/main/resources/output/main.cpp），
 * 展示翻译风格的变化。
 *
 * 对应的 ST 源码：
 * ─────────────────────────────────────
 * FUNCTION FUN : INT
 *   VAR_INPUT X : INT; END_VAR
 *   VAR TEMP result : INT; END_VAR
 *   IF X = 2 THEN
 *     FUN := 1;
 *   ELSIF X = 1 THEN
 *     FUN := 1;
 *   ELSE
 *     FUN := FUN(X - 1) + FUN(X - 2);
 *   END_IF;
 * END_FUNCTION
 *
 * PROGRAM MAIN
 *   VAR A : INT := 10; END_VAR
 *   VAR B : INT := 0;  END_VAR
 *   FOR B := 10 TO 100 BY 2 DO
 *     A := A + 20;
 *   END_FOR;
 *   B := FUN(10);
 * END_PROGRAM
 * ─────────────────────────────────────
 */
#include "rt_plc.h"
#include <cstdio>

using namespace rt_plc;


// ═══════════════════════════════════════
// 编译器生成的 FUNCTION 翻译
// ═══════════════════════════════════════

// ST: FUNCTION FUN : INT
//     VAR_INPUT X : INT; END_VAR
// 翻译为：普通 C++ 函数，VAR_INPUT 是传值参数
INT FUN(INT X) {
    // IF X = 2 THEN FUN := 1;
    if (X == 2) {
        return 1;
    }
    // ELSIF X = 1 THEN FUN := 1;
    else if (X == 1) {
        return 1;
    }
    // ELSE FUN := FUN(X-1) + FUN(X-2);
    else {
        return FUN(X - 1) + FUN(X - 2);
    }
}

// ═══════════════════════════════════════
// 编译器生成的 PROGRAM 翻译
// ═══════════════════════════════════════

// PROGRAM 的变量翻译为 static（跨扫描周期保持状态）
// 如果只做单周期执行，也可以是全局变量
static INT A = 10;
static INT B = 0;

// PROGRAM MAIN 的函数体（每个扫描周期调用一次）
void PROGRAM_MAIN() {
    // FOR B := 10 TO 100 BY 2 DO A := A + 20; END_FOR;
    for (B = 10; B <= 100; B = B + 2) {
        A = A + 20;
    }

    // B := FUN(10);
    B = FUN(10);
}


// ═══════════════════════════════════════
// 测试入口
// ═══════════════════════════════════════

int main() {
    printf("=== ST2C 实时 Runtime 验证 ===\n\n");

    // --- 测试 1: 函数调用 ---
    printf("--- FUNCTION 调用测试 ---\n");
    for (int i = 1; i <= 10; i++) {
        printf("FUN(%d) = %d\n", i, FUN((INT)i));
    }

    // --- 测试 2: PROGRAM 执行 ---
    printf("\n--- PROGRAM 执行测试 ---\n");
    printf("执行前: A=%d, B=%d\n", A, B);
    PROGRAM_MAIN();
    printf("执行后: A=%d, B=%d\n", A, B);

    // --- 测试 3: 扫描周期运行（跑 5 个周期然后停止）---
    printf("\n--- 扫描周期测试 (5 cycles) ---\n");
    Runtime rt;
    rt.setCycleTime(1000);  // 1ms 周期

    int cycleNum = 0;
    rt.run([&]() {
        PROGRAM_MAIN();
        cycleNum++;
        printf("Cycle %lu: A=%d, B=%d\n",
               (unsigned long)rt.cycleCount, A, B);
        if (cycleNum >= 5) {
            rt.stop();
        }
    });

    // --- 测试 4: 类型和基本运算验证 ---
    printf("\n--- 类型运算测试 ---\n");
    {
        SINT  s = 127;
        INT   i = 32000;
        DINT  d = 100000;
        REAL  r = 3.14f;
        BOOL  b = TRUE;

        printf("SINT  %d + 1 = %d\n", s, s + (SINT)1);
        printf("INT   %d + 1 = %d\n", i, i + 1);
        printf("DINT  %d + 1 = %d\n", d, d + 1);
        printf("REAL  %.2f + 1.0 = %.2f\n", r, r + 1.0f);
        printf("BOOL  %d AND 0 = %d\n", b, b & FALSE);
        printf("ABS(-42) = %d\n", ABS((DINT)-42));
        printf("MIN(3,7) = %d\n", MIN(3, 7));
        printf("MAX(3,7) = %d\n", MAX(3, 7));
        printf("LIMIT(0, 15, 10) = %d\n", LIMIT(0, 15, 10));
        printf("SEL(TRUE, 100, 200) = %d\n", SEL(TRUE, (INT)100, (INT)200));
    }

    // --- 测试 5: 边沿检测 ---
    printf("\n--- 边沿检测测试 ---\n");
    {
        R_TRIG rising;
        BOOL signal[] = {FALSE, FALSE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE};
        for (int i = 0; i < 8; i++) {
            BOOL result = rising(signal[i]);
            printf("signal=%d -> R_TRIG=%d\n", signal[i], result);
        }
    }

    // --- 测试 6: STRING ---
    printf("\n--- STRING 测试 ---\n");
    {
        STRING s1("Hello");
        STRING s2("Hello");
        STRING s3("World");
        printf("s1 == s2: %d\n", s1 == s2);
        printf("s1 == s3: %d\n", s1 == s3);
        printf("s1 = '%s'\n", s1.data);

        // 字符串操作
        STRING concat = str_concat(s1, STRING(" PLC"));
        printf("concat = '%s'\n", concat.data);
        printf("len(s1) = %d\n", str_len(s1));
        printf("str_eq(s1, s2) = %d\n", str_eq(s1, s2));

        // 字符串赋值
        STRING s4;
        str_assign(s4, "assigned");
        printf("s4 = '%s'\n", s4.data);
    }

    // --- 测试 7: STRUCT ---
    // ST: TYPE Point : STRUCT x : REAL; y : REAL; name : STRING; END_STRUCT; END_TYPE
    printf("\n--- STRUCT 测试 ---\n");
    {
        struct Point {
            REAL x;
            REAL y;
            STRING name;
        };

        Point p1;
        p1.x = 3.14f;
        p1.y = 2.72f;
        str_assign(p1.name, "origin");

        // 整体赋值（C++ 默认拷贝）
        Point p2 = p1;
        printf("p1 = (%.2f, %.2f, '%s')\n", p1.x, p1.y, p1.name.data);
        printf("p2 = (%.2f, %.2f, '%s')\n", p2.x, p2.y, p2.name.data);
        printf("p2 after copy: x=%.2f\n", p2.x);

        // 修改 p2 不影响 p1
        p2.x = 99.0f;
        printf("p1.x=%.2f, p2.x=%.2f (independent)\n", p1.x, p2.x);
    }

    // --- 测试 8: ARRAY ---
    // ST: VAR arr : ARRAY[1..5] OF INT; END_VAR
    printf("\n--- ARRAY 测试 ---\n");
    {
        PLC_Array<INT, 5> arr;
        printf("初始值: ");
        for (size_t i = 0; i < arr.size(); i++) {
            printf("%d ", arr[i]);
        }
        printf("\n");

        // 填充
        arr.fill(42);
        printf("fill(42): ");
        for (size_t i = 0; i < arr.size(); i++) {
            printf("%d ", arr[i]);
        }
        printf("\n");

        // 逐元素赋值
        for (size_t i = 0; i < arr.size(); i++) {
            arr[i] = (INT)(i * 10);
        }
        printf("arr[i]=i*10: ");
        for (size_t i = 0; i < arr.size(); i++) {
            printf("%d ", arr[i]);
        }
        printf("\n");

        // 带下标偏移的访问（模拟 ARRAY[3..7]）
        // ST: arr[5] 对应内部 at(5-3) = at(2)
        INT val = PLC_ARRAY_AT(arr, 5, 3);
        printf("ARRAY[3..7] arr[5] = at(5-3) = %d\n", val);

        // 越界检查
        printf("越界访问测试: ");
        arr.at(99);  // 应该打印警告并回退
        printf("\n");

        // 多维数组: ARRAY[1..2, 1..3] OF INT
        PLC_Array<PLC_Array<INT, 3>, 2> matrix;
        matrix[0][0] = 1; matrix[0][1] = 2; matrix[0][2] = 3;
        matrix[1][0] = 4; matrix[1][1] = 5; matrix[1][2] = 6;
        printf("matrix[0] = [%d, %d, %d]\n", matrix[0][0], matrix[0][1], matrix[0][2]);
        printf("matrix[1] = [%d, %d, %d]\n", matrix[1][0], matrix[1][1], matrix[1][2]);

        // 数组整体赋值
        PLC_Array<INT, 5> arr2 = arr;
        printf("arr2 = arr: ");
        for (size_t i = 0; i < arr2.size(); i++) {
            printf("%d ", arr2[i]);
        }
        printf("\n");
    }

    // --- 测试 9: ENUM ---
    // ST: TYPE Color : (Red, Green, Blue := 5, Yellow); END_TYPE
    printf("\n--- ENUM 测试 ---\n");
    {
        enum class Color : DINT {
            Red = 0, Green = 1, Blue = 5, Yellow = 6, _COUNT = 7
        };

        Color c = Color::Green;
        printf("Green = %d\n", (DINT)c);
        printf("Blue  = %d\n", (DINT)Color::Blue);

        // 枚举比较
        printf("c == Red: %d\n", c == Color::Red);
        printf("c == Green: %d\n", c == Color::Green);

        // CASE 翻译为 switch
        switch (c) {
            case Color::Red:    printf("color is Red\n"); break;
            case Color::Green:  printf("color is Green\n"); break;
            case Color::Blue:   printf("color is Blue\n"); break;
            case Color::Yellow: printf("color is Yellow\n"); break;
        }
    }

    // --- 测试 10: SUBRANGE ---
    // ST: TYPE Percentage : INT (0..100); END_TYPE
    printf("\n--- SUBRANGE 测试 ---\n");
    {
        typedef PLC_Subrange<INT, 0, 100> Percentage;

        Percentage p;
        printf("default: %d\n", (INT)p);    // 应该是 0（Low）

        p = 50;
        printf("p = 50: %d\n", (INT)p);     // 50

        p = 150;
        printf("p = 150 (clamped): %d\n", (INT)p);  // 100

        p = -10;
        printf("p = -10 (clamped): %d\n", (INT)p);  // 0

        // 运算
        Percentage q(30);
        Percentage r = q + 20;
        printf("30 + 20 = %d\n", (INT)r);   // 50

        r = q + 80;
        printf("30 + 80 (clamped) = %d\n", (INT)r);  // 100

        // 比较
        printf("50 < 100: %d\n", Percentage(50) < 100);
    }

    // --- 测试 11: REF / POINTER ---
    printf("\n--- REF/POINTER 测试 ---\n");
    {
        INT myVar = 42;
        INT* p = nullptr;

        printf("p == nullptr: %d\n", plc_is_valid_ref(p));

        // REF= 翻译为 &
        p = &myVar;
        printf("p == &myVar: %d\n", plc_is_valid_ref(p));

        // ^ 解引用
        printf("*p = %d\n", plc_deref(p, "p"));

        // 通过引用修改
        plc_deref(p, "p") = 99;
        printf("myVar after *p=99: %d\n", myVar);

        // NULL 解引用保护
        INT* null_p = nullptr;
        INT safe = plc_deref(null_p, "null_p");
        printf("null deref safe fallback: %d\n", safe);
    }

    // --- 测试 12: 定时器 TP (脉冲) ---
    printf("\n--- TP 脉冲定时器测试 ---\n");
    {
        TP pulse;
        TIME cycleTime = T_ms(1);  // 1ms 周期

        // 上升沿触发，PT=3ms
        printf("[IN=TRUE 上升沿]\n");
        for (int i = 0; i < 5; i++) {
            pulse.update(TRUE, T_ms(3), cycleTime);
            printf("  cycle %d: Q=%d, ET=%lld us\n", i, pulse.output, pulse.elapsed);
        }
        // IN 保持 TRUE，但脉冲已过期，不应重新触发
        printf("[IN 保持 TRUE, 不应重新触发]\n");
        for (int i = 0; i < 2; i++) {
            pulse.update(TRUE, T_ms(3), cycleTime);
            printf("  cycle %d: Q=%d, ET=%lld us\n", i + 5, pulse.output, pulse.elapsed);
        }
        // IN 回到 FALSE
        printf("[IN=FALSE 复位]\n");
        pulse.update(FALSE, T_ms(3), cycleTime);
        printf("  Q=%d, ET=%lld us\n", pulse.output, pulse.elapsed);
        // IN 再次上升沿 → 重新触发
        printf("[IN=TRUE 再次上升沿]\n");
        for (int i = 0; i < 3; i++) {
            pulse.update(TRUE, T_ms(3), cycleTime);
            printf("  cycle %d: Q=%d, ET=%lld us\n", i, pulse.output, pulse.elapsed);
        }
    }

    // --- 测试 13: CTD 递减计数器 ---
    printf("\n--- CTD 递减计数器测试 ---\n");
    {
        CTD counter;
        counter.update(FALSE, TRUE, 3);  // LOAD, PV=3
        printf("CDT load 3: count=%d, Q=%d\n", counter.count, counter.output);

        for (int i = 0; i < 4; i++) {
            counter.update(TRUE, FALSE, 3);
            counter.update(FALSE, FALSE, 3);  // 模拟边沿
            printf("CDT tick %d: count=%d, Q=%d\n", i + 1, counter.count, counter.output);
        }
    }

    // --- 测试 14: 类型转换 ---
    printf("\n--- 类型转换测试 ---\n");
    {
        printf("TO_REAL(42) = %.1f\n", TO_REAL((INT)42));
        printf("TO_INT(3.14) = %d\n", TO_INT(3.14f));
        printf("TO_DINT(3.7) = %d\n", TO_DINT(3.7f));
        printf("TO_LINT(100) = %lld\n", (long long)TO_LINT((DINT)100));
        printf("TO_BOOL(0) = %d\n", TO_BOOL((INT)0));
        printf("TO_BOOL(42) = %d\n", TO_BOOL((INT)42));

        STRING s1 = TO_STRING((INT)42);
        STRING s2 = TO_STRING(3.14f);
        STRING s3 = TO_STRING(TRUE);
        printf("TO_STRING(42) = '%s'\n", s1.data);
        printf("TO_STRING(3.14) = '%s'\n", s2.data);
        printf("TO_STRING(TRUE) = '%s'\n", s3.data);
    }

    // --- 测试 15: 过程映像 ---
    printf("\n--- 过程映像测试 ---\n");
    {
        ProcessImage img;
        img.clearInputs();
        img.clearOutputs();

        // 模拟写入输入（%IW0 = 偏移 0 处的 INT）
        img.writeOutput<INT>(0, 1234);
        INT val = img.readOutput<INT>(0);
        printf("write INT 1234 at offset 0, read back: %d\n", val);

        // REAL 类型 I/O
        img.writeOutput<REAL>(100, 3.14f);
        REAL r = img.readOutput<REAL>(100);
        printf("write REAL 3.14 at offset 100, read back: %.2f\n", r);
    }

    printf("\n=== 全部测试通过 ===\n");
    return 0;
}
