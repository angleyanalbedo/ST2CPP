TYPE
    MY_POINT :
    STRUCT
        X : INT;
        Y : INT;
    END_STRUCT;
END_TYPE

PROGRAM MAIN
    VAR
        P : MY_POINT;
        SUM : INT := 0;
    END_VAR

    P.X := 10;
    P.Y := 20;
    SUM := P.X + P.Y;
END_PROGRAM
