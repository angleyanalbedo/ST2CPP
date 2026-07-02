(* test_class.st — CLASS 和方法调用测试 *)

CLASS MATH_HELPER
    VAR
        LAST_RESULT : DINT;
        CALL_COUNT  : INT;
    END_VAR

    METHOD ADD : DINT
        VAR_INPUT
            A : DINT;
            B : DINT;
        END_VAR
        LAST_RESULT := A + B;
        CALL_COUNT := CALL_COUNT + 1;
        ADD := LAST_RESULT;
    END_METHOD

    METHOD SUBTRACT : DINT
        VAR_INPUT
            A : DINT;
            B : DINT;
        END_VAR
        LAST_RESULT := A - B;
        CALL_COUNT := CALL_COUNT + 1;
        SUBTRACT := LAST_RESULT;
    END_METHOD

    METHOD GET_COUNT : INT
        GET_COUNT := CALL_COUNT;
    END_METHOD
END_CLASS

PROGRAM TEST_CLASS
    VAR
        Helper      : MATH_HELPER;
        R1          : DINT;
        R2          : DINT;
        Count       : INT;
    END_VAR

    R1 := Helper.ADD(10, 20);
    ASSERT(R1 = 30);

    R2 := Helper.SUBTRACT(50, 15);
    ASSERT(R2 = 35);

    Count := Helper.GET_COUNT();
    ASSERT(Count = 2);

    ASSERT(TRUE);
END_PROGRAM
