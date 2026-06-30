(* test_fb_output.st — FB 输出参数 (=>) 测试 *)

FUNCTION_BLOCK CALC_FB
    VAR_INPUT
        X       : DINT;
        Y       : DINT;
    END_VAR
    VAR_OUTPUT
        SUM     : DINT;
        DIFF    : DINT;
        PRODUCT : DINT;
    END_VAR

    SUM := X + Y;
    DIFF := X - Y;
    PRODUCT := X * Y;
END_FUNCTION_BLOCK

PROGRAM TEST_FB_OUTPUT
    VAR
        Calc        : CALC_FB;
        InA         : DINT := 10;
        InB         : DINT := 3;
        OutSum      : DINT;
        OutDiff     : DINT;
        OutProduct  : DINT;
        I           : INT;
    END_VAR

    (* 测试 1：FB 输出参数读取（通过字段访问） *)
    Calc(X := InA, Y := InB);
    OutSum := Calc.SUM;
    OutDiff := Calc.DIFF;
    OutProduct := Calc.PRODUCT;
    ASSERT(OutSum = 13);
    ASSERT(OutDiff = 7);
    ASSERT(OutProduct = 30);

    (* 测试 2：循环中的 FB 调用 *)
    FOR I := 0 TO 4 DO
        Calc(X := I * 10, Y := I * 5);
        OutSum := Calc.SUM;
        OutProduct := Calc.PRODUCT;
        ASSERT(OutSum = I * 15);
        ASSERT(OutProduct = I * 10 * I * 5);
    END_FOR

    ASSERT(TRUE);
END_PROGRAM
