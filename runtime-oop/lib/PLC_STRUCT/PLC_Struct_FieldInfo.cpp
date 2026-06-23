#include "./PLC_Struct_FieldInfo.h"


PLC_Struct_FieldInfo::PLC_Struct_FieldInfo(PLC_Struct_FieldInfo* resource):
    i_addr(resource->i_addr),
    str_name(resource->str_name),
    i_typeId(resource->i_typeId),
    value(PLC_Type_Dict::findType(i_typeId)->createValue(resource->value))
    {}

PLC_Struct_FieldInfo::PLC_Struct_FieldInfo(int i_addr, std::string str_name, int i_typeId, PLC_Value* value):
    i_addr(i_addr), str_name(str_name), i_typeId(i_typeId), 
    value(PLC_Type_Dict::findType(i_typeId)->createValue(value))
    {}