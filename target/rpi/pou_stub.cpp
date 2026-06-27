/**
 * pou_stub.cpp — 最小 POU 注册桩（绕过编译器输出的编译错误）
 *
 * 当 output/flat/build/*.cpp 无法编译时，用此文件替代。
 * 编译时排除 POU 源文件，链接此 stub：
 *   g++ -O2 -std=c++17 ... -DRT_PLATFORM_LINUX \
 *       runtime-flat/src/*.cpp platform_rpi.cpp runtime_rpi.cpp \
 *       hal/gpio_hal.cpp hal/gpio_tci.cpp pou_stub.cpp \
 *       -lpthread -o plc_runtime_rpi
 */
#include "rt_runtime.h"
using namespace rt_plc;

void registerAllPOUs(POURegistry& reg) {
    (void)reg;
}
