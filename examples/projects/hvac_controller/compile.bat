@echo off
setlocal enabledelayedexpansion

REM HVAC Controller — 集成测试编译运行脚本
REM 用法: .\compile.bat [--build-jar]

set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%
set ST2C_HOME=%PROJECT_DIR%\..\..\..
set STDLIB=%ST2C_HOME%\examples\iec_stdlib.st
set JAR=%ST2C_HOME%\java\target\st2c-jar-with-dependencies.jar
set OUTPUT_DIR=%ST2C_HOME%\output\flat\build
set RUNTIME_DIR=%ST2C_HOME%\runtime-flat
set BUILD_DIR=%ST2C_HOME%\output\build

echo ════════════════════════════════════════════
echo  HVAC Controller — Integration Test
echo ════════════════════════════════════════════

REM Step 1: Build JAR if needed
if "%1"=="--build-jar" (
    echo [1/5] Building Java compiler JAR...
    pushd "%ST2C_HOME%\java"
    call mvn package -DskipTests
    if errorlevel 1 (
        echo ERROR: Maven build failed
        popd
        exit /b 1
    )
    popd
)

if not exist "%JAR%" (
    echo ERROR: JAR not found at %JAR%
    echo Run with --build-jar to compile first, or build manually:
    echo   cd java ^&^& mvn package -DskipTests
    exit /b 1
)

echo [1/5] JAR ready: %JAR%

REM Step 2: Compile ST files
echo [2/5] Compiling ST source files...
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

java -jar "%JAR%" ^
    --stdlib "%STDLIB%" ^
    --input "%PROJECT_DIR%\types.st" ^
    --input "%PROJECT_DIR%\io_config.st" ^
    --input "%PROJECT_DIR%\pid_func.st" ^
    --input "%PROJECT_DIR%\zone_fb.st" ^
    --input "%PROJECT_DIR%\main.st" ^
    --output-dir "%OUTPUT_DIR%" ^
    --file-id hvac ^
    --verbose

if errorlevel 1 (
    echo ERROR: ST compilation failed
    exit /b 1
)
echo [2/5] Compilation complete

REM Step 3: Configure CMake
echo [3/5] Configuring CMake...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

cmake -B "%BUILD_DIR%" -S "%RUNTIME_DIR%" ^
    -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release ^
    -DGEN_CPP_DIR="%OUTPUT_DIR%"

if errorlevel 1 (
    echo ERROR: CMake configuration failed
    exit /b 1
)
echo [3/5] CMake configured

REM Step 4: Build runtime with POU
echo [4/5] Building runtime...
cmake --build "%BUILD_DIR%" --parallel

if errorlevel 1 (
    echo ERROR: Runtime build failed
    exit /b 1
)
echo [4/5] Build complete

REM Step 5: Run integration test
echo [5/5] Running HVAC integration test...
"%BUILD_DIR%\runtime.exe"

if errorlevel 1 (
    echo ERROR: Runtime execution failed
    exit /b 1
)

echo ════════════════════════════════════════════
echo  SUCCESS: HVAC integration test passed
echo ════════════════════════════════════════════
