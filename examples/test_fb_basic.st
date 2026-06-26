FUNCTION_BLOCK ADDER
    VAR_INPUT
        A : INT;
        B : INT;
    END_VAR
    VAR_OUTPUT
        SUM : INT;
    END_VAR
    SUM := A + B;
END_FUNCTION_BLOCK

FUNCTION_BLOCK LATCH
    VAR_INPUT
        SET : BOOL;
        RESET : BOOL;
    END_VAR
    VAR_OUTPUT
        Q : BOOL;
    END_VAR
    IF SET THEN
        Q := TRUE;
    ELSIF RESET THEN
        Q := FALSE;
    END_IF;
END_FUNCTION_BLOCK

PROGRAM BASIC_TEST
    VAR
        ADD1 : ADDER;
        LATCH1 : LATCH;
        X : INT;
        Y : INT;
        RESULT : INT;
        SET_BTN : BOOL;
        RST_BTN : BOOL;
        OUTPUT : BOOL;
    END_VAR
    ADD1(A := X, B := Y);
    RESULT := ADD1.SUM;
    LATCH1(SET := SET_BTN, RESET := RST_BTN);
    OUTPUT := LATCH1.Q;
END_PROGRAM