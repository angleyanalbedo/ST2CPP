#include"PLC_Type.h"


void PLC_Type::setTypeId(int i_typeId)
{
    this->_i_typeId = i_typeId;
}

int PLC_Type::getTypeId()
{
    return this->_i_typeId;
}

void PLC_Type::setSize(int i_size)
{
    this->_i_size = i_size;
}

int PLC_Type::getSize()
{
    return this->_i_size;
}

void PLC_Type::setTypeName(std::string str_name)
{
    this->_str_typeName.assign(str_name);
}

std::string PLC_Type::getTypeName()
{
    return this->_str_typeName;
}