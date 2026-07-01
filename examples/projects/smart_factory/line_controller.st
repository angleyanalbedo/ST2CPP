(* smart_factory/line_controller.st — 产线控制器 CLASS *)

CLASS LINE_CONTROLLER
    VAR
        LINE_ID       : INT := 0;
        PROD_COUNT    : DINT := 0;
        RUN_ENABLE    : BOOL := FALSE;
    END_VAR

    METHOD INIT_LINE : BOOL
        RUN_ENABLE := TRUE;
        LINE_ID := 1;
        PROD_COUNT := 0;
        INIT_LINE := TRUE;
    END_METHOD

    METHOD PROD_CYCLE : DINT
        IF RUN_ENABLE THEN
            PROD_COUNT := PROD_COUNT + 1;
        END_IF
        PROD_CYCLE := PROD_COUNT;
    END_METHOD
END_CLASS
