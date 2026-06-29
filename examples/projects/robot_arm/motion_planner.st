(* motion_planner.st — 关节空间运动规划器 *)

FUNCTION_BLOCK JOINT_PLANNER
    VAR_INPUT
        TARGET_J    : JOINT_WAYPOINT;   (* 目标关节角度 *)
        SPEED_PCT   : REAL;             (* 速度百分比 0-100 *)
        START       : BOOL;             (* 启动运动 *)
        STOP        : BOOL;             (* 停止运动 *)
        ABORT       : BOOL;             (* 紧急终止 *)
    END_VAR
    VAR_OUTPUT
        CMD_J       : JOINT_WAYPOINT;   (* 当前周期命令 *)
        DONE        : BOOL;             (* 运动完成 *)
        MOVING      : BOOL;             (* 运动中 *)
        PROGRESS    : REAL;             (* 进度 0-100 *)
        ERROR       : BOOL;
        ERROR_CODE  : INT;
    END_VAR
    VAR
        STATE       : INT := 0;         (* 0=Idle, 1=Planning, 2=Moving, 3=Done *)
        START_J     : JOINT_WAYPOINT;   (* 起始关节角度 *)
        TIME_MS     : DINT := 0;
        TOTAL_MS    : DINT := 0;
        SPEED_SCALE : REAL;
        I           : INT;
        DUR         : DINT;
        MAX_DUR     : DINT;
    END_VAR

    IF ABORT THEN
        STATE := 0;
        MOVING := FALSE;
        DONE := FALSE;
        ERROR := TRUE;
        ERROR_CODE := 99;
        RETURN;
    END_IF;

    IF STOP AND STATE = 2 THEN
        STATE := 0;
        MOVING := FALSE;
        DONE := FALSE;
        RETURN;
    END_IF;

    CASE STATE OF
        0:
            (* Idle *)
            DONE := FALSE;
            MOVING := FALSE;
            PROGRESS := 0.0;
            ERROR := FALSE;
            ERROR_CODE := 0;
            IF START THEN
                STATE := 1;
                TIME_MS := 0;
                MAX_DUR := 0;
                SPEED_SCALE := SPEED_PCT / 100.0;
                IF SPEED_SCALE < 0.1 THEN
                    SPEED_SCALE := 0.1;
                END_IF;
                IF SPEED_SCALE > 1.0 THEN
                    SPEED_SCALE := 1.0;
                END_IF;
            END_IF;

        1:
            (* Planning — 计算运动时间 *)
            (* J1 *)
            DUR := ABS_DINT(TARGET_J.J1 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;
            (* J2 *)
            DUR := ABS_DINT(TARGET_J.J2 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;
            (* J3 *)
            DUR := ABS_DINT(TARGET_J.J3 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;
            (* J4 *)
            DUR := ABS_DINT(TARGET_J.J4 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;
            (* J5 *)
            DUR := ABS_DINT(TARGET_J.J5 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;
            (* J6 *)
            DUR := ABS_DINT(TARGET_J.J6 * 100);
            IF DUR > MAX_DUR THEN MAX_DUR := DUR; END_IF;

            TOTAL_MS := MAX_DUR * 10 / SPEED_SCALE + 500;
            IF TOTAL_MS < 100 THEN TOTAL_MS := 100; END_IF;
            IF TOTAL_MS > 5000 THEN TOTAL_MS := 5000; END_IF;

            START_J.J1 := CMD_J.J1;
            START_J.J2 := CMD_J.J2;
            START_J.J3 := CMD_J.J3;
            START_J.J4 := CMD_J.J4;
            START_J.J5 := CMD_J.J5;
            START_J.J6 := CMD_J.J6;

            STATE := 2;
            MOVING := TRUE;

        2:
            (* Moving — 每个周期更新 *)
            TIME_MS := TIME_MS + 10;

            CMD_J.J1 := TRAPZ_POSITION(START_J.J1, TARGET_J.J1, 1000, 200, TIME_MS, TOTAL_MS);
            CMD_J.J2 := TRAPZ_POSITION(START_J.J2, TARGET_J.J2, 800, 150, TIME_MS, TOTAL_MS);
            CMD_J.J3 := TRAPZ_POSITION(START_J.J3, TARGET_J.J3, 800, 150, TIME_MS, TOTAL_MS);
            CMD_J.J4 := TRAPZ_POSITION(START_J.J4, TARGET_J.J4, 1200, 300, TIME_MS, TOTAL_MS);
            CMD_J.J5 := TRAPZ_POSITION(START_J.J5, TARGET_J.J5, 1200, 300, TIME_MS, TOTAL_MS);
            CMD_J.J6 := TRAPZ_POSITION(START_J.J6, TARGET_J.J6, 1500, 400, TIME_MS, TOTAL_MS);

            IF TOTAL_MS > 0 THEN
                PROGRESS := TIME_MS * 100.0 / TOTAL_MS;
            END_IF;
            IF PROGRESS > 100.0 THEN PROGRESS := 100.0; END_IF;

            IF TIME_MS >= TOTAL_MS THEN
                CMD_J.J1 := TARGET_J.J1;
                CMD_J.J2 := TARGET_J.J2;
                CMD_J.J3 := TARGET_J.J3;
                CMD_J.J4 := TARGET_J.J4;
                CMD_J.J5 := TARGET_J.J5;
                CMD_J.J6 := TARGET_J.J6;
                STATE := 3;
                MOVING := FALSE;
                DONE := TRUE;
                PROGRESS := 100.0;
            END_IF;

        3:
            (* Done *)
            MOVING := FALSE;
            IF NOT START THEN
                STATE := 0;
                DONE := FALSE;
            END_IF;
    END_CASE;
END_FUNCTION_BLOCK
