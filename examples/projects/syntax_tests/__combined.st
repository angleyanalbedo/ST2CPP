(* test_class.st — CLASS 和方法调用测试 *)

CLASS MATH_HELPER
    VAR
        LAST_RESULT : DINT;
        CALL_COUNT  : INT;
    END_VAR

    METHOD ADD : DINT
        VAR_INPUT
            A : DINT;
            B : DINT;
        END_VAR
        LAST_RESULT := A + B;
        CALL_COUNT := CALL_COUNT + 1;
        ADD := LAST_RESULT;
    END_METHOD

    METHOD SUBTRACT : DINT
        VAR_INPUT
            A : DINT;
            B : DINT;
        END_VAR
        LAST_RESULT := A - B;
        CALL_COUNT := CALL_COUNT + 1;
        SUBTRACT := LAST_RESULT;
    END_METHOD

    METHOD GET_COUNT : INT
        GET_COUNT := CALL_COUNT;
    END_METHOD
END_CLASS

PROGRAM TEST_CLASS
    VAR
        Helper      : MATH_HELPER;
        R1          : DINT;
        R2          : DINT;
        Count       : INT;
    END_VAR

    R1 := Helper.ADD(10, 20);
    ASSERT(R1 = 30);

    R2 := Helper.SUBTRACT(50, 15);
    ASSERT(R2 = 35);

    Count := Helper.GET_COUNT();
    ASSERT(Count = 2);

    ASSERT(TRUE);
END_PROGRAM

(* test_class_minimal.st — 最小 CLASS 测试 *)

CLASS BASE_CTRL
    VAR
        BASE_STATE : INT := 0;
    END_VAR

    METHOD GET_VALUE : INT
        GET_VALUE := BASE_STATE;
    END_METHOD
END_CLASS

PROGRAM TEST_MINIMAL
    VAR
        Ctrl : BASE_CTRL;
        R    : INT := 0;
    END_VAR
    R := Ctrl.GET_VALUE();
END_PROGRAM

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

TYPE MyStruct :
STRUCT
    X : INT;
    Y : INT;
END_STRUCT;
END_TYPE

PROGRAM AT_STRUCT
  VAR_INPUT
    Raw AT %ID0 : MyStruct;
  END_VAR
  VAR_OUTPUT
    Out AT %QD0 : MyStruct;
  END_VAR
  VAR
    LocalX : INT;
  END_VAR
    LocalX := Raw.X;
    Out.Y := Raw.X + 1;
END_PROGRAM

FUNCTION_BLOCK TEST_FB
    VAR_INPUT
        X : INT;
    END_VAR
    VAR_OUTPUT
        Y : INT;
    END_VAR
    VAR
        S : INT := 0;
    END_VAR

    CASE S OF
        0:
            Y := 16#0007;
            IF X > 0 THEN
                S := 1;
            END_IF;
        1:
            Y := 16#000F;
            S := 0;
        ELSE
            Y := 0;
    END_CASE;
END_FUNCTION_BLOCK

PROGRAM IO_TEST
  VAR_INPUT
    BitInput AT %IX1.3 : BOOL;
    ByteInput AT %IB5 : USINT;
    WordInput AT %IW2 : INT;
    DWordInput AT %ID1 : DINT;
  END_VAR
  VAR_OUTPUT
    BitOutput AT %QX0.7 : BOOL;
    ByteOutput AT %QB10 : USINT;
    WordOutput AT %QW3 : INT;
    DWordOutput AT %QD2 : DINT;
  END_VAR
  VAR
    Temp : INT;
    Result : INT;
  END_VAR
    Temp := WordInput + 5;
    IF BitInput THEN
        BitOutput := TRUE;
    ELSE
        BitOutput := FALSE;
    END_IF;
    ByteOutput := ByteInput;
    WordOutput := Temp;
    DWordOutput := DWordInput * 2;
    Result := WordInput * 10;
END_PROGRAM

PROGRAM IO_READBACK
  VAR_INPUT
    Cmd AT %IX0.0 : BOOL;
  END_VAR
  VAR_OUTPUT
    OutVal AT %QW2 : INT;
    OutFlag AT %QX0.1 : BOOL;
  END_VAR
  VAR
    Readback : INT;
    FlagCopy : BOOL;
  END_VAR
    OutVal := 42;
    OutFlag := Cmd;
    Readback := OutVal;
    FlagCopy := OutFlag;
END_PROGRAM

PROGRAM AT_VAR
  VAR
    MyInput AT %IW2 : INT;
    MyOutput AT %QW1 : INT;
    MyFlag AT %QX0.5 : BOOL;
  END_VAR
    MyOutput := MyInput * 2;
    IF MyInput > 100 THEN
        MyFlag := TRUE;
    ELSE
        MyFlag := FALSE;
    END_IF;
END_PROGRAM

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

FUNCTION_BLOCK MY_FB
    VAR_INPUT X : INT; END_VAR
    VAR_OUTPUT Y : INT; END_VAR
    Y := X + 1;
