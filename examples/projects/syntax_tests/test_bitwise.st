(* test_bitwise.st — 测试 Bug 3: AND/OR 对非 BOOL 操作数
 *
 * 问题：VisitAnd_expr 强制要求 BOOL 类型，拒绝 INT 操作数
 * 预期：AND/OR 应支持 ANY_BIT 多态（BYTE/WORD/DWORD/INT 等）
 *       编译器 mapOperator 已将 AND→&, OR→|，只需放宽静态检查
 *)

PROGRAM TEST_BITWISE
    VAR
        A     : INT := 16#FF00;
        B     : INT := 16#0F0F;
        MASK  : INT := 16#00FF;
        AND_R : INT := 0;
        OR_R  : INT := 0;
        XOR_R : INT := 0;
    END_VAR

    AND_R := A AND MASK;
    OR_R  := A OR B;
    XOR_R := A XOR B;

    ASSERT(AND_R = 16#0000);
    ASSERT(OR_R = 16#FF0F);
    ASSERT(XOR_R = 16#F0F0);
END_PROGRAM
