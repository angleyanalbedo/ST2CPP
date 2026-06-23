#ifndef PLC_STRING_TYPE_H_
#define PLC_STRING_TYPE_H_
/***************************************************************
 * @file       PLC_String_Type.h
 * @brief      PLC_String_Type类是单字节字符串类型类
 * @author     chh
 * @version    0.1
 * @date       2022.4.27
 * @modify      无
 * @reason      无
 **************************************************************/

#include<string>
#include "./PLC_BasicString_Type.hpp"

typedef PLC_BasicString_Type<std::string> PLC_String_Type;

#endif