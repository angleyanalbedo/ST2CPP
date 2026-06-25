FUNCTION_BLOCK IS_NUM_Block

	VAR_INPUT
		STR :	STRING;
	END_VAR

	VAR_OUTPUT
		IS_NUM :	BOOL;
	END_VAR

	VAR
		pos :	INT;
		I :	INT;
	END_VAR

	I := LEN(str);
	FOR pos := 1 TO I DO
		IF NOT ISC_NUM(INT_TO_BYTE(GET_CHAR(str,pos))) THEN
			IS_NUM := FALSE;
			RETURN;
		END_IF;
	END_FOR;
	IS_NUM := I>0;

END_FUNCTION_BLOCK

PROGRAM program0

	VAR
		STR :	STRING;
		IS_NUM :	BOOL;
		func_block : IS_NUM_Block;
	END_VAR
	func_block.STR := STR;
	func_block();
	IS_NUM := func_block.IS_NUM;

END_PROGRAM