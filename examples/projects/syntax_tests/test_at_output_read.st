PROGRAM IO_READBACK
  VAR_INPUT
    Cmd AT %IX0.0 : BOOL;
  END_VAR
  VAR_OUTPUT
    OutVal AT %QW2 : INT;
    OutFlag AT %QX0.1 : BOOL;
  END_VAR
  VAR
    Readback : INT;
    FlagCopy : BOOL;
  END_VAR
    OutVal := 42;
    OutFlag := Cmd;
    Readback := OutVal;
    FlagCopy := OutFlag;
END_PROGRAM
