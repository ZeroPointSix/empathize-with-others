@echo off
chcp 65001 >nul

:: ============================================================
:: AI完整调试日志脚本
:: 显示完整的AI请求日志，包括提示词内容
:: ============================================================

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║              🔍 AI完整调试日志                               ║
echo ╠══════════════════════════════════════════════════════════════╣
echo ║ 显示完整的AI请求信息，包括:                                  ║
echo ║   - URL / Model / Provider                                   ║
echo ║   - Temperature / MaxTokens                                  ║
echo ║   - PromptContext 完整内容                                   ║
echo ║   - SystemInstruction 完整内容                               ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

set "DEVICE=%~1"
if defined DEVICE (
    set "DEVICE=-s %DEVICE%"
)

echo [获取最近的AI请求日志...]
echo.

adb %DEVICE% logcat -d -t 500 | findstr /i "AiRepositoryImpl"

echo.
echo ══════════════════════════════════════════════════════════════
echo 完成! 如需实时监听，请运行: ai-debug.bat
