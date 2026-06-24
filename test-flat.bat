@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ST2C++ Flat backend test script with rt_runtime scheduler
REM Usage: test-flat.bat [input.st]
REM Default: input.st = examples\test.st

set "PROJECT_ROOT=%~dp0"
set "INPUT_ST=%~1"
if "%INPUT_ST%"=="" set "INPUT_ST=%PROJECT_ROOT%examples\test.st"

echo.
echo ==========================================
echo  ST2C++ Flat Backend + rt_runtime Test
echo ==========================================
echo  Input: %INPUT_ST%
echo ==========================================
echo.

REM Step 1: Check input file
if not exist "%INPUT_ST%" (
    echo [ERROR] Input file not found: %INPUT_ST%
    exit /b 1
)

REM Step 2: Compile Java compiler
echo [1/4] Compiling Java compiler...
cd /d "%PROJECT_ROOT%java"
call mvn compile -q 2>&1
if errorlevel 1 (
    echo [ERROR] Java compilation failed
    exit /b 1
)
echo       OK

REM Step 3: Flat backend translation
echo.
echo [2/4] Flat backend ST -> C++ translation...
call mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend flat --input %INPUT_ST% --output %PROJECT_ROOT%runtime-flat\generated_pou.cpp --verbose" -q 2>&1
if errorlevel 1 (
    echo [ERROR] Flat translation failed
    exit /b 1
)
echo       Generated: runtime-flat\generated_pou.cpp

REM Step 4: Compile C++ (runtime_main + generated_pou)
echo.
echo [3/4] Compiling C++ with rt_runtime...
cd /d "%PROJECT_ROOT%runtime-flat"

where g++ >nul 2>&1
if errorlevel 1 (
    echo [ERROR] g++ not found. Please install MinGW.
    exit /b 1
)

REM Compile runtime_main.cpp + generated_pou.cpp together with new include structure
g++ -O2 -std=c++17 -I"%PROJECT_ROOT%runtime-flat\include" "%PROJECT_ROOT%runtime-flat\runtime_main.cpp" "%PROJECT_ROOT%runtime-flat\generated_pou.cpp" -o "%PROJECT_ROOT%runtime-flat\build\runtime_test.exe" 2>&1
if errorlevel 1 (
    echo [ERROR] C++ compilation failed
    exit /b 1
)
echo       OK

REM Step 5: Run
echo.
echo [4/4] Running with rt_runtime scheduler...
if exist "%PROJECT_ROOT%runtime-flat\build\runtime_test.exe" (
    "%PROJECT_ROOT%runtime-flat\build\runtime_test.exe" 2>&1
) else (
    echo [ERROR] Executable not found
    exit /b 1
)

echo.
echo ==========================================
echo  Flat + rt_runtime Test Complete
echo ==========================================
