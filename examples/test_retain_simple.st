PROGRAM SIMPLE_RETAIN
  VAR
    Counter : INT := 0;
  END_VAR
  VAR RETAIN
    SavedVal : DINT;
  END_VAR

  Counter := Counter + 1;
  SavedVal := SavedVal + Counter;
END_PROGRAM
