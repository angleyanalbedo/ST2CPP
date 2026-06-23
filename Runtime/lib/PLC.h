#ifndef PLC_H_
#define PLC_H_

/***************************************************************
 * @file        PLC.h
 * @brief       仅供测试使用,每创建一个新的头文件都需要在此处包含才能起作用。
 * @author      chh
 * @version     0.1
 * @date        2022.4.16
 * @modify      无
 * @reason      无
 **************************************************************/

//基类
#include "./PLC_Exception/PLC_Exception.h"
#include "./MemoryManager/MemoryManager.h"
#include "./PLC_Type_Dict.h"
#include "./PLC_Object.h"
#include "./PLC_Type.h"
#include "./PLC_Value.h"

//内置类型类
#include "./PLC_INNER/PLC_Inner_Type.h"
#include "./PLC_INNER/PLC_Inner_Value.h"

//整型类
#include "./PLC_INNER/PLC_INTEGER/PLC_Integer_Type.hpp"
#include "./PLC_INNER/PLC_INTEGER/PLC_Integer_Value.hpp"
#include "./PLC_INNER/PLC_INTEGER/PLC_SINT_Type.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_SINT_Value.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_INT_Type.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_INT_Value.h"

//浮点数类
#include "./PLC_INNER/PLC_INTEGER/PLC_Real_Type.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_Real_Value.h"

//字符串类
#include "./PLC_INNER/PLC_STRING/PLC_BasicString_Type.hpp"
#include "./PLC_INNER/PLC_STRING/PLC_BasicString_Value.hpp"
#include "./PLC_INNER/PLC_STRING/PLC_String_Type.h"
#include "./PLC_INNER/PLC_STRING/PLC_String_Value.h"

//枚举类
#include "./PLC_ENUM/PLC_Enum_Type.hpp"
#include "./PLC_ENUM/PLC_Enum_Value.hpp"

//结构体类
#include "./PLC_STRUCT/PLC_Struct_FieldInfo.h"
#include "./PLC_STRUCT/PLC_Struct_Type.hpp"
#include "./PLC_STRUCT/PLC_Struct_Value.hpp"

//数组类
#include "./PLC_ARRAY/PLC_Array_Value.hpp"
#include "./PLC_ARRAY/PLC_Array_Type.hpp"

//函数类
#include "./PLC_Function/PLC_Function.h"

//class类
#include "./PLC_CLASS/PLC_Class.h"

//接口类
#include "./PLC_INTERFACE/PLC_INTERFACE.h"

//全部变量容器
#include "./varMap/varMap.h"

//类中方法类
#include "PLC_Method/PLC_Method.h"

// #include "./PLC_INNER/PLC_INTEGER/PLC_DINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_DINT_Value.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_LINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_LINT_Value.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_UINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_UINT_Value.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_USINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_USINT_Value.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_UDINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_UDINT_Value.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_ULINT_Type.h"
// #include "./PLC_INNER/PLC_INTEGER/PLC_ULINT_Value.h"
// #include "./PLC_INNER/PLC_ALLREAL/PLC_AllReal_Type.h"
// #include "./PLC_INNER/PLC_ALLREAL/PLC_AllReal_Value.h"
// #include "./PLC_INNER/PLC_ALLREAL/PLC_LREAL_Type.h"
// #include "./PLC_INNER/PLC_ALLREAL/PLC_LREAL_Value.h"







namespace PLC
{
    typedef PLC_SINT_Value SINT;
    typedef PLC_INT_Value INT;
    // typedef PLC_DINT_Value DINT;
    // typedef PLC_LINT_Value LINT;
    // typedef PLC_UINT_Value UINT;
    // typedef PLC_USINT_Value USINT;
    // typedef PLC_UDINT_Value UDINT;
    // typedef PLC_ULINT_Value ULINT;
    // typedef PLC_LREAL_Value LREAL;

    typedef PLC_Real_Value REAL;
    typedef PLC_String_Value STRING;
    typedef PLC_Class CLASS;
    typedef PLC_Method METHOD;
    typedef PLC_INTERFACE INTERFACE;
//    typedef PLC_INTERFACE::PLC_METHODPROTO METHODPROTO;
//    template<int TYPEID>
//    using ARRAY = PLC_Array_Value<TYPEID>;
//    template<int TYPEID>
//    using ARRAY_TYPE = PLC_Array_Type<TYPEID>;
    typedef varMap varMap;

    varMap* RFM = new varMap();

};


#endif