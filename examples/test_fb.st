FUNCTION_BLOCK COUNTER
    VAR_INPUT
        IN : BOOL;
    END_VAR
    VAR_OUTPUT
        OUT : INT;
    END_VAR
    VAR
        CNT : INT;
    END_VAR
    IF IN THEN
        CNT := CNT + 1;
    END_IF;
    OUT := CNT;
END_FUNCTION_BLOCK

PROGRAM MAIN
    VAR
        CT : COUNTER;
        X : BOOL;
        Y : INT;
    END_VAR
    CT.IN := X;
    CT();
    Y := CT.OUT;
END_PROGRAM