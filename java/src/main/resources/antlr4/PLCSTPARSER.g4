grammar PLCSTPARSER;
import PLCSTLEXER;


/*
函数（功能）
类
PROG
命名空间
接口
全局变量
结构体
功能块

变量段

*/
startpoint
          : ( func_decl           
            | config_decl          
            | prog_decl          
            | pou_decl
            | fb_decl
            | pou_decl
            )+ EOF
          ;
// startpoint : constant ;

identifier
            : 'E'
            |'T'
            |'L'
            |'D'
            |'I'
            |'Q'
            |'M'
            |'X'
            |'B'
            |'W'
            |'N'
            |'R'
            |'S'
            |'P'
            |Identifier
            ;


constant                                                    //SC DONE
        : numeric_literal
        | char_literal
        | time_literal
        | bit_str_literal
        | bool_literal
        ;

numeric_literal                             //SC DONE
               : int_literal
               | real_literal
               ;

int_literal                                                         //SC DONE
            : ( int_type_name '#' )?
              ( signed_int
              | binary_int
              | octal_int
              | hex_int
              )
            ;

//unsigned_int
//            : Digit ( '_' ? Digit )*;

Unsigned_int
            : Digit ('_'? Digit) *;

//signed_int: ( '+'  | '-' )?unsigned_int;

signed_int
            : ( '+'
              | '-' )?
              Unsigned_int
            ;

binary_int
            : '2#' ( '_' ? Bit )+;

octal_int
            : '8#' ( '_' ? Octal_Digit )+;

hex_int
            : '16#' ( '_' ? Hex_Digit )+;

real_literal                                            //SC DONE
            : ( Real_Type_Name '#' )?
              signed_int '.' Unsigned_int
              ( 'E' signed_int )?
            ;

bit_str_literal                                     //SC DONE
            : ( Multibits_Type_Name '#' )?
              ( Unsigned_int
              | binary_int
              | octal_int
              | hex_int )
            ;

bool_literal                                                //SC DONE
            : ( Bool_Type_Name '#' )?
              ( Bit | 'FALSE' | 'TRUE' )
            ;
// Table 6
// Table 7


char_literal                                                            //SC DONE
            : ( 'STRING''#' )?
              char_str
            ;

char_str
            : s_byte_char
            | D_byte_char;

s_byte_char
            : '\'' S_byte_char_value*
              '\''
            ;

//s_byte_char_str : '\'' S_byte_char_value+ '\'';

//d_byte_char_str : '"' d_byte_char_value + '"';

d_byte_char_value
            : Common_Char_Value+
            | '$' Hex_Digit Hex_Digit
              Hex_Digit Hex_Digit
            ;

D_byte_char
            : '"' D_byte_char_value*
              '"'
            ;

// any printable characters except $, " and '

time_literal                                                        //SC DONE
            : duration
            | time_of_day
            | date
            | date_and_time
            ;

duration
            : ( Time_Type_Name
            | 'T'
            | 'L''T' ) '#'
            ( '+'
            | '-' )? interval
            ;

fix_point
            : Unsigned_int
            ( '.' Unsigned_int )?
            ;

interval
            : days
            | hours
            | minutes
            | seconds
            | milliseconds
            | microseconds
            | nanoseconds
            ;

days
            : ( fix_point 'd' )
            | Unsigned_int 'd'
            ( '_' hours )?
            ;
//days : ( fix_point 'd' ) | ( unsigned_int 'd' '_' ? )? hours ?;

hours
            : ( fix_point 'h' )
            | Unsigned_int 'h'
            ( '_' minutes )?
            ;
//hours : ( fix_point 'h' ) | ( unsigned_int 'h' '_' ? )? minutes ?;

minutes
            : ( fix_point 'm' )
            | Unsigned_int 'm'
            ( '_' seconds )?
            ;
//minutes : ( fix_point 'm' ) | ( unsigned_int 'm' '_' ? )? seconds ?;

seconds
            : ( fix_point 's' )
            | Unsigned_int 's'
            ( '_' milliseconds )?
            ;
// seconds : ( fix_point 's' ) | ( unsigned_int 's' '_' ? )? milliseconds ?;

milliseconds
            : ( fix_point 'm''s' )
            | Unsigned_int 'm''s'
              ( '_' microseconds )?
            ;
// milliseconds : ( fix_point 'ms' ) | ( unsigned_int 'ms' '_' ? )? microseconds ?;

microseconds
            : ( fix_point 'u''s' )
            | Unsigned_int 'u''s'
              ( '_' nanoseconds )?
            ;
// microseconds : ( fix_point 'us' ) | ( unsigned_int 'us' '_' ? )? nanoseconds ?;

nanoseconds
            : fix_point 'n''s'
            ;

time_of_day
            : ( Tod_Type_Name
            | 'LTIME_OF_DAY' ) '#' daytime
            ;

daytime
            : day_hour   ':'
              day_minute ':'
              day_second
            ;

day_hour
            : Unsigned_int
            ;

day_minute
            :
              Unsigned_int
            ;

day_second
            : fix_point
            ;

date
            : ( Date_Type_Name
            | 'D'
            | 'LD' ) '#'
              date_literal
            ;

date_literal
            : year  '-'
              month '-'
              day
            ;

year
            : Unsigned_int
            ;

month
            : Unsigned_int
            ;

day
            : Unsigned_int
            ;

date_and_time
            : ( DT_Type_Name
            | 'LDATE_AND_TIME' ) '#'
              date_literal '-'
              daytime
            ;
// Table 10 - Elementary data types

data_type_access
            : elem_type_name
            | derived_type_access
            ;

elem_type_name                                  //SC DONE
            : numeric_type_name
            | bit_str_type_name
//          | string_type_name
            | Date_Type_Name
            | Time_Type_Name
            | Char_Type_name
            ;

Char_Type_name
            :
              'CHAR'
            | 'WCHAR'
            ;

numeric_type_name
            : int_type_name
            | Real_Type_Name
            ;

int_type_name
            : Sign_Int_Type_Name
            | Unsign_Int_Type_Name
            ;

//string_type_name
//            :
////              'STRING'  ( '[' (Unsigned_int | identifier) ']' )?
////            | 'WSTRING' ( '[' (Unsigned_int | identifier) ']' )?  //数组长度定义支持非直接量
//            'CHAR'
//            | 'WCHAR'
//            ;

bit_str_type_name
            : Bool_Type_Name
            | Multibits_Type_Name
            ;
