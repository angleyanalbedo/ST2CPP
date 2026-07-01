(* smart_factory/line_controller.st — 产线控制器 CLASS (继承 BASE_CONTROLLER) *)

CLASS LINE_CONTROLLER EXTENDS BASE_CONTROLLER
    VAR
        LINE_ID       : INT := 0;
        PROD_COUNT    : DINT := 0;
        FAULT_ACTIVE  : BOOL := FALSE;
        FAULT_CODE    : INT := 0;
    END_VAR

    METHOD INIT_LINE : BOOL
        (* 调用父类启动 *)
        SUPER.STARTUP();
        LINE_ID := 1;
        PROD_COUNT := 0;
        FAULT_ACTIVE := FALSE;
        FAULT_CODE := 0;
        INIT_LINE := TRUE;
    END_METHOD

    METHOD PROD_CYCLE : DINT
        IF RUN_ENABLE AND NOT FAULT_ACTIVE THEN
            PROD_COUNT := PROD_COUNT + 1;
        END_IF
        PROD_CYCLE := PROD_COUNT;
    END_METHOD

    METHOD RAISE_FAULT : BOOL
        FAULT_ACTIVE := TRUE;
        FAULT_CODE := 99;
        RUN_ENABLE := FALSE;
        RAISE_FAULT := TRUE;
    END_METHOD

    METHOD GET_INFO : DINT
        GET_INFO := PROD_COUNT;
    END_METHOD
END_CLASS
