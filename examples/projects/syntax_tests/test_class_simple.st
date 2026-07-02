(* test_class_simple.st — 简单 CLASS 测试 *)

CLASS MY_CALC
    VAR
        BASE_STATE : INT := 0;
    END_VAR

    METHOD GET_VALUE : INT
        GET_VALUE := INTERNAL;
    END_METHOD
END_CLASS

PROGRAM TEST_SIMPLE
    VAR
        Calc : MY_CALC;
        R    : DINT := 0;
    END_VAR
    R := Calc.GET_VALUE();
END_PROGRAM
