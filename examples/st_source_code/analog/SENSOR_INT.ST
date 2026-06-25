FUNCTION SENSOR_INT:REAL

	VAR_INPUT
		VOLTAGE :	REAL;
		CURRENT :	REAL;
		RP :	REAL;
		_RS :	REAL;
	END_VAR

	VAR
		RG :	REAL;
	END_VAR

	IF current <> 0.0 THEN
		RG := voltage / current;
		SENSOR_INT := RP * ( RG - _RS) / ( RP + _RS - RG);
	END_IF;

END_FUNCTION