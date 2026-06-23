// Generated from D:/source/Project/ST2C/java/src/main/resources/antlr4/PLCSTPARSER.g4 by ANTLR 4.13.2
package antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PLCSTPARSERParser}.
 */
public interface PLCSTPARSERListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#startpoint}.
	 * @param ctx the parse tree
	 */
	void enterStartpoint(PLCSTPARSERParser.StartpointContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#startpoint}.
	 * @param ctx the parse tree
	 */
	void exitStartpoint(PLCSTPARSERParser.StartpointContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(PLCSTPARSERParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(PLCSTPARSERParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(PLCSTPARSERParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(PLCSTPARSERParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#numeric_literal}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_literal(PLCSTPARSERParser.Numeric_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#numeric_literal}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_literal(PLCSTPARSERParser.Numeric_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#int_literal}.
	 * @param ctx the parse tree
	 */
	void enterInt_literal(PLCSTPARSERParser.Int_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#int_literal}.
	 * @param ctx the parse tree
	 */
	void exitInt_literal(PLCSTPARSERParser.Int_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#signed_int}.
	 * @param ctx the parse tree
	 */
	void enterSigned_int(PLCSTPARSERParser.Signed_intContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#signed_int}.
	 * @param ctx the parse tree
	 */
	void exitSigned_int(PLCSTPARSERParser.Signed_intContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#binary_int}.
	 * @param ctx the parse tree
	 */
	void enterBinary_int(PLCSTPARSERParser.Binary_intContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#binary_int}.
	 * @param ctx the parse tree
	 */
	void exitBinary_int(PLCSTPARSERParser.Binary_intContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#octal_int}.
	 * @param ctx the parse tree
	 */
	void enterOctal_int(PLCSTPARSERParser.Octal_intContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#octal_int}.
	 * @param ctx the parse tree
	 */
	void exitOctal_int(PLCSTPARSERParser.Octal_intContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#hex_int}.
	 * @param ctx the parse tree
	 */
	void enterHex_int(PLCSTPARSERParser.Hex_intContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#hex_int}.
	 * @param ctx the parse tree
	 */
	void exitHex_int(PLCSTPARSERParser.Hex_intContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#real_literal}.
	 * @param ctx the parse tree
	 */
	void enterReal_literal(PLCSTPARSERParser.Real_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#real_literal}.
	 * @param ctx the parse tree
	 */
	void exitReal_literal(PLCSTPARSERParser.Real_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#bit_str_literal}.
	 * @param ctx the parse tree
	 */
	void enterBit_str_literal(PLCSTPARSERParser.Bit_str_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#bit_str_literal}.
	 * @param ctx the parse tree
	 */
	void exitBit_str_literal(PLCSTPARSERParser.Bit_str_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void enterBool_literal(PLCSTPARSERParser.Bool_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void exitBool_literal(PLCSTPARSERParser.Bool_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#char_literal}.
	 * @param ctx the parse tree
	 */
	void enterChar_literal(PLCSTPARSERParser.Char_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#char_literal}.
	 * @param ctx the parse tree
	 */
	void exitChar_literal(PLCSTPARSERParser.Char_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#char_str}.
	 * @param ctx the parse tree
	 */
	void enterChar_str(PLCSTPARSERParser.Char_strContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#char_str}.
	 * @param ctx the parse tree
	 */
	void exitChar_str(PLCSTPARSERParser.Char_strContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#s_byte_char}.
	 * @param ctx the parse tree
	 */
	void enterS_byte_char(PLCSTPARSERParser.S_byte_charContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#s_byte_char}.
	 * @param ctx the parse tree
	 */
	void exitS_byte_char(PLCSTPARSERParser.S_byte_charContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#d_byte_char_value}.
	 * @param ctx the parse tree
	 */
	void enterD_byte_char_value(PLCSTPARSERParser.D_byte_char_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#d_byte_char_value}.
	 * @param ctx the parse tree
	 */
	void exitD_byte_char_value(PLCSTPARSERParser.D_byte_char_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#time_literal}.
	 * @param ctx the parse tree
	 */
	void enterTime_literal(PLCSTPARSERParser.Time_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#time_literal}.
	 * @param ctx the parse tree
	 */
	void exitTime_literal(PLCSTPARSERParser.Time_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#duration}.
	 * @param ctx the parse tree
	 */
	void enterDuration(PLCSTPARSERParser.DurationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#duration}.
	 * @param ctx the parse tree
	 */
	void exitDuration(PLCSTPARSERParser.DurationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fix_point}.
	 * @param ctx the parse tree
	 */
	void enterFix_point(PLCSTPARSERParser.Fix_pointContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fix_point}.
	 * @param ctx the parse tree
	 */
	void exitFix_point(PLCSTPARSERParser.Fix_pointContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(PLCSTPARSERParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(PLCSTPARSERParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#days}.
	 * @param ctx the parse tree
	 */
	void enterDays(PLCSTPARSERParser.DaysContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#days}.
	 * @param ctx the parse tree
	 */
	void exitDays(PLCSTPARSERParser.DaysContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#hours}.
	 * @param ctx the parse tree
	 */
	void enterHours(PLCSTPARSERParser.HoursContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#hours}.
	 * @param ctx the parse tree
	 */
	void exitHours(PLCSTPARSERParser.HoursContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#minutes}.
	 * @param ctx the parse tree
	 */
	void enterMinutes(PLCSTPARSERParser.MinutesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#minutes}.
	 * @param ctx the parse tree
	 */
	void exitMinutes(PLCSTPARSERParser.MinutesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#seconds}.
	 * @param ctx the parse tree
	 */
	void enterSeconds(PLCSTPARSERParser.SecondsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#seconds}.
	 * @param ctx the parse tree
	 */
	void exitSeconds(PLCSTPARSERParser.SecondsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#milliseconds}.
	 * @param ctx the parse tree
	 */
	void enterMilliseconds(PLCSTPARSERParser.MillisecondsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#milliseconds}.
	 * @param ctx the parse tree
	 */
	void exitMilliseconds(PLCSTPARSERParser.MillisecondsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#microseconds}.
	 * @param ctx the parse tree
	 */
	void enterMicroseconds(PLCSTPARSERParser.MicrosecondsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#microseconds}.
	 * @param ctx the parse tree
	 */
	void exitMicroseconds(PLCSTPARSERParser.MicrosecondsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#nanoseconds}.
	 * @param ctx the parse tree
	 */
	void enterNanoseconds(PLCSTPARSERParser.NanosecondsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#nanoseconds}.
	 * @param ctx the parse tree
	 */
	void exitNanoseconds(PLCSTPARSERParser.NanosecondsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#time_of_day}.
	 * @param ctx the parse tree
	 */
	void enterTime_of_day(PLCSTPARSERParser.Time_of_dayContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#time_of_day}.
	 * @param ctx the parse tree
	 */
	void exitTime_of_day(PLCSTPARSERParser.Time_of_dayContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#daytime}.
	 * @param ctx the parse tree
	 */
	void enterDaytime(PLCSTPARSERParser.DaytimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#daytime}.
	 * @param ctx the parse tree
	 */
	void exitDaytime(PLCSTPARSERParser.DaytimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#day_hour}.
	 * @param ctx the parse tree
	 */
	void enterDay_hour(PLCSTPARSERParser.Day_hourContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#day_hour}.
	 * @param ctx the parse tree
	 */
	void exitDay_hour(PLCSTPARSERParser.Day_hourContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#day_minute}.
	 * @param ctx the parse tree
	 */
	void enterDay_minute(PLCSTPARSERParser.Day_minuteContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#day_minute}.
	 * @param ctx the parse tree
	 */
	void exitDay_minute(PLCSTPARSERParser.Day_minuteContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#day_second}.
	 * @param ctx the parse tree
	 */
	void enterDay_second(PLCSTPARSERParser.Day_secondContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#day_second}.
	 * @param ctx the parse tree
	 */
	void exitDay_second(PLCSTPARSERParser.Day_secondContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#date}.
	 * @param ctx the parse tree
	 */
	void enterDate(PLCSTPARSERParser.DateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#date}.
	 * @param ctx the parse tree
	 */
	void exitDate(PLCSTPARSERParser.DateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#date_literal}.
	 * @param ctx the parse tree
	 */
	void enterDate_literal(PLCSTPARSERParser.Date_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#date_literal}.
	 * @param ctx the parse tree
	 */
	void exitDate_literal(PLCSTPARSERParser.Date_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#year}.
	 * @param ctx the parse tree
	 */
	void enterYear(PLCSTPARSERParser.YearContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#year}.
	 * @param ctx the parse tree
	 */
	void exitYear(PLCSTPARSERParser.YearContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#month}.
	 * @param ctx the parse tree
	 */
	void enterMonth(PLCSTPARSERParser.MonthContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#month}.
	 * @param ctx the parse tree
	 */
	void exitMonth(PLCSTPARSERParser.MonthContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#day}.
	 * @param ctx the parse tree
	 */
	void enterDay(PLCSTPARSERParser.DayContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#day}.
	 * @param ctx the parse tree
	 */
	void exitDay(PLCSTPARSERParser.DayContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#date_and_time}.
	 * @param ctx the parse tree
	 */
	void enterDate_and_time(PLCSTPARSERParser.Date_and_timeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#date_and_time}.
	 * @param ctx the parse tree
	 */
	void exitDate_and_time(PLCSTPARSERParser.Date_and_timeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#data_type_access}.
	 * @param ctx the parse tree
	 */
	void enterData_type_access(PLCSTPARSERParser.Data_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#data_type_access}.
	 * @param ctx the parse tree
	 */
	void exitData_type_access(PLCSTPARSERParser.Data_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#elem_type_name}.
	 * @param ctx the parse tree
	 */
	void enterElem_type_name(PLCSTPARSERParser.Elem_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#elem_type_name}.
	 * @param ctx the parse tree
	 */
	void exitElem_type_name(PLCSTPARSERParser.Elem_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#numeric_type_name}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_type_name(PLCSTPARSERParser.Numeric_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#numeric_type_name}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_type_name(PLCSTPARSERParser.Numeric_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#int_type_name}.
	 * @param ctx the parse tree
	 */
	void enterInt_type_name(PLCSTPARSERParser.Int_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#int_type_name}.
	 * @param ctx the parse tree
	 */
	void exitInt_type_name(PLCSTPARSERParser.Int_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#bit_str_type_name}.
	 * @param ctx the parse tree
	 */
	void enterBit_str_type_name(PLCSTPARSERParser.Bit_str_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#bit_str_type_name}.
	 * @param ctx the parse tree
	 */
	void exitBit_str_type_name(PLCSTPARSERParser.Bit_str_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_type_access}.
	 * @param ctx the parse tree
	 */
	void enterDerived_type_access(PLCSTPARSERParser.Derived_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_type_access}.
	 * @param ctx the parse tree
	 */
	void exitDerived_type_access(PLCSTPARSERParser.Derived_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#string_type_access}.
	 * @param ctx the parse tree
	 */
	void enterString_type_access(PLCSTPARSERParser.String_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#string_type_access}.
	 * @param ctx the parse tree
	 */
	void exitString_type_access(PLCSTPARSERParser.String_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#single_elem_type_access}.
	 * @param ctx the parse tree
	 */
	void enterSingle_elem_type_access(PLCSTPARSERParser.Single_elem_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#single_elem_type_access}.
	 * @param ctx the parse tree
	 */
	void exitSingle_elem_type_access(PLCSTPARSERParser.Single_elem_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#simple_type_access}.
	 * @param ctx the parse tree
	 */
	void enterSimple_type_access(PLCSTPARSERParser.Simple_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#simple_type_access}.
	 * @param ctx the parse tree
	 */
	void exitSimple_type_access(PLCSTPARSERParser.Simple_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange_type_access}.
	 * @param ctx the parse tree
	 */
	void enterSubrange_type_access(PLCSTPARSERParser.Subrange_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_access}.
	 * @param ctx the parse tree
	 */
	void exitSubrange_type_access(PLCSTPARSERParser.Subrange_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_type_access}.
	 * @param ctx the parse tree
	 */
	void enterEnum_type_access(PLCSTPARSERParser.Enum_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_type_access}.
	 * @param ctx the parse tree
	 */
	void exitEnum_type_access(PLCSTPARSERParser.Enum_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_type_access}.
	 * @param ctx the parse tree
	 */
	void enterArray_type_access(PLCSTPARSERParser.Array_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_type_access}.
	 * @param ctx the parse tree
	 */
	void exitArray_type_access(PLCSTPARSERParser.Array_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_type_access}.
	 * @param ctx the parse tree
	 */
	void enterStruct_type_access(PLCSTPARSERParser.Struct_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_type_access}.
	 * @param ctx the parse tree
	 */
	void exitStruct_type_access(PLCSTPARSERParser.Struct_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#simple_type_name}.
	 * @param ctx the parse tree
	 */
	void enterSimple_type_name(PLCSTPARSERParser.Simple_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#simple_type_name}.
	 * @param ctx the parse tree
	 */
	void exitSimple_type_name(PLCSTPARSERParser.Simple_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange_type_name}.
	 * @param ctx the parse tree
	 */
	void enterSubrange_type_name(PLCSTPARSERParser.Subrange_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_name}.
	 * @param ctx the parse tree
	 */
	void exitSubrange_type_name(PLCSTPARSERParser.Subrange_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_type_name}.
	 * @param ctx the parse tree
	 */
	void enterEnum_type_name(PLCSTPARSERParser.Enum_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_type_name}.
	 * @param ctx the parse tree
	 */
	void exitEnum_type_name(PLCSTPARSERParser.Enum_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_type_name}.
	 * @param ctx the parse tree
	 */
	void enterArray_type_name(PLCSTPARSERParser.Array_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_type_name}.
	 * @param ctx the parse tree
	 */
	void exitArray_type_name(PLCSTPARSERParser.Array_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_type_name}.
	 * @param ctx the parse tree
	 */
	void enterStruct_type_name(PLCSTPARSERParser.Struct_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_type_name}.
	 * @param ctx the parse tree
	 */
	void exitStruct_type_name(PLCSTPARSERParser.Struct_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#data_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#data_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#type_decl}.
	 * @param ctx the parse tree
	 */
	void enterType_decl(PLCSTPARSERParser.Type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#type_decl}.
	 * @param ctx the parse tree
	 */
	void exitType_decl(PLCSTPARSERParser.Type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterDerived_type_decl(PLCSTPARSERParser.Derived_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitDerived_type_decl(PLCSTPARSERParser.Derived_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_type_name}.
	 * @param ctx the parse tree
	 */
	void enterDerived_type_name(PLCSTPARSERParser.Derived_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_type_name}.
	 * @param ctx the parse tree
	 */
	void exitDerived_type_name(PLCSTPARSERParser.Derived_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterDerived_spec_init(PLCSTPARSERParser.Derived_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitDerived_spec_init(PLCSTPARSERParser.Derived_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#simple_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#simple_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#simple_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterSimple_spec_init(PLCSTPARSERParser.Simple_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#simple_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitSimple_spec_init(PLCSTPARSERParser.Simple_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#simple_spec}.
	 * @param ctx the parse tree
	 */
	void enterSimple_spec(PLCSTPARSERParser.Simple_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#simple_spec}.
	 * @param ctx the parse tree
	 */
	void exitSimple_spec(PLCSTPARSERParser.Simple_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterSubrange_type_decl(PLCSTPARSERParser.Subrange_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitSubrange_type_decl(PLCSTPARSERParser.Subrange_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterSubrange_spec_init(PLCSTPARSERParser.Subrange_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitSubrange_spec_init(PLCSTPARSERParser.Subrange_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange_spec}.
	 * @param ctx the parse tree
	 */
	void enterSubrange_spec(PLCSTPARSERParser.Subrange_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange_spec}.
	 * @param ctx the parse tree
	 */
	void exitSubrange_spec(PLCSTPARSERParser.Subrange_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subrange}.
	 * @param ctx the parse tree
	 */
	void enterSubrange(PLCSTPARSERParser.SubrangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subrange}.
	 * @param ctx the parse tree
	 */
	void exitSubrange(PLCSTPARSERParser.SubrangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#named_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterNamed_spec_init(PLCSTPARSERParser.Named_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#named_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitNamed_spec_init(PLCSTPARSERParser.Named_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterEnum_spec_init(PLCSTPARSERParser.Enum_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitEnum_spec_init(PLCSTPARSERParser.Enum_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_value_spec}.
	 * @param ctx the parse tree
	 */
	void enterEnum_value_spec(PLCSTPARSERParser.Enum_value_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_value_spec}.
	 * @param ctx the parse tree
	 */
	void exitEnum_value_spec(PLCSTPARSERParser.Enum_value_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#enum_value}.
	 * @param ctx the parse tree
	 */
	void enterEnum_value(PLCSTPARSERParser.Enum_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#enum_value}.
	 * @param ctx the parse tree
	 */
	void exitEnum_value(PLCSTPARSERParser.Enum_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterArray_spec_init(PLCSTPARSERParser.Array_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitArray_spec_init(PLCSTPARSERParser.Array_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_spec}.
	 * @param ctx the parse tree
	 */
	void enterArray_spec(PLCSTPARSERParser.Array_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_spec}.
	 * @param ctx the parse tree
	 */
	void exitArray_spec(PLCSTPARSERParser.Array_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_init}.
	 * @param ctx the parse tree
	 */
	void enterArray_init(PLCSTPARSERParser.Array_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_init}.
	 * @param ctx the parse tree
	 */
	void exitArray_init(PLCSTPARSERParser.Array_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_elem_init}.
	 * @param ctx the parse tree
	 */
	void enterArray_elem_init(PLCSTPARSERParser.Array_elem_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_elem_init}.
	 * @param ctx the parse tree
	 */
	void exitArray_elem_init(PLCSTPARSERParser.Array_elem_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_elem_item_init}.
	 * @param ctx the parse tree
	 */
	void enterArray_elem_item_init(PLCSTPARSERParser.Array_elem_item_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_elem_item_init}.
	 * @param ctx the parse tree
	 */
	void exitArray_elem_item_init(PLCSTPARSERParser.Array_elem_item_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_elem_init_value}.
	 * @param ctx the parse tree
	 */
	void enterArray_elem_init_value(PLCSTPARSERParser.Array_elem_init_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_elem_init_value}.
	 * @param ctx the parse tree
	 */
	void exitArray_elem_init_value(PLCSTPARSERParser.Array_elem_init_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_spec}.
	 * @param ctx the parse tree
	 */
	void enterStruct_spec(PLCSTPARSERParser.Struct_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_spec}.
	 * @param ctx the parse tree
	 */
	void exitStruct_spec(PLCSTPARSERParser.Struct_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterStruct_spec_init(PLCSTPARSERParser.Struct_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitStruct_spec_init(PLCSTPARSERParser.Struct_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_decl}.
	 * @param ctx the parse tree
	 */
	void enterStruct_decl(PLCSTPARSERParser.Struct_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_decl}.
	 * @param ctx the parse tree
	 */
	void exitStruct_decl(PLCSTPARSERParser.Struct_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_elem_decl}.
	 * @param ctx the parse tree
	 */
	void enterStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_decl}.
	 * @param ctx the parse tree
	 */
	void exitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_elem_name}.
	 * @param ctx the parse tree
	 */
	void enterStruct_elem_name(PLCSTPARSERParser.Struct_elem_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_name}.
	 * @param ctx the parse tree
	 */
	void exitStruct_elem_name(PLCSTPARSERParser.Struct_elem_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_init}.
	 * @param ctx the parse tree
	 */
	void enterStruct_init(PLCSTPARSERParser.Struct_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_init}.
	 * @param ctx the parse tree
	 */
	void exitStruct_init(PLCSTPARSERParser.Struct_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_elem_init}.
	 * @param ctx the parse tree
	 */
	void enterStruct_elem_init(PLCSTPARSERParser.Struct_elem_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_init}.
	 * @param ctx the parse tree
	 */
	void exitStruct_elem_init(PLCSTPARSERParser.Struct_elem_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#str_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#str_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#string_type_name_identifier}.
	 * @param ctx the parse tree
	 */
	void enterString_type_name_identifier(PLCSTPARSERParser.String_type_name_identifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#string_type_name_identifier}.
	 * @param ctx the parse tree
	 */
	void exitString_type_name_identifier(PLCSTPARSERParser.String_type_name_identifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterRef_type_decl(PLCSTPARSERParser.Ref_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitRef_type_decl(PLCSTPARSERParser.Ref_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterRef_spec_init(PLCSTPARSERParser.Ref_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitRef_spec_init(PLCSTPARSERParser.Ref_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_spec}.
	 * @param ctx the parse tree
	 */
	void enterRef_spec(PLCSTPARSERParser.Ref_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_spec}.
	 * @param ctx the parse tree
	 */
	void exitRef_spec(PLCSTPARSERParser.Ref_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_type_name}.
	 * @param ctx the parse tree
	 */
	void enterRef_type_name(PLCSTPARSERParser.Ref_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_type_name}.
	 * @param ctx the parse tree
	 */
	void exitRef_type_name(PLCSTPARSERParser.Ref_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_type_access}.
	 * @param ctx the parse tree
	 */
	void enterRef_type_access(PLCSTPARSERParser.Ref_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_type_access}.
	 * @param ctx the parse tree
	 */
	void exitRef_type_access(PLCSTPARSERParser.Ref_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_name}.
	 * @param ctx the parse tree
	 */
	void enterRef_name(PLCSTPARSERParser.Ref_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_name}.
	 * @param ctx the parse tree
	 */
	void exitRef_name(PLCSTPARSERParser.Ref_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_value}.
	 * @param ctx the parse tree
	 */
	void enterRef_value(PLCSTPARSERParser.Ref_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_value}.
	 * @param ctx the parse tree
	 */
	void exitRef_value(PLCSTPARSERParser.Ref_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_addr}.
	 * @param ctx the parse tree
	 */
	void enterRef_addr(PLCSTPARSERParser.Ref_addrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_addr}.
	 * @param ctx the parse tree
	 */
	void exitRef_addr(PLCSTPARSERParser.Ref_addrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_assign}.
	 * @param ctx the parse tree
	 */
	void enterRef_assign(PLCSTPARSERParser.Ref_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_assign}.
	 * @param ctx the parse tree
	 */
	void exitRef_assign(PLCSTPARSERParser.Ref_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_deref}.
	 * @param ctx the parse tree
	 */
	void enterRef_deref(PLCSTPARSERParser.Ref_derefContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_deref}.
	 * @param ctx the parse tree
	 */
	void exitRef_deref(PLCSTPARSERParser.Ref_derefContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_type_decl}.
	 * @param ctx the parse tree
	 */
	void enterPointer_type_decl(PLCSTPARSERParser.Pointer_type_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_type_decl}.
	 * @param ctx the parse tree
	 */
	void exitPointer_type_decl(PLCSTPARSERParser.Pointer_type_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_type_name}.
	 * @param ctx the parse tree
	 */
	void enterPointer_type_name(PLCSTPARSERParser.Pointer_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_type_name}.
	 * @param ctx the parse tree
	 */
	void exitPointer_type_name(PLCSTPARSERParser.Pointer_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterPointer_spec_init(PLCSTPARSERParser.Pointer_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitPointer_spec_init(PLCSTPARSERParser.Pointer_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_name}.
	 * @param ctx the parse tree
	 */
	void enterPointer_name(PLCSTPARSERParser.Pointer_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_name}.
	 * @param ctx the parse tree
	 */
	void exitPointer_name(PLCSTPARSERParser.Pointer_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_spec}.
	 * @param ctx the parse tree
	 */
	void enterPointer_spec(PLCSTPARSERParser.Pointer_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_spec}.
	 * @param ctx the parse tree
	 */
	void exitPointer_spec(PLCSTPARSERParser.Pointer_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_value}.
	 * @param ctx the parse tree
	 */
	void enterPointer_value(PLCSTPARSERParser.Pointer_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_value}.
	 * @param ctx the parse tree
	 */
	void exitPointer_value(PLCSTPARSERParser.Pointer_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_adr}.
	 * @param ctx the parse tree
	 */
	void enterPointer_adr(PLCSTPARSERParser.Pointer_adrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_adr}.
	 * @param ctx the parse tree
	 */
	void exitPointer_adr(PLCSTPARSERParser.Pointer_adrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_dref}.
	 * @param ctx the parse tree
	 */
	void enterPointer_dref(PLCSTPARSERParser.Pointer_drefContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_dref}.
	 * @param ctx the parse tree
	 */
	void exitPointer_dref(PLCSTPARSERParser.Pointer_drefContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_assign}.
	 * @param ctx the parse tree
	 */
	void enterPointer_assign(PLCSTPARSERParser.Pointer_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_assign}.
	 * @param ctx the parse tree
	 */
	void exitPointer_assign(PLCSTPARSERParser.Pointer_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(PLCSTPARSERParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(PLCSTPARSERParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_index}.
	 * @param ctx the parse tree
	 */
	void enterArray_index(PLCSTPARSERParser.Array_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_index}.
	 * @param ctx the parse tree
	 */
	void exitArray_index(PLCSTPARSERParser.Array_indexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code thisSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 */
	void enterThisSymbolic(PLCSTPARSERParser.ThisSymbolicContext ctx);
	/**
	 * Exit a parse tree produced by the {@code thisSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 */
	void exitThisSymbolic(PLCSTPARSERParser.ThisSymbolicContext ctx);
	/**
	 * Enter a parse tree produced by the {@code namespaceSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 */
	void enterNamespaceSymbolic(PLCSTPARSERParser.NamespaceSymbolicContext ctx);
	/**
	 * Exit a parse tree produced by the {@code namespaceSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 */
	void exitNamespaceSymbolic(PLCSTPARSERParser.NamespaceSymbolicContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#var_access}.
	 * @param ctx the parse tree
	 */
	void enterVar_access(PLCSTPARSERParser.Var_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#var_access}.
	 * @param ctx the parse tree
	 */
	void exitVar_access(PLCSTPARSERParser.Var_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void enterVariable_name(PLCSTPARSERParser.Variable_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void exitVariable_name(PLCSTPARSERParser.Variable_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#multi_elem_var}.
	 * @param ctx the parse tree
	 */
	void enterMulti_elem_var(PLCSTPARSERParser.Multi_elem_varContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#multi_elem_var}.
	 * @param ctx the parse tree
	 */
	void exitMulti_elem_var(PLCSTPARSERParser.Multi_elem_varContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subscript_list}.
	 * @param ctx the parse tree
	 */
	void enterSubscript_list(PLCSTPARSERParser.Subscript_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subscript_list}.
	 * @param ctx the parse tree
	 */
	void exitSubscript_list(PLCSTPARSERParser.Subscript_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#subscript}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(PLCSTPARSERParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#subscript}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(PLCSTPARSERParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_variable}.
	 * @param ctx the parse tree
	 */
	void enterStruct_variable(PLCSTPARSERParser.Struct_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_variable}.
	 * @param ctx the parse tree
	 */
	void exitStruct_variable(PLCSTPARSERParser.Struct_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_elem_select}.
	 * @param ctx the parse tree
	 */
	void enterStruct_elem_select(PLCSTPARSERParser.Struct_elem_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_select}.
	 * @param ctx the parse tree
	 */
	void exitStruct_elem_select(PLCSTPARSERParser.Struct_elem_selectContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#input_decls}.
	 * @param ctx the parse tree
	 */
	void enterInput_decls(PLCSTPARSERParser.Input_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#input_decls}.
	 * @param ctx the parse tree
	 */
	void exitInput_decls(PLCSTPARSERParser.Input_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#input_decl}.
	 * @param ctx the parse tree
	 */
	void enterInput_decl(PLCSTPARSERParser.Input_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#input_decl}.
	 * @param ctx the parse tree
	 */
	void exitInput_decl(PLCSTPARSERParser.Input_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#edge_decl}.
	 * @param ctx the parse tree
	 */
	void enterEdge_decl(PLCSTPARSERParser.Edge_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#edge_decl}.
	 * @param ctx the parse tree
	 */
	void exitEdge_decl(PLCSTPARSERParser.Edge_declContext ctx);
	/**
	 * Enter a parse tree produced by the {@code vardeclinit}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 */
	void enterVardeclinit(PLCSTPARSERParser.VardeclinitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code vardeclinit}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 */
	void exitVardeclinit(PLCSTPARSERParser.VardeclinitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code directNum}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 */
	void enterDirectNum(PLCSTPARSERParser.DirectNumContext ctx);
	/**
	 * Exit a parse tree produced by the {@code directNum}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 */
	void exitDirectNum(PLCSTPARSERParser.DirectNumContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#str_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterStr_spec_init(PLCSTPARSERParser.Str_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#str_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitStr_spec_init(PLCSTPARSERParser.Str_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#str_spec}.
	 * @param ctx the parse tree
	 */
	void enterStr_spec(PLCSTPARSERParser.Str_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#str_spec}.
	 * @param ctx the parse tree
	 */
	void exitStr_spec(PLCSTPARSERParser.Str_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#user_defination_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterUser_defination_spec_init(PLCSTPARSERParser.User_defination_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#user_defination_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitUser_defination_spec_init(PLCSTPARSERParser.User_defination_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_access}.
	 * @param ctx the parse tree
	 */
	void enterUser_defination_type_access(PLCSTPARSERParser.User_defination_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_access}.
	 * @param ctx the parse tree
	 */
	void exitUser_defination_type_access(PLCSTPARSERParser.User_defination_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_name}.
	 * @param ctx the parse tree
	 */
	void enterUser_defination_type_name(PLCSTPARSERParser.User_defination_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_name}.
	 * @param ctx the parse tree
	 */
	void exitUser_defination_type_name(PLCSTPARSERParser.User_defination_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ref_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterRef_var_decl(PLCSTPARSERParser.Ref_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ref_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitRef_var_decl(PLCSTPARSERParser.Ref_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterInterface_var_decl(PLCSTPARSERParser.Interface_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitInterface_var_decl(PLCSTPARSERParser.Interface_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#variable_list}.
	 * @param ctx the parse tree
	 */
	void enterVariable_list(PLCSTPARSERParser.Variable_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#variable_list}.
	 * @param ctx the parse tree
	 */
	void exitVariable_list(PLCSTPARSERParser.Variable_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_var_decl_init}.
	 * @param ctx the parse tree
	 */
	void enterArray_var_decl_init(PLCSTPARSERParser.Array_var_decl_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_var_decl_init}.
	 * @param ctx the parse tree
	 */
	void exitArray_var_decl_init(PLCSTPARSERParser.Array_var_decl_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_conformand}.
	 * @param ctx the parse tree
	 */
	void enterArray_conformand(PLCSTPARSERParser.Array_conformandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_conformand}.
	 * @param ctx the parse tree
	 */
	void exitArray_conformand(PLCSTPARSERParser.Array_conformandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_conform_decl}.
	 * @param ctx the parse tree
	 */
	void enterArray_conform_decl(PLCSTPARSERParser.Array_conform_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_conform_decl}.
	 * @param ctx the parse tree
	 */
	void exitArray_conform_decl(PLCSTPARSERParser.Array_conform_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl_init}.
	 * @param ctx the parse tree
	 */
	void enterStruct_var_decl_init(PLCSTPARSERParser.Struct_var_decl_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl_init}.
	 * @param ctx the parse tree
	 */
	void exitStruct_var_decl_init(PLCSTPARSERParser.Struct_var_decl_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_decl_no_init}.
	 * @param ctx the parse tree
	 */
	void enterFb_decl_no_init(PLCSTPARSERParser.Fb_decl_no_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_decl_no_init}.
	 * @param ctx the parse tree
	 */
	void exitFb_decl_no_init(PLCSTPARSERParser.Fb_decl_no_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_decl_init}.
	 * @param ctx the parse tree
	 */
	void enterFb_decl_init(PLCSTPARSERParser.Fb_decl_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_decl_init}.
	 * @param ctx the parse tree
	 */
	void exitFb_decl_init(PLCSTPARSERParser.Fb_decl_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_name}.
	 * @param ctx the parse tree
	 */
	void enterFb_name(PLCSTPARSERParser.Fb_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_name}.
	 * @param ctx the parse tree
	 */
	void exitFb_name(PLCSTPARSERParser.Fb_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_instance_name}.
	 * @param ctx the parse tree
	 */
	void enterFb_instance_name(PLCSTPARSERParser.Fb_instance_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_instance_name}.
	 * @param ctx the parse tree
	 */
	void exitFb_instance_name(PLCSTPARSERParser.Fb_instance_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#output_decls}.
	 * @param ctx the parse tree
	 */
	void enterOutput_decls(PLCSTPARSERParser.Output_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#output_decls}.
	 * @param ctx the parse tree
	 */
	void exitOutput_decls(PLCSTPARSERParser.Output_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#output_decl}.
	 * @param ctx the parse tree
	 */
	void enterOutput_decl(PLCSTPARSERParser.Output_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#output_decl}.
	 * @param ctx the parse tree
	 */
	void exitOutput_decl(PLCSTPARSERParser.Output_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#in_out_decls}.
	 * @param ctx the parse tree
	 */
	void enterIn_out_decls(PLCSTPARSERParser.In_out_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#in_out_decls}.
	 * @param ctx the parse tree
	 */
	void exitIn_out_decls(PLCSTPARSERParser.In_out_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#in_out_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterIn_out_var_decl(PLCSTPARSERParser.In_out_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#in_out_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitIn_out_var_decl(PLCSTPARSERParser.In_out_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#var_decl}.
	 * @param ctx the parse tree
	 */
	void enterVar_decl(PLCSTPARSERParser.Var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#var_decl}.
	 * @param ctx the parse tree
	 */
	void exitVar_decl(PLCSTPARSERParser.Var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#array_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterArray_var_decl(PLCSTPARSERParser.Array_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#array_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitArray_var_decl(PLCSTPARSERParser.Array_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterStruct_var_decl(PLCSTPARSERParser.Struct_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitStruct_var_decl(PLCSTPARSERParser.Struct_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#var_decls}.
	 * @param ctx the parse tree
	 */
	void enterVar_decls(PLCSTPARSERParser.Var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#var_decls}.
	 * @param ctx the parse tree
	 */
	void exitVar_decls(PLCSTPARSERParser.Var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#retain_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterRetain_var_decls(PLCSTPARSERParser.Retain_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#retain_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitRetain_var_decls(PLCSTPARSERParser.Retain_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#loc_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterLoc_var_decls(PLCSTPARSERParser.Loc_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#loc_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitLoc_var_decls(PLCSTPARSERParser.Loc_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#loc_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterLoc_var_decl(PLCSTPARSERParser.Loc_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#loc_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitLoc_var_decl(PLCSTPARSERParser.Loc_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#temp_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterTemp_var_decls(PLCSTPARSERParser.Temp_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#temp_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitTemp_var_decls(PLCSTPARSERParser.Temp_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#external_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterExternal_var_decls(PLCSTPARSERParser.External_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#external_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitExternal_var_decls(PLCSTPARSERParser.External_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#external_decl}.
	 * @param ctx the parse tree
	 */
	void enterExternal_decl(PLCSTPARSERParser.External_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#external_decl}.
	 * @param ctx the parse tree
	 */
	void exitExternal_decl(PLCSTPARSERParser.External_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#global_var_name}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_var_name(PLCSTPARSERParser.Global_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#global_var_name}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_var_name(PLCSTPARSERParser.Global_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#global_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#global_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#global_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_var_decl(PLCSTPARSERParser.Global_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#global_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_var_decl(PLCSTPARSERParser.Global_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#global_var_spec}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_var_spec(PLCSTPARSERParser.Global_var_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#global_var_spec}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_var_spec(PLCSTPARSERParser.Global_var_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#loc_var_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterLoc_var_spec_init(PLCSTPARSERParser.Loc_var_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#loc_var_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitLoc_var_spec_init(PLCSTPARSERParser.Loc_var_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#located_at}.
	 * @param ctx the parse tree
	 */
	void enterLocated_at(PLCSTPARSERParser.Located_atContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#located_at}.
	 * @param ctx the parse tree
	 */
	void exitLocated_at(PLCSTPARSERParser.Located_atContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#located_at_init}.
	 * @param ctx the parse tree
	 */
	void enterLocated_at_init(PLCSTPARSERParser.Located_at_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#located_at_init}.
	 * @param ctx the parse tree
	 */
	void exitLocated_at_init(PLCSTPARSERParser.Located_at_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#str_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterStr_var_decl(PLCSTPARSERParser.Str_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#str_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitStr_var_decl(PLCSTPARSERParser.Str_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterS_byte_str_var_decl(PLCSTPARSERParser.S_byte_str_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitS_byte_str_var_decl(PLCSTPARSERParser.S_byte_str_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_spec}.
	 * @param ctx the parse tree
	 */
	void enterS_byte_str_spec(PLCSTPARSERParser.S_byte_str_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_spec}.
	 * @param ctx the parse tree
	 */
	void exitS_byte_str_spec(PLCSTPARSERParser.S_byte_str_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterD_byte_str_var_decl(PLCSTPARSERParser.D_byte_str_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitD_byte_str_var_decl(PLCSTPARSERParser.D_byte_str_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_spec}.
	 * @param ctx the parse tree
	 */
	void enterD_byte_str_spec(PLCSTPARSERParser.D_byte_str_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_spec}.
	 * @param ctx the parse tree
	 */
	void exitD_byte_str_spec(PLCSTPARSERParser.D_byte_str_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var_decl}.
	 * @param ctx the parse tree
	 */
	void enterLoc_partly_var_decl(PLCSTPARSERParser.Loc_partly_var_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var_decl}.
	 * @param ctx the parse tree
	 */
	void exitLoc_partly_var_decl(PLCSTPARSERParser.Loc_partly_var_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var}.
	 * @param ctx the parse tree
	 */
	void enterLoc_partly_var(PLCSTPARSERParser.Loc_partly_varContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var}.
	 * @param ctx the parse tree
	 */
	void exitLoc_partly_var(PLCSTPARSERParser.Loc_partly_varContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#var_spec}.
	 * @param ctx the parse tree
	 */
	void enterVar_spec(PLCSTPARSERParser.Var_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#var_spec}.
	 * @param ctx the parse tree
	 */
	void exitVar_spec(PLCSTPARSERParser.Var_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_name}.
	 * @param ctx the parse tree
	 */
	void enterFunc_name(PLCSTPARSERParser.Func_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_name}.
	 * @param ctx the parse tree
	 */
	void exitFunc_name(PLCSTPARSERParser.Func_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_access}.
	 * @param ctx the parse tree
	 */
	void enterFunc_access(PLCSTPARSERParser.Func_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_access}.
	 * @param ctx the parse tree
	 */
	void exitFunc_access(PLCSTPARSERParser.Func_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#scope_name}.
	 * @param ctx the parse tree
	 */
	void enterScope_name(PLCSTPARSERParser.Scope_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#scope_name}.
	 * @param ctx the parse tree
	 */
	void exitScope_name(PLCSTPARSERParser.Scope_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_func_name}.
	 * @param ctx the parse tree
	 */
	void enterDerived_func_name(PLCSTPARSERParser.Derived_func_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_func_name}.
	 * @param ctx the parse tree
	 */
	void exitDerived_func_name(PLCSTPARSERParser.Derived_func_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_decl}.
	 * @param ctx the parse tree
	 */
	void enterFunc_decl(PLCSTPARSERParser.Func_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_decl}.
	 * @param ctx the parse tree
	 */
	void exitFunc_decl(PLCSTPARSERParser.Func_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#io_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterIo_var_decls(PLCSTPARSERParser.Io_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#io_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitIo_var_decls(PLCSTPARSERParser.Io_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterFunc_var_decls(PLCSTPARSERParser.Func_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitFunc_var_decls(PLCSTPARSERParser.Func_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_body}.
	 * @param ctx the parse tree
	 */
	void enterFunc_body(PLCSTPARSERParser.Func_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_body}.
	 * @param ctx the parse tree
	 */
	void exitFunc_body(PLCSTPARSERParser.Func_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_type_name}.
	 * @param ctx the parse tree
	 */
	void enterFb_type_name(PLCSTPARSERParser.Fb_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_type_name}.
	 * @param ctx the parse tree
	 */
	void exitFb_type_name(PLCSTPARSERParser.Fb_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_type_access}.
	 * @param ctx the parse tree
	 */
	void enterFb_type_access(PLCSTPARSERParser.Fb_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_type_access}.
	 * @param ctx the parse tree
	 */
	void exitFb_type_access(PLCSTPARSERParser.Fb_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#derived_fb_name}.
	 * @param ctx the parse tree
	 */
	void enterDerived_fb_name(PLCSTPARSERParser.Derived_fb_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#derived_fb_name}.
	 * @param ctx the parse tree
	 */
	void exitDerived_fb_name(PLCSTPARSERParser.Derived_fb_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_decl}.
	 * @param ctx the parse tree
	 */
	void enterFb_decl(PLCSTPARSERParser.Fb_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_decl}.
	 * @param ctx the parse tree
	 */
	void exitFb_decl(PLCSTPARSERParser.Fb_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_io_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterFb_io_var_decls(PLCSTPARSERParser.Fb_io_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_io_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitFb_io_var_decls(PLCSTPARSERParser.Fb_io_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_input_decls}.
	 * @param ctx the parse tree
	 */
	void enterFb_input_decls(PLCSTPARSERParser.Fb_input_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_input_decls}.
	 * @param ctx the parse tree
	 */
	void exitFb_input_decls(PLCSTPARSERParser.Fb_input_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_input_decl}.
	 * @param ctx the parse tree
	 */
	void enterFb_input_decl(PLCSTPARSERParser.Fb_input_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_input_decl}.
	 * @param ctx the parse tree
	 */
	void exitFb_input_decl(PLCSTPARSERParser.Fb_input_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_output_decls}.
	 * @param ctx the parse tree
	 */
	void enterFb_output_decls(PLCSTPARSERParser.Fb_output_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_output_decls}.
	 * @param ctx the parse tree
	 */
	void exitFb_output_decls(PLCSTPARSERParser.Fb_output_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_output_decl}.
	 * @param ctx the parse tree
	 */
	void enterFb_output_decl(PLCSTPARSERParser.Fb_output_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_output_decl}.
	 * @param ctx the parse tree
	 */
	void exitFb_output_decl(PLCSTPARSERParser.Fb_output_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#other_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterOther_var_decls(PLCSTPARSERParser.Other_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#other_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitOther_var_decls(PLCSTPARSERParser.Other_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#no_retain_var_decls}.
	 * @param ctx the parse tree
	 */
	void enterNo_retain_var_decls(PLCSTPARSERParser.No_retain_var_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#no_retain_var_decls}.
	 * @param ctx the parse tree
	 */
	void exitNo_retain_var_decls(PLCSTPARSERParser.No_retain_var_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_body}.
	 * @param ctx the parse tree
	 */
	void enterFb_body(PLCSTPARSERParser.Fb_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_body}.
	 * @param ctx the parse tree
	 */
	void exitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#method_decl}.
	 * @param ctx the parse tree
	 */
	void enterMethod_decl(PLCSTPARSERParser.Method_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#method_decl}.
	 * @param ctx the parse tree
	 */
	void exitMethod_decl(PLCSTPARSERParser.Method_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#method_name}.
	 * @param ctx the parse tree
	 */
	void enterMethod_name(PLCSTPARSERParser.Method_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#method_name}.
	 * @param ctx the parse tree
	 */
	void exitMethod_name(PLCSTPARSERParser.Method_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#class_decl}.
	 * @param ctx the parse tree
	 */
	void enterClass_decl(PLCSTPARSERParser.Class_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#class_decl}.
	 * @param ctx the parse tree
	 */
	void exitClass_decl(PLCSTPARSERParser.Class_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#class_type_name}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_name(PLCSTPARSERParser.Class_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#class_type_name}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_name(PLCSTPARSERParser.Class_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#class_type_access}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_access(PLCSTPARSERParser.Class_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#class_type_access}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_access(PLCSTPARSERParser.Class_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#class_name}.
	 * @param ctx the parse tree
	 */
	void enterClass_name(PLCSTPARSERParser.Class_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#class_name}.
	 * @param ctx the parse tree
	 */
	void exitClass_name(PLCSTPARSERParser.Class_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#instance_name}.
	 * @param ctx the parse tree
	 */
	void enterInstance_name(PLCSTPARSERParser.Instance_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#instance_name}.
	 * @param ctx the parse tree
	 */
	void exitInstance_name(PLCSTPARSERParser.Instance_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_decl}.
	 * @param ctx the parse tree
	 */
	void enterInterface_decl(PLCSTPARSERParser.Interface_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_decl}.
	 * @param ctx the parse tree
	 */
	void exitInterface_decl(PLCSTPARSERParser.Interface_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#method_prototype}.
	 * @param ctx the parse tree
	 */
	void enterMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#method_prototype}.
	 * @param ctx the parse tree
	 */
	void exitMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_spec_init}.
	 * @param ctx the parse tree
	 */
	void enterInterface_spec_init(PLCSTPARSERParser.Interface_spec_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_spec_init}.
	 * @param ctx the parse tree
	 */
	void exitInterface_spec_init(PLCSTPARSERParser.Interface_spec_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_value}.
	 * @param ctx the parse tree
	 */
	void enterInterface_value(PLCSTPARSERParser.Interface_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_value}.
	 * @param ctx the parse tree
	 */
	void exitInterface_value(PLCSTPARSERParser.Interface_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_name_list}.
	 * @param ctx the parse tree
	 */
	void enterInterface_name_list(PLCSTPARSERParser.Interface_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_name_list}.
	 * @param ctx the parse tree
	 */
	void exitInterface_name_list(PLCSTPARSERParser.Interface_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_type_name}.
	 * @param ctx the parse tree
	 */
	void enterInterface_type_name(PLCSTPARSERParser.Interface_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_type_name}.
	 * @param ctx the parse tree
	 */
	void exitInterface_type_name(PLCSTPARSERParser.Interface_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_type_access}.
	 * @param ctx the parse tree
	 */
	void enterInterface_type_access(PLCSTPARSERParser.Interface_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_type_access}.
	 * @param ctx the parse tree
	 */
	void exitInterface_type_access(PLCSTPARSERParser.Interface_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#interface_name}.
	 * @param ctx the parse tree
	 */
	void enterInterface_name(PLCSTPARSERParser.Interface_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#interface_name}.
	 * @param ctx the parse tree
	 */
	void exitInterface_name(PLCSTPARSERParser.Interface_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_decl}.
	 * @param ctx the parse tree
	 */
	void enterProg_decl(PLCSTPARSERParser.Prog_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_decl}.
	 * @param ctx the parse tree
	 */
	void exitProg_decl(PLCSTPARSERParser.Prog_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_type_name}.
	 * @param ctx the parse tree
	 */
	void enterProg_type_name(PLCSTPARSERParser.Prog_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_type_name}.
	 * @param ctx the parse tree
	 */
	void exitProg_type_name(PLCSTPARSERParser.Prog_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_type_access}.
	 * @param ctx the parse tree
	 */
	void enterProg_type_access(PLCSTPARSERParser.Prog_type_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_type_access}.
	 * @param ctx the parse tree
	 */
	void exitProg_type_access(PLCSTPARSERParser.Prog_type_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_access_decls}.
	 * @param ctx the parse tree
	 */
	void enterProg_access_decls(PLCSTPARSERParser.Prog_access_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_access_decls}.
	 * @param ctx the parse tree
	 */
	void exitProg_access_decls(PLCSTPARSERParser.Prog_access_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_access_decl}.
	 * @param ctx the parse tree
	 */
	void enterProg_access_decl(PLCSTPARSERParser.Prog_access_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_access_decl}.
	 * @param ctx the parse tree
	 */
	void exitProg_access_decl(PLCSTPARSERParser.Prog_access_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#sfc}.
	 * @param ctx the parse tree
	 */
	void enterSfc(PLCSTPARSERParser.SfcContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#sfc}.
	 * @param ctx the parse tree
	 */
	void exitSfc(PLCSTPARSERParser.SfcContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#sfc_network}.
	 * @param ctx the parse tree
	 */
	void enterSfc_network(PLCSTPARSERParser.Sfc_networkContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#sfc_network}.
	 * @param ctx the parse tree
	 */
	void exitSfc_network(PLCSTPARSERParser.Sfc_networkContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#initial_step}.
	 * @param ctx the parse tree
	 */
	void enterInitial_step(PLCSTPARSERParser.Initial_stepContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#initial_step}.
	 * @param ctx the parse tree
	 */
	void exitInitial_step(PLCSTPARSERParser.Initial_stepContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#step}.
	 * @param ctx the parse tree
	 */
	void enterStep(PLCSTPARSERParser.StepContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#step}.
	 * @param ctx the parse tree
	 */
	void exitStep(PLCSTPARSERParser.StepContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#step_name}.
	 * @param ctx the parse tree
	 */
	void enterStep_name(PLCSTPARSERParser.Step_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#step_name}.
	 * @param ctx the parse tree
	 */
	void exitStep_name(PLCSTPARSERParser.Step_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#action_association}.
	 * @param ctx the parse tree
	 */
	void enterAction_association(PLCSTPARSERParser.Action_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#action_association}.
	 * @param ctx the parse tree
	 */
	void exitAction_association(PLCSTPARSERParser.Action_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#action_name}.
	 * @param ctx the parse tree
	 */
	void enterAction_name(PLCSTPARSERParser.Action_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#action_name}.
	 * @param ctx the parse tree
	 */
	void exitAction_name(PLCSTPARSERParser.Action_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#action_qualifier}.
	 * @param ctx the parse tree
	 */
	void enterAction_qualifier(PLCSTPARSERParser.Action_qualifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#action_qualifier}.
	 * @param ctx the parse tree
	 */
	void exitAction_qualifier(PLCSTPARSERParser.Action_qualifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#action_time}.
	 * @param ctx the parse tree
	 */
	void enterAction_time(PLCSTPARSERParser.Action_timeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#action_time}.
	 * @param ctx the parse tree
	 */
	void exitAction_time(PLCSTPARSERParser.Action_timeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#indicator_name}.
	 * @param ctx the parse tree
	 */
	void enterIndicator_name(PLCSTPARSERParser.Indicator_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#indicator_name}.
	 * @param ctx the parse tree
	 */
	void exitIndicator_name(PLCSTPARSERParser.Indicator_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#transition}.
	 * @param ctx the parse tree
	 */
	void enterTransition(PLCSTPARSERParser.TransitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#transition}.
	 * @param ctx the parse tree
	 */
	void exitTransition(PLCSTPARSERParser.TransitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#transition_name}.
	 * @param ctx the parse tree
	 */
	void enterTransition_name(PLCSTPARSERParser.Transition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#transition_name}.
	 * @param ctx the parse tree
	 */
	void exitTransition_name(PLCSTPARSERParser.Transition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#steps}.
	 * @param ctx the parse tree
	 */
	void enterSteps(PLCSTPARSERParser.StepsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#steps}.
	 * @param ctx the parse tree
	 */
	void exitSteps(PLCSTPARSERParser.StepsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#transition_cond}.
	 * @param ctx the parse tree
	 */
	void enterTransition_cond(PLCSTPARSERParser.Transition_condContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#transition_cond}.
	 * @param ctx the parse tree
	 */
	void exitTransition_cond(PLCSTPARSERParser.Transition_condContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#action}.
	 * @param ctx the parse tree
	 */
	void enterAction(PLCSTPARSERParser.ActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#action}.
	 * @param ctx the parse tree
	 */
	void exitAction(PLCSTPARSERParser.ActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#config_name}.
	 * @param ctx the parse tree
	 */
	void enterConfig_name(PLCSTPARSERParser.Config_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#config_name}.
	 * @param ctx the parse tree
	 */
	void exitConfig_name(PLCSTPARSERParser.Config_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#resource_type_name}.
	 * @param ctx the parse tree
	 */
	void enterResource_type_name(PLCSTPARSERParser.Resource_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#resource_type_name}.
	 * @param ctx the parse tree
	 */
	void exitResource_type_name(PLCSTPARSERParser.Resource_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#config_decl}.
	 * @param ctx the parse tree
	 */
	void enterConfig_decl(PLCSTPARSERParser.Config_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#config_decl}.
	 * @param ctx the parse tree
	 */
	void exitConfig_decl(PLCSTPARSERParser.Config_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#resource_decl}.
	 * @param ctx the parse tree
	 */
	void enterResource_decl(PLCSTPARSERParser.Resource_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#resource_decl}.
	 * @param ctx the parse tree
	 */
	void exitResource_decl(PLCSTPARSERParser.Resource_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#single_resource_decl}.
	 * @param ctx the parse tree
	 */
	void enterSingle_resource_decl(PLCSTPARSERParser.Single_resource_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#single_resource_decl}.
	 * @param ctx the parse tree
	 */
	void exitSingle_resource_decl(PLCSTPARSERParser.Single_resource_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#resource_name}.
	 * @param ctx the parse tree
	 */
	void enterResource_name(PLCSTPARSERParser.Resource_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#resource_name}.
	 * @param ctx the parse tree
	 */
	void exitResource_name(PLCSTPARSERParser.Resource_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#access_decls}.
	 * @param ctx the parse tree
	 */
	void enterAccess_decls(PLCSTPARSERParser.Access_declsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#access_decls}.
	 * @param ctx the parse tree
	 */
	void exitAccess_decls(PLCSTPARSERParser.Access_declsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#access_decl}.
	 * @param ctx the parse tree
	 */
	void enterAccess_decl(PLCSTPARSERParser.Access_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#access_decl}.
	 * @param ctx the parse tree
	 */
	void exitAccess_decl(PLCSTPARSERParser.Access_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#access_path}.
	 * @param ctx the parse tree
	 */
	void enterAccess_path(PLCSTPARSERParser.Access_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#access_path}.
	 * @param ctx the parse tree
	 */
	void exitAccess_path(PLCSTPARSERParser.Access_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#global_var_access}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_var_access(PLCSTPARSERParser.Global_var_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#global_var_access}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_var_access(PLCSTPARSERParser.Global_var_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#access_name}.
	 * @param ctx the parse tree
	 */
	void enterAccess_name(PLCSTPARSERParser.Access_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#access_name}.
	 * @param ctx the parse tree
	 */
	void exitAccess_name(PLCSTPARSERParser.Access_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_output_access}.
	 * @param ctx the parse tree
	 */
	void enterProg_output_access(PLCSTPARSERParser.Prog_output_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_output_access}.
	 * @param ctx the parse tree
	 */
	void exitProg_output_access(PLCSTPARSERParser.Prog_output_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_name}.
	 * @param ctx the parse tree
	 */
	void enterProg_name(PLCSTPARSERParser.Prog_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_name}.
	 * @param ctx the parse tree
	 */
	void exitProg_name(PLCSTPARSERParser.Prog_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#task_config}.
	 * @param ctx the parse tree
	 */
	void enterTask_config(PLCSTPARSERParser.Task_configContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#task_config}.
	 * @param ctx the parse tree
	 */
	void exitTask_config(PLCSTPARSERParser.Task_configContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#task_name}.
	 * @param ctx the parse tree
	 */
	void enterTask_name(PLCSTPARSERParser.Task_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#task_name}.
	 * @param ctx the parse tree
	 */
	void exitTask_name(PLCSTPARSERParser.Task_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#task_init}.
	 * @param ctx the parse tree
	 */
	void enterTask_init(PLCSTPARSERParser.Task_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#task_init}.
	 * @param ctx the parse tree
	 */
	void exitTask_init(PLCSTPARSERParser.Task_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#data_source}.
	 * @param ctx the parse tree
	 */
	void enterData_source(PLCSTPARSERParser.Data_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#data_source}.
	 * @param ctx the parse tree
	 */
	void exitData_source(PLCSTPARSERParser.Data_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_config}.
	 * @param ctx the parse tree
	 */
	void enterProg_config(PLCSTPARSERParser.Prog_configContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_config}.
	 * @param ctx the parse tree
	 */
	void exitProg_config(PLCSTPARSERParser.Prog_configContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elems}.
	 * @param ctx the parse tree
	 */
	void enterProg_conf_elems(PLCSTPARSERParser.Prog_conf_elemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elems}.
	 * @param ctx the parse tree
	 */
	void exitProg_conf_elems(PLCSTPARSERParser.Prog_conf_elemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elem}.
	 * @param ctx the parse tree
	 */
	void enterProg_conf_elem(PLCSTPARSERParser.Prog_conf_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elem}.
	 * @param ctx the parse tree
	 */
	void exitProg_conf_elem(PLCSTPARSERParser.Prog_conf_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_task}.
	 * @param ctx the parse tree
	 */
	void enterFb_task(PLCSTPARSERParser.Fb_taskContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_task}.
	 * @param ctx the parse tree
	 */
	void exitFb_task(PLCSTPARSERParser.Fb_taskContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_cnxn}.
	 * @param ctx the parse tree
	 */
	void enterProg_cnxn(PLCSTPARSERParser.Prog_cnxnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_cnxn}.
	 * @param ctx the parse tree
	 */
	void exitProg_cnxn(PLCSTPARSERParser.Prog_cnxnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#prog_data_source}.
	 * @param ctx the parse tree
	 */
	void enterProg_data_source(PLCSTPARSERParser.Prog_data_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#prog_data_source}.
	 * @param ctx the parse tree
	 */
	void exitProg_data_source(PLCSTPARSERParser.Prog_data_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#data_sink}.
	 * @param ctx the parse tree
	 */
	void enterData_sink(PLCSTPARSERParser.Data_sinkContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#data_sink}.
	 * @param ctx the parse tree
	 */
	void exitData_sink(PLCSTPARSERParser.Data_sinkContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#config_init}.
	 * @param ctx the parse tree
	 */
	void enterConfig_init(PLCSTPARSERParser.Config_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#config_init}.
	 * @param ctx the parse tree
	 */
	void exitConfig_init(PLCSTPARSERParser.Config_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#config_inst_init}.
	 * @param ctx the parse tree
	 */
	void enterConfig_inst_init(PLCSTPARSERParser.Config_inst_initContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#config_inst_init}.
	 * @param ctx the parse tree
	 */
	void exitConfig_inst_init(PLCSTPARSERParser.Config_inst_initContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#namespace_decl}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#namespace_decl}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#namespace_elements}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#namespace_elements}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#namespace_h_name}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_h_name(PLCSTPARSERParser.Namespace_h_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#namespace_h_name}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_h_name(PLCSTPARSERParser.Namespace_h_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#namespace_name}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_name(PLCSTPARSERParser.Namespace_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#namespace_name}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_name(PLCSTPARSERParser.Namespace_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#using_directive}.
	 * @param ctx the parse tree
	 */
	void enterUsing_directive(PLCSTPARSERParser.Using_directiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#using_directive}.
	 * @param ctx the parse tree
	 */
	void exitUsing_directive(PLCSTPARSERParser.Using_directiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pou_decl}.
	 * @param ctx the parse tree
	 */
	void enterPou_decl(PLCSTPARSERParser.Pou_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pou_decl}.
	 * @param ctx the parse tree
	 */
	void exitPou_decl(PLCSTPARSERParser.Pou_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#instruction_list}.
	 * @param ctx the parse tree
	 */
	void enterInstruction_list(PLCSTPARSERParser.Instruction_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#instruction_list}.
	 * @param ctx the parse tree
	 */
	void exitInstruction_list(PLCSTPARSERParser.Instruction_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_instruction}.
	 * @param ctx the parse tree
	 */
	void enterIl_instruction(PLCSTPARSERParser.Il_instructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_instruction}.
	 * @param ctx the parse tree
	 */
	void exitIl_instruction(PLCSTPARSERParser.Il_instructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst}.
	 * @param ctx the parse tree
	 */
	void enterIl_simple_inst(PLCSTPARSERParser.Il_simple_instContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst}.
	 * @param ctx the parse tree
	 */
	void exitIl_simple_inst(PLCSTPARSERParser.Il_simple_instContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_label}.
	 * @param ctx the parse tree
	 */
	void enterIl_label(PLCSTPARSERParser.Il_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_label}.
	 * @param ctx the parse tree
	 */
	void exitIl_label(PLCSTPARSERParser.Il_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_simple_operation}.
	 * @param ctx the parse tree
	 */
	void enterIl_simple_operation(PLCSTPARSERParser.Il_simple_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_simple_operation}.
	 * @param ctx the parse tree
	 */
	void exitIl_simple_operation(PLCSTPARSERParser.Il_simple_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_expr}.
	 * @param ctx the parse tree
	 */
	void enterIl_expr(PLCSTPARSERParser.Il_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_expr}.
	 * @param ctx the parse tree
	 */
	void exitIl_expr(PLCSTPARSERParser.Il_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_jump_operation}.
	 * @param ctx the parse tree
	 */
	void enterIl_jump_operation(PLCSTPARSERParser.Il_jump_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_jump_operation}.
	 * @param ctx the parse tree
	 */
	void exitIl_jump_operation(PLCSTPARSERParser.Il_jump_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_invocation}.
	 * @param ctx the parse tree
	 */
	void enterIl_invocation(PLCSTPARSERParser.Il_invocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_invocation}.
	 * @param ctx the parse tree
	 */
	void exitIl_invocation(PLCSTPARSERParser.Il_invocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_formal_func_call}.
	 * @param ctx the parse tree
	 */
	void enterIl_formal_func_call(PLCSTPARSERParser.Il_formal_func_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_formal_func_call}.
	 * @param ctx the parse tree
	 */
	void exitIl_formal_func_call(PLCSTPARSERParser.Il_formal_func_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_operand}.
	 * @param ctx the parse tree
	 */
	void enterIl_operand(PLCSTPARSERParser.Il_operandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_operand}.
	 * @param ctx the parse tree
	 */
	void exitIl_operand(PLCSTPARSERParser.Il_operandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_operand_list}.
	 * @param ctx the parse tree
	 */
	void enterIl_operand_list(PLCSTPARSERParser.Il_operand_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_operand_list}.
	 * @param ctx the parse tree
	 */
	void exitIl_operand_list(PLCSTPARSERParser.Il_operand_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst_list}.
	 * @param ctx the parse tree
	 */
	void enterIl_simple_inst_list(PLCSTPARSERParser.Il_simple_inst_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst_list}.
	 * @param ctx the parse tree
	 */
	void exitIl_simple_inst_list(PLCSTPARSERParser.Il_simple_inst_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_simple_instruction}.
	 * @param ctx the parse tree
	 */
	void enterIl_simple_instruction(PLCSTPARSERParser.Il_simple_instructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_simple_instruction}.
	 * @param ctx the parse tree
	 */
	void exitIl_simple_instruction(PLCSTPARSERParser.Il_simple_instructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_param_list}.
	 * @param ctx the parse tree
	 */
	void enterIl_param_list(PLCSTPARSERParser.Il_param_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_param_list}.
	 * @param ctx the parse tree
	 */
	void exitIl_param_list(PLCSTPARSERParser.Il_param_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_param_inst}.
	 * @param ctx the parse tree
	 */
	void enterIl_param_inst(PLCSTPARSERParser.Il_param_instContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_param_inst}.
	 * @param ctx the parse tree
	 */
	void exitIl_param_inst(PLCSTPARSERParser.Il_param_instContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_param_last_inst}.
	 * @param ctx the parse tree
	 */
	void enterIl_param_last_inst(PLCSTPARSERParser.Il_param_last_instContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_param_last_inst}.
	 * @param ctx the parse tree
	 */
	void exitIl_param_last_inst(PLCSTPARSERParser.Il_param_last_instContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_param_assign}.
	 * @param ctx the parse tree
	 */
	void enterIl_param_assign(PLCSTPARSERParser.Il_param_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_param_assign}.
	 * @param ctx the parse tree
	 */
	void exitIl_param_assign(PLCSTPARSERParser.Il_param_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_param_out_assign}.
	 * @param ctx the parse tree
	 */
	void enterIl_param_out_assign(PLCSTPARSERParser.Il_param_out_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_param_out_assign}.
	 * @param ctx the parse tree
	 */
	void exitIl_param_out_assign(PLCSTPARSERParser.Il_param_out_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_simple_operator}.
	 * @param ctx the parse tree
	 */
	void enterIl_simple_operator(PLCSTPARSERParser.Il_simple_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_simple_operator}.
	 * @param ctx the parse tree
	 */
	void exitIl_simple_operator(PLCSTPARSERParser.Il_simple_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_assignment}.
	 * @param ctx the parse tree
	 */
	void enterIl_assignment(PLCSTPARSERParser.Il_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_assignment}.
	 * @param ctx the parse tree
	 */
	void exitIl_assignment(PLCSTPARSERParser.Il_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#il_assign_out_operator}.
	 * @param ctx the parse tree
	 */
	void enterIl_assign_out_operator(PLCSTPARSERParser.Il_assign_out_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#il_assign_out_operator}.
	 * @param ctx the parse tree
	 */
	void exitIl_assign_out_operator(PLCSTPARSERParser.Il_assign_out_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PLCSTPARSERParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PLCSTPARSERParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#constant_expr}.
	 * @param ctx the parse tree
	 */
	void enterConstant_expr(PLCSTPARSERParser.Constant_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#constant_expr}.
	 * @param ctx the parse tree
	 */
	void exitConstant_expr(PLCSTPARSERParser.Constant_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#xor_expr}.
	 * @param ctx the parse tree
	 */
	void enterXor_expr(PLCSTPARSERParser.Xor_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#xor_expr}.
	 * @param ctx the parse tree
	 */
	void exitXor_expr(PLCSTPARSERParser.Xor_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#and_expr}.
	 * @param ctx the parse tree
	 */
	void enterAnd_expr(PLCSTPARSERParser.And_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#and_expr}.
	 * @param ctx the parse tree
	 */
	void exitAnd_expr(PLCSTPARSERParser.And_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#compare_expr}.
	 * @param ctx the parse tree
	 */
	void enterCompare_expr(PLCSTPARSERParser.Compare_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#compare_expr}.
	 * @param ctx the parse tree
	 */
	void exitCompare_expr(PLCSTPARSERParser.Compare_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#equ_expr}.
	 * @param ctx the parse tree
	 */
	void enterEqu_expr(PLCSTPARSERParser.Equ_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#equ_expr}.
	 * @param ctx the parse tree
	 */
	void exitEqu_expr(PLCSTPARSERParser.Equ_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#add_expr}.
	 * @param ctx the parse tree
	 */
	void enterAdd_expr(PLCSTPARSERParser.Add_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#add_expr}.
	 * @param ctx the parse tree
	 */
	void exitAdd_expr(PLCSTPARSERParser.Add_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#term}.
	 * @param ctx the parse tree
	 */
	void enterTerm(PLCSTPARSERParser.TermContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#term}.
	 * @param ctx the parse tree
	 */
	void exitTerm(PLCSTPARSERParser.TermContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#power_expr}.
	 * @param ctx the parse tree
	 */
	void enterPower_expr(PLCSTPARSERParser.Power_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#power_expr}.
	 * @param ctx the parse tree
	 */
	void exitPower_expr(PLCSTPARSERParser.Power_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#unary_expr}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expr(PLCSTPARSERParser.Unary_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#unary_expr}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expr(PLCSTPARSERParser.Unary_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#primary_expr}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_expr(PLCSTPARSERParser.Primary_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#primary_expr}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_expr(PLCSTPARSERParser.Primary_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#variable_access}.
	 * @param ctx the parse tree
	 */
	void enterVariable_access(PLCSTPARSERParser.Variable_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#variable_access}.
	 * @param ctx the parse tree
	 */
	void exitVariable_access(PLCSTPARSERParser.Variable_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#multibit_part_access}.
	 * @param ctx the parse tree
	 */
	void enterMultibit_part_access(PLCSTPARSERParser.Multibit_part_accessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#multibit_part_access}.
	 * @param ctx the parse tree
	 */
	void exitMultibit_part_access(PLCSTPARSERParser.Multibit_part_accessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#func_call}.
	 * @param ctx the parse tree
	 */
	void enterFunc_call(PLCSTPARSERParser.Func_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#func_call}.
	 * @param ctx the parse tree
	 */
	void exitFunc_call(PLCSTPARSERParser.Func_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void enterStmt_list(PLCSTPARSERParser.Stmt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void exitStmt_list(PLCSTPARSERParser.Stmt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(PLCSTPARSERParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(PLCSTPARSERParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#print_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPrint_stmt(PLCSTPARSERParser.Print_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#print_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPrint_stmt(PLCSTPARSERParser.Print_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#print_stmt_element}.
	 * @param ctx the parse tree
	 */
	void enterPrint_stmt_element(PLCSTPARSERParser.Print_stmt_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#print_stmt_element}.
	 * @param ctx the parse tree
	 */
	void exitPrint_stmt_element(PLCSTPARSERParser.Print_stmt_elementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code variableAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void enterVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code variableAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void exitVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRefAssignExpression(PLCSTPARSERParser.RefAssignExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRefAssignExpression(PLCSTPARSERParser.RefAssignExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pointerAssign}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPointerAssign(PLCSTPARSERParser.PointerAssignContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pointerAssign}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPointerAssign(PLCSTPARSERParser.PointerAssignContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assignmentAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentAttempt(PLCSTPARSERParser.AssignmentAttemptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assignmentAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentAttempt(PLCSTPARSERParser.AssignmentAttemptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pointerAssignAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPointerAssignAttempt(PLCSTPARSERParser.PointerAssignAttemptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pointerAssignAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPointerAssignAttempt(PLCSTPARSERParser.PointerAssignAttemptContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#assignment_attempt}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_attempt(PLCSTPARSERParser.Assignment_attemptContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#assignment_attempt}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_attempt(PLCSTPARSERParser.Assignment_attemptContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#pointer_assigment_attempt}.
	 * @param ctx the parse tree
	 */
	void enterPointer_assigment_attempt(PLCSTPARSERParser.Pointer_assigment_attemptContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#pointer_assigment_attempt}.
	 * @param ctx the parse tree
	 */
	void exitPointer_assigment_attempt(PLCSTPARSERParser.Pointer_assigment_attemptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code invocation1}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void enterInvocation1(PLCSTPARSERParser.Invocation1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code invocation1}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void exitInvocation1(PLCSTPARSERParser.Invocation1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code invocation2}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void enterInvocation2(PLCSTPARSERParser.Invocation2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code invocation2}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void exitInvocation2(PLCSTPARSERParser.Invocation2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code invocation3}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void enterInvocation3(PLCSTPARSERParser.Invocation3Context ctx);
	/**
	 * Exit a parse tree produced by the {@code invocation3}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 */
	void exitInvocation3(PLCSTPARSERParser.Invocation3Context ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#invocation1branch}.
	 * @param ctx the parse tree
	 */
	void enterInvocation1branch(PLCSTPARSERParser.Invocation1branchContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#invocation1branch}.
	 * @param ctx the parse tree
	 */
	void exitInvocation1branch(PLCSTPARSERParser.Invocation1branchContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#invocation2branch}.
	 * @param ctx the parse tree
	 */
	void enterInvocation2branch(PLCSTPARSERParser.Invocation2branchContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#invocation2branch}.
	 * @param ctx the parse tree
	 */
	void exitInvocation2branch(PLCSTPARSERParser.Invocation2branchContext ctx);
	/**
	 * Enter a parse tree produced by the {@code callFunc}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCallFunc(PLCSTPARSERParser.CallFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code callFunc}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCallFunc(PLCSTPARSERParser.CallFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code superCall}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSuperCall(PLCSTPARSERParser.SuperCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code superCall}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSuperCall(PLCSTPARSERParser.SuperCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code return}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void enterReturn(PLCSTPARSERParser.ReturnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code return}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 */
	void exitReturn(PLCSTPARSERParser.ReturnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inputParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void enterInputParam(PLCSTPARSERParser.InputParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inputParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void exitInputParam(PLCSTPARSERParser.InputParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void enterRefParam(PLCSTPARSERParser.RefParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void exitRefParam(PLCSTPARSERParser.RefParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void enterOutParam(PLCSTPARSERParser.OutParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 */
	void exitOutParam(PLCSTPARSERParser.OutParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#selection_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#selection_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_stmt(PLCSTPARSERParser.If_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_stmt(PLCSTPARSERParser.If_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#elsif_stmt}.
	 * @param ctx the parse tree
	 */
	void enterElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#elsif_stmt}.
	 * @param ctx the parse tree
	 */
	void exitElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#else_stmt}.
	 * @param ctx the parse tree
	 */
	void enterElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#else_stmt}.
	 * @param ctx the parse tree
	 */
	void exitElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#case_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#case_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#case_selection}.
	 * @param ctx the parse tree
	 */
	void enterCase_selection(PLCSTPARSERParser.Case_selectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#case_selection}.
	 * @param ctx the parse tree
	 */
	void exitCase_selection(PLCSTPARSERParser.Case_selectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#case_list}.
	 * @param ctx the parse tree
	 */
	void enterCase_list(PLCSTPARSERParser.Case_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#case_list}.
	 * @param ctx the parse tree
	 */
	void exitCase_list(PLCSTPARSERParser.Case_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#case_list_elem}.
	 * @param ctx the parse tree
	 */
	void enterCase_list_elem(PLCSTPARSERParser.Case_list_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#case_list_elem}.
	 * @param ctx the parse tree
	 */
	void exitCase_list_elem(PLCSTPARSERParser.Case_list_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#iteration_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#iteration_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#for_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFor_stmt(PLCSTPARSERParser.For_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#for_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFor_stmt(PLCSTPARSERParser.For_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#control_variable}.
	 * @param ctx the parse tree
	 */
	void enterControl_variable(PLCSTPARSERParser.Control_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#control_variable}.
	 * @param ctx the parse tree
	 */
	void exitControl_variable(PLCSTPARSERParser.Control_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#by_list}.
	 * @param ctx the parse tree
	 */
	void enterBy_list(PLCSTPARSERParser.By_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#by_list}.
	 * @param ctx the parse tree
	 */
	void exitBy_list(PLCSTPARSERParser.By_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void enterWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void exitWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#repeat_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#repeat_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#ladder_diagram}.
	 * @param ctx the parse tree
	 */
	void enterLadder_diagram(PLCSTPARSERParser.Ladder_diagramContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#ladder_diagram}.
	 * @param ctx the parse tree
	 */
	void exitLadder_diagram(PLCSTPARSERParser.Ladder_diagramContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#fb_diagram}.
	 * @param ctx the parse tree
	 */
	void enterFb_diagram(PLCSTPARSERParser.Fb_diagramContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#fb_diagram}.
	 * @param ctx the parse tree
	 */
	void exitFb_diagram(PLCSTPARSERParser.Fb_diagramContext ctx);
	/**
	 * Enter a parse tree produced by {@link PLCSTPARSERParser#reservedKeyword}.
	 * @param ctx the parse tree
	 */
	void enterReservedKeyword(PLCSTPARSERParser.ReservedKeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PLCSTPARSERParser#reservedKeyword}.
	 * @param ctx the parse tree
	 */
	void exitReservedKeyword(PLCSTPARSERParser.ReservedKeywordContext ctx);
}