// Table 11 - Declaration of user-defined data types and initialization

derived_type_access  //可用的類型入口
            : single_elem_type_access
            | string_type_access
            | user_defination_type_access
            ;

string_type_access
            : ( namespace_name '.' )*
//            string_type_name
              str_spec
            ;

single_elem_type_access
            : simple_type_access
            ;

simple_type_access                                          //SC DONE
            : ( namespace_name '.' )*
              simple_type_name
            ;

subrange_type_access
            : ( namespace_name '.' )*
              subrange_type_name
            ;

enum_type_access
            : ( namespace_name '.' )*
              enum_type_name
            ;

array_type_access
            : ( namespace_name '.' )*
              array_type_name
            ;

struct_type_access
            : ( namespace_name '.' )*
              struct_type_name
            ;

simple_type_name : identifier;
//simple_type_name
//            : Sign_Int_Type_Name
//            | Unsign_Int_Type_Name
//            | Real_Type_Name
//            | Time_Type_Name
//            | Date_Type_Name
//            | Tod_Type_Name
//            | DT_Type_Name
//            | Bool_Type_Name
//            | Multibits_Type_Name
//            ;

subrange_type_name
            : identifier
            ;

enum_type_name
            : identifier
            ;

array_type_name
            : identifier
            ;

struct_type_name
            : identifier
            ;


data_type_decl
            : 'TYPE'
              ( type_decl ';' )+
              'END_TYPE'
            ;

//SC DONE
    type_decl
            : simple_type_decl
            | subrange_type_decl
            | enum_type_decl
            | array_type_decl
            | struct_type_decl
            | str_type_decl
            | ref_type_decl
            | pointer_type_decl      //添加指针类型
//            | derived_type_decl    //Alter_Loc
            ;
//simple_type_name_identifier : identifier;//修复simple_type_name
derived_type_decl
            :
              derived_type_name ':'
              derived_spec_init
            ;

derived_type_name
            : identifier
            ;

derived_spec_init
            : simple_spec_init
            | subrange_spec_init
            | (( elem_type_name?
              named_spec_init )
            | enum_spec_init )
            | array_type_decl
            | struct_type_decl
            | ref_type_decl
            | str_type_decl
            ;

simple_type_decl                             //SC DONE
                : simple_type_name ':'
                  simple_spec_init
                ;

simple_spec_init                                    //SC DONE
                : simple_spec
                ( ':''=' constant_expr )?  //初始值类型
                ;

//PLCTypeDeclSymbol
simple_spec                                 //SC DONE
                : elem_type_name
                | simple_type_access
                ;

subrange_type_decl                          //SC DONE
                : subrange_type_name ':'
                subrange_spec_init
                ;

subrange_spec_init                        //SC DONE
                : subrange_spec
                ( ':' '=' signed_int )?
                ;

subrange_spec                              //SC DONE
                : int_type_name '(' subrange ')'
                | subrange_type_access
                ;

subrange        : constant_expr '..'       //SC DONE
                constant_expr
                ;

//SC DONE
enum_type_decl
                : enum_type_name ':'
                ( ( elem_type_name? named_spec_init )
                | enum_spec_init )
                ;

//SC DONE
named_spec_init                                         //元素可以初始化的，在前面可添加类型定义的
                : '(' enum_value_spec
                ( ',' enum_value_spec )* ')'
                ( ':' '=' enum_value )?      //可赋值
                ;

//SC DONE
enum_spec_init                                          //元素尚未初始化的,可添加type_access的，不可添加类型定义的
                : ( ( '(' identifier ( ',' identifier )* ')' ) //named_spec_init已经包含这种情况（仍需要包装这条语法）
                | enum_type_access )
                ( ':' '=' enum_value )?
                ;

enum_value_spec   //元素初始化                            //SC DONE
                : identifier
                  ( ':' '=' ( int_literal
                  | constant_expr ) )?
                ;

enum_value                                              //SC DONE
                : ( enum_type_name '#' )?
                  identifier
                ;

array_type_decl         //SC DONE
                : array_type_name ':'
                  array_spec_init
                ;

array_spec_init         //SC DONE 
                : array_spec
                  ( ':' '=' array_init )?
                ;

//PLCTypeDeclSymbol
array_spec              //SC DONE
                :'ARRAY' '[' subrange
                  ( ',' subrange )* ']' 'OF'
                  data_type_access
                ;

array_init            //SC DONE
                : '[' array_elem_init
                  ( ',' array_elem_init )*
                  ']'
                ;


array_elem_init             //SC DONE
                : array_elem_init_value//单个元素
                //| Unsigned_int '(' array_elem_init_value ? ')';
                //| Unsigned_int '(' array_elem_init_value ?(','array_elem_init_value)* ')';//初始化序列
                | array_elem_item_init;//初始化序列

array_elem_item_init
                :Unsigned_int '(' array_elem_init_value ?(','array_elem_init_value)* ')';

array_elem_init_value         //SC DONE
                : constant_expr    //常量
                | enum_value       //枚举类型
                | struct_init      //结构体
                | array_init       //数组
                ;

//PLCStructDeclSymbol
//SC DONE
struct_type_decl
                : struct_type_name ':'
                struct_spec
                ;

//PLCStructDeclSymbol
//SC DONE
struct_spec
                : struct_decl       //结构化数据类型的声明     //PLCStructDeclSymbol
                | struct_spec_init  //结构化变量的初始化      //四变量  DeclSymbol
                ;

//PLCVariable
//SC DONE
struct_spec_init
                : struct_type_access//PLCTypeDeclSymbol
                ( ':' '=' struct_init )?//PLCVariable
                ;

//PLCStructDeclSymbol
//SC DONE
struct_decl
                : 'STRUCT' 'OVERLAP' ?
                ( struct_elem_decl ';' )+
                'END_STRUCT'
                ;

//PLCStructDeclSymbol
//SC DONE
struct_elem_decl
                : struct_elem_name
                (( located_at multibit_part_access ? )? ':'| located_at_init':' )
                ( simple_spec_init//四变量
                | subrange_spec_init//declsymbol assignvar
                | enum_spec_init//declsymbol assignvar
                | array_spec_init//四变量
                | struct_spec_init )//四变量 declsymbol
                ;

//loc_partly_var : variable_name 'AT' '%' ( 'I' | 'Q' | 'M' ) '*' ':' var_spec ';'
struct_elem_name
                : identifier
                ;

