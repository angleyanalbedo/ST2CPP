(*
 * test_print.st — PRINT 测试
 *
 * 演示:
 *   1. PRINT 语句输出变量
 *   2. 外部函数声明
 *)

FUNCTION PRINTF : INT
    VAR_INPUT
        FMT : STRING;
    END_VAR
END_FUNCTION

PROGRAM MAIN
    VAR
        A : INT := 42;
        B : INT := 10;
        C : INT;
    END_VAR

    C := A + B;

    PRINT(A);
    PRINT(B);
    PRINT(C);

    IF C = 52 THEN
        PRINT(A);
    END_IF

END_PROGRAM
