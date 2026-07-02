(* test_method_output.st — 测试 Bug 5: CLASS 方法输出参数 =>
 *
 * 问题：调用端把 => 参数原样 dump 成无效 C++（STATE_OUT => myVar）
 * 预期：=> 参数应翻译为 C++ 引用传递，调用后写回变量
 * 注意：INTERNAL 是保留字，不能用作变量名
 *)

CLASS MY_CALC
    VAR
        ACCUM : DINT := 0;
    END_VAR

    METHOD ADD_AND_REPORT : DINT
        VAR_INPUT
            VALUE : DINT;
        END_VAR
        VAR_OUTPUT
            REPORT : DINT;
        END_VAR
        ACCUM := ACCUM + VALUE;
        REPORT := ACCUM;
        ADD_AND_REPORT := ACCUM;
    END_METHOD
END_CLASS

PROGRAM TEST_METHOD_OUTPUT
    VAR
        Calc     : MY_CALC;
        MyReport : DINT := 0;
        Result   : DINT := 0;
    END_VAR

    Result := Calc.ADD_AND_REPORT(VALUE := 10, REPORT => MyReport);
    ASSERT(Result = 10);
    ASSERT(MyReport = 10);

    Result := Calc.ADD_AND_REPORT(VALUE := 20, REPORT => MyReport);
    ASSERT(Result = 30);
    ASSERT(MyReport = 30);
END_PROGRAM
