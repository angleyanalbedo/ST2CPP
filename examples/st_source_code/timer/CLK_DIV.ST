FUNCTION BIT_OF_DWORD : BOOL
  VAR_INPUT
    IN : DWORD;
    N : INT;
  END_VAR

  BIT_OF_DWORD := (SHR(in,N) AND 16#00000001) > 0;
  (* from OSCAT library; www.oscat.de  *)
END_FUNCTION

FUNCTION_BLOCK CLK_DIV
	VAR_INPUT
		CLK :	BOOL;
		RST :	BOOL;
	END_VAR

	VAR_OUTPUT
		Q0 :	BOOL := FALSE;
		Q1 :	BOOL := FALSE;
		Q2 :	BOOL := FALSE;
		Q3 :	BOOL := FALSE;
		Q4 :	BOOL := FALSE;
		Q5 :	BOOL := FALSE;
		Q6 :	BOOL := FALSE;
		Q7 :	BOOL := FALSE;
	END_VAR

	VAR
		cnt :	BYTE := 0;
	END_VAR

	IF rst THEN
		cnt:= BYTE#0;
		Q0 := FALSE;
		Q1 := FALSE;
		Q2 := FALSE;
		Q3 := FALSE;
		Q4 := FALSE;
		Q5 := FALSE;
		Q6 := FALSE;
		Q7 := FALSE;
	ELSIF clk THEN
		cnt:= USINT_TO_BYTE(BYTE_TO_USINT(cnt) + USINT#1);

		Q0 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),0); (* Q0 := cnt.X0; *)
		Q1 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),1); (* Q1 := cnt.X1; *)
		Q2 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),2); (* Q2 := cnt.X2; *)
		Q3 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),3); (* Q3 := cnt.X3; *)
		Q4 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),4); (* Q4 := cnt.X4; *)
		Q5 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),5); (* Q5 := cnt.X5; *)
		Q6 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),6); (* Q6 := cnt.X6; *)
		Q7 := BIT_OF_DWORD(BYTE_TO_DWORD(cnt),7); (* Q7 := cnt.X7; *)

	END_IF;

END_FUNCTION_BLOCK