@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ST2C++ Flat backend test script
REM Usage: test-flat.bat [input.st]
REM Default: input.st = examples\test.st

set "PROJECT_ROOT=%~dp0"
set "INPUT_ST=%~1"
if "%INPUT_ST%"=="" set "INPUT_ST=%PROJECT_ROOT%examples\test.st"

echo.
echo ==========================================
echo  ST2C++ Flat Backend Test
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
echo [1/5] Compiling Java compiler...
cd /d "%PROJECT_ROOT%java"
call mvn compile -q 2>&1
if errorlevel 1 (
    echo [ERROR] Java compilation failed
    exit /b 1
)
echo       OK

REM Step 3: Flat backend translation
echo.
echo [2/5] Flat backend ST -> C++ translation...
call mvn exec:java -Dexec.mainClass="Main" -Dexec.args="--backend flat --input %INPUT_ST% --output %PROJECT_ROOT%output\flat\main.cpp --verbose" -q 2>&1
if errorlevel 1 (
    echo [ERROR] Flat translation failed
    exit /b 1
)
echo       Generated: output\flat\main.cpp

REM Step 4: Copy to runtime-flat and add main entry
echo.
echo [3/5] Preparing C++ compilation...
cd /d "%PROJECT_ROOT%"

copy /Y "output\flat\main.cpp" "runtime-flat\generated_main.cpp" >nul

REM Append main entry
echo. >> "runtime-flat\generated_main.cpp"
echo // === Test Entry === >> "runtime-flat\generated_main.cpp"
echo #include ^<cstdio^> >> "runtime-flat\generated_main.cpp"
echo int main() { >> "runtime-flat\generated_main.cpp"
echo     GVL gvl; >> "runtime-flat\generated_main.cpp"
echo     ProcessImage io; >> "runtime-flat\generated_main.cpp"
echo     TIME dt = T_ms(1); >> "runtime-flat\generated_main.cpp"
echo     PROGRAM_P(gvl, io, dt); >> "runtime-flat\generated_main.cpp"
echo     printf("Program executed.\n"); >> "runtime-flat\generated_main.cpp"
echo     return 0; >> "runtime-flat\generated_main.cpp"
echo } >> "runtime-flat\generated_main.cpp"

echo       Generated: runtime-flat\generated_main.cpp

REM Step 5: Compile C++
echo.
echo [4/5] Compiling C++...
cd /d "%PROJECT_ROOT%runtime-flat\build"

where g++ >nul 2>&1
if errorlevel 1 (
    echo [WARN] g++ not found, trying CMake...
    if not exist "CMakeCache.txt" (
        cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release >nul 2>&1
    )
    mingw32-make -j4 -q 2>&1
    if errorlevel 1 (
        echo [ERROR] CMake/MinGW compilation failed. Please install MinGW.
        exit /b 1
    )
) else (
    g++ -O2 -std=c++17 -I"%PROJECT_ROOT%runtime-flat" "%PROJECT_ROOT%runtime-flat\generated_main.cpp" -o "%PROJECT_ROOT%runtime-flat\build\generated_test.exe" 2>&1
    if errorlevel 1 (
        echo [ERROR] g++ compilation failed
        exit /b 1
    )
)
echo       OK

REM Step 6: Run
echo.
echo [5/5] Running generated program...
if exist "%PROJECT_ROOT%runtime-flat\build\generated_test.exe" (
    "%PROJECT_ROOT%runtime-flat\build\generated_test.exe" 2>&1
) else (
    echo [ERROR] Executable not found
    exit /b 1
)

echo.
echo ==========================================
echo  Flat Backend Test Complete
echo ==========================================
