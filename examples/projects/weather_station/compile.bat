@echo off
setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%
set ST2C_HOME=%PROJECT_DIR%\..\..\..
set JAR=%ST2C_HOME%\java\target\st2c-jar-with-dependencies.jar
set OUTPUT_DIR=%ST2C_HOME%\output\flat\build

echo ==========================================
echo   Weather Station - Integration Test
echo ==========================================

if "%1"=="--build-jar" (
    echo [1/4] Building Java compiler JAR...
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
    echo Run with --build-jar to compile first
    echo   cd java ^&^& mvn package -DskipTests
    exit /b 1
)

echo [1/4] JAR ready

echo [2/4] Compiling ST sources...
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
del /Q "%OUTPUT_DIR%\*.cpp" "%OUTPUT_DIR%\*.h" 2>nul

java -jar "%JAR%" ^
    --input "%PROJECT_DIR%\types.st" ^
    --input "%PROJECT_DIR%\utils.st" ^
    --input "%PROJECT_DIR%\sensor_cal.st" ^
    --input "%PROJECT_DIR%\data_logger.st" ^
    --input "%PROJECT_DIR%\comms_ctrl.st" ^
    --input "%PROJECT_DIR%\alarm_eval.st" ^
    --input "%PROJECT_DIR%\main.st" ^
    --output-dir "%OUTPUT_DIR%" ^
    --file-id weather ^
    --verbose
if errorlevel 1 (
    echo ERROR: ST compilation failed
    exit /b 1
)
echo [2/4] Compilation complete

echo [3/4] Building runtime (target\desktop)...
pushd "%ST2C_HOME%\target\desktop"
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
echo [3/4] Build complete

echo [4/4] Running Weather Station...
"%ST2C_HOME%\target\desktop\build\plc_runtime_desktop.exe"

if errorlevel 1 (
    echo WARN: Runtime exited with code !errorlevel!
)

echo ==========================================
echo  SUCCESS: Weather Station test passed
echo ==========================================
