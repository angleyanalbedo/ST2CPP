cmake_minimum_required(VERSION 3.10)
project(RT_PLC LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ═══ 编译优化选项 ═══
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG -march=native")

# ═══ 诊断开关 ═══
option(ENABLE_DIAG "Enable runtime diagnostics (scan time, watchdog)" ON)
if(ENABLE_DIAG)
    add_compile_definitions(ENABLE_DIAG)
endif()

# ═══ 目录结构 ═══
set(RT_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(RT_SRC_DIR     ${CMAKE_CURRENT_SOURCE_DIR}/src)

# 公共头文件包含路径
include_directories(${RT_INCLUDE_DIR})

# ═══ 源文件列表 ═══
set(RT_CORE_SOURCES
    ${RT_SRC_DIR}/gvl.cpp
    ${RT_SRC_DIR}/program.cpp
    ${RT_SRC_DIR}/task.cpp
    ${RT_SRC_DIR}/event.cpp
    ${RT_SRC_DIR}/watchdog.cpp
    ${RT_SRC_DIR}/diag.cpp
)

set(RT_SCHEDULER_SOURCES
    ${RT_SRC_DIR}/scheduler.cpp
)

# ═══ 静态库：核心运行时 ═══
add_library(rt_core STATIC ${RT_CORE_SOURCES})
target_include_directories(rt_core PUBLIC ${RT_INCLUDE_DIR})

# ═══ 静态库：调度器 ═══
add_library(rt_scheduler STATIC ${RT_SCHEDULER_SOURCES})
target_include_directories(rt_scheduler PUBLIC ${RT_INCLUDE_DIR})
target_link_libraries(rt_scheduler PUBLIC rt_core)

# ═══ 可执行文件：测试用例 ═══

# Fibonacci 基础类型测试
add_executable(fibonacci tests/fibonacci.cpp)
target_link_libraries(fibonacci rt_core)

# 多任务调度演示
add_executable(multitask_demo tests/multitask_demo.cpp)
target_link_libraries(multitask_demo rt_scheduler)

# 框架完整性测试
add_executable(framework_test tests/framework_test.cpp)
target_link_libraries(framework_test rt_scheduler)

# 运行时主程序（编译器生成代码 + 调度器）
add_executable(runtime_main runtime_main.cpp)
target_link_libraries(runtime_main rt_scheduler)
