//
// Created by HP on 2022/6/10.
//

#ifndef RUNTIME_TEST_PLC_FUNCTION_H
#define RUNTIME_TEST_PLC_FUNCTION_H
#include<vector>
#include<map>
#include"../PLC_Type.h"
#include"../PLC_Value.h"
#include "varMap/varMap.h"
#include "iostream"
/***************************************************************
 * @file       PLC_Function.h
 * @brief      PLC_Function是所有翻译的PLC函数的基类，提供函数参数初始化和函数调用接口以及变量容器
 * @author     wzz
 * @version    0.2
 * @date       2022.6.10
 * @modify
 **************************************************************/
template <typename PLC_Value_Typ>
class PLC_Function : public PLC_Object{
public:
    /**
     * @brief 函数返回值
     */
    PLC_Value_Typ* returnValue{};

    //函数名
    std::string funcName{};

    //函数ID
    int funcID{};

    //指向总表指针
    varMap* varMapP{};

    /***************************************************************
     *  @brief     	函数析构函数，将成员变量和函数对象一并释放
     **************************************************************/
    ~PLC_Function() override = default;

    PLC_Function() = default;

    /**
     * @brief PLC_func非默认函数
     * @param funcID
     * @param vMap
     */
    PLC_Function(int funcID, varMap* vMap)
    :funcID(funcID), varMapP(vMap), functionVariableMap(new std::map<int, PLC_Value*>()){

    }
    /**
     * @brief       函数体执行接口
     */
    virtual void funcExecute(){}

    /**
     *  @brief     	函数调用、初始化接口
     */
    void callFunc(){}

    /**
     *  @brief     	函数调用后对函数对象内的变量对象值进行重置，恢复到初始未修改状态
     */
    virtual void retValue(){}

    /**
     *  @brief     	函数内变量指针容器
     */
    std::map<int, PLC_Value*>* functionVariableMap;

    void copyVarMap(std::map<int, PLC_Value*>* cyVarMap){
//        for (const auto &item: *this->functionVariableMap){
//            cyVarMap->emplace(item.first, item.second);
//        }
        std::copy(this->functionVariableMap->begin(), this->functionVariableMap->end(),std::inserter(*cyVarMap,cyVarMap->begin()));
    }

    /**
     * @brief  创建函数内变量接口
     * @tparam PLC_valueType 创建的值类型
     * @param  varID 变量ID
     * @return
     */
    template<typename PLC_valueType>
    PLC_Value* createFuncVariable(int varID, PLC_valueType* initValue = new PLC_valueType()){
        auto* t = new PLC_valueType(initValue);
        this->functionVariableMap->emplace(varID, t);
        this->varMapP->addVarToVarMap(varID, t);
        return t;
    }

    /**
     * @brief 根据ID获取函数内变量
     * @tparam PLC_ValueType
     * @param varID
     * @return
     */
    template<typename PLC_ValueType>
    auto* getFuncVariable(int varID){
        auto iter = this->functionVariableMap->find(varID);
            return dynamic_cast<PLC_ValueType>(iter->second);

    }

    /**
     * @brief 从拷贝变量对象map中获取变量
     */
    template<typename PLC_ValueType>
    auto* getFuncVariableFromCyVarMap(std::map<int, PLC_Value*>* cyVarMap, int varID){
        auto iter = cyVarMap->find(varID);
        return dynamic_cast<PLC_ValueType>(iter->second);
    }

    /**
     * @brief 将返回值变量添加到函数容器内
     * @tparam PLC_ValueType
     * @return 返回函数返回变量
     */
    template<typename PLC_ValueType>
    auto* addReturnVar(){
        auto reVar = new PLC_ValueType();
        this->functionVariableMap->emplace(-1,new PLC_ValueType());
        return reVar;
    }

//    /**
//     * @brief 获取函数返回值变量
//     * @tparam PLC_ValueType
//     * @return 返回函数返回变量
//     */
//    template<typename PLC_ValueType>
//    auto* getFuncReturnVar(){
//        return this->functionVariableMap->find(-1)->second;
//    }

    template<typename PLC_Value_Type>
    auto getFuncReturn(){
        return dynamic_cast<PLC_Value_Type*>(this->returnValue);
    }


};

#endif //RUNTIME_TEST_PLC_FUNCTION_H
