#include "./PLC_Exception.h"

PLC_Exception::PLC_Exception(std::string str_errorText) : 
    _str_exceptionInfo(str_errorText)
    {}

std::string PLC_Exception::what()
{
    return this->_str_exceptionInfo;
}

void PLC_Exception::show()
{
    std::cout << this->_str_exceptionInfo << std::endl;
}