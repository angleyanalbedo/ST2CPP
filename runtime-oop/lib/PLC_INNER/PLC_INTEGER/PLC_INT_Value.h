#ifndef PLC_INT_VALUE_H_
#define PLC_INT_VALUE_H_

/***************************************************************
 * @file       PLC_INT_Value.h
 * @brief      PLC_INT_Value是int的值类
 * @author    chh
 * @version    0.2
 * @date       2022.4.25 
 **************************************************************/

#include"./PLC_Integer_Value.hpp"

typedef PLC_Integer_Value<int, INTTYPEID> PLC_INT_Value;

#endif
