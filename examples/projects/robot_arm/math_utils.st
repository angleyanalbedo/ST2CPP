(* math_utils.st — 数学工具函数 *)

FUNCTION DEG_TO_RAD : REAL
    VAR_INPUT DEG : REAL; END_VAR
    DEG_TO_RAD := DEG * 3.14159265 / 180.0;
END_FUNCTION

FUNCTION RAD_TO_DEG : REAL
    VAR_INPUT RAD : REAL; END_VAR
    RAD_TO_DEG := RAD * 180.0 / 3.14159265;
END_FUNCTION

FUNCTION CLAMP_REAL : REAL
    VAR_INPUT VAL : REAL; MN : REAL; MX : REAL; END_VAR
    IF VAL > MX THEN
        CLAMP_REAL := MX;
    ELSIF VAL < MN THEN
        CLAMP_REAL := MN;
    ELSE
        CLAMP_REAL := VAL;
    END_IF;
END_FUNCTION

FUNCTION CLAMP_DINT : DINT
    VAR_INPUT VAL : DINT; MN : DINT; MX : DINT; END_VAR
    IF VAL > MX THEN
        CLAMP_DINT := MX;
    ELSIF VAL < MN THEN
        CLAMP_DINT := MN;
    ELSE
        CLAMP_DINT := VAL;
    END_IF;
END_FUNCTION

FUNCTION ABS_REAL : REAL
    VAR_INPUT X : REAL; END_VAR
    IF X < 0.0 THEN
        ABS_REAL := 0.0 - X;
    ELSE
        ABS_REAL := X;
    END_IF;
END_FUNCTION

FUNCTION ABS_DINT : DINT
    VAR_INPUT X : DINT; END_VAR
    IF X < 0 THEN
        ABS_DINT := 0 - X;
    ELSE
        ABS_DINT := X;
    END_IF;
END_FUNCTION

FUNCTION SIGN_REAL : REAL
    VAR_INPUT X : REAL; END_VAR
    IF X > 0.0 THEN
        SIGN_REAL := 1.0;
    ELSIF X < 0.0 THEN
        SIGN_REAL := -1.0;
    ELSE
        SIGN_REAL := 0.0;
    END_IF;
END_FUNCTION

(* 简单梯形速度规划：返回当前周期的目标位置 *)
FUNCTION TRAPZ_POSITION : DINT
    VAR_INPUT
        POS_START   : DINT;     (* 起始位置 *)
        POS_END     : DINT;     (* 目标位置 *)
        VEL_MAX     : DINT;     (* 最大速度 *)
        ACC_MAX     : DINT;     (* 最大加速度 *)
        TIME_MS     : DINT;     (* 已用时间 ms *)
        TOTAL_MS    : DINT;     (* 总时间 ms *)
    END_VAR
    VAR
        DIST        : DINT;
        SIGN_DIR    : DINT;
        ACCEL_TIME  : DINT;
        cruise_time : DINT;
        pos         : DINT;
    END_VAR

    DIST := ABS_DINT(POS_END - POS_START);
    IF POS_END >= POS_START THEN
        SIGN_DIR := 1;
    ELSE
        SIGN_DIR := -1;
    END_IF;

    IF VEL_MAX <= 0 OR ACC_MAX <= 0 OR TOTAL_MS <= 0 THEN
        TRAPZ_POSITION := POS_START;
        RETURN;
    END_IF;

    ACCEL_TIME := VEL_MAX * 1000 / ACC_MAX;
    IF ACCEL_TIME > TOTAL_MS / 2 THEN
        ACCEL_TIME := TOTAL_MS / 2;
    END_IF;

    cruise_time := TOTAL_MS - 2 * ACCEL_TIME;

    IF TIME_MS < ACCEL_TIME THEN
        (* 加速段 *)
        pos := POS_START + SIGN_DIR * ACC_MAX * TIME_MS * TIME_MS / 2000000;
    ELSIF TIME_MS < ACCEL_TIME + cruise_time THEN
        (* 匀速段 *)
        pos := POS_START + SIGN_DIR * (VEL_MAX * ACCEL_TIME / 1000 + VEL_MAX * (TIME_MS - ACCEL_TIME) / 1000);
    ELSIF TIME_MS < TOTAL_MS THEN
        (* 减速段 *)
        pos := POS_END - SIGN_DIR * ACC_MAX * (TOTAL_MS - TIME_MS) * (TOTAL_MS - TIME_MS) / 2000000;
    ELSE
        pos := POS_END;
    END_IF;

    TRAPZ_POSITION := pos;
END_FUNCTION

(* 两点间距离 *)
FUNCTION DISTANCE_3D : REAL
    VAR_INPUT A : POSE_3D; B : POSE_3D; END_VAR
    VAR DX : REAL; DY : REAL; DZ : REAL; END_VAR
    DX := B.X - A.X;
    DY := B.Y - A.Y;
    DZ := B.Z - A.Z;
    DISTANCE_3D := (DX * DX + DY * DY + DZ * DZ);
END_FUNCTION
