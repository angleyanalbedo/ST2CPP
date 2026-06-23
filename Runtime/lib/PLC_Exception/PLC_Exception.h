#ifndef PLC_EXCEPTION
#define PLC_EXCEPTION 

#include<string>
#include<iostream>

class PLC_Exception : public std::exception
{
    private:
        std::string _str_exceptionInfo;
    public:
        explicit PLC_Exception(std::string str_errorText = "some exception");
        std::string what();
        void show();
};


#endif