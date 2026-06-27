(* zone_fb.st — HVAC 区域控制功能块 *)

FUNCTION_BLOCK ZONE_CTRL
    VAR_INPUT
        RAW_TEMP    : INT;
        OCCUPIED    : BOOL;
        SETPOINT    : REAL;
        ENABLE      : BOOL;
        SCALE       : SCALE_RANGE;
        AO_SCALE    : SCALE_RANGE;
        PID_P       : PID_PARAMS;
    END_VAR
    VAR_OUTPUT
        VALVE_POS   : INT := 0;
        HEAT_OUT    : BOOL := FALSE;
        COOL_OUT    : BOOL := FALSE;
        FAN_OUT     : BOOL := FALSE;
        ZONE_ALARM  : BOOL := FALSE;
        MODE_OUT    : INT := 0;
    END_VAR
    VAR
        STATE       : ZONE_STATE;
        PID_INT     : REAL := 0.0;
        PID_PREV    : REAL := 0.0;
        HYSTERESIS  : REAL := 2.0;
        MODE_CMD    : INT := 0;
        MODE_PREV   : INT := 0;
        FAULT_CNT   : INT := 0;
        I           : INT := 0;
        DT          : REAL := 1.0;
    END_VAR

    (* 温度工程值转换 *)
    STATE.TEMP := SCALE_AI(RAW_TEMP, SCALE);

    STATE.SETPOINT := SETPOINT;
    STATE.ENABLED := ENABLE;

    STATE.ERROR := STATE.SETPOINT - STATE.TEMP;

    IF ENABLE THEN
        (* PID 计算 *)
        STATE.OUTPUT := PID_CALC(
            SP := SETPOINT,
            PV := STATE.TEMP,
            P := PID_P,
            DT := DT,
            INTEGRAL := PID_INT,
            PREV_ERROR := PID_PREV
        );

        (* 模式选择 *)
        MODE_CMD := TEMP_TO_MODE(STATE.TEMP, SETPOINT, HYSTERESIS);

        (* 状态机 *)
        CASE MODE_CMD OF
            1:
                (* 加热模式 *)
                VALVE_POS := SCALE_AO(STATE.OUTPUT, AO_SCALE);
                HEAT_OUT := STATE.OUTPUT > 0.0;
                COOL_OUT := FALSE;
                FAN_OUT := TRUE;
                MODE_OUT := 1;

            2:
                (* 制冷模式 *)
                VALVE_POS := SCALE_AO(REAL_TO_DINT(-STATE.OUTPUT), AO_SCALE);
                HEAT_OUT := FALSE;
                COOL_OUT := STATE.OUTPUT < -5.0;
                FAN_OUT := TRUE;
                MODE_OUT := 2;

            ELSE
                (* 待机模式 *)
                VALVE_POS := 0;
                HEAT_OUT := FALSE;
                COOL_OUT := FALSE;
                FAN_OUT := MODE_PREV <> 0;
                MODE_OUT := 0;
        END_CASE;

        MODE_PREV := MODE_CMD;

        (* 告警检测 *)
        IF STATE.TEMP > 60.0 OR STATE.TEMP < -10.0 THEN
            FAULT_CNT := FAULT_CNT + 1;
        ELSE
            FAULT_CNT := 0;
        END_IF;

        IF FAULT_CNT >= 3 THEN
            STATE.ALARM := TRUE;
        ELSE
            STATE.ALARM := FALSE;
        END_IF;
    ELSE
        VALVE_POS := 0;
        HEAT_OUT := FALSE;
        COOL_OUT := FALSE;
        FAN_OUT := FALSE;
        MODE_OUT := 0;
        STATE.ALARM := FALSE;
    END_IF;

    ZONE_ALARM := STATE.ALARM;
END_FUNCTION_BLOCK
