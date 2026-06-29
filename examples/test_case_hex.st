FUNCTION_BLOCK TEST_FB
    VAR_INPUT
        X : INT;
    END_VAR
    VAR_OUTPUT
        Y : INT;
    END_VAR
    VAR
        S : INT := 0;
    END_VAR

    CASE S OF
        0:
            Y := 16#0007;
            IF X > 0 THEN
                S := 1;
            END_IF;
        1:
            Y := 16#000F;
            S := 0;
        ELSE
            Y := 0;
    END_CASE;
END_FUNCTION_BLOCK
