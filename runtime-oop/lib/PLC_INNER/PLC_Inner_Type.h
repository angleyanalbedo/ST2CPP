#ifndef PLC_INNER_TYPE_H_
#define PLC_INNER_TYPE_H_

/***************************************************************
 * @file       PLC_Inner_Type
 * @brief      PLC_Inner_Type类是所有内置类型类的基类
 * @author     chh
 * @version    0.1
 * @date       2022.4.16
 * @modify      无
 * @reason      无
 **************************************************************/

#include"../PLC_Type.h"

class PLC_Inner_Type : public PLC_Type
{
    public:
        using PLC_Type::PLC_Type;
        virtual ~PLC_Inner_Type(){}
};



#endif