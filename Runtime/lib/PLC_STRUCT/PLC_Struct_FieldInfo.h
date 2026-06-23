#ifndef PLC_STRUCT_FIELDINFO_H_
#define PLC_STRUCT_FIELDINFO_H_

/***************************************************************
 * @file       PLC_Struct_Field.h
 * @brief      PLC_Struct_Field结构体是存储结构体内变量单元，内部包含了成员的相对位置、名称、标识符、值类等
 * @author     chh
 * @version    0.1
 * @date       2022.4.18
 * @modify     修改了构造函数的定义方式
 * @reason     方便
 **************************************************************/

#include "../PLC_Value.h"

typedef struct PLC_Struct_FieldInfo{
    /*成员的相对地址（结构体相对位置访问需要）*/
    int i_addr;
    /*成员的名称（结构体按名称访问需要）*/
    std::string str_name;
    /*成员的类型标识符*/
    int i_typeId;
    /*成员的初始值的指针,对象销毁时需要释放*/
    PLC_Value* value;

    /***************************************************************
     *  @brief     	结构体类型的拷贝构造方法（需要深度拷贝）
     *  @param      resource:拷贝源
     **************************************************************/
    PLC_Struct_FieldInfo(PLC_Struct_FieldInfo* resource);

    /***************************************************************
     *  @brief     	结构体类型的默认构造方法
     *  @param      i_addr:相对地址
     *  @param      str_name:成员的名称（结构体按名称访问需要）
     *  @param      i_typeId:成员的类型标识符
     *  @param      value:成员的初始值的引用
     **************************************************************/
    PLC_Struct_FieldInfo(int i_addr, std::string str_name, int i_typeId, PLC_Value* value);
    
}FieldInfo;


#endif