(* io_config.st — 6轴机械臂 EtherCAT I/O 映射 *)

VAR_GLOBAL
    (* ─── EtherCAT 伺服驱动器 PDO（6 轴） ─── *)

    (* 轴1-3：基座/大臂/小臂（高扭矩） *)
    Srv1CmdPos     AT %ID0   : DINT;      (* 命令位置 *)
    Srv1Status     AT %IW0   : WORD;      (* 状态字 *)
    Srv1ActPos     AT %ID4   : DINT;      (* 实际位置 *)
    Srv1ActVel     AT %ID8   : DINT;      (* 实际速度 *)
    Srv1ActTorq    AT %IW4   : INT;       (* 实际扭矩 *)
    Srv1Ctrl       AT %QW0   : WORD;      (* 控制字 *)
    Srv1CmdVel     AT %QD0   : DINT;      (* 命令速度 *)
    Srv1CmdTorq    AT %QW2   : INT;       (* 命令扭矩 *)

    Srv2CmdPos     AT %ID12  : DINT;
    Srv2Status     AT %IW8   : WORD;
    Srv2ActPos     AT %ID16  : DINT;
    Srv2ActVel     AT %ID20  : DINT;
    Srv2ActTorq    AT %IW10  : INT;
    Srv2Ctrl       AT %QW4   : WORD;
    Srv2CmdVel     AT %QD4   : DINT;
    Srv2CmdTorq    AT %QW6   : INT;

    Srv3CmdPos     AT %ID24  : DINT;
    Srv3Status     AT %IW12  : WORD;
    Srv3ActPos     AT %ID28  : DINT;
    Srv3ActVel     AT %ID32  : DINT;
    Srv3ActTorq    AT %IW14  : INT;
    Srv3Ctrl       AT %QW8   : WORD;
    Srv3CmdVel     AT %QD8   : DINT;
    Srv3CmdTorq    AT %QW10  : INT;

    (* 轴4-6：腕部（小扭矩/高速） *)
    Srv4CmdPos     AT %ID36  : DINT;
    Srv4Status     AT %IW16  : WORD;
    Srv4ActPos     AT %ID40  : DINT;
    Srv4ActVel     AT %ID44  : DINT;
    Srv4ActTorq    AT %IW18  : INT;
    Srv4Ctrl       AT %QW12  : WORD;
    Srv4CmdVel     AT %QD12  : DINT;
    Srv4CmdTorq    AT %QW14  : INT;

    Srv5CmdPos     AT %ID48  : DINT;
    Srv5Status     AT %IW20  : WORD;
    Srv5ActPos     AT %ID52  : DINT;
    Srv5ActVel     AT %ID56  : DINT;
    Srv5ActTorq    AT %IW22  : INT;
    Srv5Ctrl       AT %QW16  : WORD;
    Srv5CmdVel     AT %QD16  : DINT;
    Srv5CmdTorq    AT %QW18  : INT;

    Srv6CmdPos     AT %ID60  : DINT;
    Srv6Status     AT %IW24  : WORD;
    Srv6ActPos     AT %ID64  : DINT;
    Srv6ActVel     AT %ID68  : DINT;
    Srv6ActTorq    AT %IW26  : INT;
    Srv6Ctrl       AT %QW20  : WORD;
    Srv6CmdVel     AT %QD20  : DINT;
    Srv6CmdTorq    AT %QW22  : INT;

    (* ─── 数字量 I/O ─── *)
    StartBtn       AT %IX0.0 : BOOL;      (* 启动按钮 *)
    StopBtn        AT %IX0.1 : BOOL;      (* 停止按钮 *)
    EStop          AT %IX0.2 : BOOL;      (* 急停按钮（NC） *)
    HomeSwitch     AT %IX0.3 : BOOL;      (* 原点开关 *)
    GripSensor     AT %IX0.4 : BOOL;      (* 夹爪力传感器 *)
    PartDetect     AT %IX0.5 : BOOL;      (* 工件检测 *)
    AutoMode       AT %IX0.6 : BOOL;      (* 自动模式 *)
    TeachMode      AT %IX0.7 : BOOL;      (* 示教模式 *)

    RobotRun       AT %QX0.0 : BOOL;      (* 运行指示灯 *)
    RobotError     AT %QX0.1 : BOOL;      (* 故障灯 *)
    RobotHome      AT %QX0.2 : BOOL;      (* 原点灯 *)
    GripperOpen    AT %QX0.3 : BOOL;      (* 夹爪打开 *)
    GripperClose   AT %QX0.4 : BOOL;      (* 夹爪闭合 *)
    ConveyorRun    AT %QX0.5 : BOOL;      (* 传送带运行 *)
    WeldEnable     AT %QX0.6 : BOOL;      (* 焊接使能 *)
    ToolChange     AT %QX0.7 : BOOL;      (* 换刀信号 *)

    (* ─── 模拟量 I/O ─── *)
    WeldCurrent    AT %QW24 : INT;        (* 焊接电流 *)
    WeldVoltage    AT %QW26 : INT;        (* 焊接电压 *)
    ForceSensor    AT %IW28 : INT;        (* 力传感器 *)
    VisionReady    AT %IW30 : INT;        (* 视觉系统状态 *)

    (* ─── 系统全局变量 ─── *)
    RobotEnabled   : BOOL := FALSE;
    HomeDone       : BOOL := FALSE;
    ProgramRunning : BOOL := FALSE;
    CurrentStep    : INT := 0;
    TotalCycles    : DINT := 0;
    CycleTime      : DINT := 0;
    MaxCycleTime   : DINT := 0;

    (* ─── 伺服状态数组 ─── *)
    SrvState       : ARRAY[0..5] OF SERVO_STATE;

    (* ─── 当前位姿 ─── *)
    CurrentPose    : POSE_3D;
    TargetPose     : POSE_3D;

    (* ─── 运动状态 ─── *)
    Motion         : MOTION_STATE;

    (* ─── 安全系统 ─── *)
    Safety         : SAFETY_CFG;
    Alarm          : SAFETY_ALARM;
END_VAR
