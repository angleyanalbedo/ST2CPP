lexer grammar PLCSTLEXER;

Identifier
  : IdentifierStart IdentifierPart* ;


IdentifierStart
  : [A-Z$_]+ ;


IdentifierPart
  : [A-Z0-9$_]+ ;

Digit : '0'..'9';
Bit : '0'..'1';

Octal_Digit : '0'..'7';
Hex_Digit : '0'..'9' | 'A'..'F';
Comment : ('//' ~( '\n' | '\r' )* '\r' ? '\n' | '(*' .*? '*)' | '/*' .*? '*/') -> skip;
WS : ( ' ' | '\t' | '\r' | '\n' ) ->skip;
EOL : '\n';
Pragma : ('{' .*? '}')->skip;


//Common_Char_Value : ' ' | '!' | '#' | '%' | '&' | '('..'/' | '0'..'9' | ':'..'@' | 'A'..'Z' | '['..'`' | 'a'..'z' | '{'..'~'
//| '$$' | '$L' | '$N' | '$P' | '$R' | '$T';

//修改的char以及string词法识别部分
S_byte_char_value : (Common_Char_Value | Common_Char_Byte|Char_other|Char_S|Char_Blank);
D_byte_char_value: (Common_Char_Value | Common_Char_ByteD|Char_other|Char_S|Char_Blank);
Common_Char_Byte : '$' Hex_Digit Hex_Digit;
Common_Char_Value : (Char_Value | Char_doll)+;
Char_Value : ([A-Za-z])+;
Char_doll: '$$';
//Char_other: '!'|'#'|'%'|'&'|'('..'/'|[0-9]|':'..'@'|'['..'`'|'{'..'~'| '$\'' | '$'[Ll]|'$'[Nn]| '$'[Pp] | '$'[Rr] | '$'[Tt];

//转义符词法
Char_other: '!'|'#'|'%'|'&'|[0-9]| '$\'' | '$'[Ll]|'$'[Nn]| '$'[Pp] | '$'[Rr] | '$'[Tt]| '(' | ')';
Char_S :'"';
Char_Blank: ' ';


Common_Char_ByteD: '$' Hex_Digit Hex_Digit Hex_Digit Hex_Digit;





//以下词法部分存在于parser文件中

Date_Type_Name : Date_Type;
Date_Type : 'DATE' | 'LDATE';

Tod_Type_Name : Tod_Type_;
fragment Tod_Type_ : 'TIME_OF_DAY' | 'TOD' | 'LTOD';

DT_Type_Name:Date_Type;
fragment DT_Type : 'DATE_AND_TIME' | 'DT' | 'LDT';

Bool_Type_Name : 'BOOL';

Multibits_Type_Name: Multibits_Type;
fragment Multibits_Type : 'BYTE' | 'WORD' | 'DWORD' | 'LWORD';

Std_Func_Name : 'TRUNC' | 'ABS' | 'SQRT' | 'LN' | 'LOG' | 'EXP'
| 'SIN' | 'COS' | 'TAN' | 'ASIN' | 'ACOS' | 'ATAN' | 'ATAN2 '
| 'ADD' | 'SUB' | 'MUL' | 'DIV' | 'MOD' | 'EXPT' | 'MOVE '
| 'SHL' | 'SHR' | 'ROL' | 'ROR'
| 'AND' | 'OR' | 'XOR' | 'NOT'
| 'SEL' | 'MAX' | 'MIN' | 'LIMIT' | 'MUX '
| 'GT' | 'GE' | 'EQ' | 'LE' | 'LT' | 'NE'
| 'LEN' | 'LEFT' | 'RIGHT' | 'MID' | 'CONCAT' | 'INSERT' | 'DELETE' | 'REPLACE' | 'FIND';
Std_FB_Name : 'SR' | 'RS' | 'R_TRIG' | 'F_TRIG' | 'CTU'| 'CTD' | 'CTUD' | 'TP' | 'TON' | 'TOF';

Access_Spec : Access;
fragment  Access: 'PUBLIC' | 'PROTECTED' | 'PRIVATE' | 'INTERNAL';
Access_Direction : 'READ_WRITE' | 'READ_ONLY';
IL_Expr_Operator : 'IL_Expr_Operator';
IL_Call_Operator : 'CAL' | 'CALC' | 'CALCN';
IL_Return_Operator : 'RT' | 'RETC' | 'RETCN';
IL_Jump_Operator : 'JMP' | 'JMPC' | 'JMPCN';
Null : 'NULL';
LD_Rung : 'syntaxlexer for graphical languages not shown here';
FBD_Network : 'syntaxlexer for graphical languages not shown here11';
Other_Languages : 'syntaxlexer for other languages not shown here';

ReservedKeyword
  :
  'TYPE'
  |'END_TYPE'
  |'ARRAY'
  |'OF'
  |'STRUCT'
  |'OVERLAP'
  |'END_STRUCT'
  |'REF_TO'
  |'REF'
  |'THIS'
  |'VAR_INPUT'
  |'RETAIN'
  |'NON_RETAIN'
  |'END_VAR'
  |'R_EDGE'
  |'F_EDGE'
  |'VAR_IN_OUT'
  |'CONSTANT'
  |'VAR'
  |'VAR_TEMP'
  |'VAR_EXTERNAL'
  |'VAR_GLOBAL'
  |'AT'
  |'FUNCTION'
  |'END_FUNCTION'
  |'FUNCTION_BLOCK'
  |'FINAL'
  |'ABSTRACT'
  |'EXTENDS'
  |'IMPLEMENTS'
  |'END_FUNCTION_BLOCK'
  |'VAR_OUTPUT'
  |'OVERRIDE'
  |'METHOD'
  |'END_METHOD'
  |'CLASS'
  |'END_CLASS'
  |'END_INTERFACE'
  |'PROGRAM'
  |'END_PROGRAM'
  |'VAR_ACCESS'
  |'INITIAL_STEP'
  |'END_STEP'
  |'STEP'
  |'TRANSITION'
  |'PRIORITY'
  |'FROM'
  |'TO'
  |'END_TRANSITION'
  |'ACTION'
  |'END_ACTION'
  |'CONFIGURATION'
  |'END_CONFIGURATION'
  |'RESOURCE'
  |'ON'
  |'END_RESOURCE'
  |'TASK'
  |'SINGLE'
  |'INTERVAL'
  |'WITH'
  |'VAR_CONFIG'
  |'NAMESPACE'
  |'END_NAMESPACE'
  |'USING'
  |'SUPER'
  |'IL_Operator'
  |'RETURN'
  |'IF'
  |'THEN'
  |'ELSIF'
  |'ELSE'
  |'END_IF'
  |'CASE'
  |'END_CASE'
  |'EXIT'
  |'CONTINUE'
  |'FOR'
  |'END_FOR'
  |'DO'
  |'WHILE'
  |'BY'
  |'END_WHILE'
  |'REPEAT'
  |'UNTIL'
  |'END_REPEAT';