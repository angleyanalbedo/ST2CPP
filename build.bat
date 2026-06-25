@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ─── ST2C 构建脚本 ───
REM 用法: build [--run]
REM   --run: 构建后运行

powershell -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)
exit /b 0
