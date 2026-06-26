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

PROGRAM MAIN
    VAR
        ADD1 : ADDER;
        X : INT;
        Y : INT;
    END_VAR
    ADD1(A := X, B := Y);
    Y := ADD1.SUM;
END_PROGRAM