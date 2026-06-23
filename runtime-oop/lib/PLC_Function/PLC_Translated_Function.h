//
// Created by HP on 2022/6/10.
//

#ifndef RUNTIME_TEST_PLC_TRANSLATED_FUNCTION_H
#define RUNTIME_TEST_PLC_TRANSLATED_FUNCTION_H
#include<vector>
#include<map>
#include"../PLC_Type.h"
#include"../PLC_Value.h"
#include "PLC_Function.h"

/***************************************************************
 * @file       PLC_Translated_Function
 * @brief      PLC_Translated_Function为函数翻译模板实例，不可调用，只做规范翻译示例
 * @author     wzz
 * @version    0.1
 * @date       2022.6.10
 * @modify
 **************************************************************/

class PLC_Translated_Function{

};
////将PLC函数翻译成PLC_Function的一个子类
////将函数接口重定义设置为静态，在函数类对象实例化后即可在任意位置进行调用

//// 例子
//FUNCTION A :INT
//VAR_INPUT
//  B:INT;
//  C:INT:=1;
//END_VAR
//
//VAR_OUTPUT
//  D:INT;
//END_VAR
//
//VAR_INOUT
//  E:INT:=1;
//END_VAR
//
//A := B + C + D + E;
//E := B;
//D := A + B;
//END_FUNCTION
//
//PROGRAM
//VAR
//  A1,B,C:INT:= 1;
//  D:INT;
//END_VAR
//
//A1 := 3*A(B:=B, C:=C, D=>D);//获取多个返回值时必须显式获取
//A1 := 3*A(B,C);//隐式调用，不获取其他返回值
//A(B,C);
//D = A1 * A(B,C);
//
//END_PROGRAM

////普通类内成员变量翻译模式（当前版本使用）
/*
 * class funcA : public PLC_Function{
 * public:
 *      INT* AReTemp;
 *      INT* AReFinal;
 *      INT* B;
 *      INT* C;
 *      INT* D;
 *      INT* E;
 *
 *      funcA(){
 *
 *      }
 *
 *      static auto callFunc(func* A, INT* inputB, INT* inputC = new INT(1), INT* inoutE = new INT(1), INT* outputD){
 *          //函数参数初始化
 *          *A->B = *inputB;
 *          *A->C = *inputC;
 *          *A->E = *inoutE;
 *
 *          //函数体内操作翻译
 *          *A->AReTemp = *A->B + *A->C + *A->D + *A->E;
 *          *A->E = *A->B;
 *          *A->D = *A->AReTemp + *A->B;
 *
 *          //返回结果，将函数对象内变量进行重置
 *          //返回输出值
 *          *inoutE = *A->E;
 *          *A->AReFinal = *A->AReTemp;
 *          //重置
 *          resetValue(funcA* func);
 *          //返回返回值
 *          return *A->AReFinal;
 *      }
 *
 *      static void resetValue(funcA* func){
 *          func->AReTemp->setValue();
 *          func->B->setValue(); //TODO:待优化
 *          func->C->setValue(1);
 *          func->D->setValue();
 *          func->E->setValue(1);
 *      }
 * };
 *
 * funcA* A = new funcA();
 * int main(){
 *      INT* A1 = new INT(1);
 *      INT* B = new INT(1);
 *      INT* C = new INT(1);
 *      INT* D = new INT();
 *
 *      *A1 = INT(3) * *funcA::callFunc(A, B, C, new INT(1), D);
 *      *A1 = INT(3) * *funcA::callFunc(A, B, C);
 *      funcA::callFunc(B, C);
 *      *D = *A1 * *funcA::callFunc(B, C);
 *
 * }
 * */

////静态变量版函数翻译模式（备用）
////略 在翻译映射文档处可见


////在main函数进行初始化




#endif //RUNTIME_TEST_PLC_TRANSLATED_FUNCTION_H