//PLCVariable            //PlCStructDeclSymbol
//SC DONE
struct_init            //结构体初始化
                : '(' struct_elem_init
                ( ',' struct_elem_init )* ')'
                ;

//PLCStructDeclSymbol    //PLCVariable
//SC DONE
struct_elem_init      //元素初始化
                : struct_elem_name ':' '='
                ( constant_expr
                | enum_value
                | array_init
                | struct_init//ArrayList
                | ref_value )
                ;

//用于TYPE中的换名（类似C++的define）
str_type_decl                //SC DONE
                :
//                  string_type_name ':'
//                  string_type_name
                  string_type_name_identifier ':'
                  str_spec
                  ( ':''=' char_str )?
                ;


string_type_name_identifier
                :
                  identifier
                ;
// Table 16 - Directly represented variables

Direct_variable                                 //直接量
                : '%' ( 'I' | 'Q' | 'M' )?
                  ( 'X' | 'B' | 'W' | 'D' | 'L' )?
                  Unsigned_int
                  ( '.' Unsigned_int )*
                ;
//?
//direct_variable : '%' ( 'I' | 'Q' | 'M' )( 'X' | 'B' | 'W' | 'D' | 'L' )? Unsigned_int ( '.' Unsigned_int )*;

// Table 12 - Reference operations

//SC DONE
ref_type_decl
                : ref_type_name ':'
                  ref_spec_init
                ;

ref_spec_init       //SC DONE by bal
                : ref_spec
                  ( ':' '=' ref_value )?
                ;

ref_spec            //SC DONE
                : 'REF_TO' +
                  data_type_access
                ;

ref_type_name
                : identifier
                ;

//deprecated
ref_type_access
                : ( namespace_name '.' )*
                ref_type_name
                ;

ref_name
                : identifier
                ;

ref_value         //SC DONE
                : ref_addr
                | Null
                ;

ref_addr              //SC DONE
                : 'REF' '('
                  ( symbolic_variable
                  | fb_instance_name
                  | instance_name )
                  ')'
                ;

//SC DONE
ref_assign
                : ref_name ':' '='
                  ( ref_name
                  | ref_deref
                  | ref_value
                  )
                ;

//SC DONE
ref_deref
                : ref_name '^' +
                ;

// e用于实现指针操作
pointer_type_decl:
                 pointer_type_name
                ;
pointer_type_name
                :
                identifier ':'
                pointer_spec_init
                ;
pointer_spec_init
                :pointer_spec
                (':''=' pointer_value)?
                ;
pointer_name    :
                identifier
                ;

pointer_spec
                :'POINTER' 'TO' +
                data_type_access
                ;
pointer_value
                :pointer_adr
                |Null
                ;
pointer_adr
                : 'ADR' '('
                  ( symbolic_variable
                  | fb_instance_name
                  | instance_name )
                  ')'
                ;
pointer_dref
                : 'DERF' '('
                  ( pointer_name
                  | pointer_value
                  )
                ')'
                 ;
pointer_assign
                :pointer_name ':' '='
                (pointer_name
                |pointer_value
                |pointer_dref)
                ;

// Table 13 - Declaration of variables/Table 14  Initialization of variables

variable        : Direct_variable
                | symbolic_variable
                ;


array_index
            :'[' subscript( ',' subscript )*']';

symbolic_variable
                : ( 'THIS' '.' )?
                   identifier '^'*//PLCVariable
                  ( 
                  (array_index*)//ArrayList
                  | 
                  (struct_variable*)//. var_access
                  )                            #thisSymbolic
                |
                  ( namespace_name '.' )*
                  ( var_access | multi_elem_var )    #namespaceSymbolic
                ;

//SC DONE
var_access  //PLCRefVariable
                : variable_name
                | ref_deref
                ;

variable_name                                   //SC DONE
                : identifier
                ;

//deprecated
multi_elem_var
                : var_access//identifier '^'*
                  ( subscript_list//arraylist
                  | struct_variable//. var_access
                  )+
                ;

//SC DONE
subscript_list
                : '[' subscript
                   ( ',' subscript )*
                  ']'
                ;

//SC DONE
subscript       :
                expression
                ;

//SC DONE
struct_variable
                : '.' struct_elem_select
                ;

//SC DONE
struct_elem_select
                : var_access
                ;

input_decls     : 'VAR_INPUT'               //SC DONE
                  RETAINORNONRETAIN ?
                  ( input_decl ';' )* 
                  'END_VAR'
                ;

input_decl      :                   //SC DONE 
                  var_decl_init
                | edge_decl         //边缘变量
                | array_conform_decl
                ;
//SC DONE   by bal
edge_decl       : variable_list ':'
                  Bool_Type_Name
                  ( 'R_EDGE'
                  | 'F_EDGE'
                  )
                ;

//var_decl_init : variable_list ':' ( simple_spec_init | str_var_decl | ref_spec_init )
//| array_var_decl_init | struct_var_decl_init | fb_decl_init | interface_spec_init;
//变量段的变量声明
var_decl_init                                   //SC DONE
                : (variable_list ':'
                    (simple_spec_init
                    //| str_var_decl            //废弃（原因：该语法允许 I:I:STRING 为错误语法，不会匹配到）
//                    | s_byte_str_spec           //会被simplespecinit里string匹配
//                    | d_byte_str_spec           //
                    | str_spec_init
                    | user_defination_spec_init
                    | ref_spec_init             
                    | array_var_decl_init       
//                    | struct_var_decl_init      //
                    | fb_decl_init              //
                    ))                          #vardeclinit
                   | variable_name ?  
                     located_at ':'
                     loc_var_spec_init
                                                #directNum
                  ;
//字符串初始化
str_spec_init   :                                       //SC DONE
                  str_spec
                  (':''=' constant_expr)?
                ;

str_spec                            //SC DONE
                :
                  ('STRING'
                  | 'WSTRING')
                  ( '[' (Unsigned_int | identifier) ']' )?
                ;

//用户自定义类型
user_defination_spec_init       //SC DONE
                :
                  user_defination_type_access
                  (':''=' constant_expr)?
                ;

user_defination_type_access       //SC DONE
                :
                  (namespace_name'.')*
                  user_defination_type_name
                ;

user_defination_type_name
                :
                    identifier
                ;

ref_var_decl
                : variable_list ':'
                  ref_spec
                ;

interface_var_decl
                : variable_list ':'
                  interface_type_access
                ;


variable_list   : variable_name                             //SC DONE
                  ( ',' variable_name ) *
                ;

