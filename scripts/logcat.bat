@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
:: 共情AI助手 - Logcat调试脚本
:: 用法: logcat.bat [选项]
::   无参数    - 显示应用的ERROR和WARN日志
::   -e        - 只显示ERROR级别
::   -w        - 显示WARN及以上
::   -v        - 显示所有级别（VERBOSE）
::   -a        - 显示所有应用日志（不过滤级别）
::   -c        - 清空日志后开始
::   -f        - 保存到文件
::   -crash    - 只显示崩溃日志
:: ============================================================

set "PACKAGE=com.empathy.ai"
set "LOG_LEVEL=*:S %PACKAGE%:W"
set "CLEAR_LOG="
set "SAVE_FILE="
set "CRASH_ONLY="

:: 解析参数
:parse_args
if "%~1"=="" goto :start
if "%~1"=="-e" (
    set "LOG_LEVEL=*:S %PACKAGE%:E"
    echo [INFO] 只显示ERROR级别日志
    shift
    goto :parse_args
)
if "%~1"=="-w" (
    set "LOG_LEVEL=*:S %PACKAGE%:W"
    echo [INFO] 显示WARN及以上级别日志
    shift
    goto :parse_args
)
if "%~1"=="-v" (
    set "LOG_LEVEL=*:S %PACKAGE%:V"
    echo [INFO] 显示所有级别日志
    shift
    goto :parse_args
)
if "%~1"=="-a" (
    set "LOG_LEVEL=%PACKAGE%:V *:S"
    echo [INFO] 显示应用所有日志
    shift
    goto :parse_args
)
if "%~1"=="-c" (
    set "CLEAR_LOG=1"
    echo [INFO] 将先清空日志
    shift
    goto :parse_args
)
if "%~1"=="-f" (
    set "SAVE_FILE=1"
    echo [INFO] 日志将保存到文件
    shift
    goto :parse_args
)
if "%~1"=="-crash" (
    set "CRASH_ONLY=1"
    echo [INFO] 只显示崩溃日志
    shift
    goto :parse_args
)
if "%~1"=="-h" goto :help
if "%~1"=="--help" goto :help
shift
goto :parse_args

:help
echo.
echo 共情AI助手 - Logcat调试脚本
echo ============================
echo.
echo 用法: logcat.bat [选项]
echo.
echo 选项:
echo   -e        只显示ERROR级别
echo   -w        显示WARN及以上（默认）
echo   -v        显示所有级别（VERBOSE）
echo   -a        显示应用所有日志
echo   -c        清空日志后开始
echo   -f        保存到文件（logs目录）
echo   -crash    只显示崩溃日志
echo   -h        显示帮助
echo.
echo 示例:
echo   logcat.bat           显示WARN及以上
echo   logcat.bat -e        只看ERROR
echo   logcat.bat -c -e     清空后只看ERROR
echo   logcat.bat -f -e     ERROR日志保存到文件
echo.
exit /b 0

:start
:: 检查设备连接
echo.
echo [INFO] 检查设备连接...
for /f "tokens=1" %%a in ('adb devices ^| findstr /v "List" ^| findstr "device"') do (
    set "DEVICE=%%a"
)

if not defined DEVICE (
    echo [ERROR] 未检测到设备，请连接设备或启动模拟器
    exit /b 1
)
echo [INFO] 已连接设备: %DEVICE%

:: 获取应用PID
for /f %%p in ('adb -s %DEVICE% shell pidof %PACKAGE% 2^>nul') do set "PID=%%p"

if not defined PID (
    echo [WARN] 应用未运行，将显示所有%PACKAGE%相关日志
) else (
    echo [INFO] 应用PID: %PID%
)

:: 清空日志
if defined CLEAR_LOG (
    echo [INFO] 清空日志缓冲区...
    adb -s %DEVICE% logcat -c
)

:: 设置输出
set "TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "TIMESTAMP=%TIMESTAMP: =0%"

if defined SAVE_FILE (
    if not exist "logs" mkdir logs
    set "LOG_FILE=logs\logcat_%TIMESTAMP%.txt"
    echo [INFO] 日志将保存到: !LOG_FILE!
)

:: 开始监听
echo.
echo ============================================================
echo  开始监听日志 (按 Ctrl+C 停止)
echo  设备: %DEVICE%
echo  包名: %PACKAGE%
echo ============================================================
echo.

if defined CRASH_ONLY (
    if defined SAVE_FILE (
        adb -s %DEVICE% logcat -v time *:E ^| findstr /i "FATAL Exception crash %PACKAGE%" > "!LOG_FILE!"
    ) else (
        adb -s %DEVICE% logcat -v time *:E | findstr /i "FATAL Exception crash %PACKAGE%"
    )
) else if defined PID (
    if defined SAVE_FILE (
        adb -s %DEVICE% logcat -v time --pid=%PID% > "!LOG_FILE!"
    ) else (
        adb -s %DEVICE% logcat -v time --pid=%PID%
    )
) else (
    if defined SAVE_FILE (
        adb -s %DEVICE% logcat -v time %LOG_LEVEL% > "!LOG_FILE!"
    ) else (
        adb -s %DEVICE% logcat -v time %LOG_LEVEL%
    )
)

endlocal
