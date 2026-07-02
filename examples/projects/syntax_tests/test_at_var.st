PROGRAM AT_VAR
  VAR
    MyInput AT %IW2 : INT;
    MyOutput AT %QW1 : INT;
    MyFlag AT %QX0.5 : BOOL;
  END_VAR
    MyOutput := MyInput * 2;
    IF MyInput > 100 THEN
        MyFlag := TRUE;
    ELSE
        MyFlag := FALSE;
    END_IF;
END_PROGRAM
