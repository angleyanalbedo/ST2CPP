#include <iostream>
#include "PLC.h"
using namespace PLC;
using namespace std;
class ADD_TEN: public PLC_Function<INT>{
public:
	ADD_TEN(int funcID, varMap* vMap) : PLC_Function(funcID, vMap){
		this->addReturnVar<INT>();
		this->returnValue = new INT();
		::PLC::RFM->addVarToVarMap(237,createFuncVariable<INT>(237,(new INT(0))));
	}
	void funcExecute(INT*X){
		auto* X_t = new INT(::PLC::RFM->getSymbolByID<INT*>(237));
		*X_t = *X;

		*this->returnValue  = (*X) +((*(new INT(10)))) ;
	}
	INT* callFunc(INT*X){
		funcExecute(X);
		return new INT(getFuncReturn<INT*>);
	}
};
void initFunc(){
	::PLC::RFM->addVarToVarMap(219, new ADD_TEN(219, RFM));
}

int main(){
	initFunc();
	auto* A=new INT((*(new INT(0))));
	auto* B=new INT((*(new INT(0))));
		for( *A = (*(new INT(1)));*A <= (*(new INT(5)));*A = *A + (*(new INT(1)))){
		auto _X0=*A;
		*B = *::PLC::RFM->getSymbolByID<ADD_TEN*>(219)->callFunc(&_X0);
		}
}