(* io_config.st — HVAC 全局 I/O 映射与配置 *)

VAR_GLOBAL
    (* ─── AI 模拟量输入（12 位 ADC，0-27648 对应 0-10V） ─── *)
    Zone1TempRaw AT %IW0  : INT;
    Zone2TempRaw AT %IW2  : INT;
    Zone3TempRaw AT %IW4  : INT;
    Zone4TempRaw AT %IW6  : INT;
    SupplyTempRaw AT %IW8  : INT;
    ReturnTempRaw AT %IW10 : INT;
    OutsideTempRaw AT %IW12 : INT;
    Zone1HumRaw AT %IW14  : INT;

    (* ─── DI 数字量输入 ─── *)
    StartPB AT %IX0.0   : BOOL;
    StopPB  AT %IX0.1   : BOOL;
    EStop   AT %IX0.2   : BOOL;
    Zone1Occ AT %IX0.3  : BOOL;
    Zone2Occ AT %IX0.4  : BOOL;
    Zone3Occ AT %IX0.5  : BOOL;
    Zone4Occ AT %IX0.6  : BOOL;
    FilterOk AT %IX0.7  : BOOL;

    (* ─── AO 模拟量输出 ─── *)
    Zone1Valve AT %QW0  : INT;
    Zone2Valve AT %QW2  : INT;
    Zone3Valve AT %QW4  : INT;
    Zone4Valve AT %QW6  : INT;
    SupplyValve AT %QW8 : INT;

    (* ─── DO 数字量输出 ─── *)
    SysRun    AT %QX0.0 : BOOL;
    SysAlarm  AT %QX0.1 : BOOL;
    Zone1Heat AT %QX0.2 : BOOL;
    Zone1Cool AT %QX0.3 : BOOL;
    Zone1Fan  AT %QX0.4 : BOOL;
    Zone2Heat AT %QX0.5 : BOOL;
    Zone2Cool AT %QX0.6 : BOOL;
    Zone2Fan  AT %QX0.7 : BOOL;

    (* ─── 全局变量 ─── *)
    SysEnabled    : BOOL := TRUE;
    CycleCount    : DINT := 0;
    GlobalAlarm   : BOOL := FALSE;
    AverageTemp   : REAL := 0.0;
    TotalEnabled  : INT := 0;

    (* ─── 配置常量 ─── *)
    SCALE_10V   : SCALE_RANGE := (RAW_MIN := 0, RAW_MAX := 27648, ENG_MIN := 0.0, ENG_MAX := 100.0);
    SCALE_TEMP  : SCALE_RANGE := (RAW_MIN := 0, RAW_MAX := 27648, ENG_MIN := -40.0, ENG_MAX := 120.0);
    SCALE_HUM   : SCALE_RANGE := (RAW_MIN := 0, RAW_MAX := 27648, ENG_MIN := 0.0, ENG_MAX := 100.0);
END_VAR
