#ifndef PLC_SINT_TYPE_HPP_
#define PLC_SINT_TYPE_HPP_

/***************************************************************
 * @file       PLC_SINT_Type.h
 * @brief      PLC_SINT_Type是short int的具体类型类
 * @author     chh
 * @version    0.2
 * @date       2022.4.25
 **************************************************************/

#include"./PLC_Integer_Type.hpp"

typedef PLC_Integer_Type<short int, SINTTYPEID> PLC_SINT_Type;


#endif