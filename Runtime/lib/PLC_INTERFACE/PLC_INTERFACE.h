//
// Created by HP on 2022/7/20.
//

#include "map"

#ifndef RUNTIME_TEST_PLC_INTERFACE_H
#define RUNTIME_TEST_PLC_INTERFACE_H

/**
 * @brief ???????????
 */
class PLC_INTERFACE :public PLC_Object{
public:
    /**
     * @brief ???ID
     */
    int varID;

    /**
     * @brief ????????
     */
    PLC_INTERFACE() = default;

    /**
     * @brief ??????????????
     */
    std::map<int, PLC_Object*>* methodPrototypeMap;

    /**
     * @brief ????ID????????????????????????map??
     * @tparam PLC_MethodProtoType
     * @param varID
     * @return ????ID??????????????
     */
    template<typename PLC_MethodProtoType>
    auto addMethodPrototypeToMap(int varID){
        auto methodProtoType = new PLC_MethodProtoType(varID);

        this->methodPrototypeMap->template emplace(varID, methodProtoType);
        return methodProtoType;
    }

    /**
     * @brief ????ID?????????????????
     * @tparam PLC_MethodProtoType
     * @param varID
     * @return ????ID??????????????
     */
    template<typename PLC_MethodProtoType>
    auto getMethodPrototype(int varID){
        return dynamic_cast<PLC_MethodProtoType>(this->methodPrototypeMap->find(varID)->second);
    }

    /**
     * @brief PLC??????
     * @param varID
     */
    PLC_INTERFACE(int varID) : PLC_Object(), methodPrototypeMap (new std::map<int, PLC_Object*>),
    varID(varID){

    }

};

/**
 * @brief ??????????????
 */
class PLC_MethodPrototype : public PLC_Object{
public:
    /**
     * @brief ???????ID
     */
    int varID;

    /**
     * @brief ????????????????map
     */

    std::map<int, PLC_Value*>* methodPrototypeIOVarMap;

    /**
     * @brief ???????????????????
     * @tparam PLC_ValueType
     * @param varID
     * @return
     */
    template<typename PLC_ValueType>
    auto addVarToMethodIOVarMap(int varID){
        auto var = new PLC_ValueType(varID);

        this->methodPrototypeIOVarMap->template emplace(varID, var);
        return var;
    }

    /**
     * @brief???????????��????
     * @tparam varType
     * @param varID
     * @return
     */
    template<typename varType>
    auto getMethodPrototypeVar(int varID){
        return dynamic_cast<varType>(this->methodPrototypeIOVarMap->find(varID)->second);
    }

    /**
     * @brief ????????????
     * @param varID
     */
    PLC_MethodPrototype(int varID) : PLC_Object(), varID(varID),
    methodPrototypeIOVarMap(new std::map<int, PLC_Value*>){
    }
};

#endif //RUNTIME_TEST_PLC_INTERFACE_H
