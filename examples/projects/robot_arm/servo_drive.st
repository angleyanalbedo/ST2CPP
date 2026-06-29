(* servo_drive.st — 单轴伺服驱动器控制功能块 *)

FUNCTION_BLOCK SERVO_CTRL
    VAR_INPUT
        AXIS_ID         : INT;          (* 轴号 0-5 *)
        CMD_POSITION    : DINT;         (* 命令位置 *)
        CMD_MODE        : INT;          (* 0=位置, 1=速度, 2=扭矩 *)
        ENABLE          : BOOL;         (* 使能 *)
        HOME_CMD        : BOOL;         (* 回原点命令 *)
        RESET_CMD       : BOOL;         (* 故障复位 *)
        VEL_LIMIT      : DINT;         (* 速度限制 *)
        TORQ_LIMIT     : INT;          (* 扭矩限制 *)
    END_VAR
    VAR_OUTPUT
        ACT_POSITION    : DINT;         (* 实际位置 *)
        ACT_VELOCITY    : DINT;         (* 实际速度 *)
        ACT_TORQUE      : INT;          (* 实际扭矩 *)
        IN_POSITION     : BOOL;         (* 到位 *)
        READY           : BOOL;         (* 准备就绪 *)
        FAULT           : BOOL;         (* 故障 *)
        HOMED           : BOOL;         (* 已回原点 *)
        ERROR_CODE      : WORD;         (* 故障码 *)
    END_VAR
    VAR
        STATE           : INT := 0;     (* 0=Disabled, 1=Enabling, 2=Enabled, 3=Fault *)
        CMD_CTRL        : WORD;
        IN_POS_COUNT    : INT := 0;
        HOME_STATE      : INT := 0;
        PREV_RESET      : BOOL := FALSE;
        TRIG_RESET      : BOOL;
        FAULT_LATCH     : BOOL := FALSE;
        READY_DELAY     : INT := 0;
    END_VAR

    (* ─── 读取硬件状态 ─── *)
    IF AXIS_ID = 0 THEN
        ACT_POSITION := Srv1ActPos;
        ACT_VELOCITY := Srv1ActVel;
        ACT_TORQUE := Srv1ActTorq;
        ERROR_CODE := Srv1Status;
    ELSIF AXIS_ID = 1 THEN
        ACT_POSITION := Srv2ActPos;
        ACT_VELOCITY := Srv2ActVel;
        ACT_TORQUE := Srv2ActTorq;
        ERROR_CODE := Srv2Status;
    ELSIF AXIS_ID = 2 THEN
        ACT_POSITION := Srv3ActPos;
        ACT_VELOCITY := Srv3ActVel;
        ACT_TORQUE := Srv3ActTorq;
        ERROR_CODE := Srv3Status;
    ELSIF AXIS_ID = 3 THEN
        ACT_POSITION := Srv4ActPos;
        ACT_VELOCITY := Srv4ActVel;
        ACT_TORQUE := Srv4ActTorq;
        ERROR_CODE := Srv4Status;
    ELSIF AXIS_ID = 4 THEN
        ACT_POSITION := Srv5ActPos;
        ACT_VELOCITY := Srv5ActVel;
        ACT_TORQUE := Srv5ActTorq;
        ERROR_CODE := Srv5Status;
    ELSE
        ACT_POSITION := Srv6ActPos;
        ACT_VELOCITY := Srv6ActVel;
        ACT_TORQUE := Srv6ActTorq;
        ERROR_CODE := Srv6Status;
    END_IF;

    (* ─── 故障检测 ─── *)
    IF ERROR_CODE <> 0 THEN
        FAULT := TRUE;
        FAULT_LATCH := TRUE;
        STATE := 3;
    END_IF;

    (* ─── 故障复位 ─── *)
    TRIG_RESET := RESET_CMD AND NOT PREV_RESET;
    PREV_RESET := RESET_CMD;
    IF TRIG_RESET AND FAULT_LATCH THEN
        FAULT_LATCH := FALSE;
        FAULT := FALSE;
        ERROR_CODE := 0;
        STATE := 0;
    END_IF;

    (* ─── 状态机 ─── *)
    CASE STATE OF
        0:
            (* Disabled *)
            READY := FALSE;
            HOMED := FALSE;
            CMD_CTRL := 0;
            IF ENABLE AND NOT FAULT_LATCH THEN
                STATE := 1;
                READY_DELAY := 0;
            END_IF;

        1:
            (* Enabling — 等待驱动器就绪 *)
            READY_DELAY := READY_DELAY + 1;
            CMD_CTRL := 16#0007;       (* 使能 + 位置模式 *)
            IF READY_DELAY >= 10 THEN
                STATE := 2;
                READY := TRUE;
            END_IF;

        2:
            (* Enabled *)
            READY := TRUE;
            CMD_CTRL := 16#000F;       (* 使能 + 位置模式 + 运行 *)

            IF NOT ENABLE THEN
                STATE := 0;
                READY := FALSE;
            END_IF;

            (* 回原点 *)
            IF HOME_CMD THEN
                HOME_STATE := 1;
            END_IF;

            IF HOME_STATE = 1 THEN
                CMD_CTRL := 16#001F;   (* 回原点模式 *)
                IF HomeSwitch THEN
                    CMD_CTRL := 16#002F;   (* 原点锁定 *)
                    HOME_STATE := 2;
                END_IF;
            ELSIF HOME_STATE = 2 THEN
                CMD_CTRL := 16#002F;
                HOME_STATE := 3;
            ELSIF HOME_STATE = 3 THEN
                HOMED := TRUE;
                HOME_STATE := 0;
            END_IF;

        3:
            (* Fault *)
            READY := FALSE;
            CMD_CTRL := 0;
            IF NOT FAULT_LATCH THEN
                STATE := 0;
            END_IF;
    END_CASE;

    (* ─── 到位判断 ─── *)
    IF ABS_DINT(ACT_POSITION - CMD_POSITION) < 100 THEN
        IN_POS_COUNT := IN_POS_COUNT + 1;
        IF IN_POS_COUNT >= 5 THEN
            IN_POSITION := TRUE;
        END_IF;
    ELSE
        IN_POS_COUNT := 0;
        IN_POSITION := FALSE;
    END_IF;

    (* ─── 写入控制字和命令 ─── *)
    IF AXIS_ID = 0 THEN
        Srv1Ctrl := CMD_CTRL;
        Srv1CmdPos := CMD_POSITION;
    ELSIF AXIS_ID = 1 THEN
        Srv2Ctrl := CMD_CTRL;
        Srv2CmdPos := CMD_POSITION;
    ELSIF AXIS_ID = 2 THEN
        Srv3Ctrl := CMD_CTRL;
        Srv3CmdPos := CMD_POSITION;
    ELSIF AXIS_ID = 3 THEN
        Srv4Ctrl := CMD_CTRL;
        Srv4CmdPos := CMD_POSITION;
    ELSIF AXIS_ID = 4 THEN
        Srv5Ctrl := CMD_CTRL;
        Srv5CmdPos := CMD_POSITION;
    ELSE
        Srv6Ctrl := CMD_CTRL;
        Srv6CmdPos := CMD_POSITION;
    END_IF;
END_FUNCTION_BLOCK
