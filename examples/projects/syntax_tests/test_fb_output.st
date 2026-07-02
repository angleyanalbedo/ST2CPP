(* test_fb_output.st — 测试 FB 输出参数 => 在 update 后读回
 *
 * 问题：FB 调用时 => 参数在 update() 后没有从 FB 实例读回
 * 预期：SM(STATUS => MyStatus) 应在 update 后将 FB 的 STATUS 字段读回 MyStatus
 *)

FUNCTION_BLOCK STATE_MACHINE
    VAR_INPUT
        CMD : INT;
    END_VAR
    VAR_OUTPUT
        STATUS : INT;
        DONE   : BOOL;
    END_VAR
    VAR
        MODE : INT := 0;
    END_VAR

    CASE CMD OF
        0:
            MODE := 0;
            STATUS := 0;
            DONE := FALSE;
        1:
            MODE := 1;
            STATUS := 10;
            DONE := TRUE;
        2:
            MODE := 2;
            STATUS := 20;
            DONE := TRUE;
    END_CASE
END_FUNCTION_BLOCK

PROGRAM TEST_FB_OUTPUT
    VAR
        SM       : STATE_MACHINE;
        MyStatus : INT := 0;
        MyDone   : BOOL := FALSE;
    END_VAR

    SM(CMD := 1, STATUS => MyStatus, DONE => MyDone);
    ASSERT(MyStatus = 10);
    ASSERT(MyDone = TRUE);

    SM(CMD := 2, STATUS => MyStatus, DONE => MyDone);
    ASSERT(MyStatus = 20);
    ASSERT(MyDone = TRUE);

    SM(CMD := 0, STATUS => MyStatus, DONE => MyDone);
    ASSERT(MyStatus = 0);
    ASSERT(MyDone = FALSE);
END_PROGRAM
