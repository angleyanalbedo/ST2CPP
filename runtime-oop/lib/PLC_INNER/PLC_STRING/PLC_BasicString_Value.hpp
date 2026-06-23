#ifndef PLC_BASICSTRING_VALUE_HPP_
#define PLC_BASICSTRING_VALUE_HPP_
/***************************************************************
 * @file       PLC_BasicString_Value.hpp
 * @brief      PLC_BasicString_Value类是所有字符串值类类的基类
 * @author     chh
 * @note		支持宽字符时需要考虑输出信息toString的实现
 * @todo        字符串类型的特殊操作，如拼接等
 * @todo        字符串长度检查
 **************************************************************/

#include "../PLC_Inner_Value.h"
#include "../../PLC_Type_Dict.h"
#include "./PLC_BasicString_Type.hpp"

template<typename STRING>
class PLC_BasicString_Value : public PLC_Inner_Value{
	private:
		/*存储数据的结构*/
    	STRING _data;
        /*字符串长度，最长为宏SSTRINGMAXLENGTH*/
        short int _i_length;

	public:
        /***************************************************************
		 *  @brief     	设置对象的长度_i_length
		 **************************************************************/
		short int getLength()
		{
			return this->_i_length;
		}

		/***************************************************************
		 *  @brief     	直接设置data对象内的值
		 **************************************************************/
		STRING getData()
		{
			return this->_data;
		}

    public:
        /***************************************************************
		 *  @brief     	根据具体值/默认值构造
		 *  @param      si_data:值类的初始值，默认为0
         *  @param      i_length:指定长度
		 *  @param      str_name:值类的名称，默认为"SINT"
		 **************************************************************/
    	PLC_BasicString_Value(int i_length, STRING data = "", std::string str_name = "") :
			PLC_Inner_Value(SSTRINGTYPEID, str_name),
			_i_length(i_length),
			_data(data)
		{}

        /***************************************************************
		 *  @brief     	拷贝构造
		 *  @param      resource:源值类的引用
		 **************************************************************/
    	PLC_BasicString_Value(PLC_BasicString_Value<STRING>* resource) :
			PLC_Inner_Value(SSTRINGTYPEID, resource->getValueName()),
			_i_length(resource->getLength()),
			_data(resource->getData())
		{}

        /***************************************************************
		 *  @brief     	将数据的值转为字符串，打印输出测试使用
		 *  @return		值类存放的数据字符串(std::string类型)
		 **************************************************************/
		std::string toString() override
		{
			//TODO:区分宽字符串和单字符串
			return this->getData();
		}
};

#endif