//array_var_decl_init : variable_list ':' array_spec_init;
array_var_decl_init           //SC DONE
                :  array_spec_init
                ;

array_conformand                             //变长数组
                : 'ARRAY' '[' '*' ( ',' '*' )* ']' 'OF'    //childcount 6,8,10...
                  data_type_access
                ;

array_conform_decl
                : variable_list ':'
                  array_conformand
                ;

struct_var_decl_init
                : variable_list ':'
                  struct_spec_init
                ;

fb_decl_no_init
                : fb_name
                  ( ',' fb_name )* ':'
                  fb_type_access
                ;

fb_decl_init
                : fb_decl_no_init
                  ( ':' '=' struct_init )?
                ;

fb_name
                : identifier
                ;

fb_instance_name
                : ( namespace_name '.' )*
                  fb_name '^' *
                ;

output_decls                    //SC DONE
                : 'VAR_OUTPUT'
                  RETAINORNONRETAIN?
                  ( output_decl ';' )*
                  'END_VAR'
                ;

output_decl                 //SC DONE
                : var_decl_init
                | array_conform_decl
                ;

in_out_decls                    //SC DONE
                : 'VAR_IN_OUT'
                  ( in_out_var_decl ';' )* 
                  'END_VAR'
                ;

in_out_var_decl                 //SC DONE
                : var_decl
                | array_conform_decl
                | fb_decl_no_init
                ;

var_decl                //SC DONE
                : variable_list ':'
                  ( simple_spec_init
                  | str_spec_init
                  | user_defination_spec_init
                  | ref_spec_init             //引用类型声明
                  | array_var_decl_init
                  )
                ;

array_var_decl                                //多个变量声明
                : variable_list ':'
                  array_spec
                ;

struct_var_decl
                : variable_list ':'
                  struct_type_access
                ;
// var_decls : 'VAR' 'CONSTANT'? Access_Spec ? ( var_decl_init ';' )* 'END_VAR';

//在该语法结点下进行翻译
var_decls                                           //SC DONE
                : 'VAR' CONSTANT?  //如果声明为const变量需要检查是否赋值
                  Access_Spec ?
                  ( var_decl_init ';' )*  
                  'END_VAR'
                ;

retain_var_decls            //SC DONE
                : 'VAR' 'RETAIN'
                  Access_Spec ?
                  ( var_decl_init ';' )*
                  'END_VAR'
                ;

loc_var_decls           //SC DONE
                : 'VAR'
                  ( 'CONSTANT'
                  | 'RETAIN'
                  | 'NON_RETAIN'
                  )?
                  ( loc_var_decl ';' )*
                  'END_VAR'
                ;

loc_var_decl            //SC DONE
                : variable_name ?
                  located_at ':'
                  loc_var_spec_init
                ;

temp_var_decls                  //SC DONE         Bal  2022/5/16
                : 'VAR_TEMP'
                  (( var_decl
                   | ref_var_decl
                   ) ';'                      //将Arraylist<PLCSymbol <variableName><variableType><variableValue> >挂树
                  )*
                  'END_VAR'
                ;


external_var_decls               //SC DONE
                : 'VAR_EXTERNAL' 'CONSTANT' ?
                  ( external_decl ';' )* 
                  'END_VAR'
                ;

external_decl                   //SC DONE
                : global_var_name ':'
                  ( simple_spec
                  | array_spec
                  |user_defination_type_access
                  );
//                  | struct_type_access
//                  | fb_type_access
//                  | ref_type_access
//                  )

global_var_name                      //SC DONE
                : identifier
                ;

global_var_decls             //SC DONE
                : 'VAR_GLOBAL'
                  ( 'CONSTANT' | 'RETAIN' )?
                  ( global_var_decl ';' )*
                  'END_VAR'
                ;

global_var_decl         //SC DONE
                : global_var_spec ':'
                  ( loc_var_spec_init
                  | fb_type_access )
                ;

global_var_spec         //SC DONE
                : ( global_var_name
                  ( ',' global_var_name )*
                  )
                  | ( global_var_name located_at )
                  ;

loc_var_spec_init           //SC DONE
                : simple_spec_init
                | array_spec_init
                | struct_spec_init
                | str_spec_init
                // | s_byte_str_spec
                // | d_byte_str_spec    //二者被弃用
                ;

located_at
                : 'AT' Direct_variable
                ;
//located_at_init: 'AT' '%' ( 'I' | 'Q' | 'M' ) '*' ':';
located_at_init
                : 'AT' Direct_represented '*'
                ;
Direct_represented
                : '%'
                  ( 'I'
                  | 'Q'
                  | 'M'
                  )
                ;
//loc_partly_var : variable_name 'AT' '%' ( 'I' | 'Q' | 'M' ) '*' ':' var_spec ';';

str_var_decl
                : s_byte_str_var_decl
                | d_byte_str_var_decl
                ;

s_byte_str_var_decl
                : variable_list ':'
                  s_byte_str_spec
                ;

s_byte_str_spec
                : 'STRING'
                  ( '[' Unsigned_int ']' )?
                  ( ':' '=' s_byte_char )?
                ;

d_byte_str_var_decl
                : variable_list ':'
                  d_byte_str_spec
                ;

d_byte_str_spec
                : 'WSTRING'
                  ( '[' Unsigned_int ']' )?
                  ( ':' '=' D_byte_char )?
                ;

loc_partly_var_decl                     //SC DONE
                : 'VAR' RETAINORNONRETAIN? 
                  loc_partly_var *
                  'END_VAR'
                ;

loc_partly_var          //SC DONE
                : variable_name 'AT' '%'
                  ( 'I'
                  | 'Q'
                  | 'M'
                  ) '*' ':'
                  var_spec ';'
                ;

var_spec            //SC DONE
                : simple_spec
                | array_spec
                | str_spec
                ;
// Table 19 - Function declaration

func_name
                : Std_Func_Name
                | derived_func_name
                ;

func_access     : ('THIS' '.')?               //SC DONE
                  ( scope_name '.' )*    
                  func_name
                ;

scope_name : identifier '^'*;

 // incomplete list

derived_func_name
                : identifier
                ;

func_decl       : 'FUNCTION'
                  derived_func_name
                  ( ':' data_type_access )?
                  using_directive *
                  ( io_var_decls
                  | func_var_decls
                  | temp_var_decls
                  )* func_body?
                  'END_FUNCTION'
                ;

io_var_decls                                //SC DONE

                : input_decls
                | output_decls  
                | in_out_decls
                ;

