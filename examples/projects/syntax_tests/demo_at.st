PROGRAM DEMO
  VAR_INPUT
    Start AT %IX0.0 : BOOL;
    Stop AT %IX0.1 : BOOL;
    SpeedRef AT %IW2 : INT;
  END_VAR
  VAR_OUTPUT
    Run AT %QX0.0 : BOOL;
    SpeedOut AT %QW4 : INT;
  END_VAR
  VAR
    Counter : INT;
    Temp AT %IW6 : INT;
  END_VAR
    Counter := Counter + 1;
    IF Start AND NOT Stop THEN
        Run := TRUE;
        SpeedOut := SpeedRef;
    ELSE
        Run := FALSE;
        SpeedOut := 0;
    END_IF;
    SpeedOut := SpeedRef + Temp;
END_PROGRAM
