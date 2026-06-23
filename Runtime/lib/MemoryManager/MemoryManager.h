//
// Created by 86176 on 2022/7/18.
//

#ifndef RUNTIME_TEST_MEMORYMANAGER_H
#define RUNTIME_TEST_MEMORYMANAGER_H


#include <vector>
#include "../PLC_Object.h"

class MemoryManager {
private:
    static MemoryManager memoryManager;
    MemoryManager();
    std::vector <PLC_Object*> _objects;
public:
    static MemoryManager& getManager();
    static void addObject(PLC_Object* obj);
    static void releaseMemory();
    static void showObjs();
};


#endif //RUNTIME_TEST_MEMORYMANAGER_H
