FUNCTION_BLOCK MY_COUNTER
    VAR_INPUT
        CLK : BOOL;
        RESET : BOOL;
    END_VAR
    VAR_OUTPUT
        Q : BOOL;
        CV : INT;
    END_VAR
    VAR
        PREV_CLK : BOOL := FALSE;
        COUNT : INT := 0;
    END_VAR

    Q := FALSE;
    IF RESET THEN
        COUNT := 0;
    ELSIF CLK AND NOT PREV_CLK THEN
        COUNT := COUNT + 1;
    END_IF

    PREV_CLK := CLK;
    CV := COUNT;

    IF COUNT >= 5 THEN
        Q := TRUE;
    END_IF
END_FUNCTION_BLOCK
