@echo off
REM 真机测试脚本 - 自动安装、启动、截图
REM 用法: scripts\device-test.bat [命令]
REM 
REM 命令:
REM   (无参数)  - 安装并启动应用
REM   install   - 仅安装APK
REM   start     - 仅启动应用
REM   screenshot- 截图并保存
REM   log       - 查看应用日志
REM   clear     - 清除应用数据
REM   uninstall - 卸载应用

setlocal enabledelayedexpansion

set "PACKAGE=com.empathy.ai"
set "MAIN_ACTIVITY=%PACKAGE%.presentation.ui.MainActivity"
set "APK_PATH=app\build\outputs\apk\debug\app-debug.apk"
set "SCREENSHOT_DIR=screenshots"

echo.
echo ╔════════════════════════════════════════╗
echo ║       真机测试脚本 v1.0                ║
echo ╚════════════════════════════════════════╝
echo.

REM 检查ADB连接
adb devices | findstr /r "device$" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 未检测到设备连接
    echo    请确保:
    echo    1. 手机已通过USB连接
    echo    2. 已开启USB调试
    echo    3. 已授权此电脑调试
    echo.
    echo    运行 adb devices 检查设备状态
    exit /b 1
)

REM 获取设备信息
for /f "tokens=1" %%d in ('adb devices ^| findstr /r "device$"') do set "DEVICE_ID=%%d"
for /f "tokens=*" %%m in ('adb shell getprop ro.product.model 2^>nul') do set "DEVICE_MODEL=%%m"
echo 📱 设备: %DEVICE_MODEL% (%DEVICE_ID%)
echo.

REM 根据参数执行不同操作
if "%1"=="" goto :install_and_start
if "%1"=="install" goto :install
if "%1"=="start" goto :start
if "%1"=="screenshot" goto :screenshot
if "%1"=="log" goto :log
if "%1"=="clear" goto :clear
if "%1"=="uninstall" goto :uninstall
goto :help

:install_and_start
call :install
if %ERRORLEVEL% NEQ 0 exit /b 1
call :start
goto :end

:install
echo [安装] 正在安装APK...
if not exist "%APK_PATH%" (
    echo ❌ APK不存在: %APK_PATH%
    echo    请先运行: scripts\quick-build.bat
    exit /b 1
)
adb install -r "%APK_PATH%"
if %ERRORLEVEL% EQU 0 (
    echo ✅ 安装成功
) else (
    echo ❌ 安装失败
    exit /b 1
)
goto :eof

:start
echo [启动] 正在启动应用...
adb shell am start -n "%MAIN_ACTIVITY%"
if %ERRORLEVEL% EQU 0 (
    echo ✅ 应用已启动
) else (
    echo ❌ 启动失败
)
goto :eof

:screenshot
echo [截图] 正在截取屏幕...
if not exist "%SCREENSHOT_DIR%" mkdir "%SCREENSHOT_DIR%"

REM 生成时间戳文件名
for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set "DATE=%%c%%a%%b"
for /f "tokens=1-2 delims=:. " %%a in ('time /t') do set "TIME=%%a%%b"
set "FILENAME=%SCREENSHOT_DIR%\screenshot_%DATE%_%TIME%.png"

adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png "%FILENAME%" >nul 2>&1
adb shell rm /sdcard/screenshot.png

if exist "%FILENAME%" (
    echo ✅ 截图已保存: %FILENAME%
) else (
    echo ❌ 截图失败
)
goto :end

:log
echo [日志] 显示应用日志 (Ctrl+C 退出)...
echo ────────────────────────────────────────
adb logcat -v time %PACKAGE%:V *:S
goto :end

:clear
echo [清除] 正在清除应用数据...
adb shell pm clear %PACKAGE%
if %ERRORLEVEL% EQU 0 (
    echo ✅ 数据已清除
) else (
    echo ❌ 清除失败
)
goto :end

:uninstall
echo [卸载] 正在卸载应用...
adb uninstall %PACKAGE%
if %ERRORLEVEL% EQU 0 (
    echo ✅ 卸载成功
) else (
    echo ❌ 卸载失败（可能未安装）
)
goto :end

:help
echo 用法: device-test.bat [命令]
echo.
echo 命令:
echo   (无参数)   安装并启动应用
echo   install    仅安装APK
echo   start      仅启动应用
echo   screenshot 截图并保存到screenshots目录
echo   log        查看应用日志（实时）
echo   clear      清除应用数据
echo   uninstall  卸载应用
echo.
echo 示例:
echo   scripts\device-test.bat
echo   scripts\device-test.bat screenshot
echo   scripts\device-test.bat log
goto :end

:end
echo.
endlocal
