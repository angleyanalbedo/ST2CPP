lexer grammar PLCSTLEXER;

// 类型构造关键字（必须在 Identifier 之前定义，确保优先匹配）
ARRAY_KW : 'ARRAY';
OF_KW : 'OF';
STRUCT_KW : 'STRUCT';
END_STRUCT_KW : 'END_STRUCT';
OVERLAP_KW : 'OVERLAP';
TYPE_KW : 'TYPE';
END_TYPE_KW : 'END_TYPE';
STRING_KW : 'STRING';
WSTRING_KW : 'WSTRING';
REF_TO_KW : 'REF_TO';
REF_KW : 'REF';
THIS_KW : 'THIS';
ASSERT_KW : 'ASSERT';

// 字符串字面量
StringLiteralS : '\'' (~['\r\n] | '\'\'' | '$$' | '$' [LlNnPpRrTt] | '$' Hex_Digit Hex_Digit)* '\'';
StringLiteralD : '"' (~["\r\n] | '""' | '$$' | '$' [LlNnPpRrTt] | '$' Hex_Digit Hex_Digit Hex_Digit Hex_Digit)* '"';

// 关键字 — 每条独立规则，必须在 Identifier 之前
// 长关键字优先（ANTLR 匹配最长，但同长时先定义优先）
FUNCTION_BLOCK      : 'FUNCTION_BLOCK';
END_FUNCTION_BLOCK  : 'END_FUNCTION_BLOCK';
VAR_INPUT           : 'VAR_INPUT';
VAR_OUTPUT          : 'VAR_OUTPUT';
VAR_IN_OUT          : 'VAR_IN_OUT';
VAR_EXTERNAL        : 'VAR_EXTERNAL';
VAR_GLOBAL          : 'VAR_GLOBAL';
VAR_ACCESS          : 'VAR_ACCESS';
VAR_CONFIG          : 'VAR_CONFIG';
VAR_TEMP            : 'VAR_TEMP';
END_VAR             : 'END_VAR';
RETAIN              : 'RETAIN';
NON_RETAIN          : 'NON_RETAIN';
CONSTANT            : 'CONSTANT';
FUNCTION            : 'FUNCTION';
END_FUNCTION        : 'END_FUNCTION';
PROGRAM             : 'PROGRAM';
END_PROGRAM         : 'END_PROGRAM';
CONFIGURATION       : 'CONFIGURATION';
END_CONFIGURATION   : 'END_CONFIGURATION';
RESOURCE            : 'RESOURCE';
END_RESOURCE        : 'END_RESOURCE';
NAMESPACE           : 'NAMESPACE';
END_NAMESPACE       : 'END_NAMESPACE';
CLASS               : 'CLASS';
END_CLASS           : 'END_CLASS';
END_INTERFACE       : 'END_INTERFACE';
METHOD              : 'METHOD';
END_METHOD          : 'END_METHOD';
INITIAL_STEP        : 'INITIAL_STEP';
END_STEP            : 'END_STEP';
STEP                : 'STEP';
TRANSITION          : 'TRANSITION';
END_TRANSITION      : 'END_TRANSITION';
ACTION              : 'ACTION';
END_ACTION          : 'END_ACTION';
VAR                 : 'VAR';
AT                  : 'AT';
R_EDGE              : 'R_EDGE';
F_EDGE              : 'F_EDGE';
FINAL               : 'FINAL';
ABSTRACT            : 'ABSTRACT';
EXTENDS             : 'EXTENDS';
IMPLEMENTS          : 'IMPLEMENTS';
OVERRIDE            : 'OVERRIDE';
PRIORITY            : 'PRIORITY';
INTERVAL            : 'INTERVAL';
TASK                : 'TASK';
SINGLE              : 'SINGLE';
WITH                : 'WITH';
FROM                : 'FROM';
TO                  : 'TO';
ON                  : 'ON';
USING               : 'USING';
SUPER               : 'SUPER';
IL_Operator         : 'IL_Operator';
RETURN              : 'RETURN';
IF                  : 'IF';
THEN                : 'THEN';
ELSIF               : 'ELSIF';
ELSE                : 'ELSE';
END_IF              : 'END_IF';
CASE                : 'CASE';
END_CASE            : 'END_CASE';
EXIT                : 'EXIT';
CONTINUE            : 'CONTINUE';
FOR                 : 'FOR';
END_FOR             : 'END_FOR';
DO                  : 'DO';
WHILE               : 'WHILE';
BY                  : 'BY';
END_WHILE           : 'END_WHILE';
REPEAT              : 'REPEAT';
UNTIL               : 'UNTIL';
END_REPEAT          : 'END_REPEAT';

// 时间单位后缀（必须在 Identifier 之前定义，确保 ms/us/ns 优先于 Identifier 匹配）
MS_SUFFIX : 'ms';
US_SUFFIX : 'us';
NS_SUFFIX : 'ns';

// 标识符 — 必须在关键字和时间后缀之后定义
Identifier
  : IdentifierStart IdentifierPart* ;

fragment IdentifierStart
  : [A-Za-z$_]+ ;

fragment IdentifierPart
  : [A-Za-z0-9$_]+ ;

HEX_PREFIX : '16#';
BIN_PREFIX : '2#';
OCT_PREFIX : '8#';
HEX_LITERAL : '16#' ( '_'? [0-9A-Fa-f] )+;
BIN_LITERAL : '2#' ( '_'? [01] )+;
OCT_LITERAL : '8#' ( '_'? [0-7] )+;
Digit : '0'..'9';
Bit : '0'..'1';
Octal_Digit : '0'..'7';
Hex_Digit : '0'..'9' | 'A'..'F';
Comment : ('//' ~( '\n' | '\r' )* '\r' ? '\n' | '(*' .*? '*)' | '/*' .*? '*/') -> skip;
WS : ( ' ' | '\t' | '\r' | '\n' ) ->skip;
EOL : '\n';
Pragma : ('{' .*? '}')->skip;

S_byte_char_value : (Common_Char_Value | Common_Char_Byte|Char_other|Char_S|Char_Blank);
D_byte_char_value: (Common_Char_Value | Common_Char_ByteD|Char_other|Char_S|Char_Blank);
Common_Char_Byte : '$' Hex_Digit Hex_Digit;
Common_Char_Value : (Char_Value | Char_doll)+;
Char_Value : ([A-Za-z])+;
Char_doll: '$$';

Char_other: '!'|'#'|'%'|'&'|[0-9]| '$\'' | '$'[Ll]|'$'[Nn]| '$'[Pp] | '$'[Rr] | '$'[Tt]| '(' | ')';
Char_S :'"';
Char_Blank: ' ';

Common_Char_ByteD: '$' Hex_Digit Hex_Digit Hex_Digit Hex_Digit;

Date_Type_Name : Date_Type;
Date_Type : 'DATE' | 'LDATE';

Tod_Type_Name : Tod_Type_;
fragment Tod_Type_ : 'TIME_OF_DAY' | 'TOD' | 'LTOD';

DT_Type_Name:Date_Type;
fragment DT_Type : 'DATE_AND_TIME' | 'DT' | 'LDT';

Bool_Type_Name : 'BOOL';

Multibits_Type_Name: Multibits_Type;
fragment Multibits_Type : 'BYTE' | 'WORD' | 'DWORD' | 'LWORD';



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