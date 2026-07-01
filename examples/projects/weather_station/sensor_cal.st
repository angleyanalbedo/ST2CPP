(* weather_station/sensor_cal.st — 传感器校准 FB *)

FUNCTION_BLOCK SENSOR_CAL
    VAR_INPUT
        ENABLE       : BOOL;
        RAW_TEMP     : REAL;
        RAW_HUMID    : REAL;
        RAW_PRESS    : REAL;
        RAW_WIND_SPD : REAL;
        RAW_WIND_DIR : INT;
        RAW_RAIN     : REAL;
        RAW_UV       : INT;
    END_VAR
    VAR_OUTPUT
        CALIBRATED   : WEATHER_DATA;
        QUALITY      : INT;       (* 0-100 质量分 *)
        SENSOR_FAULT : BOOL;
    END_VAR
    VAR
        CALIB_TEMP   : REAL := 0.0;
        CALIB_PRESS  : REAL := 0.0;
        SAMPLE_CNT   : DINT := 0;
        FAULT_MASK    : INT := 0;
    END_VAR

    IF NOT ENABLE THEN
        SENSOR_FAULT := FALSE;
        QUALITY := 0;
        RETURN;
    END_IF

    SAMPLE_CNT := SAMPLE_CNT + 1;
    SENSOR_FAULT := FALSE;
    FAULT_MASK := 0;

    (* 温度校准 *)
    CALIBRATED.TEMP := RAW_TEMP + CALIB_TEMP;

    (* 湿度限幅 *)
    CALIBRATED.HUMIDITY := CLAMP_REAL(VAL := RAW_HUMID, MN := 0.0, MX := 100.0);

    (* 气压校准 *)
    CALIBRATED.PRESSURE := RAW_PRESS + CALIB_PRESS;

    (* 风速 *)
    CALIBRATED.WIND_SPD := RAW_WIND_SPD;

    (* 风向 *)
    CALIBRATED.WIND_DIR := WIND_DIR_TO_DEG(DIR_CODE := RAW_WIND_DIR);

    (* 降雨量 *)
    CALIBRATED.RAIN_MM := RAW_RAIN;

    (* UV 指数 *)
    CALIBRATED.UV_INDEX := RAW_UV;

    (* 时间戳 *)
    CALIBRATED.TIMESTAMP := SAMPLE_CNT;

    (* 质量评分 *)
    QUALITY := 100;

    (* 异常检测 *)
    IF RAW_TEMP < -40.0 OR RAW_TEMP > 60.0 THEN
        FAULT_MASK := SET_BIT(VALUE := FAULT_MASK, BIT := 0);
        SENSOR_FAULT := TRUE;
        QUALITY := QUALITY - 30;
    END_IF

    IF RAW_HUMID < 0.0 OR RAW_HUMID > 100.0 THEN
        FAULT_MASK := SET_BIT(VALUE := FAULT_MASK, BIT := 1);
        SENSOR_FAULT := TRUE;
        QUALITY := QUALITY - 25;
    END_IF

    IF RAW_PRESS < 800.0 OR RAW_PRESS > 1100.0 THEN
        FAULT_MASK := SET_BIT(VALUE := FAULT_MASK, BIT := 2);
        SENSOR_FAULT := TRUE;
        QUALITY := QUALITY - 20;
    END_IF

    IF RAW_WIND_SPD > 60.0 THEN
        FAULT_MASK := SET_BIT(VALUE := FAULT_MASK, BIT := 3);
        SENSOR_FAULT := TRUE;
        QUALITY := QUALITY - 15;
    END_IF

    IF QUALITY < 0 THEN
        QUALITY := 0;
    END_IF
END_FUNCTION_BLOCK
