#ifndef PLC_ARRAY_VALUE_HPP_
#define PLC_ARRAY_VALUE_HPP_


#include <vector>
#include "../PLC_Value.h"
#include "PLC_Array_Type.hpp"


template<int TYPEID>
class PLC_Array_Type;

template<int TYPEID>
class PLC_Array_Value : public PLC_Value{
private:
    /*存储数据的结构*/
    std::vector<PLC_Value*> _data;


private:
    /*数组长度*/
    int _i_length;

    /*数据的类型id*/
    int _i_dataTypeId;

    /*数据的类型类*/
//    PLC_Type* _dataType;


public:
    /***************************************************************
     *  @brief     	设置对象的长度_i_length
     **************************************************************/
    short int getLength()
    {
        return this->_i_length;
    }

    std::vector<PLC_Value*>* getData(){
        return &this->_data;
    }

private:
    /**
     * @brief 将data中的内容深拷贝进本数组中
     * */
    void _initArray(std::vector<PLC_Value*>* data){
        std::vector<PLC_Value*>::iterator t = data->begin();
        while(t != data->end()){
            PLC_Value* newVar = (*t)->getThisType()->createValue(*t);
            this->_data.push_back(newVar);
            t++;
        }
    }


public:
    PLC_Array_Value(std::string str_name = "") :
    PLC_Value(TYPEID, str_name)
    {
        PLC_Array_Type<TYPEID>* thisType = dynamic_cast<PLC_Array_Type<TYPEID>*>(this->getThisType());
        this->_i_length = thisType->getLength();
        this->_initArray(thisType->getInitVars());

    }

    PLC_Array_Value(PLC_Array_Value<TYPEID>* resource):
            PLC_Value(TYPEID, resource->getValueName())
    {
        this->_i_length = resource->getLength();
        this->_initArray(resource->getData());
    }

    PLC_Value* at(int index){
        return this->_data.at(index);
    }

    void add(PLC_Value* e){
        this->_data.push_back(e);
    }

    std::string toString() override{
        std::string s = "ARRAY : {";
        for (PLC_Value* item: _data){
            s += item->toString();
        }
        s += "}";
        return s;
    }

};




#endif