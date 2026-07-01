(* weather_station/utils.st — 工具函数 *)

(* 限幅 *)
FUNCTION CLAMP_REAL : REAL
    VAR_INPUT
        VAL : REAL;
        MN  : REAL;
        MX  : REAL;
    END_VAR
    IF VAL < MN THEN
        CLAMP_REAL := MN;
    ELSIF VAL > MX THEN
        CLAMP_REAL := MX;
    ELSE
        CLAMP_REAL := VAL;
    END_IF
END_FUNCTION

(* 整数限幅 *)
FUNCTION CLAMP_INT : DINT
    VAR_INPUT
        VAL : DINT;
        MN  : DINT;
        MX  : DINT;
    END_VAR
    IF VAL < MN THEN
        CLAMP_INT := MN;
    ELSIF VAL > MX THEN
        CLAMP_INT := MX;
    ELSE
        CLAMP_INT := VAL;
    END_IF
END_FUNCTION

(* 线性插值 *)
FUNCTION LERP : REAL
    VAR_INPUT
        A    : REAL;
        B    : REAL;
        T    : REAL;
    END_VAR
    LERP := A + (B - A) * T;
END_FUNCTION

(* 角度转弧度 *)
FUNCTION DEG_TO_RAD : REAL
    VAR_INPUT
        DEG : REAL;
    END_VAR
    DEG_TO_RAD := DEG * 3.14159265 / 180.0;
END_FUNCTION

(* 风向转角度（16方位 → 0-359度）*)
FUNCTION WIND_DIR_TO_DEG : INT
    VAR_INPUT
        DIR_CODE : INT;
    END_VAR
    VAR
        ANGLE : INT;
    END_VAR
    ANGLE := DIR_CODE * 22;
    IF ANGLE >= 360 THEN
        ANGLE := ANGLE - 360;
    END_IF
    WIND_DIR_TO_DEG := ANGLE;
END_FUNCTION

(* 位掩码操作 *)
FUNCTION SET_BIT : INT
    VAR_INPUT
        VALUE : INT;
        BIT   : INT;
    END_VAR
    VAR
        MASK : INT;
    END_VAR
    MASK := 1;
    IF BIT > 0 THEN
        MASK := SHL(MASK, BIT);
    END_IF
    SET_BIT := VALUE OR MASK;
END_FUNCTION

FUNCTION CLR_BIT : INT
    VAR_INPUT
        VALUE : INT;
        BIT   : INT;
    END_VAR
    VAR
        MASK : INT;
        INV  : INT;
    END_VAR
    MASK := 1;
    IF BIT > 0 THEN
        MASK := SHL(MASK, BIT);
    END_IF
    INV := VALUE XOR MASK;
    IF (VALUE AND MASK) <> 0 THEN
        CLR_BIT := INV;
    ELSE
        CLR_BIT := VALUE;
    END_IF
END_FUNCTION

FUNCTION TST_BIT : BOOL
    VAR_INPUT
        VALUE : INT;
        BIT   : INT;
    END_VAR
    VAR
        MASK : INT;
    END_VAR
    MASK := 1;
    IF BIT > 0 THEN
        MASK := SHL(MASK, BIT);
    END_IF
    IF (VALUE AND MASK) <> 0 THEN
        TST_BIT := TRUE;
    ELSE
        TST_BIT := FALSE;
    END_IF
END_FUNCTION

(* 移动平均（简单3点）*)
FUNCTION MOVING_AVG : REAL
    VAR_INPUT
        NEW_VAL : REAL;
        OLD_A   : REAL;
        OLD_B   : REAL;
    END_VAR
    MOVING_AVG := (NEW_VAL + OLD_A + OLD_B) / 3.0;
END_FUNCTION
