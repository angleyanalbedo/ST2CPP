(* test_not_int.st — AND/OR 位运算测试 *)
(* 注意：当前静态分析器要求 AND/OR 操作数必须为 BOOL *)
(* 此测试验证 BOOL 类型的 AND/OR 生成正确的 & / | *)

PROGRAM TEST_NOT_INT
    VAR
        A       : BOOL;
        B       : BOOL;
        C       : BOOL;
        X       : BOOL := TRUE;
        Y       : BOOL := FALSE;
    END_VAR

    A := TRUE;
    B := FALSE;

    (* AND — 验证生成 & *)
    C := A AND B;
    ASSERT(C = FALSE);

    C := X AND Y;
    ASSERT(C = FALSE);

    C := X AND X;
    ASSERT(C = TRUE);

    (* OR — 验证生成 | *)
    C := A OR B;
    ASSERT(C = TRUE);

    C := Y OR Y;
    ASSERT(C = FALSE);

    C := X OR Y;
    ASSERT(C = TRUE);

    (* NOT — 验证生成 ! *)
    C := NOT X;
    ASSERT(C = FALSE);

    C := NOT Y;
    ASSERT(C = TRUE);

    (* 复合表达式 *)
    C := (X AND Y) OR (X OR Y);
    ASSERT(C = TRUE);

    C := NOT (X AND Y);
    ASSERT(C = TRUE);

    ASSERT(TRUE);
END_PROGRAM
