#include <iostream>
#include "PLC.h"
using namespace PLC;
using namespace std;
class FUN: public PLC_Function<INT>{
public:
	FUN(int funcID, varMap* vMap) : PLC_Function(funcID, vMap){
		this->addReturnVar<INT>();
		this->returnValue = new INT();
		::PLC::RFM->addVarToVarMap(237,createFuncVariable<INT>(237,(new INT(0))));
	}
	void funcExecute(INT*X){
		auto* X_t = new INT(::PLC::RFM->getSymbolByID<INT*>(237));
		*X_t = *X;
if((*X) ==((*(new INT(2)))) ){
		*this->returnValue  = (*(new INT(1)));
}
else if((*X) ==((*(new INT(1)))) ){
		*this->returnValue  = (*(new INT(1)));
}else{
		auto _X0=(*X) -((*(new INT(1)))) ;
		auto _X1=(*X) -((*(new INT(2)))) ;
		*this->returnValue  = (*::PLC::RFM->getSymbolByID<FUN*>(219)->callFunc(&_X0)) +(*::PLC::RFM->getSymbolByID<FUN*>(219)->callFunc(&_X1)) ;
}
	}
	INT* callFunc(INT*X){
		funcExecute(X);
		return new INT(getFuncReturn<INT*>());
	}
};
void initFunc(){
	::PLC::RFM->addVarToVarMap(219, new FUN(219, RFM));
}

int main(){
	initFunc();
	auto* A=new INT((*(new INT(10))));
	auto* B=new INT((*(new INT(0))));
	for( *B=(*(new INT(10)));*B<=(*(new INT(100)));*B = *B+(*(new INT(2)))){
		*A = (*A) +((*(new INT(20)))) ;
	}
		auto _X2=(*(new INT(10)));
		*B = *::PLC::RFM->getSymbolByID<FUN*>(219)->callFunc(&_X2);
}