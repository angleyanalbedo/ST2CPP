//
// Created by 86198 on 2022/4/18.
//
#ifndef PLC_INT_TYPE_H_
#define PLC_INT_TYPE_H_

/***************************************************************
 * @file       PLC_INT_Type.h
 * @brief      PLC_INT_Type是int的具体类型类
 * @author     bikaixuan
 * @version    0.2
 * @date       2022.4.25
 **************************************************************/

#include"./PLC_Integer_Type.hpp"

typedef PLC_Integer_Type<int, INTTYPEID> PLC_INT_Type;



#endif