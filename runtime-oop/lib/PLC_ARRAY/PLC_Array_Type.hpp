#ifndef PLC_ARRAY_TYPE_HPP_
#define PLC_ARRAY_TYPE_HPP_

/***************************************************************
 * @file       PLC_Array_Type.hpp
 * @brief      PLC_Array_Type是数组类，模板参数为数组类型的类型编号,不同长度、同一内部类型的数组认定为同一类型
 *              数组长度认定为值的属性
 **************************************************************/

#include <vector>
#include "../PLC_Type.h"
#include "../PLC_Value.h"
#include "../PLC_Type_Dict.h"
#include "./PLC_Array_Value.hpp"

template<int TYPEID>
class PLC_Array_Value;

template<int TYPEID>
class PLC_Array_Type : public PLC_Type{
	private:
    	/*数组内变量的初始值*/
    	std::vector<PLC_Value*> _initVals;
    	/*数组元素的类型标识符*/
    	int _i_varId;
public:
    int getIVarId() const {
        return _i_varId;
    }

    std::vector<PLC_Value*>* getInitVars(){
        return &this->_initVals;
    }

private:
    /*数组的默认长度*/
        int _i_length;

public:
    PLC_Array_Type(int i_varId, int i_length=0, std::vector<PLC_Value*>* initVars = NULL) :
    PLC_Type()
    {
        this->setTypeId(TYPEID);
        this->_i_varId = i_varId;
        this->_i_length = i_length;
        this->setTypeName("ARRAY_OF_" + PLC_Type_Dict::findType(i_varId)->getTypeName());
        PLC_Type_Dict::registerType(TYPEID, this);
        //填入初始化值，则保存
        if(initVars){
            this->_initVals = *initVars;
        }
    }

    PLC_Value* createValue() override
    {
        auto* p_tempValue = new PLC_Array_Value<TYPEID>("");
        return p_tempValue;
    }

    PLC_Value* createValue(PLC_Value* resource) override
    {
        auto* tempValue = dynamic_cast<PLC_Array_Value<TYPEID>*>(resource);
        auto* p_newValue = new PLC_Array_Value<TYPEID>(tempValue);
        return p_newValue;
    }

    PLC_Type * deepCloneType() override{
        PLC_Array_Type<TYPEID>* clonedType = new PLC_Array_Type<TYPEID>(this->_i_varId);
        return clonedType;
    }

    int getLength() const {
        return _i_length;
    }

};
#endif