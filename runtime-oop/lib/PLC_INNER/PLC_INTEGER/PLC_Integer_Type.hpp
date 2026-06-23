#ifndef PLC_INTEGER_TYPE_HPP_
#define PLC_INTEGER_TYPE_HPP_

/***************************************************************
 * @file       PLC_Integer_Type.h
 * @brief      PLC_Integer_Type类是所有整型类型类的实现类
 * @author     chh
 * @version    0.2
 * @date       2022.4.25
 **************************************************************/

#include"../PLC_Inner_Type.h"
#include"./PLC_Integer_Value.hpp"

template<typename T, int TYPEID>
class PLC_Integer_Type : public PLC_Inner_Type
{
    public:
        /***************************************************************
		 *  @brief     	根据入参设置类型信息
         *  @param      i_typeId:类型标识符
         *  @param      _initValue:初始值
         *  @param      str_name:类型名称
		 **************************************************************/
    	PLC_Integer_Type(T _initValue, std::string str_name, int i_size) :
        PLC_Inner_Type()
        {
            this->setTypeId(TYPEID);
            this->setTypeName(str_name);
            this->setSize(i_size);
            this->setInitValue(_initValue);
        }

    public:
        /***************************************************************
		 *  @brief     	通过类型对象直接创建值对象，调用者负责释放创建的对象
         *  @return     创建的新SINT值类的引用
		 **************************************************************/
        PLC_Value* createValue() override
        {
            auto* p_newValue = new PLC_Integer_Value<T, TYPEID>();
            return p_newValue;
        }

        /***************************************************************
		 *  @brief     	通过类型对象直接创建值对象，调用者负责释放创建的对象
         *  @return     创建的新SINT值类的引用
         *  @param      resource:拷贝源
		 **************************************************************/
        PLC_Value* createValue(PLC_Value* resource) override
        {
            auto* tempValue = dynamic_cast<PLC_Integer_Value<T, TYPEID>*>(resource);
            auto* p_newValue = new PLC_Integer_Value<T, TYPEID>(tempValue->getValue());
            return p_newValue;
        }

        PLC_Type* deepCloneType() override{
            PLC_Integer_Type<T, TYPEID>* clonedType = new PLC_Integer_Type<T, TYPEID>(this->getInitValue(), this->getTypeName(), this->getSize());
            return clonedType;
        }

        /***************************************************************
		 *  @brief     	设置类型的初始值
         *  @param      _initValue:新的初始值
		 **************************************************************/
        void setInitValue(T _initValue)
        {
            this->_initValue = _initValue;
        }

        /***************************************************************
		 *  @brief     	获取类型的初始值
         *  @return     类型的初始值
		 **************************************************************/
        T getInitValue()
        {
            return this->_initValue;
        }

    private:
        /*此短整数类型的初始值*/
        T _initValue;
        
};

#endif