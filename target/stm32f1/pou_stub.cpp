// pou_stub.cpp — POU 注册表占位，当 Java 编译器尚未生成 POU 代码时使用
#include "rt_runtime.h"
#include "rt_plc.h"

void __attribute__((weak)) registerAllPOUs(rt_plc::POURegistry& reg) {
    (void)reg;
}