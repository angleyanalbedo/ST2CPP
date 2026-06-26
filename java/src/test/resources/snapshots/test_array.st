(* ST2C++ ARRAY 测试用例 *)

PROGRAM P
    VAR
        ARR : ARRAY[0..4] OF INT;
        I   : INT;
        SUM : INT;
    END_VAR

    SUM := 0;
    FOR I := 0 TO 4 DO
        ARR[I] := I * 10;
        SUM := SUM + ARR[I];
    END_FOR;
END_PROGRAM
