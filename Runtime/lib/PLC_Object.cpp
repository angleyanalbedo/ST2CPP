//
// Created by 86176 on 2022/7/18.
//

#include "./PLC_Object.h"
#include "./MemoryManager/MemoryManager.h"

PLC_Object::PLC_Object() {
    MemoryManager::getManager().addObject(this);
}
