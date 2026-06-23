#ifndef PLC_VALUE_H_
#define PLC_VALUE_H_

/***************************************************************
 * @file       PLC_Value.h
 * @brief      PLC_Value类是所有值类的基类，所有具体的值类都间接或直接继承自这个类。
 * @author     chh
 * @version    0.1
 * @date       2022.4.16
 * @modify     所有值类的getTypeName->getValueName; setTypeName->setValueName
 * @reason     合理
 **************************************************************/

#include<string>

#include "./PLC_Object.h"
#include "PLC_Type_Dict.h"

class PLC_Type;

class PLC_Value : public PLC_Object
{
	public: 
		virtual ~PLC_Value(){}
		
	private:
		/*值所属类型的标识符，可以用于索引值的方法*/
		int _i_typeId;
    	/*值对应的变量的名称*/
    	std::string _str_name;
        /*值类对应的类型类*/
        PLC_Type* _ptr_thisType;
	public:
		PLC_Value(int i_typeId, std::string str_name);
		/***************************************************************
		 *  @brief     设置类型标识符
		 *  @param     i_typeId:新的类型标识符  
		 **************************************************************/
		void setTypeId(int i_typeId);


		/***************************************************************
		 *  @brief     	获取类型标识符
		 *  @return		值类的类型标识符(int类型)
		 **************************************************************/
		int getTypeId();

		/***************************************************************
		 *  @brief     	设置类型名称
		 *  @param		str_name:新的类型名称
		 **************************************************************/
		void setValueName(std::string str_name);

		/***************************************************************
		 *  @brief     	获得类型名称
		 *  @return		值类的类型名称(std::string类型)
		 **************************************************************/
		std::string getValueName();

		/***************************************************************
		 *  @brief     	获得类型类
		 **************************************************************/
		PLC_Type* getThisType();

		/***************************************************************
		 *  @brief     	将数据的值转为字符串，打印输出测试使用
		 *  @return		值类存放的数据字符串(std::string类型)
		 **************************************************************/
		virtual std::string toString(){
            return "variable of type " + std::to_string(_i_typeId);
        }
};

#endif