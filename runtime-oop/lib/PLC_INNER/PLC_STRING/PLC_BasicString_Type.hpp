#ifndef PLC_BASICSTRING_TYPE_HPP_
#define PLC_BASICSTRING_TYPE_HPP_
/***************************************************************
 * @file       PLC_BasicString_Type.hpp
 * @brief      PLC_BasicString_Type类是所有字符串类型类类的基类
 * @author     chh
 * @version    0.1
 * @date       2022.4.27创建
 * @modify      将字符串基类声明为模板类
 * @reason      减少代码，将长度作为值类的一个参数，STRING作为一种内置类型，
 *              不再将不同长度的字符串声明为新类型
 *              放松对string类的要求，但是需要添加更多的异常检测
 **************************************************************/

#include <typeinfo>
#include "../PLC_Inner_Type.h"
#include "./PLC_BasicString_Value.hpp"

template<typename STRING>
class PLC_BasicString_Type : public PLC_Inner_Type{
	private:
        /*单字节字符串的默认值*/
        STRING _initValue;

    public:
        /***************************************************************
		 *  @brief     	根据入参设置string的类型信息
         *  @param      i_typeId:类型标识符
         *  @param      initValue:初始值
         *  @param      str_name:类型名称
		 **************************************************************/        
    	PLC_BasicString_Type(STRING initValue, std::string str_name, int i_typeId)
        {
            this->_initValue.assign(initValue);

            this->setSize(initValue.size() * 
                            (typeid(STRING) == typeid(std::string) ? sizeof(char) : sizeof(wchar_t)));
            this->setTypeId(i_typeId);
            this->setTypeName(str_name);
        }

        /***************************************************************
		 *  @brief     	通过类型对象直接创建值对象，调用者负责释放创建的对象
         *  @return     创建的新STRING值类的引用
		 **************************************************************/
        PLC_Value* createValue()
        {
            PLC_BasicString_Value<STRING>* p_tempValue = new PLC_BasicString_Value<STRING>(0, this->_initValue, "");
            return p_tempValue;
        }

        /***************************************************************
		 *  @brief     	通过类型对象直接创建值对象，调用者负责释放创建的对象
         *  @return     创建的新STRING值类的引用
         *  @param      resource:拷贝源
		 **************************************************************/
        PLC_Value* createValue(PLC_Value* resource)
        {
            PLC_BasicString_Value<STRING>* tempValue = dynamic_cast<PLC_BasicString_Value<STRING>*>(resource);
            PLC_BasicString_Value<STRING>* p_newValue = new PLC_BasicString_Value<STRING>(tempValue);
            return p_newValue;
        }

        PLC_Type * deepCloneType() override{
            PLC_BasicString_Type<STRING>* newString = new PLC_BasicString_Type<STRING>(_initValue, this->getTypeName(), this->getTypeId());
            return newString;
        }
};

#endif