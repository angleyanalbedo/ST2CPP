//
// Created by 69496 on 2022/8/27.
//
/**
 * @brief PLC??????????????�Z?? ?????????
 */
#ifndef RUNTIME_TEST_PLC_INTERFACE_TRANSLATE_H
#define RUNTIME_TEST_PLC_INTERFACE_TRANSLATE_H


#include "PLC_INTERFACE.h"
class PLC_MethodPrototype_Translate : public PLC_MethodPrototype{
public:
    PLC_MethodPrototype_Translate(int varID): PLC_MethodPrototype(varID){
        //this->addVarToMethodIOVarMap<INT>(1); //??????????????INT????
    }

};


class PLC_INTERFACE_Translate : public PLC_INTERFACE {
public:
    PLC_INTERFACE_Translate(int varID): PLC_INTERFACE(varID) {
        //this->addMethodPrototypeToMap<PLC_MethodPrototype_Translate>(3);
    }
};


#endif //RUNTIME_TEST_PLC_INTERFACE_TRANSLATE_H
