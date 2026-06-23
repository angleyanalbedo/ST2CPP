#ifndef PLC_SINT_VALUE_H_
#define PLC_SINT_VALUE_H_

/***************************************************************
 * @file       PLC_SINT_Value.h
 * @brief      PLC_SINT_Value是short int的值类
 * @author     chh
 * @version    0.2
 * @date       2022.4.25
 **************************************************************/

#include"./PLC_Integer_Value.hpp"

typedef PLC_Integer_Value<short int, SINTTYPEID> PLC_SINT_Value;

#endif