(* test_class_minimal.st — 最小 CLASS 测试 *)

CLASS BASE_CTRL
    VAR
        BASE_STATE : INT := 0;
    END_VAR

    METHOD GET_VALUE : INT
        GET_VALUE := BASE_STATE;
    END_METHOD
END_CLASS

PROGRAM TEST_MINIMAL
    VAR
        Ctrl : BASE_CTRL;
        R    : INT := 0;
    END_VAR
    R := Ctrl.GET_VALUE();
END_PROGRAM
