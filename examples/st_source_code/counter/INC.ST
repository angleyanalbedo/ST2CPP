FUNCTION INC:INT
	VAR_INPUT
		X :	INT;
		D :	INT;
		M :	INT;
	END_VAR

	inc := (X + D + M + 1) MOD (M + 1);
END_FUNCTION