(* smart_factory/types.st — 类型定义 *)

(* 传感器数据包 *)
TYPE SENSOR_DATA :
STRUCT
    TEMPERATURE : REAL;
    HUMIDITY    : REAL;
    PRESSURE    : REAL;
    LIGHT       : INT;
END_STRUCT;
END_TYPE

(* 电机状态 *)
TYPE MOTOR_STATUS :
STRUCT
    SPEED       : INT;
    TORQUE      : REAL;
    DIRECTION   : INT;       (* 0=停止, 1=正转, 2=反转 *)
    ERROR_CODE  : INT;
END_STRUCT;
END_TYPE

(* 报警级别 *)
TYPE ALARM_LEVEL : (NONE, LOW, MEDIUM, HIGH, CRITICAL);
END_TYPE

(* 配方参数 *)
TYPE RECIPE :
STRUCT
    TEMP_TARGET   : REAL;
    TIME_SECONDS  : INT;
    SPEED_SET     : INT;
    TOLERANCE     : REAL;
END_STRUCT;
END_TYPE
