@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
:: AI调试日志脚本
:: 用于过滤和显示AI请求相关的重要日志信息
:: ============================================================

set "DEVICE="
set "MODE=realtime"
set "LINES=100"
set "OUTPUT_FILE="

:parse_args
if "%~1"=="" goto :main
if /i "%~1"=="-d" (
    set "DEVICE=-s %~2"
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-h" (
    set "MODE=history"
    shift
    goto :parse_args
)
if /i "%~1"=="-n" (
    set "LINES=%~2"
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-f" (
    set "OUTPUT_FILE=%~2"
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--help" goto :show_help
shift
goto :parse_args

:show_help
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║              AI调试日志脚本 - 使用说明                       ║
echo ╠══════════════════════════════════════════════════════════════╣
echo ║ 用法: ai-debug.bat [选项]                                    ║
echo ║                                                              ║
echo ║ 选项:                                                        ║
echo ║   -d ^<device^>   指定设备 (如: 127.0.0.1:7555)               ║
echo ║   -h            历史模式 (获取最近日志，非实时)              ║
echo ║   -n ^<lines^>    历史模式下获取的行数 (默认: 100)            ║
echo ║   -f ^<file^>     输出到文件                                  ║
echo ║   --help        显示此帮助信息                               ║
echo ║                                                              ║
echo ║ 示例:                                                        ║
echo ║   ai-debug.bat                    实时监听AI日志             ║
echo ║   ai-debug.bat -h                 获取最近100条AI日志        ║
echo ║   ai-debug.bat -h -n 200          获取最近200条AI日志        ║
echo ║   ai-debug.bat -d 127.0.0.1:7555  指定MuMu模拟器             ║
echo ║   ai-debug.bat -f ai_log.txt      输出到文件                 ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.
exit /b 0

:main
echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║              🔍 AI调试日志监控                               ║
echo ╠══════════════════════════════════════════════════════════════╣
echo ║ 过滤关键词:                                                  ║
echo ║   - AiRepositoryImpl (AI请求详情)                            ║
echo ║   - Temperature / MaxTokens (高级参数)                       ║
echo ║   - FloatingWindowService (悬浮窗服务)                       ║
echo ║   - PolishDraftUseCase / GenerateReplyUseCase                ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

if "%MODE%"=="history" (
    echo [模式] 历史日志 - 获取最近 %LINES% 条
    echo.
    if defined OUTPUT_FILE (
        adb %DEVICE% logcat -d -t %LINES% | findstr /i "AiRepositoryImpl Temperature MaxTokens API请求 高级参数 FloatingWindowService PolishDraft GenerateReply analyzeChat polishDraft generateReply" > "%OUTPUT_FILE%"
        echo 日志已保存到: %OUTPUT_FILE%
    ) else (
        adb %DEVICE% logcat -d -t %LINES% | findstr /i "AiRepositoryImpl Temperature MaxTokens API请求 高级参数 FloatingWindowService PolishDraft GenerateReply analyzeChat polishDraft generateReply"
    )
) else (
    echo [模式] 实时监听 - 按 Ctrl+C 停止
    echo.
    if defined OUTPUT_FILE (
        adb %DEVICE% logcat | findstr /i "AiRepositoryImpl Temperature MaxTokens API请求 高级参数 FloatingWindowService PolishDraft GenerateReply analyzeChat polishDraft generateReply" > "%OUTPUT_FILE%"
    ) else (
        adb %DEVICE% logcat | findstr /i "AiRepositoryImpl Temperature MaxTokens API请求 高级参数 FloatingWindowService PolishDraft GenerateReply analyzeChat polishDraft generateReply"
    )
)

echo.
echo 完成!
