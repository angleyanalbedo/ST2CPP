PROGRAM MAIN
    VAR
        P1 : MyPoint;
        P2 : MyPoint;
        R  : MyResult;
        Total : DINT;
    END_VAR
    P1.X := 10;
    P1.Y := 20;
    P2.X := 30;
    P2.Y := 40;
    R := ADD_POINTS(A := P1, B := P2);
    Total := R.SUM + R.AVG;
END_PROGRAM
