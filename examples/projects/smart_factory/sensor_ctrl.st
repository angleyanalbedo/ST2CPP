(* smart_factory/sensor_ctrl.st — 传感器控制 FB *)

FUNCTION_BLOCK SENSOR_CTRL
    VAR_INPUT
        ENABLE      : BOOL;
        RAW_TEMP    : REAL;
        RAW_HUMID   : REAL;
        RAW_PRESS   : REAL;
        RAW_LIGHT   : INT;
    END_VAR
    VAR_OUTPUT
        DATA        : SENSOR_DATA;
        ALARM       : ALARM_LEVEL;
        FILTERED_OK : BOOL;
    END_VAR
    VAR
        SAMPLE_COUNT : DINT := 0;
        SUM_TEMP     : REAL := 0.0;
        SUM_HUMID    : REAL := 0.0;
        BASE_TEMP    : REAL := 25.0;
    END_VAR

    IF NOT ENABLE THEN
        FILTERED_OK := FALSE;
        ALARM := ALARM_LEVEL#NONE;
        RETURN;
    END_IF

    (* 累加采样 *)
    SAMPLE_COUNT := SAMPLE_COUNT + 1;
    SUM_TEMP := SUM_TEMP + RAW_TEMP;
    SUM_HUMID := SUM_HUMID + RAW_HUMID;

    (* 输出原始数据 *)
    DATA.TEMPERATURE := RAW_TEMP;
    DATA.HUMIDITY := RAW_HUMID;
    DATA.PRESSURE := RAW_PRESS;
    DATA.LIGHT := RAW_LIGHT;

    (* 简单滤波判断 *)
    FILTERED_OK := TRUE;

    (* 温度报警 *)
    IF RAW_TEMP > 80.0 THEN
        ALARM := ALARM_LEVEL#HIGH;
        FILTERED_OK := FALSE;
    ELSIF RAW_TEMP > 60.0 THEN
        ALARM := ALARM_LEVEL#MEDIUM;
    ELSIF RAW_TEMP < 0.0 THEN
        ALARM := ALARM_LEVEL#LOW;
    ELSE
        ALARM := ALARM_LEVEL#NONE;
    END_IF
END_FUNCTION_BLOCK
