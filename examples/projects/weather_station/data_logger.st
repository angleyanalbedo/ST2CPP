(* weather_station/data_logger.st — 数据记录 FB *)

FUNCTION_BLOCK DATA_LOGGER
    VAR_INPUT
        ENABLE    : BOOL;
        DATA      : WEATHER_DATA;
        LOG_CMD   : INT;          (* 0=无, 1=记录, 2=清空 *)
    END_VAR
    VAR_OUTPUT
        LOG_ENTRY : LOG_ENTRY;
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

        LOG_ENTRY.ENTRY_ID := ENTRY_ID;
        LOG_ENTRY.ENTRY_TYPE := 1;
        LOG_ENTRY.VALUE_A := DATA.TEMP;
        LOG_ENTRY.VALUE_B := DATA.HUMIDITY;
        LOG_ENTRY.FLAGS := DATA.UV_INDEX;

        BUFFER[WRITE_IDX] := LOG_ENTRY;

        WRITE_IDX := WRITE_IDX + 1;
        LOG_COUNT := WRITE_IDX;

        IF WRITE_IDX >= 50 THEN
            LOG_FULL := TRUE;
            WRITE_IDX := 0;
        END_IF
    END_IF
END_FUNCTION_BLOCK
