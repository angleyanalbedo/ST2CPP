FUNCTION_BLOCK ADVANCED_CTRL
    VAR_INPUT
        CMD : INT;
        VALUE : INT;
        RESET : BOOL;
    END_VAR
    VAR_OUTPUT
        STATUS : INT;
        RESULT : INT;
        BUSY : BOOL;
        DONE : BOOL;
        ERROR : BOOL;
    END_VAR
    VAR
        STATE : INT := 0;
        ACCUM : INT := 0;
        CYCLE_CNT : INT := 0;
    END_VAR

    BUSY := TRUE;
    DONE := FALSE;
    ERROR := FALSE;

    IF RESET THEN
        STATE := 0;
        ACCUM := 0;
        CYCLE_CNT := 0;
        RESULT := 0;
        STATUS := 0;
    ELSIF STATE = 0 THEN
        IF CMD > 0 THEN
            STATE := 10;
            ACCUM := 0;
        END_IF
        STATUS := 0;
    ELSIF STATE = 10 THEN
        ACCUM := ACCUM + VALUE;
        CYCLE_CNT := CYCLE_CNT + 1;
        IF CYCLE_CNT >= 5 THEN
            STATE := 20;
            RESULT := ACCUM;
        END_IF
        STATUS := 1;
    ELSIF STATE = 20 THEN
        DONE := TRUE;
        BUSY := FALSE;
        STATUS := 2;
    ELSE
        STATE := 0;
        STATUS := 99;
        ERROR := TRUE;
    END_IF
END_FUNCTION_BLOCK
