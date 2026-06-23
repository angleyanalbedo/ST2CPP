#include"./PLC_Value.h"

PLC_Value::PLC_Value(int i_typeId, std::string str_name):
    PLC_Object(),
    _i_typeId(i_typeId),
    _str_name(str_name),
    _ptr_thisType(PLC_Type_Dict::findType(i_typeId))
    {}


void PLC_Value::setTypeId(int i_typeId)
{
    this->_i_typeId = i_typeId;
}

int PLC_Value::getTypeId()
{
    return this->_i_typeId;
}

void PLC_Value::setValueName(std::string str_name)
{
     this->_str_name.assign(str_name);
}

std::string PLC_Value::getValueName()
{
    return this->_str_name;
}

PLC_Type* PLC_Value::getThisType()
{
    return this->_ptr_thisType;
}


