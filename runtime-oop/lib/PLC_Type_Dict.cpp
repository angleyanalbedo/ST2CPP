#include"./PLC_Type_Dict.h"
#include "./PLC_INNER/PLC_Inner_Type.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_Integer_Type.hpp"
#include "./PLC_INNER/PLC_INTEGER/PLC_SINT_Type.h"
#include "./PLC_INNER/PLC_INTEGER/PLC_INT_Type.h"

#include "./PLC_INNER/PLC_STRING/PLC_String_Type.h"
#include "PLC_INNER/PLC_INTEGER/PLC_Real_Type.h"
// #include "./PLC_INNER/PLC_STRING/PLC_WString_Type.h"

PLC_Type_Dict PLC_Type_Dict::PLCTypeDict;

void PLC_Type_Dict::registerType(int i_typeId, PLC_Type* type)
{
    std::map<int, PLC_Type*>* tempMap = PLC_Type_Dict::getDict()->_typeDict;
    auto t = tempMap->find(i_typeId);
    try
    {
        if(t == tempMap->end())
        {
            tempMap->insert(std::pair<int, PLC_Type*>(i_typeId, type));
        }
        else
        {
            throw PLC_Exception("registing a existed type identify\n");
        }
    }
    catch(PLC_Exception& exception)
    {
        exception.show();
    }
    
}
PLC_Type* PLC_Type_Dict::findType(int i_typeId)
{
    std::map<int, PLC_Type*>* tempMap = PLC_Type_Dict::getDict()->_typeDict;
    auto t = tempMap->find(i_typeId);
    try
    {
        if(t == tempMap->end())
        {
            throw PLC_Exception("finding an inexistent type\n");
        }
        else
        {
            return t->second;
        }
    }
    catch(PLC_Exception& exception)
    {
        exception.show();
        exit(0);
    }
}

PLC_Type_Dict::PLC_Type_Dict()
{
    //初始化字典
    this->_typeDict = new std::map<int, PLC_Type*>;

    /*TODO:需要补齐*/
    PLC_SINT_Type* p_sIntType = new PLC_SINT_Type(0, "SINT", 2);
    PLC_Type_Dict::registerType(SINTTYPEID, p_sIntType);

    PLC_INT_Type* p_intType = new PLC_INT_Type(0, "INT", 4);
    PLC_Type_Dict::registerType(INTTYPEID, p_intType);

    PLC_String_Type* p_stringType = new PLC_String_Type("", "STRING", SSTRINGTYPEID);
    PLC_Type_Dict::registerType(SSTRINGTYPEID, p_stringType);

    // PLC_WString_Type* p_wstringType = new PLC_WString_Type(L"", "WSTRING", WSTRINGTYPEID);
    // PLC_Type_Dict::registerType(WSTRINGTYPEID, p_wstringType);

    PLC_Real_Type* p_realType = new PLC_Real_Type(0, "REAL", 4);
    PLC_Type_Dict::registerType(REALTYPEID, p_realType);
}

PLC_Type_Dict* PLC_Type_Dict::getDict()
{
    return &PLCTypeDict;
}

void PLC_Type_Dict::showDict() {
    auto* dict = PLC_Type_Dict::getDict();
    auto* dictMap = dict->getTypeDict();
    auto it = dictMap->begin();
    while(it != dictMap->end()){
        std::cout << it->first << std::endl;
        it++;
    }
}

std::map<int, PLC_Type*>* PLC_Type_Dict::getTypeDict(){
    return _typeDict;
}
