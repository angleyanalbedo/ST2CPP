FUNCTION GAUSS:REAL

	VAR_INPUT
		X :	REAL;
		U :	REAL;
		SI :	REAL;
	END_VAR


	VAR
		temp :	REAL;
		si_inv :	REAL;
	END_VAR

	temp := X - U;
	si_inv := 1.0  / si;
	GAUSS := EXP(Temp * Temp * si_inv * SI_inv * - 0.5) * 0.39894228 * si_inv;

END_FUNCTION