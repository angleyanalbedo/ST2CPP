cmake_minimum_required(VERSION 3.10)
project(RT_PLC LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Fibonacci 基础类型测试
add_executable(fibonacci fibonacci.cpp)
target_include_directories(fibonacci PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 多任务调度演示
add_executable(multitask_demo multitask_demo.cpp)
target_include_directories(multitask_demo PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 框架完整性测试
add_executable(framework_test framework_test.cpp)
target_include_directories(framework_test PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
