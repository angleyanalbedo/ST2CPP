(* ST2C++ 测试用例 *)

FUNCTION ADD_TEN : INT
    VAR_INPUT
        X : INT;
    END_VAR
    ADD_TEN := X + 10;
END_FUNCTION

PROGRAM P
    VAR
        A : INT := 0;
        B : INT;
    END_VAR

    FOR A := 1 TO 5 BY 1 DO
        B := ADD_TEN(X := A);
    END_FOR
END_PROGRAM
