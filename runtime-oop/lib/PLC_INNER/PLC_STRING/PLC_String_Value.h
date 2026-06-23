#ifndef PLC_STRING_VALUE_H_
#define PLC_STRING_VALUE_H_
/***************************************************************
 * @file       PLC_String_Value.h
 * @brief      PLC_String_Value类是单字节字符串值类
 * @author     chh
 * @version    0.1
 * @date       2022.4.27
 * @modify      无
 * @reason      无
 **************************************************************/

#include<string>
#include"./PLC_BasicString_Value.hpp"

typedef PLC_BasicString_Value<std::string> PLC_String_Value;
#endif