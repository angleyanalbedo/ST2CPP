FUNCTION_BLOCK SIMPLE_ADDER
    VAR_INPUT
        X : INT;
        Y : INT;
    END_VAR
    VAR_OUTPUT
        RESULT : INT;
    END_VAR
    RESULT := X + Y;
END_FUNCTION_BLOCK

FUNCTION_BLOCK BIT_FLAGS
    VAR_INPUT
        BIT0 : BOOL;
        BIT1 : BOOL;
        BIT2 : BOOL;
        BIT3 : BOOL;
    END_VAR
    VAR_OUTPUT
        FLAGS : INT;
    END_VAR
    FLAGS := 0;
    IF BIT0 THEN
        FLAGS := FLAGS + 1;
    END_IF;
    IF BIT1 THEN
        FLAGS := FLAGS + 2;
    END_IF;
    IF BIT2 THEN
        FLAGS := FLAGS + 4;
    END_IF;
    IF BIT3 THEN
        FLAGS := FLAGS + 8;
    END_IF;
END_FUNCTION_BLOCK

PROGRAM CALL_METHODS_TEST
    VAR
        ADD1 : SIMPLE_ADDER;
        FLAGS1 : BIT_FLAGS;
        A : INT;
        B : INT;
        SUM : INT;
        F0 : BOOL;
        F1 : BOOL;
        F2 : BOOL;
        F3 : BOOL;
        FLAG_VAL : INT;
    END_VAR
    ADD1(X := A, Y := B);
    SUM := ADD1.RESULT;
    FLAGS1(BIT0 := F0, BIT1 := F1, BIT2 := F2, BIT3 := F3);
    FLAG_VAL := FLAGS1.FLAGS;
END_PROGRAM