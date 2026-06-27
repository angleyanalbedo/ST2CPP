(* main.st — HVAC 主控程序 *)

PROGRAM HVAC_MAIN
    VAR_INPUT
        Start AT %IX1.0  : BOOL;
        Stop  AT %IX1.1  : BOOL;
    END_VAR
    VAR_OUTPUT
        Running AT %QX1.0 : BOOL;
        Fault   AT %QX1.1 : BOOL;
    END_VAR
    VAR
        (* FB 实例 *)
        Z1          : ZONE_CTRL;
        Z2          : ZONE_CTRL;
        Z3          : ZONE_CTRL;
        Z4          : ZONE_CTRL;

        (* 设定值 *)
        HeatSet     : REAL := 22.0;
        CoolSet     : REAL := 24.0;
        EconSet     : REAL := 21.0;

        (* 循环变量 *)
        I           : INT := 0;
        J           : INT := 0;

        (* 运行统计 *)
        ZonesOn     : INT := 0;
        TotalOutput : REAL := 0.0;
        MaxTemp     : REAL := -999.0;
        MinTemp     : REAL := 999.0;
        AnyAlarm    : BOOL := FALSE;

        (* PID 参数 *)
        PID_HOT     : PID_PARAMS := (KP := 2.0, KI := 0.5, KD := 0.1, TS := 1.0, MIN := 0.0, MAX := 100.0);
        PID_COLD    : PID_PARAMS := (KP := 1.8, KI := 0.3, KD := 0.15, TS := 1.0, MIN := -100.0, MAX := 0.0);

        (* 状态 *)
        SysMode     : INT := 0;
        OverTemp    : BOOL := FALSE;
        UnderTemp   : BOOL := FALSE;

        (* 测试用 *)
        TestVal     : INT := 0;
        LoopCount   : INT := 0;
        ResultCheck : BOOL := TRUE;
    END_VAR

    (* ─── 系统使能开关 ─── *)
    IF Start AND NOT Stop AND NOT EStop THEN
        SysEnabled := TRUE;
        Running := TRUE;
    ELSE
        SysEnabled := FALSE;
        Running := FALSE;
    END_IF;

    CycleCount := CycleCount + 1;

    (* ─── FOR 循环：处理每个区域 ─── *)
    FOR I := 0 TO 3 BY 1 DO
        IF I = 0 THEN
            (* 区域 1 *)
            Z1(
                RAW_TEMP := Zone1TempRaw,
                OCCUPIED := Zone1Occ,
                SETPOINT := HeatSet,
                ENABLE := SysEnabled,
                SCALE := SCALE_TEMP,
                AO_SCALE := SCALE_10V,
                PID_P := PID_HOT
            );
            Zone1Valve := Z1.VALVE_POS;
            Zone1Heat := Z1.HEAT_OUT;
            Zone1Cool := Z1.COOL_OUT;
            Zone1Fan := Z1.FAN_OUT;

            PRINT("Zone1 active");

            IF Z1.ZONE_ALARM THEN
                GlobalAlarm := TRUE;
            END_IF;

        ELSIF I = 1 THEN
            (* 区域 2 *)
            Z2(
                RAW_TEMP := Zone2TempRaw,
                OCCUPIED := Zone2Occ,
                SETPOINT := CoolSet,
                ENABLE := SysEnabled,
                SCALE := SCALE_TEMP,
                AO_SCALE := SCALE_10V,
                PID_P := PID_COLD
            );
            Zone2Valve := Z2.VALVE_POS;
            Zone2Heat := Z2.HEAT_OUT;
            Zone2Cool := Z2.COOL_OUT;
            Zone2Fan := Z2.FAN_OUT;

        ELSIF I = 2 THEN
            (* 区域 3 *)
            Z3(
                RAW_TEMP := Zone3TempRaw,
                OCCUPIED := Zone3Occ,
                SETPOINT := EconSet,
                ENABLE := SysEnabled,
                SCALE := SCALE_TEMP,
                AO_SCALE := SCALE_10V,
                PID_P := PID_HOT
            );
            Zone3Valve := Z3.VALVE_POS;

        ELSE
            (* 区域 4 *)
            Z4(
                RAW_TEMP := Zone4TempRaw,
                OCCUPIED := Zone4Occ,
                SETPOINT := HeatSet,
                ENABLE := SysEnabled,
                SCALE := SCALE_TEMP,
                AO_SCALE := SCALE_10V,
                PID_P := PID_HOT
            );
            Zone4Valve := Z4.VALVE_POS;
        END_IF;
    END_FOR;

    (* ─── WHILE 循环：统计加热区域数 ─── *)
    J := 0;
    ZonesOn := 0;
    WHILE J <= 3 DO
        IF J = 0 AND Z1.HEAT_OUT THEN
            ZonesOn := ZonesOn + 1;
        ELSIF J = 1 AND Z2.HEAT_OUT THEN
            ZonesOn := ZonesOn + 1;
        ELSIF J = 2 AND Z3.HEAT_OUT THEN
            ZonesOn := ZonesOn + 1;
        ELSIF J = 3 AND Z4.HEAT_OUT THEN
            ZonesOn := ZonesOn + 1;
        END_IF;
        J := J + 1;
    END_WHILE;

    (* ─── REPEAT 循环：计算平均温度 ─── *)
    TotalOutput := 0.0;
    I := 0;
    REPEAT
        TotalOutput := TotalOutput + 25.0;
        I := I + 1;
    UNTIL I >= 4
    END_REPEAT;

    AverageTemp := TotalOutput / 4.0;

    (* ─── CASE 系统模式选择 ─── *)
    SysMode := 0;
    IF Z1.HEAT_OUT OR Z2.HEAT_OUT THEN
        SysMode := 1;
    END_IF;
    IF Z1.COOL_OUT OR Z2.COOL_OUT THEN
        SysMode := 2;
    END_IF;

    CASE SysMode OF
        1:
            SysRun := TRUE;
        2:
            SysRun := TRUE;
        ELSE
            SysRun := ZonesOn > 0;
    END_CASE;

    (* ─── 告警汇总逻辑 ─── *)
    AnyAlarm := Z1.ZONE_ALARM OR Z2.ZONE_ALARM OR Z3.ZONE_ALARM OR Z4.ZONE_ALARM;
    SysAlarm := AnyAlarm;
    Fault := AnyAlarm;

    (* ─── EXIT/CONTINUE 测试 ─── *)
    TestVal := 0;
    FOR I := 0 TO 10 BY 1 DO
        IF I = 5 THEN
            CONTINUE;
        END_IF;
        TestVal := TestVal + 1;
        IF I > 8 THEN
            EXIT;
        END_IF;
    END_FOR;

    (* ─── 复杂表达式测试 ─── *)
    OverTemp := (Z1.STATE.TEMP > 50.0 AND Z1.STATE.TEMP <> -999.0) OR Z1.ZONE_ALARM;
    UnderTemp := Z1.STATE.TEMP < -5.0 AND NOT Z1.ZONE_ALARM;

    TotalEnabled := 0;
    IF Zone1Occ THEN TotalEnabled := TotalEnabled + 1; END_IF;
    IF Zone2Occ THEN TotalEnabled := TotalEnabled + 1; END_IF;
    IF Zone3Occ THEN TotalEnabled := TotalEnabled + 1; END_IF;
    IF Zone4Occ THEN TotalEnabled := TotalEnabled + 1; END_IF;

    LoopCount := I * 10 + J;
    ResultCheck := LoopCount > 0 AND TestVal = 9;

    (* ─── 集成断言验证 ─── *)
    PRINT("HVAC cycle complete");
    ASSERT(LoopCount >= 0);
    ASSERT(SysEnabled OR NOT Running);
    ASSERT(ResultCheck OR AnyAlarm);
END_PROGRAM
