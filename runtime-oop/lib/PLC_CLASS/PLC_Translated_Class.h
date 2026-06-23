//
// Created by HP on 2022/6/10.
//

#ifndef RUNTIME_TEST_PLC_TRANSLATED_CLASS_H
#define RUNTIME_TEST_PLC_TRANSLATED_CLASS_H

#include<vector>
#include<map>
#include"../PLC_Type.h"
#include"../PLC_Value.h"

/***************************************************************
 * @file       PLC_Translated_Class
 * @brief      PLC_Translated_Class不可调用，为PLC_Class下派生的翻译好的子类的翻译模板示例
 * @author     wzz
 * @version    0.1
 * @date       2022.6.10
 * @modify
 **************************************************************/

class PLC_Translated_Class {

};

//class B1 : public PLC_Class {
//public:
//    PLC_Value* I, * B;
//
//    class A : public PLC_Class_Method {
//    public:
//        PLC_Value* I, * B;
//        PLC_Value* RE, * A_RE;
//
//        A() {
//            this->classMethodName = "A";
//            this->I = new PLC_Value(std::string("I"));
//            this->B = new PLC_Value(std::string("B"));
//            this->RE = new PLC_Value(std::string("RE"));
//            this->A_RE = new PLC_Value(std::string("A_RE"));
//        }
//          //重写实现接口
//        void callFunc() override{
//            *this->RE = *this->I + *this->B;
//            *this->A_RE = *this->RE;
//        }
//          //重定义实现接口
//        void initInputVariable(PLC_Value* I, PLC_Value* B) {
//            this->I = I;
//            this->B = B;
//        }
//
//    };
//          //翻译好的类构造函数将相关变量和方法添加到容器中
//    B1() {
//        this->I = new PLC_Value(std::string("I"));
//        this->addClassVairalbe(I);
//        this->B = new PLC_Value(std::string("B"));
//        this->addClassVairalbe(B);
//
//        this->addClassMethod(new A());
//    }
//};
#endif //RUNTIME_TEST_PLC_TRANSLATED_CLASS_H
