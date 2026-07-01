(* smart_factory/main.st — 主程序 *)

PROGRAM SMART_FACTORY_MAIN
    VAR
        Sensor      : SENSOR_CTRL;
        SensorData  : SENSOR_DATA;
        SensorAlarm : ALARM_LEVEL;
        SensorOK    : BOOL := FALSE;

        Motor       : MOTOR_CTRL;
        MotorStatus : MOTOR_STATUS;
        MotorSpeed  : INT := 0;
        MotorRunning : BOOL := FALSE;

        LineCtrl    : LINE_CONTROLLER;
        LineProd    : DINT := 0;

        CurrentRecipe : RECIPE;

        CYCLE       : DINT := 0;
        STOP_CMD    : BOOL := FALSE;

        RAW_T   : REAL := 25.0;
        RAW_H   : REAL := 50.0;
        RAW_P   : REAL := 1013.0;
        RAW_L   : INT := 500;

        SPD_CMD : INT := 1000;
        DIR_CMD : INT := 1;

        WRAPPED : INT := 0;
        SCALED  : REAL := 0.0;
        BIT_VAL : INT := 16#00FF;
        BIT_OK  : BOOL := FALSE;

        INIT_DONE : BOOL := FALSE;
    END_VAR

    IF NOT INIT_DONE THEN
        LineCtrl.INIT_LINE();
        INIT_DONE := TRUE;
        CurrentRecipe.TEMP_TARGET := 85.0;
        CurrentRecipe.TIME_SECONDS := 300;
        CurrentRecipe.SPEED_SET := 1500;
        CurrentRecipe.TOLERANCE := 0.5;
        PRINT("=== SMART FACTORY START ===");
    END_IF

    LineProd := LineCtrl.PROD_CYCLE();

    WRAPPED := WRAP_ANGLE(ANGLE := 450);
    ASSERT(WRAPPED = 90);

    SCALED := SCALE_REAL(INPUT_VAL := 50.0, IN_MIN := 0.0, IN_MAX := 100.0, OUT_MIN := 0.0, OUT_MAX := 270.0);

    BIT_OK := BIT_CHECK(VALUE := BIT_VAL, BIT_POS := 3);
    ASSERT(BIT_OK = TRUE);

    Sensor(
        ENABLE := TRUE,
        RAW_TEMP := RAW_T,
        RAW_HUMID := RAW_H,
        RAW_PRESS := RAW_P,
        RAW_LIGHT := RAW_L,
        DATA => SensorData,
        ALARM => SensorAlarm,
        FILTERED_OK => SensorOK
    );

    Motor(
        ENABLE := TRUE,
        TARGET_SPEED := SPD_CMD,
        CMD_DIR := DIR_CMD,
        STATUS => MotorStatus,
        ACTUAL_SPEED => MotorSpeed,
        IS_RUNNING => MotorRunning
    );

    IF (BIT_VAL AND 16#0010) <> 0 THEN
        PRINT("BIT 4 is set");
    END_IF

    IF (BIT_VAL OR 16#FF00) = 16#FFFF THEN
        PRINT("OR mask OK");
    END_IF

    ASSERT(LineProd >= 0);
    ASSERT(SensorOK = TRUE);
    ASSERT(MotorSpeed > 0);
    ASSERT(MotorRunning = TRUE);
    ASSERT(SensorData.TEMPERATURE = 25.0);
    ASSERT(CurrentRecipe.TEMP_TARGET = 85.0);

    CYCLE := CYCLE + 1;
    PRINT("CYCLE=" + CYCLE + " PROD=" + LineProd + " SPD=" + MotorSpeed);
END_PROGRAM