END_FUNCTION_BLOCK

PROGRAM TEST_FB_ARR
    VAR
        Fbs : ARRAY[0..2] OF MY_FB;
        I : INT;
    END_VAR
    FOR I := 0 TO 2 DO
        Fbs[I](X := I);
    END_FOR;
END_PROGRAM

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

(* test_fc_return.st — 测试 Bug 1: FC 末尾多余 return;
 *
 * 问题：TranslateReturn 对非 void FC 也输出 return; 导致编译报错
 * 预期：非 void FC 应该 return <returnValue>; 而不是 return;
 *)

FUNCTION ADD : DINT
    VAR_INPUT
        A : DINT;
        B : DINT;
    END_VAR
    ADD := A + B;
END_FUNCTION

FUNCTION CLAMP : DINT
    VAR_INPUT
        VAL  : DINT;
        MINV : DINT;
        MAXV : DINT;
    END_VAR
    IF VAL < MINV THEN
        CLAMP := MINV;
    ELSIF VAL > MAXV THEN
        CLAMP := MAXV;
    ELSE
        CLAMP := VAL;
    END_IF
END_FUNCTION

FUNCTION SIMPLE : DINT
    VAR_INPUT
        X : DINT;
    END_VAR
    SIMPLE := X * 2;
    RETURN;
END_FUNCTION

PROGRAM TEST_FC_RETURN
    VAR
        R1 : DINT := 0;
        R2 : DINT := 0;
        R3 : DINT := 0;
    END_VAR
    R1 := ADD(A := 10, B := 20);
    R2 := CLAMP(VAL := -5, MINV := 0, MAXV := 100);
    R3 := SIMPLE(X := 7);

    ASSERT(R1 = 30);
    ASSERT(R2 = 0);
    ASSERT(R3 = 14);
END_PROGRAM

PROGRAM TEST_HEX
    VAR
        X : INT := 0;
        Y : WORD;
    END_VAR
    Y := 16#0007;
    X := 16#FF;
END_PROGRAM

FUNCTION_BLOCK TestFB
    VAR_INPUT LD : BOOL; END_VAR
    VAR_OUTPUT Q : BOOL; END_VAR
END_FUNCTION_BLOCK

PROGRAM main
END_PROGRAM

PROGRAM TEST_ARRAY
    VAR
        JOINT_LIM : ARRAY[0..5, 0..1] OF DINT;
    END_VAR
    JOINT_LIM[0, 0] := -170000;
    JOINT_LIM[0, 1] := 170000;
END_PROGRAM

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

PROGRAM MOTOR_CTRL
  VAR_INPUT
    StartBtn AT %IX0.0 : BOOL;
    StopBtn AT %IX0.1 : BOOL;
    SpeedSensor AT %IW0 : INT;
  END_VAR
  VAR_OUTPUT
    MotorRun AT %QX0.0 : BOOL;
    SpeedOut AT %QW0 : INT;
  END_VAR
  VAR
    TargetSpeed : INT := 1000;
    CurrentSpeed : INT;
    RunLatch : BOOL;
  END_VAR
  VAR RETAIN
    TotalRunTime : DINT;
    ErrorCount : INT;
  END_VAR

  IF StartBtn AND NOT StopBtn THEN
    RunLatch := TRUE;
  END_IF;
  IF StopBtn THEN
    RunLatch := FALSE;
  END_IF;

  MotorRun := RunLatch;

  IF MotorRun THEN
    CurrentSpeed := SpeedSensor;
    SpeedOut := TargetSpeed;
    TotalRunTime := TotalRunTime + 1;
  ELSE
    SpeedOut := 0;
  END_IF;

  IF CurrentSpeed > TargetSpeed * 2 THEN
    ErrorCount := ErrorCount + 1;
  END_IF;
END_PROGRAM

PROGRAM SIMPLE_RETAIN
  VAR
    Counter : INT := 0;
  END_VAR
  VAR RETAIN
    SavedVal : DINT;
  END_VAR

  Counter := Counter + 1;
  SavedVal := SavedVal + Counter;
END_PROGRAM

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

PROGRAM MAIN
  VAR_INPUT
    Start AT %IX0.0 : BOOL;
    Stop AT %IX0.1 : BOOL;
    SpeedRef AT %IW2 : INT;
  END_VAR
  VAR_OUTPUT
    Run AT %QX0.0 : BOOL;
    SpeedOut AT %QW4 : INT;
  END_VAR
  VAR
    Counter : INT;
    Temp AT %IW4 : INT;
    Result AT %QW6 : INT;
  END_VAR
    Counter := Counter + 1;
    IF Start AND NOT Stop THEN
        Run := TRUE;
        SpeedOut := SpeedRef;
    ELSE
        Run := FALSE;
        SpeedOut := 0;
    END_IF;
    Temp := SpeedRef + 10;
    Result := Temp * 2;
END_PROGRAM
