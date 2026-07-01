(* smart_factory/base_controller.st — 基础控制器 CLASS *)

CLASS BASE_CONTROLLER
    VAR
        SYSTEM_STATE  : INT := 0;
        CYCLE_COUNT   : DINT := 0;
        RUN_ENABLE    : BOOL := FALSE;
    END_VAR

    METHOD STARTUP : BOOL
        RUN_ENABLE := TRUE;
        SYSTEM_STATE := 1;
        STARTUP := TRUE;
    END_METHOD

    METHOD SHUTDOWN : BOOL
        RUN_ENABLE := FALSE;
        SYSTEM_STATE := 0;
        SHUTDOWN := TRUE;
    END_METHOD

    METHOD GET_STATE : INT
        GET_STATE := SYSTEM_STATE;
    END_METHOD

    METHOD TICK
        IF RUN_ENABLE THEN
            CYCLE_COUNT := CYCLE_COUNT + 1;
        END_IF
    END_METHOD
END_CLASS
