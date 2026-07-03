PROGRAM MAIN
    VAR_INPUT
        Switch AT %IX0.0 : BOOL;
    END_VAR
    VAR_OUTPUT
        LED AT %QX0.0 : BOOL;
    END_VAR
    VAR
        Counter : INT := 0;
        Factor  : REAL := 2.5;
        RawBuf  : ARRAY[0..3] OF INT;
    END_VAR

    Counter := Counter + 1;
    Factor := Factor * 1.0;

    IF Switch THEN
        LED := TRUE;
    ELSE
        LED := FALSE;
    END_IF;

    RawBuf[0] := Counter;
    RawBuf[1] := Counter + 100;
    RawBuf[2] := Counter + 200;
    RawBuf[3] := Counter + 300;
END_PROGRAM
