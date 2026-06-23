//
// Created by HP on 2022/6/10.
//

#ifndef RUNTIME_TEST_PLC_CLASS_H
#define RUNTIME_TEST_PLC_CLASS_H
#include <utility>
#include<vector>
#include<map>
#include"../PLC_Type.h"
#include"../PLC_Value.h"
#include "varMap/varMap.h"
#include "PLC_INTERFACE/PLC_INTERFACE.h"


/***************************************************************
 * @file       PLC_Class
 * @brief      PLC_Class为所以翻译出的PLC类的基类，提供函数调用初始化，变量获取等方法支持
 * @author     wzz
 * @version    0.1
 * @date       2022.6.10
 * @modify
 **************************************************************/
class PLC_Method;
class PLC_Class : public PLC_Object{
public:
    /**
     * @brief 默认构造函数
     */
    PLC_Class() = default;

    /**
     * @brief 类名
     */
    std::string className{};

    std::map<int, PLC_INTERFACE*>* impleInterfaceMap{};

    /**
     *  @brief 类实例的ID
     */
    int classInstanceID{};

    /**
     * @brief 类的变量拷贝容器，用于实例生成时将该表加入到总表下
     */
    std::map<int, PLC_Object*>* classFieldMap{};

    /**
     * @brief 将类下所有变量添加到runtime总对象表下
     * @param vMap runtime中所有对象容器
     */
    void addFieldMapToInstanceMap(varMap* vMap) const;

    /**
     * @brief 初始化类的ID，并将该类对应的实例的变量map拷贝进总对象表中
     * @param instanceID
     * @param vMap
     */
    PLC_Class(int instanceID, varMap* vMap)
    :classInstanceID(instanceID),classFieldMap(new std::map<int, PLC_Object*>()),
    classVariableMap(new std::map<int, PLC_Value*>()),classMethodMap(new std::map<int, PLC_Method*>())
    , impleInterfaceMap(new std::map<int, PLC_INTERFACE*>){
        this->addFieldMapToInstanceMap(vMap);
    }

    ~PLC_Class() override = default;

    /**
     *  @brief     	类成员变量容器
     */
    std::map<int, PLC_Value*>* classVariableMap;

    /**
     *  @brief     	类中方法对象指针容器
     */
    std::map<int, PLC_Method*>* classMethodMap;
    //向容器添加类方法对象指针

    /**
     *  @brief     	获取类中某个函数指针
     *  @param classMethodName
     */
    template<class classMethod>
    auto getClassMethod(const int methodID) {
        return dynamic_cast<classMethod*>(this->classMethodMap->find(methodID)->second);
    }

    /**
     *  @brief     	获取类内函数内的变量指针
     *  @param classMethodVariable 该方法类
     */
    template<class classMethodVariable>
    auto getClassMethodVariable(int classMethodID, int classMethodVariableID);

    /**
     * @brief 创建类内的方法对象
     * @tparam methodType
     * @tparam classType
     * @param methodName
     * @param methodID
     * @param classP
     * @return 返回类实例的指针
     */

    template<typename methodType>
    auto createMethod(const std::string& methodName, int methodID, PLC_Class* classP){
        auto* newMethod = new methodType(methodID, methodName, classP);

        this->classFieldMap->emplace(methodID, newMethod);
        return newMethod;
    }

    /**
     * @brief 将类内普通变量添加到当前实例map下
     * @tparam PLC_Value
     * @param variableName
     * @param varID
     * @param initValue
     * @return 返回值实例的指针
     */
    template<typename PLC_ValueType>
    auto createClassVariable(const std::string& variableName, int varID, PLC_ValueType initValue){
        auto* newVariable = new PLC_ValueType(initValue);

        this->classFieldMap->emplace(varID, newVariable);
        return newVariable;
    }

    /**
     * @brief 创建类实现的接口对象，并将其添加到相应的map中
     * @tparam PLC_Interface
     * @return 返回
     */
    template<typename PLC_Interface>
    auto addImpleInterface(int varID){
        auto* interfaceField = new PLC_Interface(varID);

        this->impleInterfaceMap->template emplace(varID, interfaceField);
        return interfaceField;
    }

};

class PLC_Method :public PLC_Object{
public:
    PLC_Method() = default;

    /**
     * @brief 类内方法构造函数
     * @param methodID 类实例的ID
     * @param methodName 方法名
     * @param classP 方法绑定的类
     */
    PLC_Method(int methodID, std::string methodName, PLC_Class* classP)
    :methodID(methodID),classMethodName(std::move(methodName)),classField(classP){}

    /**
     *  @brief     	类内函数名字
     */
    std::string classMethodName{};

    /**
     * @brief       类内方法ID
     */
    int methodID {};

    /**
     *  @brief     	指向函数所属类
     */
    PLC_Class* classField{};

    /**
     *  @brief     	类内函数调用接口
     */
    virtual void callFunc(){}

    /**
     *  @brief     	类内变量重置接口
     */
    virtual void resetValue(){}

    /**
     * @brief 函数执行接口
     */
    virtual void funcExecute(){}

    /**
     *  @brief     	类内函数内的值变量容器
     */
    std::map<int, PLC_Value*> methodVariableMap;

    /**
     *  @brief     	类内函数变量获取接口
     *  @param varID 寻找的变量ID
     */
    auto* getMethodVariable(int varID)
    {
        return this->methodVariableMap.find(varID)->second;
    }

    /**
     * @brief 将当前方法对象添加到类实例的map中
     * @tparam PLC_Value
     * @param varID
     * @param initValue
     * @return 返回值实例的指针
     */
    template<typename PLC_Value>
    auto createMethodVariable(int varID, PLC_Value* initValue){
        auto* newVariable = new PLC_Value(initValue);
        this->methodVariableMap.emplace(varID, newVariable);
        //将其插入总表中
        this->classField->classFieldMap->emplace(varID, newVariable);
        return newVariable;
    }
};

void PLC_Class::addFieldMapToInstanceMap(varMap *vMap) const {
    vMap->addClassFieldMapToInstanceMap(this->classInstanceID, this->classFieldMap);
}

template<class classMethodVariable>
auto PLC_Class::getClassMethodVariable(int classMethodID, int classMethodVariableID) {
    return dynamic_cast<classMethodVariable>(this->classMethodMap->find(classMethodID)->second->getMethodVariable(classMethodVariableID));
}


#endif //RUNTIME_TEST_PLC_CLASS_H
