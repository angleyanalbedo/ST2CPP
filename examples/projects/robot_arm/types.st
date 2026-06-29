(* types.st — 6轴机械臂类型定义 *)

TYPE
    (* 伺服驱动器状态 *)
    SERVO_STATE :
    STRUCT
        POSITION    : DINT;      (* 编码器位置，单位: 脉冲 *)
        VELOCITY    : DINT;      (* 当前速度，单位: 脉冲/周期 *)
        TORQUE      : INT;       (* 当前扭矩，单位: 额定扭矩百分比 *)
        STATUS      : WORD;      (* 状态字 *)
        ERROR_CODE  : WORD;      (* 故障码 *)
        ENABLED     : BOOL;      (* 使能状态 *)
        IN_POSITION : BOOL;      (* 到位信号 *)
        FAULT       : BOOL;      (* 故障标志 *)
    END_STRUCT;

    (* 关节配置 *)
    JOINT_CFG :
    STRUCT
        JOINT_NAME      : INT;         (* 关节名称编号 *)
        JOINT_TYPE      : INT;         (* 0=旋转, 1=移动 *)
        GEAR_RATIO      : REAL;        (* 减速比 *)
        PULSES_PER_REV  : DINT;        (* 编码器每转脉冲数 *)
        POS_MIN         : DINT;        (* 正向软限位 *)
        POS_MAX         : DINT;        (* 反向软限位 *)
        VEL_MAX         : DINT;        (* 最大速度 *)
        ACC_MAX         : DINT;        (* 最大加速度 *)
        HOME_OFFSET     : DINT;        (* 零点偏移 *)
        DIRECTION       : INT;         (* 1=正向, -1=反向 *)
    END_STRUCT;

    (* DH 参数（Denavit-Hartenberg） *)
    DH_PARAM :
    STRUCT
        A       : REAL;        (* 连杆长度 mm *)
        ALPHA   : REAL;        (* 连杆扭角 rad *)
        D       : REAL;        (* 连杆偏距 mm *)
        THETA   : REAL;        (* 关节角度 rad *)
    END_STRUCT;

    (* 3D 位姿 *)
    POSE_3D :
    STRUCT
        X   : REAL;            (* X 坐标 mm *)
        Y   : REAL;            (* Y 坐标 mm *)
        Z   : REAL;            (* Z 坐标 mm *)
        RX  : REAL;            (* 旋转 X rad *)
        RY  : REAL;            (* 旋转 Y rad *)
        RZ  : REAL;            (* 旋转 Z rad *)
    END_STRUCT;

    (* 轨迹点 *)
    WAYPOINT :
    STRUCT
        POSE        : POSE_3D;
        SPEED       : REAL;        (* 速度百分比 0-100 *)
        BLEND_R     : REAL;        (* 轨迹过渡半径 mm *)
        DWELL_MS    : INT;         (* 停留时间 ms *)
    END_STRUCT;

    (* 关节空间轨迹点 *)
    JOINT_WAYPOINT :
    STRUCT
        J1      : REAL;            (* 关节1角度 deg *)
        J2      : REAL;
        J3      : REAL;
        J4      : REAL;
        J5      : REAL;
        J6      : REAL;
        SPEED   : REAL;
        DWELL_MS : INT;
    END_STRUCT;

    (* 运动状态 *)
    MOTION_STATE :
    STRUCT
        TARGET      : POSE_3D;
        CURRENT     : POSE_3D;
        ERROR       : POSE_3D;
        JOINT_TARGET : JOINT_WAYPOINT;
        JOINT_CURRENT : JOINT_WAYPOINT;
        STATE       : INT;         (* 0=空闲, 1=运动中, 2=到位, 3=暂停, 4=错误 *)
        PROGRESS    : REAL;        (* 运动进度 0-100% *)
        TOTAL_TIME  : DINT;        (* 总运行时间 ms *)
        ERR_MSG     : INT;         (* 错误码 *)
    END_STRUCT;

    (* 安全参数 *)
    SAFETY_CFG :
    STRUCT
        E_STOP_ACTIVE     : BOOL;
        JOINT_LIMITS_EN   : BOOL;
        COLLISION_EN      : BOOL;
        SPEED_LIMIT_EN    : BOOL;
        WORKSPACE_EN      : BOOL;
        MAX_SPEED_PCT     : REAL;        (* 全局速度限制 % *)
        MAX_TORQUE_PCT    : REAL;        (* 全局扭矩限制 % *)
    END_STRUCT;

    (* 安全告警 *)
    SAFETY_ALARM :
    STRUCT
        E_STOP          : BOOL;
        JOINT_OVERTRAVEL : BOOL;
        COLLISION       : BOOL;
        OVERSPEED       : BOOL;
        WORKSPACE_VIOL  : BOOL;
        SERVO_FAULT     : BOOL;
        COMM_LOST       : BOOL;
        ALARM_CODE      : INT;
    END_STRUCT;

    (* 程序段 *)
    PROGRAM_SEGMENT :
    STRUCT
        SEG_TYPE    : INT;         (* 0=关节运动, 1=直线运动, 2=圆弧, 3=IO操作, 4=延时 *)
        TARGET      : JOINT_WAYPOINT;
        LINEAR_TARGET : POSE_3D;
        SPEED_PCT   : REAL;
        IO_CMD      : INT;         (* IO 操作编号 *)
        IO_VALUE    : BOOL;        (* IO 值 *)
        DELAY_MS    : INT;
        ENABLED     : BOOL;
    END_STRUCT;
END_TYPE
