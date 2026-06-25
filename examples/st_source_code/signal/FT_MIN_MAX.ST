FUNCTION_BLOCK FT_MIN_MAX_Block

  VAR_INPUT
    IN :	REAL;
    RST :	BOOL;
  END_VAR


  VAR_OUTPUT
    MX :	REAL;
    MN :	REAL;
  END_VAR


  VAR
    init :	BOOL;
  END_VAR

  IF (rst = TRUE) OR (init = FALSE) THEN
    mn := in;
    mx := in;
    init := TRUE;
  ELSIF (in < mn) THEN
    mn := in;
  ELSIF (in > mx) THEN
    mx := in;
  END_IF;

END_FUNCTION_BLOCK

PROGRAM program0
  VAR
    IN :	REAL;
    RST :	BOOL;

	  MX :	REAL;
    MN :	REAL;

  	func_block : FT_MIN_MAX_Block;
  END_VAR

	func_block.IN :=IN;
	func_block.RST := RST;
	func_block();
	MX := func_block.MX;
	MN := func_block.MN;

END_PROGRAM