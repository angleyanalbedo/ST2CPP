(* smart_factory/utils.st — 工具函数 *)

FUNCTION CLAMP_INT : DINT
    VAR_INPUT
        VAL  : DINT;
        MINV : DINT;
        MAXV : DINT;
    END_VAR
    IF VAL < MINV THEN
        CLAMP_INT := MINV;
    ELSIF VAL > MAXV THEN
        CLAMP_INT := MAXV;
    ELSE
        CLAMP_INT := VAL;
    END_IF
END_FUNCTION

FUNCTION SCALE_REAL : REAL
    VAR_INPUT
        INPUT_VAL : REAL;
        IN_MIN    : REAL;
        IN_MAX    : REAL;
        OUT_MIN   : REAL;
        OUT_MAX   : REAL;
    END_VAR
    VAR
        RATIO : REAL;
    END_VAR
    IF IN_MAX = IN_MIN THEN
        SCALE_REAL := OUT_MIN;
    ELSE
        RATIO := (INPUT_VAL - IN_MIN) / (IN_MAX - IN_MIN);
        SCALE_REAL := OUT_MIN + RATIO * (OUT_MAX - OUT_MIN);
    END_IF
END_FUNCTION

FUNCTION BIT_CHECK : BOOL
    VAR_INPUT
        VALUE : INT;
        BIT_POS : INT;
    END_VAR
    VAR
        MASK : INT;
    END_VAR
    MASK := 1;
    IF BIT_POS > 0 THEN
        MASK := SHL(MASK, BIT_POS);
    END_IF
    IF (VALUE AND MASK) <> 0 THEN
        BIT_CHECK := TRUE;
    ELSE
        BIT_CHECK := FALSE;
    END_IF
END_FUNCTION

FUNCTION WRAP_ANGLE : INT
    VAR_INPUT
        ANGLE : INT;
    END_VAR
    WHILE ANGLE < 0 DO
        ANGLE := ANGLE + 360;
    END_WHILE
    WHILE ANGLE >= 360 DO
        ANGLE := ANGLE - 360;
    END_WHILE
    WRAP_ANGLE := ANGLE;
END_FUNCTION
