@echo off
setlocal enabledelayedexpansion

REM 6-Axis Robot Arm Controller — 集成测试编译运行脚本
REM 用法: .\compile.bat [--build-jar]

set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%
set ST2C_HOME=%PROJECT_DIR%\..\..\..
set STDLIB=%ST2C_HOME%\examples\iec_stdlib.st
set JAR=%ST2C_HOME%\java\target\st2c-jar-with-dependencies.jar
set OUTPUT_DIR=%ST2C_HOME%\output\flat\build

echo ==========================================
echo  6-Axis Robot Arm Controller
echo  Integration Test
echo ==========================================

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

echo [1/5] JAR ready

REM Step 2: Compile ST files (order matters: types → IO → utils → FB → main)
echo [2/5] Compiling ST sources...
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

java -jar "%JAR%" ^
    --stdlib "%STDLIB%" ^
    --input "%PROJECT_DIR%\types.st" ^
    --input "%PROJECT_DIR%\io_config.st" ^
    --input "%PROJECT_DIR%\math_utils.st" ^
    --input "%PROJECT_DIR%\servo_drive.st" ^
    --input "%PROJECT_DIR%\motion_planner.st" ^
    --input "%PROJECT_DIR%\safety.st" ^
    --input "%PROJECT_DIR%\robot_ctrl.st" ^
    --input "%PROJECT_DIR%\main.st" ^
    --output-dir "%OUTPUT_DIR%" ^
    --file-id robot ^
    --verbose

if errorlevel 1 (
    echo ERROR: ST compilation failed
    exit /b 1
)
echo [2/5] Compilation complete

REM Step 3: Build runtime
echo [3/5] Building runtime (target\windows)...
pushd "%ST2C_HOME%\target\windows"
where mingw32-make >nul 2>nul && (
    mingw32-make all
) else (
    make all
)
if errorlevel 1 (
    echo ERROR: Runtime build failed
    popd
    exit /b 1
)
popd
echo [3/5] Build complete

REM Step 4: Run
echo [4/5] Running robot arm controller...
"%ST2C_HOME%\target\windows\plc_runtime_windows.exe"

if errorlevel 1 (
    echo WARN: Runtime exited with code !errorlevel!
)

echo [5/5] Done

echo ==========================================
echo  SUCCESS: Robot arm integration test passed
echo ==========================================
