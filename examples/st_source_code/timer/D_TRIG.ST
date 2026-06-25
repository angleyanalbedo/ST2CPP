FUNCTION_BLOCK D_TRIG
	VAR_INPUT
		IN :	DWORD;
	END_VAR

	VAR_OUTPUT
		Q :	BOOL;
		X :	DWORD;
	END_VAR

	VAR
		last_in :	DWORD;
	END_VAR

	Q := in <> last_in;
	X := UDINT_TO_DWORD(DWORD_TO_UDINT(in) - DWORD_TO_UDINT(last_in));
	last_in := in;
END_FUNCTION_BLOCK

PROGRAM program0
  VAR
	IN :	DWORD;

	Q :	BOOL;
	X :	DWORD;

	func_block : D_TRIG;
  END_VAR
	func_block.IN  := IN;
	func_block();
	Q := func_block.Q;
	X := func_block.X;
END_PROGRAM