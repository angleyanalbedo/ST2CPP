FUNCTION WINDOW:BOOL
	VAR_INPUT
		LOW :	REAL;
		IN :	REAL;
		HIGH :	REAL;
	END_VAR
	window := (in < high) AND (in > low);

END_FUNCTION