//
// Created by 86176 on 2022/7/20.
//

#ifndef RUNTIME_TEST_PLC_SUBTYPE_TYPE_HPP
#define RUNTIME_TEST_PLC_SUBTYPE_TYPE_HPP

#include "PLC_Type.h"
#include "PLC_Type_Dict.h"

template<int TYPEID>
class PLC_Subtype_Type : public PLC_Type{
private:

    //仅仅初始值信息与父类型不一样的类型类
    PLC_Type* initValueType;

public:

    PLC_Subtype_Type(std::string str_typeName = "") :
    PLC_Type()
    {
        this->setTypeId(TYPEID);
        this->setTypeName(str_typeName);
        PLC_Type_Dict::registerType(TYPEID, this);
    }

    void setParentType(PLC_Type* p_initValueType){
        this->initValueType = p_initValueType;
    }


    ~PLC_Subtype_Type(){}


    PLC_Value* createValue()
    {
        return initValueType->createValue();
    }

    PLC_Value* createValue(PLC_Value* resource)
    {
        return initValueType->createValue(resource);
    }

    PLC_Type* deepCloneType() override{
        PLC_Subtype_Type<TYPEID>* clonedType = new PLC_Subtype_Type<TYPEID>(this->getTypeName());
        return clonedType;
    }
};


#endif
