@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ═══════════════════════════════════════════════════════
REM  ST2C++ 一键测试脚本
REM  用法: test.bat [input.st]
REM  默认: input.st = examples\test.st
REM ═══════════════════════════════════════════════════════

set "PROJECT_ROOT=%~dp0"
set "INPUT_ST=%~1"
if "%INPUT_ST%"=="" set "INPUT_ST=%PROJECT_ROOT%examples\test.st"

echo.
echo ══════════════════════════════════════════
echo  ST2C++ 双后端测试
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
echo [1/6] 编译 Java 编译器...
cd /d "%PROJECT_ROOT%java"
call mvn compile -q 2>&1
if errorlevel 1 (
    echo [ERROR] Java 编译失败
    exit /b 1
)
echo       OK

REM ─── Step 3: OOP 后端翻译 ───
echo.
echo [2/6] OOP 后端翻译 ST → C++...
call mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend oop --input %INPUT_ST% --output %PROJECT_ROOT%output\oop\main.cpp" -q 2>&1
if errorlevel 1 (
    echo [ERROR] OOP 翻译失败
    exit /b 1
)
echo       生成: output\oop\main.cpp

REM ─── Step 4: Flat 后端翻译 ───
echo.
echo [3/6] Flat 后端翻译 ST → C++...
call mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend flat --input %INPUT_ST% --output %PROJECT_ROOT%output\flat\main.cpp" -q 2>&1
if errorlevel 1 (
    echo [ERROR] Flat 翻译失败
    exit /b 1
)
echo       生成: output\flat\main.cpp

REM ─── Step 5: 编译 Flat 运行时 + 生成代码 ───
echo.
echo [4/6] 编译 Flat 运行时...
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

REM ─── Step 6: 运行 Flat 测试 ───
echo.
echo [5/6] 运行 Flat 框架测试...
cd /d "%PROJECT_ROOT%runtime-flat\build"
.\framework_test.exe 2>&1 | findstr /C:"通过" /C:"失败" /C:"全部"
echo.

REM ─── Step 7: 对比输出 ───
echo [6/6] 生成代码对比:
echo.
echo ─── OOP 后端生成的代码（前 10 行）───
head -10 "%PROJECT_ROOT%output\oop\main.cpp" 2>nul || powershell -c "Get-Content '%PROJECT_ROOT%output\oop\main.cpp' | Select-Object -First 10"
echo.
echo ─── Flat 后端生成的代码（前 10 行）───
head -10 "%PROJECT_ROOT%output\flat\main.cpp" 2>nul || powershell -c "Get-Content '%PROJECT_ROOT%output\flat\main.cpp' | Select-Object -First 10"
echo.

echo ══════════════════════════════════════════
echo  测试完成
echo ══════════════════════════════════════════
