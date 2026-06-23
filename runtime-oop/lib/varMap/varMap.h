//
// Created by HP on 2022/7/23.
//

#ifndef RUNTIME_TEST_VARMAP_H
#define RUNTIME_TEST_VARMAP_H
#include "map"
#include "../PLC_Object.h"

class varMap {
public:
    /**
     * @brief 根据ID存储所有Runtime中的变量对象信息，包括类型、变量、实例
     */
    std::map<int, PLC_Object*>* variableMap;

    /**
     * @brief 根据ID存储所有Runtime中的class类实例的变量容器map
     */
    std::map<int, std::map<int, PLC_Object*>*>* instanceVarMap;

    varMap(){
        variableMap = new std::map<int, PLC_Object*>();
        instanceVarMap= new std::map<int, std::map<int, PLC_Object*>*>();
    }

    /**
     * @brief 向runtime总对象表中添加对象
     * @param symbolID
     * @param object
     */
    void addVarToVarMap(int symbolID, PLC_Object* object) const{
        this->variableMap->emplace(symbolID, object);
    }

    /**
     * @brief 根据取出总对象表下的对象
     * @tparam varType
     * @param symbolID
     * @return 返回ID对应的类型对象
     */
    template<typename varType>
    auto getSymbolByID(int symbolID){
        return dynamic_cast<varType>(this->variableMap->find(symbolID)->second);
    }

    /**
     * @brief 根据实例ID和实例内变量ID来取出实例对应的变量
     * @tparam varType
     * @return 返回根据两个ID对应的对象
     */
    template<typename varType>
    auto getInstanceVarByID(int instanceID, int varID){
        //拿取实例对应的map
        auto tempInstanceVarMap = this->instanceVarMap->find(instanceID)->second;
        //从相应的map中拿取varID对应的变量进行转向然后返回
        return dynamic_cast<varType>(tempInstanceVarMap->find(varID)->second);
    }

    /**
     * @brief 将实例符号对应的内置变量对象表加入到总表下的instanceVarMap
     * @param instanceID 实例对应的ID
     * @param classFieldMap 实例中Runtime下生成的内置变量容器指针
     */
    void addClassFieldMapToInstanceMap(int instanceID, std::map<int, PLC_Object*>* classFieldMap){
        this->instanceVarMap->emplace(instanceID, classFieldMap);
    }
};


#endif //RUNTIME_TEST_VARMAP_H
