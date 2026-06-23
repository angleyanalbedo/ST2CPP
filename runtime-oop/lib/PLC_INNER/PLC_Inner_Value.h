#ifndef PLC_INNER_VALUE_H_
#define PLC_INNER_VALUE_H_

/***************************************************************
 * @file       PLC_Inner_Value.h
 * @brief      PLC_Inner_Value类是所有内置类型值类的基类
 * @author     chh
 * @version    0.1
 * @date       2022.4.16
 * @modify      无
 * @reason      无
 **************************************************************/

#include"../PLC_Value.h"

class PLC_Inner_Value : public PLC_Value
{  
    public:
        PLC_Inner_Value(int i_typeId, std::string str_name);
        
        virtual ~PLC_Inner_Value(){}
};

#endif