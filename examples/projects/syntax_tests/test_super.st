(* test_super.st — SUPER:: 调用测试 *)

CLASS BASE_CALC
    VAR
        BASE_VALUE : DINT;
    END_VAR

    METHOD INIT_BASE
        VAR_INPUT
            VAL : DINT;
        END_VAR
        BASE_VALUE := VAL;
    END_METHOD

    METHOD GET_VALUE : DINT
        GET_VALUE := BASE_VALUE;
    END_METHOD
END_CLASS

CLASS EXTEND_CALC EXTENDS BASE_CALC
    VAR
        EXT_VALUE : DINT;
    END_VAR

    METHOD INIT_EXT
        VAR_INPUT
            BASE_VAL : DINT;
            EXT_VAL  : DINT;
        END_VAR
        SUPER.INIT_BASE();
        EXT_VALUE := EXT_VAL;
    END_METHOD

    METHOD GET_TOTAL : DINT
        GET_TOTAL := SUPER.GET_VALUE() + EXT_VALUE;
    END_METHOD
END_CLASS

PROGRAM TEST_SUPER
    VAR
        Calc    : EXTEND_CALC;
        R1      : DINT;
    END_VAR

    Calc.INIT_EXT();
    R1 := Calc.GET_TOTAL();
    ASSERT(TRUE);
END_PROGRAM
