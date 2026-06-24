(* ST2C++ CLASS 测试用例 *)

CLASS Counter
    VAR
        count : INT;
    END_VAR

    METHOD Increment : INT
        VAR_INPUT
            step : INT;
        END_VAR
        count := count + step;
        Increment := count;
    END_METHOD

    METHOD GetCount : INT
        GetCount := count;
    END_METHOD
END_CLASS

PROGRAM P
    VAR
        c   : Counter;
        val : INT;
    END_VAR

    val := c.Increment(5);
    val := c.Increment(3);
END_PROGRAM
