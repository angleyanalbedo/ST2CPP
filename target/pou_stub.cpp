/**
 * pou_stub.cpp — 最小 POU 注册桩（绕过编译器输出的编译错误）
 *
 * 当编译器输出的 .cpp 无法编译时，用此文件替代。
 * 编译时排除 POU 源文件，链接此 stub。
 */
#include "rt_runtime.h"
using namespace rt_plc;

void registerAllPOUs(POURegistry& reg) {
    (void)reg;
}
