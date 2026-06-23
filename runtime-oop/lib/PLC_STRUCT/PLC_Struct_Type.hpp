#ifndef PLC_STRUCT_TYPE_H_
#define PLC_STRUCT_TYPE_H_

/***************************************************************
 * @file       PLC_Struct_Type.h
 * @brief      PLC_Struct_Type类是所有结构体的类型类，所有自定义结构体类型都是此类的实例化。
 *             原则：内部存储的数据存放在堆内，随类释放而释放
 * @author     chh
 * @version    0.1
 * @date       2022.4.18
 * @modify     从h cpp文件合并而来
 **************************************************************/

#include<vector>
#include"../PLC_Type.h"
#include"../PLC_Value.h"
#include"./PLC_Struct_FieldInfo.h"
#include"./PLC_Struct_Value.hpp"

template<int TYPEID>
class PLC_Struct_Type : public PLC_Type
{
    private:
        /*存储结构体成员数据信息的向量*/
        std::vector<FieldInfo*> _fieldArr;
    public:
        /***************************************************************
         *  @brief     	获得内部_fieldArr的引用
         **************************************************************/
        std::vector<FieldInfo*>* getFieldArr()
        {
            return &this->_fieldArr;
        }

        /***************************************************************
         *  @brief     	向结构体类型添加内部数据信息
         *  @param      i_addr:相对地址
         *  @param      str_name:成员的名称（结构体按名称访问需要）
         *  @param      i_typeId:成员的类型标识符
         *  @param      initVal:成员的初始值的引用,根据此值在堆内创建并保存
         **************************************************************/
    	void addField(int i_addr, std::string str_name, PLC_Value& initVal)
        {
            int i_typeId = initVal.getTypeId();
            PLC_Type* fieldType = initVal.getThisType();
            PLC_Value* tempVal = fieldType->createValue(&initVal);
            PLC_Struct_FieldInfo* fieldInfo = new PLC_Struct_FieldInfo(i_addr, str_name, i_typeId, tempVal);
            this->_fieldArr.push_back(fieldInfo);
            int i_newSize = fieldType->getSize() + this->getSize();
            this->setSize(i_newSize);
        }
        
        /***************************************************************
         *  @brief     	向结构体类型添加内部数据信息,数据的初始值为类型的默认值
         *  @param      i_addr:相对地址
         *  @param      str_name:成员的名称（结构体按名称访问需要）
         *  @param      i_typeId:成员的类型标识符
         **************************************************************/
        void addField(int i_addr, std::string str_name, int i_typeId)
        {
            /*TODO:检查类型是否正确*/
            PLC_Type* fieldType = PLC_Type_Dict::findType(i_typeId);
            PLC_Value* initVal = fieldType->createValue();
            PLC_Struct_FieldInfo* fieldInfo = new PLC_Struct_FieldInfo(i_addr, str_name, i_typeId, initVal);
            this->_fieldArr.push_back(fieldInfo);
            int i_newSize = fieldType->getSize() + this->getSize();
            this->setSize(i_newSize);
        }

        /**
        * TODO:size信息补全
        * */
        void addField(PLC_Struct_FieldInfo* p_fieldInfo){
            this->_fieldArr.push_back(p_fieldInfo);
        }

        /***************************************************************
         *  @brief     	从传入的类型id号、类型名称初始化结构体类(需要另外调用addField方法来添加内部域)
         *  @param      str_typeName:结构体的名称
         **************************************************************/
        PLC_Struct_Type(std::string str_typeName = "") :
        PLC_Type()
        {
            this->setTypeId(TYPEID);
            this->setTypeName(str_typeName);
            PLC_Type_Dict::registerType(TYPEID, this);
        }

        /***************************************************************
         *  @brief     	域数组中的PLC_Value声明在堆中，类型对象摧毁时需要释放掉结构体内引用的PLC_VALUE
         **************************************************************/
        ~PLC_Struct_Type()
        {
        }

        /***************************************************************
         *  @brief     	调用值类的构造函数，在堆内创建值对象并返回
         *  @return     新创建的结构体类(使用需要转型)
         **************************************************************/
        PLC_Value* createValue()
        {
            PLC_Value* p = new PLC_Struct_Value<TYPEID>();
            return p;
        }


        /***************************************************************
         *  @brief     	调用值类的构造函数，在堆内创建值对象并返回
         *  @return     新创建的结构体类(使用需要转型)
         *  @param      resource：拷贝源
         **************************************************************/
        PLC_Value* createValue(PLC_Value* resource)
        {
            PLC_Value* p = new PLC_Struct_Value<TYPEID>(dynamic_cast<PLC_Struct_Value<TYPEID>*>(resource));
            return p;
        }

        /**
         * 类型类的深拷贝函数
         * */
        PLC_Type* deepCloneType() override{
            auto* clonedType = new PLC_Struct_Type<TYPEID>(this->getTypeName());
            for (const auto &item: this->_fieldArr){
                auto* newField = new PLC_Struct_FieldInfo(item);
                clonedType->addField(newField);
            }
            return clonedType;
        }
};
#endif