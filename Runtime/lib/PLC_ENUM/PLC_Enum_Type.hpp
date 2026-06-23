#ifndef PLC_ENUM_TYPE_HPP_
#define PLC_ENUM_TYPE_HPP_

/***************************************************************
 * @file       PLC_Enum_Type.hpp
 * @brief      枚举类
 * @author     chh
 * @version    0.1
 * @date       2022.4.29
 **************************************************************/

#include <vector>
#include <string>
#include "../PLC_Type.h"
#include "../PLC_INNER/PLC_Inner_Value.h"
#include "../PLC_Type_Dict.h"
#include "./PLC_Enum_Value.hpp"

template<int TYPEID>
class PLC_Enum_Type : public PLC_Type{
    private:
        /*存储枚举常量信息的向量，即上例中RED、GREEN等*/
        std::vector<PLC_Inner_Value*> _enumTable;
    	/*枚举变量初始值*/
    	PLC_Inner_Value* _initVal;
        /*内部数据的类型id*/
        int _i_inValId;

    public:
        /***************************************************************
         *  @brief     	获得枚举类型的初始值
         **************************************************************/    
        PLC_Inner_Value* getInitVal()
        {
            return this->_initVal;
        }

        int getInValId()
        {
            return this->_i_inValId;
        }
    
	public:
        /***************************************************************
         *  @brief     	构造方法
         **************************************************************/    
    	PLC_Enum_Type(std::string str_name, int i_inValId):
        PLC_Type()
        {
            this->setTypeId(TYPEID);
            this->setTypeName(str_name);
            this->_i_inValId = i_inValId;
            //TODO:检查i_inValId是不是内置
            PLC_Type_Dict::registerType(TYPEID, this);
		}
        /***************************************************************
         *  @brief     	析构方法，释放掉初始值、向量
         **************************************************************/    
        ~PLC_Enum_Type()
        {
        }
        
    	 /***************************************************************
         *  @brief     	添加枚举变量,复制一份传入的值加入 this->_enumTable
         **************************************************************/    
    	void addEnumValue(PLC_Inner_Value& enumValue)
        {
            if(enumValue.getTypeId() != this->_i_inValId)
            {
                /*TODO:throw error*/
                return;
            }
            PLC_Value* tempVal = enumValue.getThisType()->createValue(&enumValue);
            PLC_Inner_Value* processedVal = dynamic_cast<PLC_Inner_Value*>(tempVal);
            this->_enumTable.push_back(processedVal);
        }

        /***************************************************************
         *  @brief     	设置枚举类型的初始值
        **************************************************************/
        void setInitVal(PLC_Inner_Value& initVal)
        {
            this->_initVal = &initVal;
        }

        /***************************************************************
         *  @brief     	创建默认值
         **************************************************************/    
        PLC_Value* createValue()
        {
            PLC_Enum_Value<TYPEID>* p_tempVal = new PLC_Enum_Value<TYPEID>();
            return p_tempVal;
        }

        /***************************************************************
         *  @brief     	拷贝构造
         **************************************************************/    
        PLC_Value* createValue(PLC_Value* resource)
        {
            PLC_Enum_Value<TYPEID>* tempVal = dynamic_cast<PLC_Enum_Value<TYPEID>*>(resource);
            PLC_Enum_Value<TYPEID>* p_tempVal = new PLC_Enum_Value<TYPEID>(tempVal);
            return p_tempVal;
        }

        PLC_Type* deepCloneType() override{
            PLC_Enum_Value<TYPEID>* clonedType = new PLC_Enum_Value<TYPEID>(this->getTypeName(), this->_i_inValId);
            clonedType->setInitVal(this->_initVal);
            return clonedType;
        }
};  

#endif