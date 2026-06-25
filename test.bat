@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ═══════════════════════════════════════════════════════
REM  ST2C++ 一键测试脚本（Flat 后端）
REM  用法: test.bat [input.st]
REM  默认: input.st = examples\test.st
REM ═══════════════════════════════════════════════════════

set "PROJECT_ROOT=%~dp0"
set "INPUT_ST=%~1"
if "%INPUT_ST%"=="" set "INPUT_ST=%PROJECT_ROOT%examples\test.st"

echo.
echo ══════════════════════════════════════════
echo  ST2C++ Flat 后端测试
echo ══════════════════════════════════════════
echo  输入: %INPUT_ST%
echo ══════════════════════════════════════════
echo.

REM ─── Step 1: 检查输入文件 ───
if not exist "%INPUT_ST%" (
    echo [ERROR] 输入文件不存在: %INPUT_ST%
    exit /b 1
)

REM ─── Step 2: 编译 Java 编译器 ───
echo [1/4] 编译 Java 编译器...
cd /d "%PROJECT_ROOT%java"
call mvn compile -q 2>&1
if errorlevel 1 (
    echo [ERROR] Java 编译失败
    exit /b 1
)
echo       OK

REM ─── Step 3: Flat 后端翻译 ───
echo.
echo [2/4] Flat 后端翻译 ST → C++...
call mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--input %INPUT_ST% --output %PROJECT_ROOT%output\flat\main.cpp" -q 2>&1
if errorlevel 1 (
    echo [ERROR] Flat 翻译失败
    exit /b 1
)
echo       生成: output\flat\main.cpp

REM ─── Step 4: 编译 Flat 运行时 + 生成代码 ───
echo.
echo [3/4] 编译 Flat 运行时...
cd /d "%PROJECT_ROOT%runtime-flat\build"
if not exist CMakeCache.txt (
    cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release -DENABLE_DIAG=OFF >nul 2>&1
)
mingw32-make -j4 -q 2>&1
if errorlevel 1 (
    cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release -DENABLE_DIAG=OFF >nul 2>&1
    mingw32-make -j4 2>&1
)
echo       OK

REM ─── Step 5: 运行 Flat 测试 ───
echo.
echo [4/4] 运行 Flat 框架测试...
cd /d "%PROJECT_ROOT%runtime-flat\build"
.\framework_test.exe 2>&1 | findstr /C:"通过" /C:"失败" /C:"全部"
echo.

echo ══════════════════════════════════════════
echo  测试完成
echo ══════════════════════════════════════════