func_var_decls                  //SC DONE
                : external_var_decls
                | var_decls
                ;

func_body       : ladder_diagram
                | fb_diagram
                | stmt_list         //将实现的接口挂树
                | instruction_list
                | Other_Languages
                ;

// Table 40  Function block type declaration
// Table 41 - Function block instance declaration

fb_type_name
                : Std_FB_Name
                | derived_fb_name
                ;

fb_type_access            //SC DONE
                : ( namespace_name '.' )*
                  fb_type_name
                ;
 // incomplete list

derived_fb_name
                : identifier
                ;

fb_decl                   //SC DONE
                : 'FUNCTION_BLOCK'
                  FINALORABSTRACT?
                  derived_fb_name 
                  using_directive *
                  ( 'EXTENDS'
                   ( fb_type_access
                   | class_type_access
                   )
                  )?
                  ( 'IMPLEMENTS' interface_name_list )?
                  ( fb_io_var_decls
                  | func_var_decls
                  | temp_var_decls
                  | other_var_decls
                  )*
                  ( method_decl )* fb_body?
                  'END_FUNCTION_BLOCK'
                ;

fb_io_var_decls           //SC DONE
                : fb_input_decls
                | fb_output_decls
                | in_out_decls
                ;

fb_input_decls          //SC DONE
                : 'VAR_INPUT'
                  ( 'RETAIN' | 'NON_RETAIN' )?
                  ( fb_input_decl ';' )*
                  'END_VAR'
                ;

fb_input_decl           //SC DONE
                : var_decl_init
                | edge_decl
                | array_conform_decl
                ;

fb_output_decls       //SC DONE
                : 'VAR_OUTPUT'
                  ( 'RETAIN' | 'NON_RETAIN' )?
                  ( fb_output_decl ';' )*
                  'END_VAR'
                ;

fb_output_decl        //SC DONE
                : var_decl_init
                | array_conform_decl
                ;

other_var_decls       //SC DONE        Bal 2022/5/16
                : retain_var_decls
                | no_retain_var_decls
                | loc_partly_var_decl
                ;

no_retain_var_decls   //SC DONE        Bal 2022/5/16
                : 'VAR' 'NON_RETAIN' Access_Spec ?
                  ( var_decl_init ';' )*
                  'END_VAR'
                ;

fb_body               //SC DONE        Bal 2022/5/16
                : sfc
                | ladder_diagram
                | fb_diagram
                | instruction_list
                | stmt_list
                | Other_Languages
                ;

method_decl
                : 'METHOD' Access_Spec?
                  FINALORABSTRACT?
                  OVERRIDE ?
                  method_name
                  ( ':' data_type_access )? //将方法返回类型挂树
                  ( io_var_decls
                  | func_var_decls
                  | temp_var_decls
                  )* func_body
                  'END_METHOD'
                ;

method_name
                : identifier
                ;
// Table 48 - Class
// Table 50 Textual call of methods  Formal and non-formal parameter list

class_decl      : 'CLASS'
                  FINALORABSTRACT?
                  class_type_name
                  using_directive ?
                  ( 'EXTENDS' class_type_access )?
                  ( 'IMPLEMENTS' interface_name_list )?
                  ( func_var_decls
                  | other_var_decls )*
                  ( method_decl )*
                  'END_CLASS'
                ;

class_type_name
                : identifier
                ;

class_type_access
                : ( namespace_name '.' )*
                  class_type_name
                ;

class_name
                : identifier
                ;

instance_name
                : ( namespace_name '.' )*
                  class_name '^' *
                ;

interface_decl  : 'INTERFACE' interface_type_name
                  using_directive *
                  ( 'EXTENDS' interface_name_list )?
                  method_prototype *
                  'END_INTERFACE'
                ;

method_prototype
                : 'METHOD' method_name
                  ( ':' data_type_access )?
                  io_var_decls *
                  'END_METHOD'
                ;

//interface_spec_init
//                : variable_list
//                  ( ':' '=' interface_value )?
//                ;

interface_spec_init
                : identifier    //接口名称
                  ( ':' '=' interface_value )?//接口初始化名
                ;
interface_value
                : symbolic_variable
                | fb_instance_name
                | instance_name
                | Null
                ;

interface_name_list
                : interface_type_access
                  ( ',' interface_type_access )*
                ;

interface_type_name
                : identifier
                ;

interface_type_access
                : ( namespace_name '.' )*
                  interface_type_name
                ;

interface_name
                : identifier
                ;
// Table 47 - Program declaration
prog_decl                                           //SC DONE
                : 'PROGRAM' prog_type_name
                  ( io_var_decls               
                  | func_var_decls
                  | temp_var_decls
                  | other_var_decls
                  | loc_var_decls              
                  | prog_access_decls          
                  )* fb_body?
                  'END_PROGRAM'
                ;

prog_type_name                          //SC DONE
                : identifier
                ;

prog_type_access
                : ( namespace_name '.' )*
                  prog_type_name
                ;

prog_access_decls
                : 'VAR_ACCESS'
                  ( prog_access_decl ';' )*
                  'END_VAR'
                ;

prog_access_decl
                : access_name ':'
                  symbolic_variable
                  multibit_part_access ?
                  ':' data_type_access
                  Access_Direction ?
                ;
// Table 54 - 61 - Sequential Function Chart (sfc)

sfc
                : sfc_network +
                ;

sfc_network
                : initial_step
                  ( step | transition | action )*
                ;

initial_step    : 'INITIAL_STEP'
                  step_name ':'
                  ( action_association ';' )*
                  'END_STEP'
                ;

step
                : 'STEP' step_name ':'
                  ( action_association ';' )*
                  'END_STEP'
                ;

step_name
                : identifier
                ;

action_association
                : action_name
                  '('
                  action_qualifier ?
                  ( ',' indicator_name )*
                  ')'
                ;

action_name
                : identifier
                ;

action_qualifier
                : 'N'
                | 'R'
                | 'S'
                | 'P'
                | (( 'L'
                   | 'D'
                   | 'SD'
                   | 'DS'
                   | 'SL'
                   ) ',' action_time
                  )
                ;

action_time
                : duration
                | variable_name
                ;

indicator_name
                : variable_name
                ;

transition
                : 'TRANSITION'
                  transition_name ?
                  ( '(' 'PRIORITY' ':' '=' Unsigned_int ')' )?
                  'FROM' steps
                  'TO' steps
                  ':' transition_cond
                  'END_TRANSITION'
                ;

