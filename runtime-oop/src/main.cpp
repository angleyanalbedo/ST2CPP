#include <iostream>
#include <utility>
#include "PLC.h"
using namespace PLC;
using namespace std;

class M : public METHOD{
public:
    M( int methodID, std::string methodName, CLASS* classP)
    :METHOD(methodID, std::move(methodName), classP){
        createMethodVariable(12,new INT());
        createMethodVariable(13, new INT());
        createMethodVariable(14,new INT());
    }

    auto funcExecute(int varID = 0){
        ::PLC::RFM->getInstanceVarByID<INT*>(this->classField->classInstanceID, 12)->setValue(2);
        cout<<"M Method Execute"<<endl;
    }

    void resetValue() {
        ::PLC::RFM->getInstanceVarByID<INT*>(this->classField->classInstanceID, 12)->setValue(0);
    }

    auto callFunc(int varID = 0){
        funcExecute(varID);
        resetValue();
    }

};

class M1 : public METHOD{
public:
    M1(int methodID, std::string methodName, CLASS* classP)
            :METHOD(methodID, std::move(methodName), classP){
        createMethodVariable(12,new INT());
        createMethodVariable(13, new INT());
        createMethodVariable(14,new INT());
    }

    auto funcExecute(int varID = 0){
        ::PLC::RFM->getInstanceVarByID<INT*>(this->classField->classInstanceID, 12)->setValue(2);
        cout<<"M1 Method Execute"<<endl;
    }

    void resetValue() {
        ::PLC::RFM->getInstanceVarByID<INT*>(this->classField->classInstanceID, 12)->setValue(0);
    }

    auto callFunc(int varID = 0){
        funcExecute(varID);
        resetValue();
    }

};

class A :public CLASS{
public:
    A(int instanceId, PLC::varMap *vMap) : CLASS(instanceId, vMap) {
        createClassVariable<INT>("I", 3, new INT());
        createClassVariable<INT>("E", 4, new INT());
        createMethod<M>("M", 5, this);
        createMethod<M1>("M1", 6, this);
    }
};

int main(){
    auto A1 = new A(4, ::PLC::RFM);
    auto A2 = new A(5, ::PLC::RFM);

    ::PLC::RFM->getInstanceVarByID<INT*>(4, 3 )->setValue(2);
    ::PLC::RFM->getInstanceVarByID<INT*>(5, 3)->setValue(3);

    cout<<::PLC::RFM->getInstanceVarByID<INT*>(4,3)->getValue()<<endl;
    cout<<::PLC::RFM->getInstanceVarByID<INT*>(5,3)->getValue()<<endl;

    ::PLC::RFM->getInstanceVarByID<M*>(4, 5)->callFunc();

    ::PLC::RFM->getInstanceVarByID<M1*>(5, 6)->callFunc();
}
