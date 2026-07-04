PROGRAM MAIN
    VAR
        P1 : MyPoint;
        P2 : MyPoint;
        Total : DINT;
    END_VAR
    P1.X := 10;
    P1.Y := 20;
    P2.X := P1.X + P1.Y;
    P2.Y := P2.X * 2;
    Total := SUM_POINTS(X := P1.X, Y := P1.Y);
END_PROGRAM
