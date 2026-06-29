(* robot_ctrl.st — 6轴机械臂主控制器 *)

FUNCTION_BLOCK ROBOT_CTRL
    VAR_INPUT
        START           : BOOL;
        STOP            : BOOL;
        HOME_CMD        : BOOL;
        AUTO_MODE       : BOOL;
        TEACH_MODE      : BOOL;
        SPEED_OVERRIDE  : REAL;          (* 速度倍率 0-100% *)
    END_VAR
    VAR_OUTPUT
        STATE_OUT       : INT;           (* 0=Disabled, 1=Idle, 2=Running, 3=Homing, 4=Error *)
        FAULT           : BOOL;
        FAULT_CODE      : INT;
        HOME_DONE       : BOOL;
        STEP_NUM        : INT;
        CYCLE_COUNT     : DINT;
    END_VAR
    VAR
        (* 6轴伺服 *)
        Srv             : ARRAY[0..5] OF SERVO_CTRL;

        (* 运动规划器 *)
        Planner         : JOINT_PLANNER;

        (* 安全监控 *)
        SafetyMon       : SAFETY_MONITOR;

        (* 关节限位 [轴号][0=下限,1=上限] *)
        JOINT_LIM       : ARRAY[0..5, 0..1] OF DINT;

        (* 伺服状态 *)
        SrvReady        : ARRAY[0..5] OF BOOL;
        SrvFaultArr     : ARRAY[0..5] OF BOOL;
        ActPosArr       : ARRAY[0..5] OF DINT;

        (* 工序步 *)
        PROG_STEP       : INT := 0;
        STEP_ACTIVE     : BOOL := FALSE;
        ALL_DONE        : BOOL := FALSE;

        (* 工序定义：抓取-放置 *)
        GRAB_POS        : JOINT_WAYPOINT := (J1 := 0.0, J2 := -45.0, J3 := 90.0, J4 := 0.0, J5 := 45.0, J6 := 0.0, SPEED := 50.0, DWELL_MS := 500);
        PRE_GRAB_POS    : JOINT_WAYPOINT := (J1 := 0.0, J2 := -20.0, J3 := 45.0, J4 := 0.0, J5 := 20.0, J6 := 0.0, SPEED := 30.0, DWELL_MS := 200);
        HOME_POS        : JOINT_WAYPOINT := (J1 := 0.0, J2 := 0.0, J3 := 0.0, J4 := 0.0, J5 := 0.0, J6 := 0.0, SPEED := 20.0, DWELL_MS := 0);
        PLACE_POS       : JOINT_WAYPOINT := (J1 := 90.0, J2 := -30.0, J3 := 60.0, J4 := 0.0, J5 := 30.0, J6 := 90.0, SPEED := 40.0, DWELL_MS := 300);
        RETRACT_POS     : JOINT_WAYPOINT := (J1 := 90.0, J2 := 0.0, J3 := 0.0, J4 := 0.0, J5 := 0.0, J6 := 0.0, SPEED := 50.0, DWELL_MS := 200);

        (* 辅助 *)
        I               : INT;
        SRV_EN          : ARRAY[0..5] OF BOOL;
        START_PREV      : BOOL := FALSE;
        TRIG_START      : BOOL;
        HOME_PREV       : BOOL := FALSE;
        TRIG_HOME       : BOOL;
        DWELL_TIMER     : INT := 0;
        DWELL_TARGET    : INT := 0;
    END_VAR

    TRIG_START := START AND NOT START_PREV;
    START_PREV := START;
    TRIG_HOME := HOME_CMD AND NOT HOME_PREV;
    HOME_PREV := HOME_CMD;

    (* ─── 关节限位初始化 ─── *)
    JOINT_LIM[0, 0] := -170000;
    JOINT_LIM[0, 1] := 170000;
    JOINT_LIM[1, 0] := -120000;
    JOINT_LIM[1, 1] := 120000;
    JOINT_LIM[2, 0] := -150000;
    JOINT_LIM[2, 1] := 150000;
    JOINT_LIM[3, 0] := -180000;
    JOINT_LIM[3, 1] := 180000;
    JOINT_LIM[4, 0] := -120000;
    JOINT_LIM[4, 1] := 120000;
    JOINT_LIM[5, 0] := -360000;
    JOINT_LIM[5, 1] := 360000;

    (* ─── 速度倍率钳位 ─── *)
    SPEED_OVERRIDE := CLAMP_REAL(SPEED_OVERRIDE, 5.0, 100.0);

    (* ─── 使能所有伺服 ─── *)
    FOR I := 0 TO 5 DO
        SRV_EN[I] := START AND NOT FAULT;
    END_FOR;

    (* ─── 执行伺服控制 ─── *)
    FOR I := 0 TO 5 DO
        Srv[I](
            AXIS_ID := I,
            CMD_POSITION := Planner.CMD_J.J1,
            CMD_MODE := 0,
            ENABLE := SRV_EN[I],
            HOME_CMD := TRIG_HOME,
            RESET_CMD := FALSE,
            VEL_LIMIT := 2000,
            TORQ_LIMIT := 80
        );
        SrvReady[I] := Srv[I].READY;
        SrvFaultArr[I] := Srv[I].FAULT;
        ActPosArr[i] := Srv[I].ACT_POSITION;
    END_FOR;

    (* ─── 安全监控 ─── *)
    SafetyMon(
        E_STOP_IN := EStop,
        SERVO_READY := SrvReady,
        SERVO_FAULT := SrvFaultArr,
        ACT_POSITIONS := ActPosArr,
        JOINT_LIMITS := JOINT_LIM,
        CFG := Safety
    );

    FAULT := NOT SafetyMon.SAFE_OK;
    FAULT_CODE := SafetyMon.ALARM_OUT.ALARM_CODE;

    (* ─── 运动规划器 ─── *)
    Planner(
        SPEED_PCT := SPEED_OVERRIDE,
        STOP := STOP,
        ABORT := FAULT
    );

    (* ─── 主状态机 ─── *)
    CASE STATE_OUT OF
        0:
            (* Disabled *)
            HOME_DONE := FALSE;
            PROG_STEP := 0;
            STEP_ACTIVE := FALSE;
            ALL_DONE := FALSE;
            IF START AND SafetyMon.SAFE_OK THEN
                STATE_OUT := 1;
            END_IF;

        1:
            (* Idle — 等待启动 *)
            IF NOT START THEN
                STATE_OUT := 0;
            END_IF;

            IF TRIG_HOME AND NOT HOME_DONE THEN
                STATE_OUT := 3;
            END_IF;

            IF AUTO_MODE AND HOME_DONE AND TRIG_START THEN
                STATE_OUT := 2;
                PROG_STEP := 0;
                STEP_ACTIVE := FALSE;
                ALL_DONE := FALSE;
                CYCLE_COUNT := 0;
            END_IF;

        2:
            (* Running — 执行工序 *)
            IF STOP OR FAULT THEN
                STATE_OUT := 1;
                Planner.STOP := TRUE;
            END_IF;

            IF NOT STEP_ACTIVE THEN
                CASE PROG_STEP OF
                    0:
                        (* 安全位 *)
                        Planner.TARGET_J := PRE_GRAB_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := PRE_GRAB_POS.DWELL_MS;

                    1:
                        (* 抓取位 *)
                        Planner.TARGET_J := GRAB_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := GRAB_POS.DWELL_MS;

                    2:
                        (* 抓取后抬升 *)
                        Planner.TARGET_J := PRE_GRAB_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := 200;

                    3:
                        (* 放置位 *)
                        Planner.TARGET_J := PLACE_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := PLACE_POS.DWELL_MS;

                    4:
                        (* 回缩 *)
                        Planner.TARGET_J := RETRACT_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := RETRACT_POS.DWELL_MS;

                    5:
                        (* 回零 *)
                        Planner.TARGET_J := HOME_POS;
                        Planner.START := TRUE;
                        STEP_ACTIVE := TRUE;
                        DWELL_TIMER := 0;
                        DWELL_TARGET := HOME_POS.DWELL_MS;

                    ELSE
                        ALL_DONE := TRUE;
                        CYCLE_COUNT := CYCLE_COUNT + 1;
                        PROG_STEP := 0;
                        STEP_ACTIVE := FALSE;
                END_CASE;
            ELSE
                Planner.START := FALSE;
                IF Planner.DONE THEN
                    DWELL_TIMER := DWELL_TIMER + 10;
                    IF DWELL_TIMER >= DWELL_TARGET THEN
                        PROG_STEP := PROG_STEP + 1;
                        STEP_ACTIVE := FALSE;
                    END_IF;
                END_IF;
            END_IF;

            STEP_NUM := PROG_STEP;

        3:
            (* Homing *)
            Planner.TARGET_J := HOME_POS;
            Planner.SPEED_PCT := 20.0;
            Planner.START := TRUE;

            IF Planner.DONE THEN
                HOME_DONE := TRUE;
                STATE_OUT := 1;
                Planner.START := FALSE;
            END_IF;

        4:
            (* Error *)
            IF NOT FAULT AND START THEN
                STATE_OUT := 0;
            END_IF;
    END_CASE;

    (* ─── 故障跳转 ─── *)
    IF FAULT AND STATE_OUT <> 4 THEN
        STATE_OUT := 4;
        Planner.ABORT := TRUE;
    END_IF;
END_FUNCTION_BLOCK
