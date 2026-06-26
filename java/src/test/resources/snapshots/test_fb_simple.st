FUNCTION_BLOCK MY_TIMER
    VAR_INPUT
        START : BOOL;
        PT : INT;
    END_VAR
    VAR_OUTPUT
        Q : BOOL;
        ET : INT;
    END_VAR
    VAR
        RUNNING : BOOL := FALSE;
        ELAPSED : INT := 0;
    END_VAR

    IF START AND NOT RUNNING THEN
        RUNNING := TRUE;
        ELAPSED := 0;
    END_IF

    IF RUNNING THEN
        ELAPSED := ELAPSED + 1;
        IF ELAPSED >= PT THEN
            Q := TRUE;
        END_IF
    END_IF

    ET := ELAPSED;
END_FUNCTION_BLOCK
