TYPE
	oscat_pt_ARRAY: ARRAY [1..100] OF REAL;
END_TYPE

FUNCTION_BLOCK _ARRAY_INIT2

	VAR_IN_OUT
		PT :	oscat_pt_ARRAY;
	END_VAR

	VAR_INPUT
		SIZE :	UINT;
		INIT :	REAL;
	END_VAR

	VAR_OUTPUT
		_ARRAY_INIT:	BOOL;
	END_VAR

	VAR
		i :	INT;
		stop :	INT;
	END_VAR

	stop :=LIMIT_INT(1,UINT_TO_INT(size),1000);

	FOR i := 1 TO stop DO
		pt[i] := init;
	END_FOR;
	_ARRAY_INIT := TRUE;

END_FUNCTION_BLOCK

PROGRAM program0
  VAR
    LocalVar0 : oscat_pt_ARRAY;
    LocalVar1 : UINT;
	LocalVar2 : REAL;
	LocalOUT  : BOOL;
	func_block: _ARRAY_INIT2;
  END_VAR
  func_block.pt := LocalVar0;
  func_block.size := LocalVar1;
  func_block.init := LocalVar2;
  func_block();
  LocalOUT := func_block._ARRAY_INIT;
END_PROGRAM