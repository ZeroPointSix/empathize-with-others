@echo off
REM 开发循环脚本 - 快速构建+安装+启动一条龙
REM 用法: scripts\dev-cycle.bat
REM 
REM 适用场景: 修改代码后快速验证效果
REM 执行流程: 快速构建 → 安装到设备 → 启动应用

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════╗
echo ║       开发循环脚本 v1.0                ║
echo ╚════════════════════════════════════════╝
echo.

set "START_TIME=%TIME%"

REM ========================================
REM 步骤 1: 快速构建
REM ========================================
echo [1/3] 快速构建...
call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ❌ 构建失败
    echo.
    echo 查看错误详情:
    call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel
    exit /b 1
)
echo      ✅ 构建成功
echo.

REM ========================================
REM 步骤 2: 检查设备
REM ========================================
echo [2/3] 检查设备...
adb devices | findstr /r "device$" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ⚠️  未检测到设备
    echo      APK已构建: app\build\outputs\apk\debug\app-debug.apk
    echo      请手动安装或连接设备后运行: scripts\device-test.bat
    goto :summary
)

for /f "tokens=*" %%m in ('adb shell getprop ro.product.model 2^>nul') do set "DEVICE=%%m"
echo      📱 设备: %DEVICE%
echo.

REM ========================================
REM 步骤 3: 安装并启动
REM ========================================
echo [3/3] 安装并启动...
adb install -r app\build\outputs\apk\debug\app-debug.apk >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ❌ 安装失败
    exit /b 1
)
echo      ✅ 安装成功

adb shell am start -n com.empathy.ai/.presentation.ui.MainActivity >nul 2>&1
echo      ✅ 应用已启动
echo.

:summary
echo ════════════════════════════════════════
echo   开始: %START_TIME%
echo   结束: %TIME%
echo ════════════════════════════════════════

endlocal
