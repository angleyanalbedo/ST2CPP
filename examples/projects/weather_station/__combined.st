(* weather_station/types.st — 全局类型定义 *)

(* 坐标点 *)
TYPE COORD :
STRUCT
    LAT : REAL;
    LON : REAL;
END_STRUCT;
END_TYPE

(* 气象数据包 *)
TYPE WEATHER_DATA :
STRUCT
    TEMP      : REAL;
    HUMIDITY  : REAL;
    PRESSURE  : REAL;
    WIND_SPD  : REAL;
    WIND_DIR  : INT;
    RAIN_MM   : REAL;
    UV_INDEX  : INT;
    TIMESTAMP : DINT;
END_STRUCT;
END_TYPE

(* 站点配置 *)
TYPE STATION_CONFIG :
STRUCT
    SITE_ID     : INT;
    ALTITUDE_M  : REAL;
    CALIB_TEMP  : REAL;
    CALIB_PRESS : REAL;
    UPLOAD_SEC  : INT;
    LOG_ENABLE  : BOOL;
END_STRUCT;
END_TYPE

(* 报警阈值 *)
TYPE ALARM_THRESHOLDS :
STRUCT
    TEMP_HIGH     : REAL;
    TEMP_LOW      : REAL;
    WIND_HIGH     : REAL;
    RAIN_HIGH     : REAL;
    UV_HIGH       : INT;
    PRESSURE_LOW  : REAL;
END_STRUCT;
END_TYPE

(* 报警状态 *)
TYPE ALARM_STATE :
STRUCT
    ACTIVE      : BOOL;
    CODE        : INT;
    MESSAGE     : INT;
    TIMESTAMP   : DINT;
    ACK_FLAG    : BOOL;
END_STRUCT;
END_TYPE

(* 日志记录 *)
TYPE LOG_ENTRY :
STRUCT
    ENTRY_ID    : DINT;
    ENTRY_TYPE  : INT;
    VALUE_A     : REAL;
    VALUE_B     : REAL;
    FLAGS       : INT;
END_STRUCT;
END_TYPE

(* 通信状态 *)
TYPE COMMS_STATUS :
STRUCT
    CONNECTED   : BOOL;
    TX_COUNT    : DINT;
    RX_COUNT    : DINT;
    ERR_COUNT   : DINT;
    LAST_TX     : DINT;
    SIGNAL_PCT  : INT;
END_STRUCT;
END_TYPE

(* 风向枚举 *)
TYPE WIND_DIR_ENUM : (N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW);
END_TYPE

(* 报警级别枚举 *)
TYPE SEVERITY : (INFO, WARNING, ERROR, CRITICAL);
END_TYPE

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

(* weather_station/alarm_eval.st — 报警评估 FB *)

FUNCTION_BLOCK ALARM_EVAL
    VAR_INPUT
        ENABLE     : BOOL;
        DATA       : WEATHER_DATA;
        THRESHOLDS : ALARM_THRESHOLDS;
        ACK_CMD    : BOOL;
    END_VAR
    VAR_OUTPUT
        ALARM      : ALARM_STATE;
        ALARM_CNT  : INT;
    END_VAR
    VAR
        PREV_ACTIVE : BOOL := FALSE;
        ALARM_BITS  : INT := 0;
        MAX_SEV     : INT := 0;
    END_VAR

    IF NOT ENABLE THEN
        ALARM.ACTIVE := FALSE;
        ALARM_CNT := 0;
        RETURN;
    END_IF

    ALARM_BITS := 0;
    MAX_SEV := 0;

    (* 温度高 *)
    IF DATA.TEMP > THRESHOLDS.TEMP_HIGH THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 0);
        IF MAX_SEV < 1 THEN MAX_SEV := 1; END_IF
    END_IF

    (* 温度低 *)
    IF DATA.TEMP < THRESHOLDS.TEMP_LOW THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 1);
        IF MAX_SEV < 1 THEN MAX_SEV := 1; END_IF
    END_IF

    (* 风速高 *)
    IF DATA.WIND_SPD > THRESHOLDS.WIND_HIGH THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 2);
        IF MAX_SEV < 2 THEN MAX_SEV := 2; END_IF
    END_IF

    (* 降雨高 *)
    IF DATA.RAIN_MM > THRESHOLDS.RAIN_HIGH THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 3);
        IF MAX_SEV < 2 THEN MAX_SEV := 2; END_IF
    END_IF

    (* UV 高 *)
    IF DATA.UV_INDEX > THRESHOLDS.UV_HIGH THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 4);
        IF MAX_SEV < 1 THEN MAX_SEV := 1; END_IF
    END_IF

    (* 气压低 *)
    IF DATA.PRESSURE < THRESHOLDS.PRESSURE_LOW THEN
        ALARM_BITS := SET_BIT(VALUE := ALARM_BITS, BIT := 5);
        IF MAX_SEV < 3 THEN MAX_SEV := 3; END_IF
    END_IF

    (* 更新报警状态 *)
    IF ALARM_BITS <> 0 THEN
        ALARM.ACTIVE := TRUE;
        ALARM.CODE := ALARM_BITS;
        ALARM.TIMESTAMP := DATA.TIMESTAMP;

        IF NOT PREV_ACTIVE THEN
            ALARM.ACK_FLAG := FALSE;
        END_IF
    ELSE
        ALARM.ACTIVE := FALSE;
        ALARM.CODE := 0;
        ALARM.ACK_FLAG := FALSE;
    END_IF

    (* 确认 *)
    IF ACK_CMD AND ALARM.ACTIVE THEN
        ALARM.ACK_FLAG := TRUE;
    END_IF

    PREV_ACTIVE := ALARM.ACTIVE;

    (* 报警计数 *)
    ALARM_CNT := 0;
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 0) THEN ALARM_CNT := ALARM_CNT + 1; END_IF
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 1) THEN ALARM_CNT := ALARM_CNT + 1; END_IF
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 2) THEN ALARM_CNT := ALARM_CNT + 1; END_IF
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 3) THEN ALARM_CNT := ALARM_CNT + 1; END_IF
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 4) THEN ALARM_CNT := ALARM_CNT + 1; END_IF
    IF TST_BIT(VALUE := ALARM_BITS, BIT := 5) THEN ALARM_CNT := ALARM_CNT + 1; END_IF

