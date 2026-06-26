PROGRAM IO_TEST
  VAR_INPUT
    BitInput AT %IX1.3 : BOOL;
    ByteInput AT %IB5 : USINT;
    WordInput AT %IW2 : INT;
    DWordInput AT %ID1 : DINT;
  END_VAR
  VAR_OUTPUT
    BitOutput AT %QX0.7 : BOOL;
    ByteOutput AT %QB10 : USINT;
    WordOutput AT %QW3 : INT;
    DWordOutput AT %QD2 : DINT;
  END_VAR
  VAR
    Temp : INT;
    Result : INT;
  END_VAR
    Temp := WordInput + 5;
    IF BitInput THEN
        BitOutput := TRUE;
    ELSE
        BitOutput := FALSE;
    END_IF;
    ByteOutput := ByteInput;
    WordOutput := Temp;
    DWordOutput := DWordInput * 2;
    Result := WordInput * 10;
END_PROGRAM
