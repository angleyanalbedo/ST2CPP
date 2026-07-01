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
