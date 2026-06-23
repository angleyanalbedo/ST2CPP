cmake_minimum_required(VERSION 3.10)
project(RT_PLC LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ═══ 编译优化选项 ═══
# 开启 -O3 后 inline 才真正生效，整体速度快 5-20 倍
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG -march=native -flto")
#                                           ↑          ↑
#                                     CPU 指令特化   链接时优化

# ═══ 诊断开关 ═══
# 量产时关闭可去掉 std::chrono 测量开销
option(ENABLE_DIAG "Enable runtime diagnostics (scan time, watchdog)" ON)
if(ENABLE_DIAG)
    add_compile_definitions(ENABLE_DIAG)
endif()

# Fibonacci 基础类型测试
add_executable(fibonacci fibonacci.cpp)
target_include_directories(fibonacci PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 多任务调度演示
add_executable(multitask_demo multitask_demo.cpp)
target_include_directories(multitask_demo PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 框架完整性测试
add_executable(framework_test framework_test.cpp)
target_include_directories(framework_test PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