END_FUNCTION_BLOCK

(* weather_station/data_logger.st — 数据记录 FB *)

FUNCTION_BLOCK DATA_LOGGER
    VAR_INPUT
        ENABLE    : BOOL;
        DATA      : WEATHER_DATA;
        LOG_CMD   : INT;          (* 0=无, 1=记录, 2=清空 *)
    END_VAR
    VAR_OUTPUT
        LAST_ENTRY : LOG_ENTRY;
        LOG_FULL  : BOOL;
        LOG_COUNT : DINT;
    END_VAR
    VAR
        BUFFER    : ARRAY[0..49] OF LOG_ENTRY;
        WRITE_IDX : DINT := 0;
        ENTRY_ID  : DINT := 0;
    END_VAR

    IF NOT ENABLE THEN
        RETURN;
    END_IF

    (* 清空 *)
    IF LOG_CMD = 2 THEN
        WRITE_IDX := 0;
        ENTRY_ID := 0;
        LOG_COUNT := 0;
        LOG_FULL := FALSE;
        RETURN;
    END_IF

    (* 记录 *)
    IF LOG_CMD = 1 THEN
        ENTRY_ID := ENTRY_ID + 1;

        LAST_ENTRY.ENTRY_ID := ENTRY_ID;
        LAST_ENTRY.ENTRY_TYPE := 1;
        LAST_ENTRY.VALUE_A := DATA.TEMP;
        LAST_ENTRY.VALUE_B := DATA.HUMIDITY;
        LAST_ENTRY.FLAGS := DATA.UV_INDEX;

        BUFFER[WRITE_IDX] := LAST_ENTRY;

        WRITE_IDX := WRITE_IDX + 1;
        LOG_COUNT := WRITE_IDX;

        IF WRITE_IDX >= 50 THEN
            LOG_FULL := TRUE;
            WRITE_IDX := 0;
        END_IF
    END_IF
END_FUNCTION_BLOCK

(* weather_station/comms_ctrl.st — 通信控制 FB *)

FUNCTION_BLOCK COMMS_CTRL
    VAR_INPUT
        ENABLE     : BOOL;
        DATA       : WEATHER_DATA;
        ALARM      : ALARM_STATE;
        TX_ENABLE  : BOOL;
    END_VAR
    VAR_OUTPUT
        STATUS     : COMMS_STATUS;
        TX_READY   : BOOL;
        TX_DATA_A  : REAL;
        TX_DATA_B  : REAL;
        TX_DATA_C  : INT;
    END_VAR
    VAR
        TX_TIMER   : DINT := 0;
        TX_PERIOD  : DINT := 60;
        TX_COUNT   : DINT := 0;
        RX_COUNT   : DINT := 0;
        ERR_COUNT  : DINT := 0;
    END_VAR

    IF NOT ENABLE THEN
        STATUS.CONNECTED := FALSE;
        TX_READY := FALSE;
        RETURN;
    END_IF

    STATUS.CONNECTED := TRUE;
    STATUS.TX_COUNT := TX_COUNT;
    STATUS.RX_COUNT := RX_COUNT;
    STATUS.ERR_COUNT := ERR_COUNT;
    STATUS.SIGNAL_PCT := 85;

    TX_READY := FALSE;

    IF TX_ENABLE THEN
        TX_TIMER := TX_TIMER + 1;

        IF TX_TIMER >= TX_PERIOD THEN
            TX_TIMER := 0;
            TX_COUNT := TX_COUNT + 1;
            STATUS.LAST_TX := TX_COUNT;

            (* 打包发送数据 *)
            TX_DATA_A := DATA.TEMP;
            TX_DATA_B := DATA.PRESSURE;
            TX_DATA_C := DATA.WIND_DIR;
            TX_READY := TRUE;

            (* 模拟接收确认 *)
            RX_COUNT := RX_COUNT + 1;
        END_IF
    END_IF
END_FUNCTION_BLOCK

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

        (* 数据记录 *)
        Logger      : DATA_LOGGER;
        LastEntry   : LOG_ENTRY;
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
        ALARM_CNT => AlarmCnt
    );

    (* === 3. 数据记录（FB + 输出参数 =>）=== *)
    Logger(
        ENABLE := ENABLE,
        DATA := Weather,
        LOG_CMD := LOG_CMD,
        LAST_ENTRY => LastEntry,
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
        PRINT("CYCLE=" + CYCLE + " Q=" + Quality);
    END_IF
END_PROGRAM
