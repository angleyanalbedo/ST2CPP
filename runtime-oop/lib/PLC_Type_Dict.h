#ifndef PLC_TYPE_DICT_H_
#define PLC_TYPE_DICT_H_

/***************************************************************
 * @file       PLC_Type_Dict.h
 * @brief      PLC_Type_Dict类是用于存放注册的类型类，并且提供类型的索引方法
 * @author     chh
 * @version    0.1
 * @date       2022.4.17
 * @modify     将registerType findType声明为静态函数
 * @reason     方便调用
 * @modify     2022.04.18删除getUnmappedTypeId,类型id号编排应由静态编译负责指定，不由库文件负责
 **************************************************************/


#include<map>

#include "PLC_Type.h"

class PLC_Type_Dict
{
    private:
        /*存储类型对象与类型标识符的字典*/
        std::map<int, PLC_Type*>* _typeDict;
public:
    std::map<int, PLC_Type *> *getTypeDict();

private:

    /*内置的字典类，可以通过静态方法getDict()获得*/
        static PLC_Type_Dict PLCTypeDict;

		/***************************************************************
		 *  @brief     	初始化内置类型的类型标识符
         *  @todo       创建新类型时需要在此处补上
		 **************************************************************/
        PLC_Type_Dict();

    public:
        /***************************************************************
		 *  @brief     	注册新类型，注册前需要检查类型标识符是否已经被使用
         *  @param      i_typeId:新类型的类型标识符
         *  @param      type:新类型的类型对象的引用
         *  @todo       检查、报错功能待实现
		 **************************************************************/
        static void registerType(int i_typeId, PLC_Type* type);

        /***************************************************************
		 *  @brief     	通过类型标识符查找类型引用
         *  @param      i_typeId:待查找的类型的类型标识符
         *  @return     查找到的类型对象的引用
         *  @todo       未查找到后报错功能待实现
		 **************************************************************/
        static PLC_Type* findType(int i_typeId);

        /***************************************************************
		 *  @brief     	获得字典对象的引用
         *  @return     类的静态变量PLCTypeDict
		 **************************************************************/
        static PLC_Type_Dict* getDict();

        /***************************************************************
        *  @brief     	打印字典
        **************************************************************/
        static void showDict();
};
#endif