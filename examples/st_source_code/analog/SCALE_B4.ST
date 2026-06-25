FUNCTION _BYTE_TO_REAL:REAL
	VAR_INPUT
		IN :	BYTE;
	END_VAR

	_BYTE_TO_REAL := USINT_TO_REAL(BYTE_TO_USINT(IN));
END_FUNCTION

FUNCTION SCALE_B4:REAL

	VAR_INPUT
		IN1 :	BYTE;
		IN2 :	BYTE;
		IN3 :	BYTE;
		IN4 :	BYTE;
		K :	REAL;
		O :	REAL;
		IN1_MIN :	REAL;
		IN1_MAX :	REAL;
		IN2_MIN :	REAL;
		IN2_MAX :	REAL;
		IN3_MIN :	REAL;
		IN3_MAX :	REAL;
		IN4_MIN :	REAL;
		IN4_MAX :	REAL;
	END_VAR

	SCALE_B4 := 	(((in1_max - in1_min)* _BYTE_TO_REAL(in1) + (in2_max - in2_min)* _BYTE_TO_REAL(in2) + (in3_max - in3_min)* _BYTE_TO_REAL(in3) + (in4_max - in4_min)* _BYTE_TO_REAL(in4))* 0.003921569 + in1_min + in2_min + in3_min + in4_min) * K + O;

END_FUNCTION