transition_name
                : identifier
                ;

steps
                : step_name
                | '(' step_name
                  ( ',' step_name )+
                  ')'
                ;

transition_cond
                : ':' '=' expression ';'
                | ':'
                  ( FBD_Network
                  | LD_Rung
                  )
                | ':' '=' il_simple_inst
                ;

action
                : 'ACTION'
                  action_name
                  ':' fb_body
                  'END_ACTION'
                ;
// Table 62 - Configuration and resource declaration
config_name
                : identifier
                ;

resource_type_name
                : identifier
                ;

config_decl
                : 'CONFIGURATION' config_name
                  global_var_decls ?
                  ( single_resource_decl
                  | resource_decl +
                  )
                  access_decls ?
                  config_init ?
                  'END_CONFIGURATION'
                ;

resource_decl   : 'RESOURCE' resource_name
                  'ON' resource_type_name
                  global_var_decls ?
                  single_resource_decl
                  'END_RESOURCE'
                ;

single_resource_decl
                : ( task_config ';' )*
                  ( prog_config ';' )+
                ;

resource_name
                : identifier
                ;

access_decls
                : 'VAR_ACCESS'
                  ( access_decl ';' )*
                  'END_VAR'
                ;

access_decl
                : access_name ':'
                  access_path ':'
                  data_type_access
                  Access_Direction ?
                ;

access_path     : ( resource_name '.' )? Direct_variable
                | ( resource_name '.' )?
                  ( prog_name '.' )?
                  (( fb_instance_name
                   | instance_name
                   ) '.'
                  )* symbolic_variable
                ;

global_var_access
                : ( resource_name '.' )?
                  global_var_name
                  ( '.' struct_elem_name )?
                ;

access_name
                : identifier
                ;

prog_output_access
                : prog_name '.'
                  symbolic_variable
                ;

prog_name
                : identifier
                ;

task_config
                : 'TASK'
                  task_name task_init
                ;

task_name
                : identifier
                ;

task_init       : '(' ( 'SINGLE' ':' '=' data_source ',' )?
                  ( 'INTERVAL' ':' '=' data_source ',' )?
                'PRIORITY' ':' '=' Unsigned_int ')'
                ;

data_source
                : constant
                | global_var_access
                | prog_output_access
                | Direct_variable
                ;

prog_config     : 'PROGRAM'
                  ( 'RETAIN'
                  | 'NON_RETAIN'
                  )? prog_name
                  ( 'WITH' task_name )? ':'
                  prog_type_access
                  ( '(' prog_conf_elems ')' )?
                ;

prog_conf_elems
                : prog_conf_elem
                  ( ',' prog_conf_elem )*
                ;

prog_conf_elem
                : fb_task
                | prog_cnxn
                ;

fb_task         : fb_instance_name
                  'WITH' task_name
                ;

prog_cnxn       : symbolic_variable ':' '='
                  prog_data_source
                | symbolic_variable '=>' data_sink;

prog_data_source
                : constant
                | enum_value
                | global_var_access
                | Direct_variable
                ;

data_sink
                : global_var_access
                | Direct_variable
                ;

config_init     : 'VAR_CONFIG'
                  ( config_inst_init ';' )*
                'END_VAR'
                ;

config_inst_init
                : resource_name '.'
                  prog_name '.'
                  (( fb_instance_name
                   | instance_name
                   ) '.'
                  )*
                  ( variable_name located_at ? ':'
                    loc_var_spec_init
                  | (( fb_instance_name ':'
                       fb_type_access
                     )
                     | ( instance_name ':'
                       class_type_access
                       )
                    ) ':' '=' struct_init );
// Table 64 - Namespace

namespace_decl                //SC DONE
                : 'NAMESPACE' 'INTERNAL' ?
                  namespace_h_name
                  using_directive *
                  namespace_elements
                  'END_NAMESPACE'
                ;

namespace_elements                //SC DONE
                : ( data_type_decl
                  | func_decl
                  | fb_decl
                  | class_decl
                  | interface_decl
                  | namespace_decl )+
                ;

namespace_h_name                //SC DONE
                : namespace_name
                  ( '.' namespace_name )*
                ;
namespace_name                //SC DONE
                : identifier
                ;

using_directive                     //SC DONE //将命名空间挂树
                : 'USING'
                  namespace_h_name
                  ( ',' namespace_h_name )* ';'
                ;

pou_decl                //SC DONE
                : using_directive *
                  ( global_var_decls
                  | data_type_decl
                  | access_decls
                  | func_decl
                  | fb_decl
                  | class_decl
                  | interface_decl
                  | namespace_decl )+
                ;
// Table 67 - 70 - Instruction List (IL)

instruction_list
                : il_instruction +
                ;

il_instruction
                : ( il_label ':' )?
                  ( il_simple_operation
                  | il_expr
                  | il_jump_operation
                  | il_invocation
                  | il_formal_func_call
                  | IL_Return_Operator )?
                    EOL +
                  ;

il_simple_inst
                : il_simple_operation
                | il_expr
                | il_formal_func_call
                ;

il_label
                : identifier
                ;

il_simple_operation
                : il_simple_operator il_operand ?
                | func_access il_operand_list ?
                ;

il_expr
                : IL_Expr_Operator
                  '(' il_operand ?
                  EOL +
                  il_simple_inst_list ?
                  ')'
                ;

il_jump_operation
                : IL_Jump_Operator
                  il_label
                ;

il_invocation
                : IL_Call_Operator
                  ((( fb_instance_name
                    | func_name
                    | method_name
                    | 'THIS '
                    | (( 'THIS' '.'
                        (( fb_instance_name
                         | instance_name
                         ) '.'
                        )*
                       ) method_name
                      )
                    )
                    ( '('
                     ( ( EOL + il_param_list ? )
                     | il_operand_list ?
                     ) ')'
                    )?
                   )
                  | 'SUPER''.' derived_func_name '(' ')'
                  )
                ;

il_formal_func_call
                : func_access
                  '(' EOL +
                  il_param_list ?
                  ')'
                ;

il_operand
                : constant
                | enum_value
                | variable_access
                ;

il_operand_list
                : il_operand
                  ( ',' il_operand )*
                ;

il_simple_inst_list
                : il_simple_instruction +
                ;

il_simple_instruction
                : ( il_simple_operation
                  | il_expr
                  | il_formal_func_call
                  ) EOL +
                ;

il_param_list
                : il_param_inst *
                  il_param_last_inst
                ;

