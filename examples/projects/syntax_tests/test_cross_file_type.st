(* test_cross_file_type.st — 测试 Bug 2: 跨文件类型解析
 *
 * 问题：多文件编译时 deepFindSymbol 只查 scope chain，不查 PLCTotalSymbolTable
 * 预期：file2.st 能找到 file1.st 中定义的 MY_STRUCT 和 MY_FB
 *)

(* === file1.st 内容 === *)
TYPE
    MY_STRUCT :
    STRUCT
        X : INT;
        Y : DINT;
    END_STRUCT;
END_TYPE

FUNCTION_BLOCK MY_FB
    VAR_INPUT
        CMD : INT;
    END_VAR
    VAR_OUTPUT
        STATE : INT;
    END_VAR
    VAR
        DATA : MY_STRUCT;
    END_VAR

    STATE := CMD;
    DATA.X := CMD;
    DATA.Y := CMD * 10;
END_FUNCTION_BLOCK

(* === file2.st 内容（引用 file1.st 的类型）=== *)
PROGRAM TEST_CROSS_FILE
    VAR
        FB1    : MY_FB;
        RESULT : INT := 0;
        S      : MY_STRUCT;
    END_VAR

    FB1(CMD := 42);
    RESULT := FB1.STATE;
    S.X := 99;

    ASSERT(RESULT = 42);
    ASSERT(S.X = 99);
END_PROGRAM
