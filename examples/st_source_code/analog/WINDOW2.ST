FUNCTION WINDOW2:BOOL
	VAR_INPUT
		LOW :	REAL;
		IN :	REAL;
		HIGH :	REAL;
	END_VAR
	window2 := (in <= high) AND (in >= low);
END_FUNCTION