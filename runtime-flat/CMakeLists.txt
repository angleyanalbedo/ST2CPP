cmake_minimum_required(VERSION 3.10)
project(rt_plc LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ═══ 编译选项 ═══
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG -march=native")

# GVL / ProcessImage 大小可配（STM32 小 RAM 平台）
set(GVL_SIZE_OVERRIDE "" CACHE STRING "Override GVL size in bytes")
set(GVL_RETAIN_SIZE_OVERRIDE "" CACHE STRING "Override GVL RETAIN size")
set(PROCESS_IMAGE_SIZE_OVERRIDE "" CACHE STRING "Override ProcessImage size")

if(GVL_SIZE_OVERRIDE)
    add_compile_definitions(GVL_SIZE_OVERRIDE=${GVL_SIZE_OVERRIDE})
endif()
if(GVL_RETAIN_SIZE_OVERRIDE)
    add_compile_definitions(GVL_RETAIN_SIZE_OVERRIDE=${GVL_RETAIN_SIZE_OVERRIDE})
endif()
if(PROCESS_IMAGE_SIZE_OVERRIDE)
    add_compile_definitions(PROCESS_IMAGE_SIZE_OVERRIDE=${PROCESS_IMAGE_SIZE_OVERRIDE})
endif()

option(ENABLE_DIAG "Enable runtime diagnostics" ON)
if(ENABLE_DIAG)
    add_compile_definitions(ENABLE_DIAG)
endif()

# ═══ 目录 ═══
set(RT_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(RT_SRC_DIR     ${CMAKE_CURRENT_SOURCE_DIR}/src)
include_directories(${RT_INCLUDE_DIR})

# ═══ 源文件 ═══
set(RT_CORE_SOURCES
    ${RT_SRC_DIR}/gvl.cpp
    ${RT_SRC_DIR}/error_policy.cpp
    ${RT_SRC_DIR}/io_manager.cpp
    ${RT_SRC_DIR}/program.cpp
    ${RT_SRC_DIR}/retain_manager.cpp
    ${RT_SRC_DIR}/runtime_validator.cpp
    ${RT_SRC_DIR}/task.cpp
    ${RT_SRC_DIR}/task_executor.cpp
    ${RT_SRC_DIR}/event.cpp
    ${RT_SRC_DIR}/watchdog.cpp
    ${RT_SRC_DIR}/diag.cpp
    ${RT_SRC_DIR}/diag_manager.cpp
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

message(STATUS "[rt_plc] Static libraries: rt_core, rt_scheduler")

# ═══ 测试（仅开发时启用） ═══
option(RT_BUILD_TESTS "Build test executables" OFF)

if(RT_BUILD_TESTS)
    # 基础类型验证
    add_executable(fibonacci tests/fibonacci.cpp)
    target_link_libraries(fibonacci rt_core)

    # 多任务调度演示
    add_executable(multitask_demo tests/multitask_demo.cpp)
    target_link_libraries(multitask_demo rt_scheduler)

    # 框架完整性测试
    add_executable(framework_test tests/framework_test.cpp)
    target_link_libraries(framework_test rt_scheduler)

    # Runtime 集成测试
    add_executable(runtime_integration_test tests/runtime_integration_test.cpp)
    target_link_libraries(runtime_integration_test rt_scheduler)

    # 单元测试合集（doctest 框架）
    add_executable(unit_test
        tests/test_main.cpp
        tests/gvl_test.cpp
        tests/error_manager_test.cpp
        tests/fb_test.cpp
        tests/type_conversion_test.cpp
        tests/registry_test.cpp
    )
    target_link_libraries(unit_test rt_core)
    target_include_directories(unit_test PRIVATE ${RT_INCLUDE_DIR} tests)

    if(MINGW)
        target_link_options(unit_test PRIVATE -mconsole)
    endif()

    message(STATUS "[rt_plc] Tests: fibonacci, multitask_demo, framework_test, runtime_integration_test, unit_test")
endif()

message(STATUS "══════════════════════════════════════════")
message(STATUS "  runtime-flat/  — 核心静态库 (rt_core + rt_scheduler)")
message(STATUS "  平台运行时见 target/<platform>/")
message(STATUS "  cmake -DRT_BUILD_TESTS=ON  — 启用测试")
message(STATUS "══════════════════════════════════════════")
