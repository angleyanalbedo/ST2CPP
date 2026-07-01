(* weather_station/main.st — 主程序：气象站 *)

PROGRAM WEATHER_STATION_MAIN
    VAR
        (* 站点配置 *)
        CONFIG      : STATION_CONFIG;

        (* 传感器 *)
        SensorCal   : SENSOR_CAL;
        Weather     : WEATHER_DATA;
        Quality     : INT := 0;
        SensorFault : BOOL := FALSE;

        (* 报警 *)
        AlarmEval   : ALARM_EVAL;
        Thresholds  : ALARM_THRESHOLDS;
        Alarm       : ALARM_STATE;
        AlarmCnt    : INT := 0;
        AlarmSev    : SEVERITY;

        (* 数据记录 *)
        Logger      : DATA_LOGGER;
        LogEntry    : LOG_ENTRY;
        LogFull     : BOOL := FALSE;
        LogCount    : DINT := 0;

        (* 通信 *)
        Comms       : COMMS_CTRL;
        CommsStatus : COMMS_STATUS;
        TxReady     : BOOL := FALSE;
        TxDataA     : REAL := 0.0;
        TxDataB     : REAL := 0.0;
        TxDataC     : INT := 0;

        (* 原始传感器值 *)
        RAW_TEMP    : REAL := 23.5;
        RAW_HUMID   : REAL := 65.0;
        RAW_PRESS   : REAL := 1013.25;
        RAW_WIND_S  : REAL := 12.5;
        RAW_WIND_D  : INT := 4;
        RAW_RAIN    : REAL := 0.0;
        RAW_UV      : INT := 5;

        (* 控制 *)
        ENABLE      : BOOL := TRUE;
        ACK_CMD     : BOOL := FALSE;
        LOG_CMD     : INT := 1;
        TX_ENABLE   : BOOL := TRUE;
        CYCLE       : DINT := 0;

        (* 工具测试 *)
        WRAPPED     : INT := 0;
        SCALED      : REAL := 0.0;
        BIT_TEST    : INT := 0;
        BIT_OK      : BOOL := FALSE;
        AVG_VAL     : REAL := 0.0;
        LERP_VAL    : REAL := 0.0;
    END_VAR

    IF CYCLE = 0 THEN
        (* 初始化配置 *)
        CONFIG.SITE_ID := 1001;
        CONFIG.ALTITUDE_M := 150.0;
        CONFIG.CALIB_TEMP := -0.5;
        CONFIG.CALIB_PRESS := 1.2;
        CONFIG.UPLOAD_SEC := 60;
        CONFIG.LOG_ENABLE := TRUE;

        (* 初始化报警阈值 *)
        Thresholds.TEMP_HIGH := 35.0;
        Thresholds.TEMP_LOW := -10.0;
        Thresholds.WIND_HIGH := 25.0;
        Thresholds.RAIN_HIGH := 50.0;
        Thresholds.UV_HIGH := 8;
        Thresholds.PRESSURE_LOW := 980.0;

        PRINT("=== WEATHER STATION v2.0 ===");
        PRINT("SITE_ID=" + CONFIG.SITE_ID);
    END_IF

    (* === 1. 传感器校准（FB + 输出参数 =>）=== *)
    SensorCal(
        ENABLE := ENABLE,
        RAW_TEMP := RAW_TEMP,
        RAW_HUMID := RAW_HUMID,
        RAW_PRESS := RAW_PRESS,
        RAW_WIND_SPD := RAW_WIND_S,
        RAW_WIND_DIR := RAW_WIND_D,
        RAW_RAIN := RAW_RAIN,
        RAW_UV := RAW_UV,
        CALIBRATED => Weather,
        QUALITY => Quality,
        SENSOR_FAULT => SensorFault
    );

    (* === 2. 报警评估（FB + 输出参数 =>）=== *)
    AlarmEval(
        ENABLE := ENABLE,
        DATA := Weather,
        THRESHOLDS := Thresholds,
        ACK_CMD := ACK_CMD,
        ALARM => Alarm,
        ALARM_CNT => AlarmCnt,
        SEVERITY => AlarmSev
    );

    (* === 3. 数据记录（FB + 输出参数 =>）=== *)
    Logger(
        ENABLE := ENABLE,
        DATA := Weather,
        LOG_CMD := LOG_CMD,
        LOG_ENTRY => LogEntry,
        LOG_FULL => LogFull,
        LOG_COUNT => LogCount
    );

    (* === 4. 通信控制（FB + 输出参数 =>）=== *)
    Comms(
        ENABLE := ENABLE,
        DATA := Weather,
        ALARM := Alarm,
        TX_ENABLE := TX_ENABLE,
        STATUS => CommsStatus,
        TX_READY => TxReady,
        TX_DATA_A => TxDataA,
        TX_DATA_B => TxDataB,
        TX_DATA_C => TxDataC
    );

    (* === 5. FC 调用 + 多返回路径 === *)
    WRAPPED := WIND_DIR_TO_DEG(DIR_CODE := RAW_WIND_D);
    ASSERT(WRAPPED = 88);

    SCALED := LERP(A := 0.0, B := 100.0, T := 0.5);
    ASSERT(SCALED = 50.0);

    AVG_VAL := MOVING_AVG(NEW_VAL := RAW_TEMP, OLD_A := 22.0, OLD_B := 21.0);

    LERP_VAL := LERP(A := 20.0, B := 30.0, T := 0.75);

    (* === 6. AND/OR 位运算（多态）=== *)
    BIT_TEST := SET_BIT(VALUE := 0, BIT := 3);
    ASSERT(TST_BIT(VALUE := BIT_TEST, BIT := 3) = TRUE);

    BIT_TEST := CLR_BIT(VALUE := BIT_TEST, BIT := 3);
    ASSERT(TST_BIT(VALUE := BIT_TEST, BIT := 3) = FALSE);

    IF (CommsStatus.TX_COUNT AND 16#0001) <> 0 THEN
        PRINT("ODD TX");
    END_IF

    (* === 7. 断言验证 === *)
    ASSERT(Quality >= 0 AND Quality <= 100);
    ASSERT(Weather.TEMP > 20.0 AND Weather.TEMP < 30.0);
    ASSERT(Weather.HUMIDITY >= 0.0 AND Weather.HUMIDITY <= 100.0);
    ASSERT(Weather.WIND_DIR >= 0 AND Weather.WIND_DIR < 360);
    ASSERT(LogCount >= 0);
    ASSERT(CommsStatus.CONNECTED = TRUE);
    ASSERT(CommsStatus.SIGNAL_PCT > 0);

    CYCLE := CYCLE + 1;

    IF CYCLE MOD 10 = 0 THEN
        PRINT("CYCLE=" + CYCLE + " TEMP=" + Weather.TEMP + " Q=" + Quality);
    END_IF
END_PROGRAM
