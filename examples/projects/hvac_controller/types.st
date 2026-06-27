(* types.st — HVAC 区域控制器类型定义 *)

TYPE
    (* PID 参数结构体 *)
    PID_PARAMS :
    STRUCT
        KP  : REAL;
        KI  : REAL;
        KD  : REAL;
        TS  : REAL;
        MIN : REAL;
        MAX : REAL;
    END_STRUCT;

    (* 区域运行状态 *)
    ZONE_STATE :
    STRUCT
        TEMP      : REAL;
        SETPOINT  : REAL;
        ERROR     : REAL;
        INTEGRAL  : REAL;
        DERIVATIVE : REAL;
        OUTPUT    : REAL;
        MODE      : INT;
        ENABLED   : BOOL;
        ALARM     : BOOL;
        OVERRIDE  : BOOL;
    END_STRUCT;

    (* 传感器量程配置 *)
    SCALE_RANGE :
    STRUCT
        RAW_MIN  : INT;
        RAW_MAX  : INT;
        ENG_MIN  : REAL;
        ENG_MAX  : REAL;
    END_STRUCT;

    (* 区域配置表 *)
    ZONE_CFG :
    STRUCT
        ID          : INT;
        SENSOR_ADDR : INT;
        ACT_ADDR    : INT;
        OCCUPIED    : BOOL;
    END_STRUCT;

    (* 系统告警记录 *)
    ALARM_RECORD :
    STRUCT
        CODE    : INT;
        VALUE   : REAL;
        LATCh   : BOOL;
        ACK     : BOOL;
    END_STRUCT;

    (* 8 区域配置数组类型 *)
    ZONE_CFG_ARR :
    ARRAY[0..7] OF ZONE_CFG;

    (* 4 通道量程数组类型 *)
    SCALE_ARR :
    ARRAY[0..3] OF SCALE_RANGE;
END_TYPE
