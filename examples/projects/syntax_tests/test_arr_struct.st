TYPE
    MY_POINT :
    STRUCT
        X : INT;
        Y : INT;
    END_STRUCT;
END_TYPE

PROGRAM MAIN
    VAR
        P_ARR : ARRAY[0..4] OF MY_POINT;
        I : INT := 0;
        SUM_X : INT := 0;
    END_VAR

    FOR I := 0 TO 4 BY 1 DO
        P_ARR[I].X := I * 10;
        P_ARR[I].Y := I * 20;
    END_FOR

    SUM_X := P_ARR[0].X + P_ARR[1].X;
END_PROGRAM
