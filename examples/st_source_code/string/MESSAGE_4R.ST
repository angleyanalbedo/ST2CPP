FUNCTION INC1:INT

	VAR_INPUT
		X :	INT;
		N :	INT;
	END_VAR

	IF X >= N - 1 THEN
		INC1 := 0;
	ELSE
		INC1 := X + 1;
	END_IF;
END_FUNCTION

FUNCTION_BLOCK MESSAGE_4R_Block

	VAR_INPUT
		M0 :	STRING;
		M1 :	STRING;
		M2 :	STRING;
		M3 :	STRING;
		MM :	INT := 3;
		ENQ :	BOOL := TRUE;
		CLK :	BOOL := TRUE;
		T1 :	TIME := T#3s;
	END_VAR


	VAR_OUTPUT
		MX :	STRING;
		MN :	INT;
		TR :	BOOL;
	END_VAR


	VAR
		timer :	TON;
		edge :	BOOL;
	END_VAR

	TR := FALSE;
	IF ENQ THEN
		IF (NOT edge AND clk) OR timer.q THEN
				MN := INC1(MN, MM);
				TR := TRUE;
				timer(in := FALSE);
				CASE MN OF
					0 : MX := M0;
					1 : MX := M1;
					2 : MX := M2;
					3 : MX := M3;
				END_CASE;
		END_IF;
		edge := clk;
		timer( in := CLK, pt := T1);
	ELSE
		MX := '';
		MN := 0;
	END_IF;

END_FUNCTION_BLOCK