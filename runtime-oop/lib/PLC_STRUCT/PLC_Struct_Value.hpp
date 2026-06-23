#ifndef PLC_STRUCT_VALUE_H_
#define PLC_STRUCT_VALUE_H_

/***************************************************************
 * @file       PLC_Struct_Value.hpp
 * @brief      PLC_Struct_Value类是结构体的值类，所有自定义结构体都是此类的实例化。
 * @author     chh
 * @version    0.1
 * @date       2022.4.18
 * @modify     从h cpp文件合并而来
 **************************************************************/

#include<string>
#include <vector>
#include "../PLC_Value.h"
#include "./PLC_Struct_FieldInfo.h"
#include "./PLC_Struct_Type.hpp"

template<int TYPEID>
class PLC_Struct_Type;

template<int TYPEID>
class PLC_Struct_Value : public PLC_Value
{
	private:
    	/*包装类型内部值的数据结构*/
		std::vector<FieldInfo*> _valueArr;

		/***************************************************************
         *  @brief     	查字典找到结构体类型类，根据类型类的_fieldArr初始化_valueArr
		 *  @param      valueArr:即将复制给_valueArr
         **************************************************************/
    	void _initValueTable(std::vector<FieldInfo*>* valueArr)
        {
            auto t = valueArr->begin();
            while(t != valueArr->end()){
                auto* p = new FieldInfo(*t);
                this->_valueArr.push_back(p);
                t++;
            }
        }
		
    public:

		/***************************************************************
         *  @brief     	标准初始化结构体,构造函数流程：找到FieldInfo Array传入_initValueTable
         **************************************************************/
        PLC_Struct_Value(std::string str_name = "") : 
            PLC_Value(TYPEID, str_name)
        {
            auto* _tempType = dynamic_cast<PLC_Struct_Type<TYPEID>*>(getThisType());
            std::vector<FieldInfo*>* _tempArr = _tempType->getFieldArr();
            this->_initValueTable(_tempArr);
        }

		/***************************************************************
         *  @brief     	拷贝构造结构体
         **************************************************************/
		PLC_Struct_Value(PLC_Struct_Value* resourse) :
            PLC_Value(TYPEID, resourse->getValueName())
        {
            std::vector<FieldInfo*>* _tempArr = resourse->getFieldArr();
            this->_initValueTable(_tempArr);
        }

		/***************************************************************
         *  @brief     	得到对象内部的fieldArr，供拷贝构造函数使用
         **************************************************************/
		std::vector<FieldInfo*>* getFieldArr()
        {
            return &this->_valueArr;
        }

		/***************************************************************
         *  @brief     	根据名称索引结构体内的值,谁使用此方法，谁负责做向下转型，查找不到则报错
		 *  @param      str_name:索引的域成员的名称
		 *  @return     查找到的值的引用
         **************************************************************/
    	PLC_Value* GetFieldByName(std::string str_name)
        {
            auto t = this->_valueArr.begin();
            while(t != this->_valueArr.end()){
                if(str_name == (*t)->str_name){
                    return (*t)->value;
                }
                t++;
            }
            return nullptr;
            /*TODO:throw error*/
        }

		/***************************************************************
         *  @brief     	根据位置索引结构体内的值,谁使用此方法，谁负责做向下转型，查找不到则报错
		 *  @param      i_index:索引的域成员的次序
		 *  @return     查找到的值的引用
         **************************************************************/
    	PLC_Value* GetFieldByIndex(int i_index)
        {
            /*TODO:i_index意义暂定*/
            if(this->_valueArr.size() >= i_index){
                return this->_valueArr.at(i_index)->value;
            }
            return NULL;
            /*TODO:throw error*/
        }

		/***************************************************************
         *  @brief     	根据相对位置索引,谁使用此方法，谁负责做向下转型，查找不到则报错
		 *  @param      i_addr:索引的域成员的相对位置
		 *  @return     查找到的值的引用
         **************************************************************/
    	PLC_Value* GetFieldByAddr(int i_addr)
        {
            auto t = this->_valueArr.begin();
            /*TODO:i_addr意义暂定,可能需要修改*/
            while(t != this->_valueArr.end()){
                if(i_addr == (*t)->i_addr){
                    return (*t)->value;
                }
                t++;
            }
            return nullptr;
        }

        void setValue(PLC_Struct_Value<TYPEID>* value){
            this->_initValueTable(value->_valueArr);
        }
		
		/***************************************************************
		 *  @brief     	将数据的值转为字符串，打印输出测试使用
		 *  @return		值类存放的数据字符串(std::string类型)
		 **************************************************************/
		std::string toString()
        {
            std::string str("{");
            auto t = this->_valueArr.begin();
            while(t != this->_valueArr.end()){
                str += "<TypeId:" + std::to_string((*t)->i_typeId) +
                            ", Address:" + std::to_string((*t)->i_addr) +
                            ", Name:" + (*t)->str_name +
                            ", Value:" + (*t)->value->toString() + ">";
                t++;
            }
            str += "}";
            return str;
        }
};



#endif