#ifndef PLC_OBJECT_H_
#define PLC_OBJECT_H_

/***************************************************************
 * @file       PLC_Object.h
 * @brief      PLC_Object类是所有类的基类，所有的类型类或值类都直接或间接继承自这个类
 * @author     chh
 * @version    0.1
 * @date       2022.4.16
 * @modify      无
 * @reason      无
 **************************************************************/

#include <vector>
#include "./PLC_Exception/PLC_Exception.h"

#define SINTTYPEID 0
#define INTTYPEID 1
#define DINTTYPEID 2
#define LINTTYPEID 3

#define USINTTYPEID 4
#define UINTTYPEID 5
#define UDINTTYPEID 6
#define ULINTTYPEID 7

#define REALTYPEID 8
#define LREALTYPEID 9

#define SINTSIZE 2
#define INTSIZE 3
#define DINTSIZE 4
#define LINTSIZE 5

#define USINTSIZE 2
#define UINTSIZE 3
#define UDINTSIZE 4
#define ULINTSIZE 5

#define REALSIZE 4
#define LREALSIZE 5

#define ERROR 99999

#define SSTRINGTYPEID 10
#define SSTRINGMAXLENGTH 1024
#define WSTRINGTYPEID 11
#define WSTRINGMAXLENGTH 1024


class PLC_Object
{
    public:
        PLC_Object();
        virtual ~PLC_Object()= default;
};

#endif