(* test_fc_return.st — 测试 Bug 1: FC 末尾多余 return;
 *
 * 问题：TranslateReturn 对非 void FC 也输出 return; 导致编译报错
 * 预期：非 void FC 应该 return <returnValue>; 而不是 return;
 *)

FUNCTION ADD : DINT
    VAR_INPUT
        A : DINT;
        B : DINT;
    END_VAR
    ADD := A + B;
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

FUNCTION SIMPLE : DINT
    VAR_INPUT
        X : DINT;
    END_VAR
    SIMPLE := X * 2;
    RETURN;
END_FUNCTION

PROGRAM TEST_FC_RETURN
    VAR
        R1 : DINT := 0;
        R2 : DINT := 0;
        R3 : DINT := 0;
    END_VAR
    R1 := ADD(A := 10, B := 20);
    R2 := CLAMP(VAL := -5, MINV := 0, MAXV := 100);
    R3 := SIMPLE(X := 7);

    ASSERT(R1 = 30);
    ASSERT(R2 = 0);
    ASSERT(R3 = 14);
END_PROGRAM
