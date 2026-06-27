(* pid_func.st — PID 控制函数 *)

FUNCTION SCALE_AI : REAL
    VAR_INPUT
        RAW     : INT;
        SCALE   : SCALE_RANGE;
    END_VAR
    VAR
        NORM    : REAL;
    END_VAR

    IF RAW > SCALE.RAW_MAX THEN
        NORM := 1.0;
    ELSIF RAW < SCALE.RAW_MIN THEN
        NORM := 0.0;
    ELSE
        NORM := INT_TO_REAL(RAW - SCALE.RAW_MIN) / INT_TO_REAL(SCALE.RAW_MAX - SCALE.RAW_MIN);
    END_IF;

    SCALE_AI := SCALE.ENG_MIN + NORM * (SCALE.ENG_MAX - SCALE.ENG_MIN);
END_FUNCTION

FUNCTION SCALE_AO : INT
    VAR_INPUT
        ENG     : REAL;
        SCALE   : SCALE_RANGE;
    END_VAR
    VAR
        NORM    : REAL;
        RESULT  : INT;
    END_VAR

    NORM := (ENG - SCALE.ENG_MIN) / (SCALE.ENG_MAX - SCALE.ENG_MIN);

    IF NORM > 1.0 THEN
        NORM := 1.0;
    ELSIF NORM < 0.0 THEN
        NORM := 0.0;
    END_IF;

    RESULT := SCALE.RAW_MIN + REAL_TO_INT(NORM * INT_TO_REAL(SCALE.RAW_MAX - SCALE.RAW_MIN));
    SCALE_AO := RESULT;
END_FUNCTION

FUNCTION PID_CALC : REAL
    VAR_INPUT
        SP      : REAL;
        PV      : REAL;
        P       : PID_PARAMS;
        DT      : REAL;
    END_VAR
    VAR_IN_OUT
        INTEGRAL  : REAL;
        PREV_ERROR : REAL;
    END_VAR
    VAR
        ERR     : REAL;
        P_TERM  : REAL;
        I_TERM  : REAL;
        D_TERM  : REAL;
        OUTPUT  : REAL;
    END_VAR

    ERR := SP - PV;

    P_TERM := P.KP * ERR;

    INTEGRAL := INTEGRAL + ERR * DT;
    I_TERM := P.KI * INTEGRAL;

    D_TERM := P.KD * (ERR - PREV_ERROR) / DT;
    PREV_ERROR := ERR;

    OUTPUT := P_TERM + I_TERM + D_TERM;

    IF OUTPUT > P.MAX THEN
        OUTPUT := P.MAX;
    END_IF;
    IF OUTPUT < P.MIN THEN
        OUTPUT := P.MIN;
    END_IF;

    PID_CALC := OUTPUT;
END_FUNCTION

FUNCTION TEMP_TO_MODE : INT
    VAR_INPUT
        TEMP    : REAL;
        SP      : REAL;
        HYST    : REAL;
    END_VAR

    IF TEMP < SP - HYST THEN
        TEMP_TO_MODE := 1;
    ELSIF TEMP > SP + HYST THEN
        TEMP_TO_MODE := 2;
    ELSE
        TEMP_TO_MODE := 0;
    END_IF;
END_FUNCTION

FUNCTION LIMIT_REAL : REAL
    VAR_INPUT
        X  : REAL;
        MN : REAL;
        MX : REAL;
    END_VAR

    IF X > MX THEN
        LIMIT_REAL := MX;
    ELSIF X < MN THEN
        LIMIT_REAL := MN;
    ELSE
        LIMIT_REAL := X;
    END_IF;
END_FUNCTION
