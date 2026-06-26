TYPE MyStruct :
STRUCT
    X : INT;
    Y : INT;
END_STRUCT
END_TYPE

PROGRAM AT_STRUCT
  VAR_INPUT
    Raw AT %ID0 : MyStruct;
  END_VAR
  VAR_OUTPUT
    Out AT %QD0 : MyStruct;
  END_VAR
  VAR
    LocalX : INT;
  END_VAR
    LocalX := Raw.X;
    Out.Y := Raw.X + 1;
END_PROGRAM
