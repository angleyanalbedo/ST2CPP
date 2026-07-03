FUNCTION ADD_POINTS : MyResult
    VAR_INPUT
        A : MyPoint;
        B : MyPoint;
    END_VAR
    ADD_POINTS.SUM := A.X + A.Y + B.X + B.Y;
    ADD_POINTS.AVG := ADD_POINTS.SUM / 2;
END_FUNCTION
