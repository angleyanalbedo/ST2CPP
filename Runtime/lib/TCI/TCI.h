//
// Created by 86176 on 2022/7/19.
//

#ifndef RUNTIME_TEST_TCI_H
#define RUNTIME_TEST_TCI_H

#include "PLC_Value.h"

class TCI{
public:
    /**
     * 从输入位置读取
     * @param typeId 输出数据的类型id
     * @param size 读取数据的大小
     * @param location 数据位置
     * */
    virtual PLC_Value* readInputLocation(int typeId, int size, int location) = 0;
    //分级寻址
    virtual PLC_Value* readInputLocation(int typeId, int size, int location[]) = 0;

    /**
     * 向输出位置写入
     * @param value 要输入的变量
     * */
    virtual void writeOutputLocation(PLC_Value& value, int size, int location) = 0;
    virtual void writeOutputLocation(PLC_Value& value, int size, int location[]) = 0;

    /**
     * 同步存储器
     * @param value 与存储器同步的变量的指针
     * */
    virtual void SyncMemoryLocation(PLC_Value* value, int size, int location) = 0;
    virtual void SyncMemoryLocation(PLC_Value* value, int size, int location[]) = 0;
};

#endif
