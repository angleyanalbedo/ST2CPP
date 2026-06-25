TYPE
  oscat_NW_BUF_LONG  : ARRAY [0..4095] OF BYTE;
END_TYPE

FUNCTION_BLOCK _BUFFER_CLEAR_NW
	VAR_INPUT
		SIZE :	UINT;
	END_VAR

	VAR_OUTPUT
		BUFFER_CLEAR :	BOOL;
	END_VAR

	VAR_IN_OUT
		PT :	oscat_NW_BUF_LONG;
	END_VAR

	VAR
		err :	INT;
		result :	WORD;
	END_VAR
	result:=MEMSET(err,BYTE#00,UINT_TO_DINT(size),pt[0]);
	BUFFER_CLEAR := (err = 0);
END_FUNCTION_BLOCK

PROGRAM program0
  VAR
	SIZE :	UINT;

	BUFFER_CLEAR :	BOOL;

	PT :	oscat_NW_BUF_LONG;

	func_block : _BUFFER_CLEAR_NW;
  END_VAR
	func_block.SIZE  :=SIZE ;
	func_block.PT  :=PT ;
	func_block();
	PT := func_block.PT;
	BUFFER_CLEAR := func_block.BUFFER_CLEAR;
END_PROGRAM