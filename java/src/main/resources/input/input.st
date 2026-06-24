FUNCTION FUN : INT
        VAR_INPUT
            X : INT;
        END_VAR
        IF X = 2 THEN
            FUN := 1;
        ELSIF X = 1 THEN
            FUN := 1;
        ELSE
            FUN := FUN(X := X-1) + FUN(X := X-2);
        END_IF
END_FUNCTION

PROGRAM P
    VAR
        A : INT := 10;
        B : INT;
    END_VAR
    FOR B := 10 TO 100 BY 2 DO
        A := A + 20;
    END_FOR
    B := FUN(X := 10);
END_PROGRAM