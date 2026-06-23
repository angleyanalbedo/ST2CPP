#ifndef PLC_ENUM_VALUE_HPP_
#define PLC_ENUM_VALUE_HPP_

/***************************************************************
 * @file       PLC_Enum_Value.hpp
 * @brief      枚举类
 * @author     chh
 * @version    0.1
 * @date       2022.4.29
 **************************************************************/

#include "../PLC_Value.h"
#include "../PLC_Type_Dict.h"
#include "./PLC_Enum_Type.hpp"

template<int TYPEID>
class PLC_Enum_Type;

template<int TYPEID>
class PLC_Enum_Value : public PLC_Value
{
    private:
        /*枚举类型的内部结构*/
        PLC_Inner_Value* _data;

    public:

        /***************************************************************
         *  @brief     	获取值
         **************************************************************/
        PLC_Inner_Value* getData()
        {
            return this->_data;
        }

        void setData(PLC_Inner_Value* data){
            this->_data = data;
        }

        void setValue(PLC_Enum_Value* value){
            this->_data = value->getData();
        }

        /***************************************************************
         *  @brief     	根据枚举类型的初始值构造
         **************************************************************/    
        PLC_Enum_Value(std::string str_name = "") :
            PLC_Value(TYPEID, str_name)
        {
            auto* thisType = dynamic_cast<PLC_Enum_Type<TYPEID>*>(this->getThisType());
            this->_data = thisType->getInitVal();
        }

        /***************************************************************
         *  @brief     	拷贝构造
         **************************************************************/    
        PLC_Enum_Value(PLC_Enum_Value<TYPEID>* resource) :
            PLC_Value(TYPEID, resource->getValueName())
        {
            this->_data = resource->getData();
        }

        /***************************************************************
         *  @brief     	析构，释放内部data
         **************************************************************/    
        ~PLC_Enum_Value()
        {
//            delete(this->_data);
        }

        /***************************************************************
         *  @brief     	测试用方法
         **************************************************************/    
        std::string toString()
        {
            return this->_data->toString();
        }
};






#endif