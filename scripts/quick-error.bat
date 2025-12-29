@echo off
chcp 65001 >nul

:: ============================================================
:: 快速查看ERROR日志（一次性获取最近的错误）
:: 用法: quick-error.bat [行数]
::   默认显示最近50行ERROR日志
:: ============================================================

set "PACKAGE=com.empathy.ai"
set "LINES=50"

if not "%~1"=="" set "LINES=%~1"

echo.
echo [INFO] 获取最近 %LINES% 行ERROR日志...
echo.

:: 检查设备
for /f "tokens=1" %%a in ('adb devices ^| findstr /v "List" ^| findstr "device"') do (
    set "DEVICE=%%a"
)

if not defined DEVICE (
    echo [ERROR] 未检测到设备
    exit /b 1
)

echo ============================================================
echo  设备: %DEVICE%
echo  包名: %PACKAGE%
echo  级别: ERROR
echo ============================================================
echo.

:: 获取ERROR日志
adb -s %DEVICE% logcat -d -v time *:E | findstr /i "%PACKAGE% AndroidRuntime FATAL" | more +0

echo.
echo ============================================================
echo  日志获取完成
echo ============================================================
