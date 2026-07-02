(*
 * test_tricky.st — 刁钻综合测试
 *
 * 覆盖:
 *   1. ENUM + CASE 分支 (语法: TYPE NAME : (VAL1, VAL2); END_TYPE)
 *   2. 嵌套 FOR + 结构体数组
 *   3. WHILE/REPEAT 循环
 *   4. 多返回路径函数
 *   5. 函数调用函数
 *   6. 混合类型运算 (INT/DINT/REAL)
 *   7. 复杂布尔表达式 (AND/OR/NOT)
 *   8. STRING 拼接 (PRINT)
 *)

TYPE
    DIRECTION : (NORTH, EAST, SOUTH, WEST);
END_TYPE

TYPE
    MATRIX_ROW :
    STRUCT
        VALUES : ARRAY[0..3] OF INT;
        TAG    : INT;
    END_STRUCT;
END_TYPE

FUNCTION DOUBLE_IT : DINT
    VAR_INPUT
        X : INT;
    END_VAR
    DOUBLE_IT := X * 2;
END_FUNCTION

FUNCTION CLAMP : DINT
    VAR_INPUT
        VAL  : DINT;
        MINV : DINT;
        MAXV : DINT;
    END_VAR
    IF VAL < MINV THEN
        CLAMP := MINV;
    ELSIF VAL > MAXV THEN
        CLAMP := MAXV;
    ELSE
        CLAMP := VAL;
    END_IF
END_FUNCTION

FUNCTION WRAP_ANGLE : INT
    VAR_INPUT
        ANGLE : INT;
    END_VAR
    WHILE ANGLE < 0 DO
        ANGLE := ANGLE + 360;
    END_WHILE
    WHILE ANGLE >= 360 DO
        ANGLE := ANGLE - 360;
    END_WHILE
    WRAP_ANGLE := ANGLE;
END_FUNCTION

FUNCTION AVERAGE : REAL
    VAR_INPUT
        A : REAL;
        B : REAL;
    END_VAR
    AVERAGE := (A + B) / 2.0;
END_FUNCTION

FUNCTION_BLOCK STATE_MACHINE
    VAR_INPUT
        CMD      : INT;
        POSITION : DINT;
    END_VAR
    VAR_OUTPUT
        DIR      : DIRECTION;
        SPEED    : INT;
        FAULT    : BOOL;
    END_VAR
    VAR
        MODE     : INT := 0;
        ACCUM    : DINT := 0;
        RUNNING  : BOOL := FALSE;
        RETRY    : INT := 0;
    END_VAR

    FAULT := FALSE;
    RUNNING := TRUE;

    CASE CMD OF
        0:
            MODE := 0;
            ACCUM := 0;
            SPEED := 0;
            DIR := DIRECTION#NORTH;
            RUNNING := FALSE;
        1:
            MODE := 10;
            ACCUM := 0;
            DIR := DIRECTION#EAST;
        2:
            MODE := 20;
            ACCUM := 0;
            DIR := DIRECTION#WEST;
        3:
            MODE := 30;
            DIR := DIRECTION#SOUTH;
        10:
            MODE := 0;
            FAULT := TRUE;
        ELSE
            MODE := 99;
            FAULT := TRUE;
    END_CASE

    IF RUNNING THEN
        CASE MODE OF
            10:
                ACCUM := ACCUM + POSITION;
                IF ACCUM >= 1000 THEN
                    MODE := 11;
                END_IF
            20:
                ACCUM := ACCUM - POSITION;
                IF ACCUM <= -1000 THEN
                    MODE := 21;
                END_IF
            30:
                SPEED := 0;
                RETRY := RETRY + 1;
                IF RETRY >= 3 THEN
                    MODE := 31;
                END_IF
            11, 21, 31:
                SPEED := 0;
                RUNNING := FALSE;
        END_CASE
    END_IF
END_FUNCTION_BLOCK

PROGRAM TRICKY_MAIN
    VAR
        MATRIX    : ARRAY[0..2] OF MATRIX_ROW;
        I         : INT := 0;
        J         : INT := 0;
        TOTAL     : DINT := 0;
        COUNT     : DINT := 0;
        VAL       : INT := -450;
        WRAPPED   : INT := 0;
        AVG       : REAL := 0.0;
        CLAMPED   : DINT := 0;
        CLAMPED2  : DINT := 0;
        SM        : STATE_MACHINE;
        PHASE     : INT := 0;
        FLAG      : BOOL := FALSE;
        RESULT_ARR : ARRAY[0..5] OF DINT;
        K         : INT := 0;
        ARR_VAL   : DINT := 0;
    END_VAR

    PRINT("=== TRICKY TEST START ===");

    (* 1. 嵌套 FOR + STRUCT 数组赋值 *)
    FOR I := 0 TO 2 BY 1 DO
        MATRIX[I].TAG := I * 100;
        FOR J := 0 TO 3 BY 1 DO
            MATRIX[I].VALUES[J] := (I * 10) + J;
            TOTAL := TOTAL + MATRIX[I].VALUES[J];
        END_FOR
    END_FOR

    PRINT("TOTAL = " + TOTAL);

    (* 2. WHILE 取模 (角度归约) *)
    WRAPPED := WRAP_ANGLE(ANGLE := VAL);
    PRINT("WRAPPED = " + WRAPPED);

    (* 3. 函数调用函数 (浮点) *)
    AVG := AVERAGE(A := 10.5, B := 20.5);
    PRINT("AVG = " + AVG);

    (* 4. 多返回路径 (CLAMP) *)
    CLAMPED := CLAMP(VAL := -100, MINV := 0, MAXV := 50);
    PRINT("CLAMPED = " + CLAMPED);
    CLAMPED2 := CLAMP(VAL := 999, MINV := 0, MAXV := 100);
    PRINT("CLAMPED2 = " + CLAMPED2);

    (* 5. ENUM + CASE 状态机 *)
    PHASE := 1;
    SM(CMD := PHASE, POSITION := 500);
    PHASE := 2;
    SM(CMD := PHASE, POSITION := 1200);
    PHASE := 3;
    SM(CMD := PHASE, POSITION := -800);

    (* 6. REPEAT 循环 *)
    K := 0;
    REPEAT
        RESULT_ARR[K] := DOUBLE_IT(X := K * 10);
        K := K + 1;
    UNTIL K >= 6
    END_REPEAT

    ARR_VAL := 42;
    PRINT("RESULT_ARR[3] = " + ARR_VAL);

    (* 7. 复杂布尔表达式 *)
    FLAG := (I >= 2) AND (TOTAL > 0) OR (WRAPPED <> 270);
    IF FLAG AND NOT (CLAMPED < 0) THEN
        PRINT("FLAG IS TRUE");
    END_IF

    (* 8. WHILE 模拟 BREAK *)
    COUNT := 0;
    WHILE COUNT < 10 DO
        COUNT := COUNT + 1;
        IF COUNT = 5 THEN
            COUNT := 10;
        END_IF
    END_WHILE
    PRINT("COUNT = " + COUNT);

    (* 9. ASSERT 综合验证 *)
    ASSERT(TOTAL = 66);
    ASSERT(WRAPPED = 270);
    ASSERT(CLAMPED = 50);
    ASSERT(AVG > 15.0);
    ASSERT(MATRIX[0].TAG = 0);
    ASSERT(MATRIX[2].VALUES[3] = 23);
    ASSERT(K >= 6);
    ASSERT(NOT FLAG);

    PRINT("=== TRICKY TEST PASS ===");
END_PROGRAM
