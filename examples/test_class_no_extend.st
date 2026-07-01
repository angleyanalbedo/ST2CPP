(* test_class_no_extend.st — CLASS 无继承测试 *)

CLASS MY_CLASS
    VAR
        DATA : INT := 0;
    END_VAR

    METHOD GET_DATA : INT
        GET_DATA := DATA;
    END_METHOD
END_CLASS

PROGRAM TEST_NO_EXTEND
    VAR
        Obj : MY_CLASS;
        R   : INT := 0;
    END_VAR
    R := Obj.GET_DATA();
END_PROGRAM
