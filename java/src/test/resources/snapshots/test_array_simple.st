PROGRAM P
    VAR
        ARR : ARRAY[0..4] OF INT;
        I   : INT;
        SUM : INT;
    END_VAR

    SUM := 0;
    I := 0;
    ARR[I] := I * 10;
    SUM := SUM + ARR[I];
END_PROGRAM
