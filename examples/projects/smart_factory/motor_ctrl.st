(* smart_factory/motor_ctrl.st — 电机控制 FB *)

FUNCTION_BLOCK MOTOR_CTRL
    VAR_INPUT
        ENABLE      : BOOL;
        TARGET_SPEED : INT;
        CMD_DIR     : INT;       (* 0=停止, 1=正转, 2=反转 *)
    END_VAR
    VAR_OUTPUT
        STATUS      : MOTOR_STATUS;
        ACTUAL_SPEED : INT;
        IS_RUNNING  : BOOL;
    END_VAR
    VAR
        RAMP_STEP   : INT := 10;
        CURRENT_SPD : INT := 0;
        ERROR       : INT := 0;
    END_VAR

    IS_RUNNING := ENABLE AND (CMD_DIR <> 0);

    IF NOT ENABLE OR CMD_DIR = 0 THEN
        (* 停车：减速 *)
        IF CURRENT_SPD > 0 THEN
            CURRENT_SPD := CURRENT_SPD - RAMP_STEP;
            IF CURRENT_SPD < 0 THEN
                CURRENT_SPD := 0;
            END_IF
        ELSIF CURRENT_SPD < 0 THEN
            CURRENT_SPD := CURRENT_SPD + RAMP_STEP;
            IF CURRENT_SPD > 0 THEN
                CURRENT_SPD := 0;
            END_IF
        END_IF
    ELSE
        (* 加速到目标 *)
        IF CURRENT_SPD < TARGET_SPEED THEN
            CURRENT_SPD := CURRENT_SPD + RAMP_STEP;
            IF CURRENT_SPD > TARGET_SPEED THEN
                CURRENT_SPD := TARGET_SPEED;
            END_IF
        ELSIF CURRENT_SPD > TARGET_SPEED THEN
            CURRENT_SPD := CURRENT_SPD - RAMP_STEP;
            IF CURRENT_SPD < TARGET_SPEED THEN
                CURRENT_SPD := TARGET_SPEED;
            END_IF
        END_IF
    END_IF

    ACTUAL_SPEED := CURRENT_SPD;

    (* 填充状态 *)
    STATUS.SPEED := CURRENT_SPD;
    STATUS.TORQUE := REAL#0.0;
    STATUS.DIRECTION := CMD_DIR;
    STATUS.ERROR_CODE := ERROR;
END_FUNCTION_BLOCK
