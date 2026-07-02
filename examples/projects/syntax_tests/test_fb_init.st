TYPE
    JOINT_WAYPOINT :
    STRUCT
        J1      : REAL;
        J2      : REAL;
        SPEED   : REAL;
        DWELL_MS : INT;
    END_STRUCT;
END_TYPE

FUNCTION_BLOCK MY_FB
    VAR_INPUT
        CMD : INT;
    END_VAR
    VAR_OUTPUT
        STATE : INT;
    END_VAR
    VAR
        POS1 : JOINT_WAYPOINT := (J1 := 1.0, J2 := -2.0, SPEED := 10.0, DWELL_MS := 500);
        POS2 : JOINT_WAYPOINT := (J1 := 3.0, J2 := -4.0, SPEED := 20.0, DWELL_MS := 100);
        COUNT : INT := 0;
    END_VAR

    COUNT := COUNT + 1;
END_FUNCTION_BLOCK

PROGRAM TEST_INIT
    VAR
        FB1 : MY_FB;
    END_VAR
END_PROGRAM
