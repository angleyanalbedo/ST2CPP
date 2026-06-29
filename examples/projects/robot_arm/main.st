(* main.st — 6轴机械臂主程序 *)

PROGRAM ROBOT_MAIN
    VAR_INPUT
        StartBtn    AT %IX0.0 : BOOL;
        StopBtn     AT %IX0.1 : BOOL;
        EStop       AT %IX0.2 : BOOL;
        HomeSw      AT %IX0.3 : BOOL;
        GripSensor  AT %IX0.4 : BOOL;
        PartDetect  AT %IX0.5 : BOOL;
        AutoMode    AT %IX0.6 : BOOL;
        TeachMode   AT %IX0.7 : BOOL;
    END_VAR
    VAR_OUTPUT
        RunLamp     AT %QX0.0 : BOOL;
        ErrorLamp   AT %QX0.1 : BOOL;
        HomeLamp    AT %QX0.2 : BOOL;
        GripOpen    AT %QX0.3 : BOOL;
        GripClose   AT %QX0.4 : BOOL;
        ConvRun     AT %QX0.5 : BOOL;
        WeldEn      AT %QX0.6 : BOOL;
        ToolChg     AT %QX0.7 : BOOL;
    END_VAR
    VAR
        Robot       : ROBOT_CTRL;
        SpeedOvr    : REAL := 80.0;
        CycleTimeMs : DINT := 0;
        TotalParts  : DINT := 0;
        PartReady   : BOOL := FALSE;
        GripDone    : BOOL := FALSE;
        I           : INT;
    END_VAR

    (* ─── 安全配置 ─── *)
    Safety.E_STOP_ACTIVE := TRUE;
    Safety.JOINT_LIMITS_EN := TRUE;
    Safety.COLLISION_EN := TRUE;
    Safety.SPEED_LIMIT_EN := TRUE;
    Safety.WORKSPACE_EN := TRUE;
    Safety.MAX_SPEED_PCT := 100.0;
    Safety.MAX_TORQUE_PCT := 80.0;

    (* ─── 速度倍率 ─── *)
    SpeedOvr := CLAMP_REAL(SpeedOvr, 5.0, 100.0);

    (* ─── 主控制器调用 ─── *)
    Robot(
        START := StartBtn,
        STOP := StopBtn,
        HOME_CMD := HomeSw,
        AUTO_MODE := AutoMode,
        TEACH_MODE := TeachMode,
        SPEED_OVERRIDE := SpeedOvr
    );

    (* ─── 指示灯 ─── *)
    RunLamp := Robot.STATE_OUT = 2;
    ErrorLamp := Robot.FAULT;
    HomeLamp := Robot.HOME_DONE;

    (* ─── 传送带控制 ─── *)
    IF AutoMode AND PartDetect AND NOT GripDone THEN
        ConvRun := TRUE;
    ELSIF GripDone THEN
        ConvRun := FALSE;
    END_IF;

    (* ─── 夹爪控制 ─── *)
    IF Robot.STATE_OUT = 2 AND Robot.STEP_NUM = 1 THEN
        GripClose := TRUE;
        GripOpen := FALSE;
    ELSIF Robot.STATE_OUT = 2 AND (Robot.STEP_NUM = 3 OR Robot.STEP_NUM = 4) THEN
        GripOpen := TRUE;
        GripClose := FALSE;
    ELSE
        GripOpen := TRUE;
        GripClose := FALSE;
    END_IF;

    (* ─── 焊接使能 ─── *)
    WeldEn := FALSE;

    (* ─── 换刀信号 ─── *)
    ToolChg := FALSE;

    (* ─── 统计 ─── *)
    TotalParts := Robot.CYCLE_COUNT;

    (* ─── 伺服状态采集 ─── *)
    SrvState[0].ENABLED := Robot.Srv[0].READY;
    SrvState[0].FAULT := Robot.Srv[0].FAULT;
    SrvState[0].IN_POSITION := Robot.Srv[0].IN_POSITION;
    SrvState[0].POSITION := Robot.Srv[0].ACT_POSITION;
    SrvState[0].VELOCITY := Robot.Srv[0].ACT_VELOCITY;
    SrvState[0].TORQUE := Robot.Srv[0].ACT_TORQUE;

    SrvState[1].ENABLED := Robot.Srv[1].READY;
    SrvState[1].FAULT := Robot.Srv[1].FAULT;
    SrvState[1].IN_POSITION := Robot.Srv[1].IN_POSITION;
    SrvState[1].POSITION := Robot.Srv[1].ACT_POSITION;
    SrvState[1].VELOCITY := Robot.Srv[1].ACT_VELOCITY;
    SrvState[1].TORQUE := Robot.Srv[1].ACT_TORQUE;

    SrvState[2].ENABLED := Robot.Srv[2].READY;
    SrvState[2].FAULT := Robot.Srv[2].FAULT;
    SrvState[2].IN_POSITION := Robot.Srv[2].IN_POSITION;
    SrvState[2].POSITION := Robot.Srv[2].ACT_POSITION;
    SrvState[2].VELOCITY := Robot.Srv[2].ACT_VELOCITY;
    SrvState[2].TORQUE := Robot.Srv[2].ACT_TORQUE;

    SrvState[3].ENABLED := Robot.Srv[3].READY;
    SrvState[3].FAULT := Robot.Srv[3].FAULT;
    SrvState[3].IN_POSITION := Robot.Srv[3].IN_POSITION;
    SrvState[3].POSITION := Robot.Srv[3].ACT_POSITION;
    SrvState[3].VELOCITY := Robot.Srv[3].ACT_VELOCITY;
    SrvState[3].TORQUE := Robot.Srv[3].ACT_TORQUE;

    SrvState[4].ENABLED := Robot.Srv[4].READY;
    SrvState[4].FAULT := Robot.Srv[4].FAULT;
    SrvState[4].IN_POSITION := Robot.Srv[4].IN_POSITION;
    SrvState[4].POSITION := Robot.Srv[4].ACT_POSITION;
    SrvState[4].VELOCITY := Robot.Srv[4].ACT_VELOCITY;
    SrvState[4].TORQUE := Robot.Srv[4].ACT_TORQUE;

    SrvState[5].ENABLED := Robot.Srv[5].READY;
    SrvState[5].FAULT := Robot.Srv[5].FAULT;
    SrvState[5].IN_POSITION := Robot.Srv[5].IN_POSITION;
    SrvState[5].POSITION := Robot.Srv[5].ACT_POSITION;
    SrvState[5].VELOCITY := Robot.Srv[5].ACT_VELOCITY;
    SrvState[5].TORQUE := Robot.Srv[5].ACT_TORQUE;

    (* ─── 周期统计 ─── *)
    TotalCycles := TotalCycles + 1;
    CycleTime := CycleTimeMs;
    IF CycleTimeMs > MaxCycleTime THEN
        MaxCycleTime := CycleTimeMs;
    END_IF;

    Motion.STATE := Robot.STATE_OUT;
    Motion.PROGRESS := Robot.Planner.PROGRESS;
    CurrentStep := Robot.STEP_NUM;
    HomeDone := Robot.HOME_DONE;
    ProgramRunning := Robot.STATE_OUT = 2;
    RobotEnabled := Robot.STATE_OUT <> 0;
    RobotRun := ProgramRunning;
    RobotError := Robot.FAULT;
    RobotHome := HomeDone;
END_PROGRAM
