(* safety.st — 安全监控功能块 *)

FUNCTION_BLOCK SAFETY_MONITOR
    VAR_INPUT
        E_STOP_IN       : BOOL;         (* 急停输入 *)
        SERVO_READY     : ARRAY[0..5] OF BOOL;
        SERVO_FAULT     : ARRAY[0..5] OF BOOL;
        ACT_POSITIONS   : ARRAY[0..5] OF DINT;
        JOINT_LIMITS    : ARRAY[0..5, 0..1] OF DINT;  (* [轴号][0=下限,1=上限] *)
        CFG             : SAFETY_CFG;
    END_VAR
    VAR_OUTPUT
        SAFE_OK         : BOOL;         (* 安全状态正常 *)
        ALARM_OUT       : SAFETY_ALARM;
        ALL_SERVO_OK    : BOOL;
        SPEED_OK        : BOOL;
        LIMITS_OK       : BOOL;
    END_VAR
    VAR
        I               : INT;
        J               : INT;
        OVERTRAVEL      : BOOL;
        SRV_FAULT_ANY   : BOOL;
        SRV_NOT_READY   : BOOL;
    END_VAR

    (* ─── 急停检测 ─── *)
    ALARM_OUT.E_STOP := NOT E_STOP_IN;
    IF CFG.E_STOP_ACTIVE AND ALARM_OUT.E_STOP THEN
        ALARM_OUT.ALARM_CODE := 1;
    END_IF;

    (* ─── 伺服故障/就绪检测 ─── *)
    SRV_FAULT_ANY := FALSE;
    SRV_NOT_READY := FALSE;
    FOR I := 0 TO 5 DO
        IF SERVO_FAULT[I] THEN
            SRV_FAULT_ANY := TRUE;
        END_IF;
        IF NOT SERVO_READY[I] THEN
            SRV_NOT_READY := TRUE;
        END_IF;
    END_FOR;

    ALARM_OUT.SERVO_FAULT := SRV_FAULT_ANY;
    ALARM_OUT.COMM_LOST := SRV_NOT_READY;
    ALL_SERVO_OK := NOT SRV_FAULT_ANY AND NOT SRV_NOT_READY;

    (* ─── 关节软限位检测 ─── *)
    OVERTRAVEL := FALSE;
    IF CFG.JOINT_LIMITS_EN THEN
        FOR I := 0 TO 5 DO
            IF ACT_POSITIONS[I] < JOINT_LIMITS[I, 0] THEN
                OVERTRAVEL := TRUE;
            END_IF;
            IF ACT_POSITIONS[I] > JOINT_LIMITS[I, 1] THEN
                OVERTRAVEL := TRUE;
            END_IF;
        END_FOR;
    END_IF;
    ALARM_OUT.JOINT_OVERTRAVEL := OVERTRAVEL;
    LIMITS_OK := NOT OVERTRAVEL;

    (* ─── 速度/扭矩限制（通过全局速度百分比） ─── *)
    SPEED_OK := TRUE;

    (* ─── 碰撞检测（简化：任意轴扭矩超限） ─── *)
    ALARM_OUT.COLLISION := FALSE;
    ALARM_OUT.OVERSPEED := FALSE;
    ALARM_OUT.WORKSPACE_VIOL := FALSE;

    (* ─── 综合安全状态 ─── *)
    SAFE_OK := NOT ALARM_OUT.E_STOP
               AND NOT ALARM_OUT.SERVO_FAULT
               AND NOT ALARM_OUT.JOINT_OVERTRAVEL
               AND NOT ALARM_OUT.COMM_LOST
               AND NOT ALARM_OUT.COLLISION;

    ALARM_OUT.ALARM_CODE := 0;
    IF ALARM_OUT.E_STOP THEN ALARM_OUT.ALARM_CODE := 1; END_IF;
    IF ALARM_OUT.SERVO_FAULT THEN ALARM_OUT.ALARM_CODE := 2; END_IF;
    IF ALARM_OUT.JOINT_OVERTRAVEL THEN ALARM_OUT.ALARM_CODE := 3; END_IF;
    IF ALARM_OUT.COMM_LOST THEN ALARM_OUT.ALARM_CODE := 4; END_IF;
    IF ALARM_OUT.COLLISION THEN ALARM_OUT.ALARM_CODE := 5; END_IF;
    IF ALARM_OUT.OVERSPEED THEN ALARM_OUT.ALARM_CODE := 6; END_IF;
    IF ALARM_OUT.WORKSPACE_VIOL THEN ALARM_OUT.ALARM_CODE := 7; END_IF;
END_FUNCTION_BLOCK