il_param_inst
                : ( il_param_assign
                  | il_param_out_assign
                  ) ',' EOL +
                ;

il_param_last_inst
                : ( il_param_assign
                  | il_param_out_assign
                  ) EOL +
                ;

il_param_assign
                : il_assignment
                  ( il_operand
                  | ( '(' EOL +
                          il_simple_inst_list
                      ')'
                    )
                  )
                  ;

il_param_out_assign
                : il_assign_out_operator
                  variable_access
                ;

il_simple_operator
                : 'IL_Operator'
                | IL_Expr_Operator
                ;
// il_simple_operator : 'LD' | 'LDN' | 'ST' | 'STN' | 'ST?' | 'NOT' | 'S' | 'R'
// | 'S1' | 'R1' | 'CLK' | 'CU' | 'CD' | 'PV'
// | 'IN' | 'PT' | IL_Expr_Operator;
// IL_Expr_Operator : 'AND' | '&' | 'OR' | 'XOR' | 'ANDN' | '&N' | 'ORN'
// | 'XORN' | 'ADD' | 'SUB' | 'MUL' | 'DIV'
// | 'MOD' | 'GT' | 'GE' | 'EQ' | 'LT' | 'LE' | 'NE';

il_assignment
                : variable_name ':' '='
                ;

il_assign_out_operator
                : 'NOT' ? variable_name '=''>'
                ;
// Table 71 - 72 - Language Structured Text (ST)

expression                              //SC DONE
                : xor_expr ( 'OR' xor_expr )*
                ;

constant_expr                           //SC DONE
                : expression
                ;
// a constant expression must evaluate to a constant value at compile time

xor_expr                                    //SC DONE
                : and_expr ( 'XOR' and_expr )*
                ;

and_expr                                    //SC DONE
                : compare_expr
                  ( ( '&'
                    | 'AND'
                    ) compare_expr
                  )*
                ;

compare_expr                                    //SC DONE
                : ( equ_expr
                    ( ( '='
                      | '<>'
                      ) equ_expr
                    )*
                  )
                ;

equ_expr                                //SC DONE
                : add_expr
                  ( ( '<'
                    | '>'
                    | '<='
                    | '>='
                    ) 
                    add_expr
                  )*;

add_expr                                    //SC DONE
                : term
                  ( ( '+'
                    | '-'   
                    ) term
                  )*
                ;

term                                                                    //SC DONE
                : power_expr
                  ( ('*' 
                  | '/' 
                  | 'MOD') 
                  power_expr
                  )*
                  ;

power_expr                                          //SC DONE
                : unary_expr
                  ( '**' unary_expr )*
                ;

unary_expr                                                  //SC DONE
                : '-'
                | '+'
                | 'NOT' ? primary_expr
                ;


//enum_value和variable_access都能匹配id，但是由于enum_value在前，所以id会匹配到enum_value上
primary_expr                                        //SC DONE
                : constant
                | enum_value
                | variable_access
                | func_call
                | ref_value
                | '(' expression ')'
                ;

variable_access
                : variable
                  multibit_part_access?
                ;

multibit_part_access
                : '.' ( Unsigned_int
                      | '%' ( 'X'
                            | 'B'
                            | 'W'
                            | 'D'
                            | 'L'
                            ) ? Unsigned_int
                      )
                ;

func_call
                : func_access                
                '('
                ( param_assign
                ( ',' param_assign )*       //将调用函数的参数信息挂树 按照特殊的顺序装进容器（还有处理默认参数问题）
                )?
                ')'
                ;

//stmt_list
//                : ( stmt ? ';' )+
//                ;
stmt_list                       //SC DONE
                : stmt *
                ;
// stmt_list : ( stmt ? ';' )*;

stmt                            //SC DONE
                : assign_stmt';'
                | subprog_ctrl_stmt';'
                | selection_stmt';'?
                | iteration_stmt';'?
                | print_stmt ';'?       //内部测试语法
                ;

print_stmt
                :
                  'PRINT'
                  '('
                   (print_stmt_element
                    ('+' print_stmt_element)*
                   )?
                  ')'
                ;

print_stmt_element
                :
                    s_byte_char
                  | D_byte_char
                  | identifier
                ;


assign_stmt
                  //ariable挂树信息 等号左值名称 例 A->B
                : ( variable ':' '=' expression )   #variableAssignExpression   //SC DONE //expre结点挂树信息 例：*A * *B ...
                | ref_assign                        #refAssignExpression
                | pointer_assign                    #pointerAssign
                | assignment_attempt                #assignmentAttempt  //赋值尝试（暂时搁置的功能 不在当前version计划）
                | pointer_assigment_attempt         #pointerAssignAttempt
                ;

assignment_attempt
                : ( ref_name
                  | ref_deref
                  ) '?''='
                  ( ref_name
                  | ref_deref
                  | ref_value
                  )
                ;
// 指针的尝试赋值首先写着但不实现
pointer_assigment_attempt
                :(pointer_name
                  | pointer_dref
                  )'?''='
                  ( pointer_name
                   | pointer_dref
                   | pointer_value
                   )
                 ;

//invocation
//                : ( fb_instance_name
//                  | method_name
//                  | 'THIS'
//                  | (( 'THIS' '.'
//                     )?
//                     (( ( fb_instance_name
//                        | class_instance_name
//                        ) '.'
//                      )+
//                     ) method_name
//                    )
//                  )
//                  '('
//                  ( param_assign
//                  ( ',' param_assign )*
//                  )?
//                  ')'
//                ;

invocation
                :
//                ( 'THIS' '.')?
//                   ((( fb_instance_name
//                     | class_instance_name
//                     ) '.'
//                    )+
//                   )
                   invocation1branch
                   method_name
                    '('
                    ( param_assign
                     ( ',' param_assign )*
                    )?
                    ')'                                         #invocation1
                  |
//                  (fb_instance_name | method_name | 'THIS')
                  invocation2branch
                    '('
                    ( param_assign
                     ( ',' param_assign )*
                    )?
                    ')'                                         #invocation2
                  |
                  ( 'THIS' '.')?
                  method_name
                    '('
                    ( param_assign
                     ( ',' param_assign )*
                    )?
                    ')'                                              #invocation3
                ;

invocation1branch
                : ( 'THIS' '.')?
                  (
                  instance_name '.'
                  )+
                ;

invocation2branch
                :
                  fb_instance_name | method_name | 'THIS'
                ;

