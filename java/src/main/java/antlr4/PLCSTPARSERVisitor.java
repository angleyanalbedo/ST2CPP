// Generated from D:/source/Project/ST2C/java/src/main/resources/antlr4/PLCSTPARSER.g4 by ANTLR 4.13.2
package antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PLCSTPARSERParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PLCSTPARSERVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#startpoint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartpoint(PLCSTPARSERParser.StartpointContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(PLCSTPARSERParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(PLCSTPARSERParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#numeric_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_literal(PLCSTPARSERParser.Numeric_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#int_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt_literal(PLCSTPARSERParser.Int_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#signed_int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_int(PLCSTPARSERParser.Signed_intContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#binary_int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_int(PLCSTPARSERParser.Binary_intContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#octal_int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOctal_int(PLCSTPARSERParser.Octal_intContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#hex_int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHex_int(PLCSTPARSERParser.Hex_intContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#real_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReal_literal(PLCSTPARSERParser.Real_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#bit_str_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBit_str_literal(PLCSTPARSERParser.Bit_str_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#bool_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_literal(PLCSTPARSERParser.Bool_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#char_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChar_literal(PLCSTPARSERParser.Char_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#char_str}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChar_str(PLCSTPARSERParser.Char_strContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#s_byte_char}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitS_byte_char(PLCSTPARSERParser.S_byte_charContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#d_byte_char_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitD_byte_char_value(PLCSTPARSERParser.D_byte_char_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#time_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime_literal(PLCSTPARSERParser.Time_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#duration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDuration(PLCSTPARSERParser.DurationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fix_point}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFix_point(PLCSTPARSERParser.Fix_pointContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(PLCSTPARSERParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#days}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDays(PLCSTPARSERParser.DaysContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#hours}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHours(PLCSTPARSERParser.HoursContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#minutes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinutes(PLCSTPARSERParser.MinutesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#seconds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeconds(PLCSTPARSERParser.SecondsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#milliseconds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMilliseconds(PLCSTPARSERParser.MillisecondsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#microseconds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMicroseconds(PLCSTPARSERParser.MicrosecondsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#nanoseconds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNanoseconds(PLCSTPARSERParser.NanosecondsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#time_of_day}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime_of_day(PLCSTPARSERParser.Time_of_dayContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#daytime}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDaytime(PLCSTPARSERParser.DaytimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#day_hour}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDay_hour(PLCSTPARSERParser.Day_hourContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#day_minute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDay_minute(PLCSTPARSERParser.Day_minuteContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#day_second}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDay_second(PLCSTPARSERParser.Day_secondContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#date}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate(PLCSTPARSERParser.DateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#date_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_literal(PLCSTPARSERParser.Date_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#year}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYear(PLCSTPARSERParser.YearContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#month}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMonth(PLCSTPARSERParser.MonthContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#day}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDay(PLCSTPARSERParser.DayContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#date_and_time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_and_time(PLCSTPARSERParser.Date_and_timeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#data_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_type_access(PLCSTPARSERParser.Data_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#elem_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElem_type_name(PLCSTPARSERParser.Elem_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#numeric_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_type_name(PLCSTPARSERParser.Numeric_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#int_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt_type_name(PLCSTPARSERParser.Int_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#bit_str_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBit_str_type_name(PLCSTPARSERParser.Bit_str_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_type_access(PLCSTPARSERParser.Derived_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#string_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_type_access(PLCSTPARSERParser.String_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#single_elem_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_elem_type_access(PLCSTPARSERParser.Single_elem_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#simple_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_type_access(PLCSTPARSERParser.Simple_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange_type_access(PLCSTPARSERParser.Subrange_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_type_access(PLCSTPARSERParser.Enum_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_type_access(PLCSTPARSERParser.Array_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_type_access(PLCSTPARSERParser.Struct_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#simple_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_type_name(PLCSTPARSERParser.Simple_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange_type_name(PLCSTPARSERParser.Subrange_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_type_name(PLCSTPARSERParser.Enum_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_type_name(PLCSTPARSERParser.Array_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_type_name(PLCSTPARSERParser.Struct_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#data_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_type_decl(PLCSTPARSERParser.Data_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_decl(PLCSTPARSERParser.Type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_type_decl(PLCSTPARSERParser.Derived_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_type_name(PLCSTPARSERParser.Derived_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_spec_init(PLCSTPARSERParser.Derived_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#simple_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_type_decl(PLCSTPARSERParser.Simple_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#simple_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_spec_init(PLCSTPARSERParser.Simple_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#simple_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_spec(PLCSTPARSERParser.Simple_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange_type_decl(PLCSTPARSERParser.Subrange_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange_spec_init(PLCSTPARSERParser.Subrange_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange_spec(PLCSTPARSERParser.Subrange_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subrange}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubrange(PLCSTPARSERParser.SubrangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_type_decl(PLCSTPARSERParser.Enum_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#named_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamed_spec_init(PLCSTPARSERParser.Named_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_spec_init(PLCSTPARSERParser.Enum_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_value_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_value_spec(PLCSTPARSERParser.Enum_value_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#enum_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_value(PLCSTPARSERParser.Enum_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_type_decl(PLCSTPARSERParser.Array_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_spec_init(PLCSTPARSERParser.Array_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_spec(PLCSTPARSERParser.Array_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_init(PLCSTPARSERParser.Array_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_elem_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_elem_init(PLCSTPARSERParser.Array_elem_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_elem_item_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_elem_item_init(PLCSTPARSERParser.Array_elem_item_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_elem_init_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_elem_init_value(PLCSTPARSERParser.Array_elem_init_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_type_decl(PLCSTPARSERParser.Struct_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_spec(PLCSTPARSERParser.Struct_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_spec_init(PLCSTPARSERParser.Struct_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_decl(PLCSTPARSERParser.Struct_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_elem_decl(PLCSTPARSERParser.Struct_elem_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_elem_name(PLCSTPARSERParser.Struct_elem_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_init(PLCSTPARSERParser.Struct_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_elem_init(PLCSTPARSERParser.Struct_elem_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#str_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStr_type_decl(PLCSTPARSERParser.Str_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#string_type_name_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_type_name_identifier(PLCSTPARSERParser.String_type_name_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_type_decl(PLCSTPARSERParser.Ref_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_spec_init(PLCSTPARSERParser.Ref_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_spec(PLCSTPARSERParser.Ref_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_type_name(PLCSTPARSERParser.Ref_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_type_access(PLCSTPARSERParser.Ref_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_name(PLCSTPARSERParser.Ref_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_value(PLCSTPARSERParser.Ref_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_addr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_addr(PLCSTPARSERParser.Ref_addrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_assign(PLCSTPARSERParser.Ref_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_deref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_deref(PLCSTPARSERParser.Ref_derefContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_type_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_type_decl(PLCSTPARSERParser.Pointer_type_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_type_name(PLCSTPARSERParser.Pointer_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_spec_init(PLCSTPARSERParser.Pointer_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_name(PLCSTPARSERParser.Pointer_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_spec(PLCSTPARSERParser.Pointer_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_value(PLCSTPARSERParser.Pointer_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_adr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_adr(PLCSTPARSERParser.Pointer_adrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_dref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_dref(PLCSTPARSERParser.Pointer_drefContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_assign(PLCSTPARSERParser.Pointer_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(PLCSTPARSERParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_index(PLCSTPARSERParser.Array_indexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code thisSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisSymbolic(PLCSTPARSERParser.ThisSymbolicContext ctx);
	/**
	 * Visit a parse tree produced by the {@code namespaceSymbolic}
	 * labeled alternative in {@link PLCSTPARSERParser#symbolic_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespaceSymbolic(PLCSTPARSERParser.NamespaceSymbolicContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#var_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_access(PLCSTPARSERParser.Var_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#variable_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_name(PLCSTPARSERParser.Variable_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#multi_elem_var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_elem_var(PLCSTPARSERParser.Multi_elem_varContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subscript_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript_list(PLCSTPARSERParser.Subscript_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#subscript}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(PLCSTPARSERParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_variable(PLCSTPARSERParser.Struct_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_elem_select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_elem_select(PLCSTPARSERParser.Struct_elem_selectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#input_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInput_decls(PLCSTPARSERParser.Input_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#input_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInput_decl(PLCSTPARSERParser.Input_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#edge_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdge_decl(PLCSTPARSERParser.Edge_declContext ctx);
	/**
	 * Visit a parse tree produced by the {@code vardeclinit}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVardeclinit(PLCSTPARSERParser.VardeclinitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code directNum}
	 * labeled alternative in {@link PLCSTPARSERParser#var_decl_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectNum(PLCSTPARSERParser.DirectNumContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#str_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStr_spec_init(PLCSTPARSERParser.Str_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#str_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStr_spec(PLCSTPARSERParser.Str_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#user_defination_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_defination_spec_init(PLCSTPARSERParser.User_defination_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_defination_type_access(PLCSTPARSERParser.User_defination_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#user_defination_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_defination_type_name(PLCSTPARSERParser.User_defination_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ref_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_var_decl(PLCSTPARSERParser.Ref_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_var_decl(PLCSTPARSERParser.Interface_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#variable_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_list(PLCSTPARSERParser.Variable_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_var_decl_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_var_decl_init(PLCSTPARSERParser.Array_var_decl_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_conformand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_conformand(PLCSTPARSERParser.Array_conformandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_conform_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_conform_decl(PLCSTPARSERParser.Array_conform_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_var_decl_init(PLCSTPARSERParser.Struct_var_decl_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_decl_no_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_decl_no_init(PLCSTPARSERParser.Fb_decl_no_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_decl_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_decl_init(PLCSTPARSERParser.Fb_decl_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_name(PLCSTPARSERParser.Fb_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_instance_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_instance_name(PLCSTPARSERParser.Fb_instance_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#output_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_decls(PLCSTPARSERParser.Output_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#output_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_decl(PLCSTPARSERParser.Output_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#in_out_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_out_decls(PLCSTPARSERParser.In_out_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#in_out_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_out_var_decl(PLCSTPARSERParser.In_out_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_decl(PLCSTPARSERParser.Var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#array_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_var_decl(PLCSTPARSERParser.Array_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#struct_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_var_decl(PLCSTPARSERParser.Struct_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_decls(PLCSTPARSERParser.Var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#retain_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRetain_var_decls(PLCSTPARSERParser.Retain_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#loc_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoc_var_decls(PLCSTPARSERParser.Loc_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#loc_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoc_var_decl(PLCSTPARSERParser.Loc_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#temp_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemp_var_decls(PLCSTPARSERParser.Temp_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#external_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternal_var_decls(PLCSTPARSERParser.External_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#external_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternal_decl(PLCSTPARSERParser.External_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#global_var_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_var_name(PLCSTPARSERParser.Global_var_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#global_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_var_decls(PLCSTPARSERParser.Global_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#global_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_var_decl(PLCSTPARSERParser.Global_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#global_var_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_var_spec(PLCSTPARSERParser.Global_var_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#loc_var_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoc_var_spec_init(PLCSTPARSERParser.Loc_var_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#located_at}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocated_at(PLCSTPARSERParser.Located_atContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#located_at_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocated_at_init(PLCSTPARSERParser.Located_at_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#str_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStr_var_decl(PLCSTPARSERParser.Str_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitS_byte_str_var_decl(PLCSTPARSERParser.S_byte_str_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#s_byte_str_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitS_byte_str_spec(PLCSTPARSERParser.S_byte_str_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitD_byte_str_var_decl(PLCSTPARSERParser.D_byte_str_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#d_byte_str_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitD_byte_str_spec(PLCSTPARSERParser.D_byte_str_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoc_partly_var_decl(PLCSTPARSERParser.Loc_partly_var_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#loc_partly_var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoc_partly_var(PLCSTPARSERParser.Loc_partly_varContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#var_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_spec(PLCSTPARSERParser.Var_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_name(PLCSTPARSERParser.Func_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_access(PLCSTPARSERParser.Func_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#scope_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScope_name(PLCSTPARSERParser.Scope_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_func_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_func_name(PLCSTPARSERParser.Derived_func_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_decl(PLCSTPARSERParser.Func_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#io_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIo_var_decls(PLCSTPARSERParser.Io_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_var_decls(PLCSTPARSERParser.Func_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_body(PLCSTPARSERParser.Func_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_type_name(PLCSTPARSERParser.Fb_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_type_access(PLCSTPARSERParser.Fb_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#derived_fb_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_fb_name(PLCSTPARSERParser.Derived_fb_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_decl(PLCSTPARSERParser.Fb_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_io_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_io_var_decls(PLCSTPARSERParser.Fb_io_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_input_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_input_decls(PLCSTPARSERParser.Fb_input_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_input_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_input_decl(PLCSTPARSERParser.Fb_input_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_output_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_output_decls(PLCSTPARSERParser.Fb_output_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_output_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_output_decl(PLCSTPARSERParser.Fb_output_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#other_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOther_var_decls(PLCSTPARSERParser.Other_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#no_retain_var_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNo_retain_var_decls(PLCSTPARSERParser.No_retain_var_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_body(PLCSTPARSERParser.Fb_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#method_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_decl(PLCSTPARSERParser.Method_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#method_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_name(PLCSTPARSERParser.Method_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#class_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_decl(PLCSTPARSERParser.Class_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#class_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_name(PLCSTPARSERParser.Class_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#class_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_access(PLCSTPARSERParser.Class_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#class_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_name(PLCSTPARSERParser.Class_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#instance_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstance_name(PLCSTPARSERParser.Instance_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_decl(PLCSTPARSERParser.Interface_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#method_prototype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_prototype(PLCSTPARSERParser.Method_prototypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_spec_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_spec_init(PLCSTPARSERParser.Interface_spec_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_value(PLCSTPARSERParser.Interface_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_name_list(PLCSTPARSERParser.Interface_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_type_name(PLCSTPARSERParser.Interface_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_type_access(PLCSTPARSERParser.Interface_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#interface_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterface_name(PLCSTPARSERParser.Interface_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_decl(PLCSTPARSERParser.Prog_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_type_name(PLCSTPARSERParser.Prog_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_type_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_type_access(PLCSTPARSERParser.Prog_type_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_access_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_access_decls(PLCSTPARSERParser.Prog_access_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_access_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_access_decl(PLCSTPARSERParser.Prog_access_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#sfc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSfc(PLCSTPARSERParser.SfcContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#sfc_network}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSfc_network(PLCSTPARSERParser.Sfc_networkContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#initial_step}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitial_step(PLCSTPARSERParser.Initial_stepContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#step}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStep(PLCSTPARSERParser.StepContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#step_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStep_name(PLCSTPARSERParser.Step_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#action_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_association(PLCSTPARSERParser.Action_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#action_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_name(PLCSTPARSERParser.Action_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#action_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_qualifier(PLCSTPARSERParser.Action_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#action_time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_time(PLCSTPARSERParser.Action_timeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#indicator_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndicator_name(PLCSTPARSERParser.Indicator_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#transition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransition(PLCSTPARSERParser.TransitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#transition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransition_name(PLCSTPARSERParser.Transition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#steps}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSteps(PLCSTPARSERParser.StepsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#transition_cond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransition_cond(PLCSTPARSERParser.Transition_condContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction(PLCSTPARSERParser.ActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#config_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_name(PLCSTPARSERParser.Config_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#resource_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource_type_name(PLCSTPARSERParser.Resource_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#config_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_decl(PLCSTPARSERParser.Config_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#resource_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource_decl(PLCSTPARSERParser.Resource_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#single_resource_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_resource_decl(PLCSTPARSERParser.Single_resource_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#resource_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource_name(PLCSTPARSERParser.Resource_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#access_decls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccess_decls(PLCSTPARSERParser.Access_declsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#access_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccess_decl(PLCSTPARSERParser.Access_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#access_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccess_path(PLCSTPARSERParser.Access_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#global_var_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_var_access(PLCSTPARSERParser.Global_var_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#access_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccess_name(PLCSTPARSERParser.Access_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_output_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_output_access(PLCSTPARSERParser.Prog_output_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_name(PLCSTPARSERParser.Prog_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#task_config}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTask_config(PLCSTPARSERParser.Task_configContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#task_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTask_name(PLCSTPARSERParser.Task_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#task_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTask_init(PLCSTPARSERParser.Task_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#data_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_source(PLCSTPARSERParser.Data_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_config}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_config(PLCSTPARSERParser.Prog_configContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elems}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_conf_elems(PLCSTPARSERParser.Prog_conf_elemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_conf_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_conf_elem(PLCSTPARSERParser.Prog_conf_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_task}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_task(PLCSTPARSERParser.Fb_taskContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_cnxn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_cnxn(PLCSTPARSERParser.Prog_cnxnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#prog_data_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg_data_source(PLCSTPARSERParser.Prog_data_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#data_sink}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_sink(PLCSTPARSERParser.Data_sinkContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#config_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_init(PLCSTPARSERParser.Config_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#config_inst_init}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_inst_init(PLCSTPARSERParser.Config_inst_initContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#namespace_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_decl(PLCSTPARSERParser.Namespace_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#namespace_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_elements(PLCSTPARSERParser.Namespace_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#namespace_h_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_h_name(PLCSTPARSERParser.Namespace_h_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#namespace_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_name(PLCSTPARSERParser.Namespace_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#using_directive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_directive(PLCSTPARSERParser.Using_directiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pou_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPou_decl(PLCSTPARSERParser.Pou_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#instruction_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstruction_list(PLCSTPARSERParser.Instruction_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_instruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_instruction(PLCSTPARSERParser.Il_instructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_simple_inst(PLCSTPARSERParser.Il_simple_instContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_label(PLCSTPARSERParser.Il_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_simple_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_simple_operation(PLCSTPARSERParser.Il_simple_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_expr(PLCSTPARSERParser.Il_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_jump_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_jump_operation(PLCSTPARSERParser.Il_jump_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_invocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_invocation(PLCSTPARSERParser.Il_invocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_formal_func_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_formal_func_call(PLCSTPARSERParser.Il_formal_func_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_operand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_operand(PLCSTPARSERParser.Il_operandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_operand_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_operand_list(PLCSTPARSERParser.Il_operand_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_simple_inst_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_simple_inst_list(PLCSTPARSERParser.Il_simple_inst_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_simple_instruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_simple_instruction(PLCSTPARSERParser.Il_simple_instructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_param_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_param_list(PLCSTPARSERParser.Il_param_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_param_inst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_param_inst(PLCSTPARSERParser.Il_param_instContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_param_last_inst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_param_last_inst(PLCSTPARSERParser.Il_param_last_instContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_param_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_param_assign(PLCSTPARSERParser.Il_param_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_param_out_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_param_out_assign(PLCSTPARSERParser.Il_param_out_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_simple_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_simple_operator(PLCSTPARSERParser.Il_simple_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_assignment(PLCSTPARSERParser.Il_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#il_assign_out_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIl_assign_out_operator(PLCSTPARSERParser.Il_assign_out_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PLCSTPARSERParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#constant_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_expr(PLCSTPARSERParser.Constant_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#xor_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXor_expr(PLCSTPARSERParser.Xor_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#and_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd_expr(PLCSTPARSERParser.And_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#compare_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompare_expr(PLCSTPARSERParser.Compare_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#equ_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqu_expr(PLCSTPARSERParser.Equ_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#add_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_expr(PLCSTPARSERParser.Add_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#term}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTerm(PLCSTPARSERParser.TermContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#power_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPower_expr(PLCSTPARSERParser.Power_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#unary_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expr(PLCSTPARSERParser.Unary_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#primary_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expr(PLCSTPARSERParser.Primary_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#variable_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_access(PLCSTPARSERParser.Variable_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#multibit_part_access}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultibit_part_access(PLCSTPARSERParser.Multibit_part_accessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#func_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_call(PLCSTPARSERParser.Func_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#stmt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_list(PLCSTPARSERParser.Stmt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(PLCSTPARSERParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#print_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_stmt(PLCSTPARSERParser.Print_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#print_stmt_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_stmt_element(PLCSTPARSERParser.Print_stmt_elementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code variableAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableAssignExpression(PLCSTPARSERParser.VariableAssignExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refAssignExpression}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefAssignExpression(PLCSTPARSERParser.RefAssignExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pointerAssign}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointerAssign(PLCSTPARSERParser.PointerAssignContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignmentAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentAttempt(PLCSTPARSERParser.AssignmentAttemptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code pointerAssignAttempt}
	 * labeled alternative in {@link PLCSTPARSERParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointerAssignAttempt(PLCSTPARSERParser.PointerAssignAttemptContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#assignment_attempt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_attempt(PLCSTPARSERParser.Assignment_attemptContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#pointer_assigment_attempt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer_assigment_attempt(PLCSTPARSERParser.Pointer_assigment_attemptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code invocation1}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvocation1(PLCSTPARSERParser.Invocation1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code invocation2}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvocation2(PLCSTPARSERParser.Invocation2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code invocation3}
	 * labeled alternative in {@link PLCSTPARSERParser#invocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvocation3(PLCSTPARSERParser.Invocation3Context ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#invocation1branch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvocation1branch(PLCSTPARSERParser.Invocation1branchContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#invocation2branch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvocation2branch(PLCSTPARSERParser.Invocation2branchContext ctx);
	/**
	 * Visit a parse tree produced by the {@code callFunc}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallFunc(PLCSTPARSERParser.CallFuncContext ctx);
	/**
	 * Visit a parse tree produced by the {@code superCall}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperCall(PLCSTPARSERParser.SuperCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code return}
	 * labeled alternative in {@link PLCSTPARSERParser#subprog_ctrl_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn(PLCSTPARSERParser.ReturnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inputParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputParam(PLCSTPARSERParser.InputParamContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefParam(PLCSTPARSERParser.RefParamContext ctx);
	/**
	 * Visit a parse tree produced by the {@code outParam}
	 * labeled alternative in {@link PLCSTPARSERParser#param_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutParam(PLCSTPARSERParser.OutParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#selection_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelection_stmt(PLCSTPARSERParser.Selection_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#if_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_stmt(PLCSTPARSERParser.If_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#elsif_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElsif_stmt(PLCSTPARSERParser.Elsif_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#else_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElse_stmt(PLCSTPARSERParser.Else_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#case_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_stmt(PLCSTPARSERParser.Case_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#case_selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_selection(PLCSTPARSERParser.Case_selectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#case_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_list(PLCSTPARSERParser.Case_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#case_list_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_list_elem(PLCSTPARSERParser.Case_list_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#iteration_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIteration_stmt(PLCSTPARSERParser.Iteration_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#for_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_stmt(PLCSTPARSERParser.For_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#control_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControl_variable(PLCSTPARSERParser.Control_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#by_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBy_list(PLCSTPARSERParser.By_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#while_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_stmt(PLCSTPARSERParser.While_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#repeat_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepeat_stmt(PLCSTPARSERParser.Repeat_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#ladder_diagram}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLadder_diagram(PLCSTPARSERParser.Ladder_diagramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#fb_diagram}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFb_diagram(PLCSTPARSERParser.Fb_diagramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PLCSTPARSERParser#reservedKeyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReservedKeyword(PLCSTPARSERParser.ReservedKeywordContext ctx);
}