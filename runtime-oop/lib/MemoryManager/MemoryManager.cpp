//
// Created by 86176 on 2022/7/18.
//

#include "MemoryManager.h"

MemoryManager MemoryManager::memoryManager;

MemoryManager::MemoryManager() {
}

MemoryManager &MemoryManager::getManager() {
    return MemoryManager::memoryManager;
}

void MemoryManager::addObject(PLC_Object *obj) {
    getManager()._objects.push_back(obj);
}

void MemoryManager::releaseMemory() {
    std::vector<PLC_Object*>& objVector = getManager()._objects;
    for (const auto &item: objVector){
        delete item;
    }
}

void MemoryManager::showObjs() {
    std::vector<PLC_Object*>& objVector = getManager()._objects;
    for (const auto &item: objVector){
        std::cout << item;
    }
}