subprog_ctrl_stmt
                : func_call                         #callFunc     
                // | invocation                        #invocationCall
                | 'SUPER''.'derived_func_name'(' ')'#superCall
                | 'RETURN'                          #return
                ;

param_assign
                : ( ( variable_name ':' '=' )?
                      expression
                  )                             #inputParam
                | ref_assign                    #refParam
                | ( 'NOT' ? variable_name '=>'
                    variable
                  )                             #outParam
                ;

selection_stmt                 //SC DONE
                : if_stmt
                | case_stmt
                ;

if_stmt                                             //SC DONE
                : 'IF' expression 'THEN' stmt_list
//                  ( 'ELSIF' expression 'THEN'
//                  stmt_list
//                  )*
                  elsif_stmt*
//                  ( 'ELSE' stmt_list )?
                  else_stmt?
                  'END_IF'
                ;

elsif_stmt                      //SC DONE
                :
                  'ELSIF' expression 'THEN'
                  stmt_list
                ;

else_stmt       :                   //SC DONE
                  'ELSE' stmt_list
                ;


case_stmt                                                       //SC DONE
                : 'CASE' expression 'OF' case_selection +
//                  ( 'ELSE' stmt_list )?
                  else_stmt?
                  'END_CASE'
                ;

case_selection                                  //SC DONE
                : case_list ':'
                  stmt_list
                ;

case_list
                : case_list_elem            //SC DONE
                  ( ',' case_list_elem )*
                ;
case_list_elem                          //SC DONE
                : subrange
                | constant_expr
                ;

iteration_stmt          //SC DONE
                : for_stmt
                | while_stmt
                | repeat_stmt
                | EXITORCONTINUE
//                | 'EXIT'
//                | 'CONTINUE'
                ;

for_stmt
                : 'FOR' control_variable ':' '='
//                for_list
                  expression 'TO' expression
                  by_list? 'DO' stmt_list
                  'END_FOR'
                ;

control_variable
                : identifier
                ;

//for_list
//                : expression 'TO' expression
//                  by_list?
//                ;

by_list         :
                  'BY' expression
                ;

while_stmt      : 'WHILE' expression 'DO'
                  stmt_list
                  'END_WHILE'
                ;

repeat_stmt
                : 'REPEAT' stmt_list 'UNTIL'
                  expression
                  'END_REPEAT'
                ;
// Table 73 - 76 - Graphic languages elements

ladder_diagram
                : LD_Rung +
                ;

fb_diagram
                : FBD_Network +
                ;

reservedKeyword
                :
                |'TYPE'
                |'END_TYPE'
                |'ARRAY'
                |'OF'
                |'STRUCT'
                |'OVERLAP'
                |'END_STRUCT'
                |'REF_TO'
                |'REF'
                |'POINTER'
                |'TO'
                |'ADR'
                |'DREF'
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
                |'END_REPEAT'
                ;




Sign_Int_Type_Name
                : Sign_Int_Type
                ;
fragment
Sign_Int_Type
                : 'SINT'
                | 'INT'
                | 'DINT'
                | 'LINT'
                ;

Unsign_Int_Type_Name
                : Unsign_Int_Type
                ;
fragment
Unsign_Int_Type
                : 'USINT'
                | 'UINT'
                | 'UDINT'
                | 'ULINT'
                ;

Real_Type_Name
                :Real_Type
                ;
fragment
Real_Type
                : 'REAL'
                | 'LREAL'
                ;

Time_Type_Name
                : Time_Type
                ;
fragment
Time_Type       : 'TIME'
                | 'LTIME'
                ;

Access_Spec
                : Access
                ;
fragment
Access          : 'PUBLIC'
                | 'PROTECTED'
                | 'PRIVATE'
                | 'INTERNAL'
                ;

Tod_Type_Name
                : Tod_Type_
                ;
fragment
Tod_Type_       : 'TIME_OF_DAY'
                | 'TOD'
                | 'LTOD'
                ;

Multibits_Type_Name
                : Multibits_Type
                ;
fragment
Multibits_Type
                : 'BYTE'
                | 'WORD'
                | 'DWORD'
                | 'LWORD'
                ;

Std_Func_Name : 'TRUNC' | 'ABS' | 'SQRT' | 'LN' | 'LOG' | 'EXP'
  | 'SIN' | 'COS' | 'TAN' | 'ASIN' | 'ACOS' | 'ATAN' | 'ATAN2 '
  | 'ADD' | 'SUB' | 'MUL' | 'DIV' | 'MOD' | 'EXPT' | 'MOVE '
  | 'SHL' | 'SHR' | 'ROL' | 'ROR'
  | 'AND' | 'OR' | 'XOR' | 'NOT'
  | 'SEL' | 'MAX' | 'MIN' | 'LIMIT' | 'MUX '
  | 'GT' | 'GE' | 'EQ' | 'LE' | 'LT' | 'NE'
  | 'LEN' | 'LEFT' | 'RIGHT' | 'MID' | 'CONCAT' | 'INSERT' | 'DELETE' | 'REPLACE' | 'FIND';

Std_FB_Name : 'SR' | 'RS' | 'R_TRIG' | 'F_TRIG' | 'CTU'| 'CTD' | 'CTUD' | 'TP' | 'TON' | 'TOF';

Access_Direction : 'READ_WRITE' | 'READ_ONLY';
IL_Expr_Operator : 'IL_Expr_Operator';
IL_Call_Operator : 'CAL' | 'CALC' | 'CALCN';
IL_Return_Operator : 'RT' | 'RETC' | 'RETCN';
IL_Jump_Operator : 'JMP' | 'JMPC' | 'JMPCN';
Null : 'NULL';
LD_Rung : 'syntaxlexer for graphical languages not shown here';
FBD_Network : 'syntaxlexer for graphical languages not shown here11';
Other_Languages : 'syntaxlexer for other languages not shown here';

Date_Type_Name : Date_Type;
Date_Type : 'DATE' | 'LDATE';

DT_Type_Name:Date_Type;
fragment DT_Type : 'DATE_AND_TIME' | 'DT' | 'LDT';

Bool_Type_Name : 'BOOL';

FINALORABSTRACT :
                  'FINAL'
                  | 'ABSTRACT'
                ;
OVERRIDE        :
                  'OVERRIDE'
                ;

RETAINORNONRETAIN
                :
                  'RETAIN' | 'NON_RETAIN'
                ;
CONSTANT
                :
                  'CONSTANT'
                ;
EXITORCONTINUE  :
                  'CONTINUE'
                | 'EXIT'
                ;