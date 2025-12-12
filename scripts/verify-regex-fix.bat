@echo off
REM AI响应解析正则表达式修复验证脚本
REM 用途: 自动化验证修复是否成功

echo ========================================
echo AI响应解析正则表达式修复验证脚本
echo ========================================
echo.

REM 步骤1: 检查代码修复
echo [1/5] 检查代码修复...
echo.
echo 检查 AiRepositoryImpl.kt 中的 Regex() 使用:
findstr /C:"Regex(" app\src\main\java\com\empathy\ai\data\repository\AiRepositoryImpl.kt | find /C "Regex(" > nul
if %errorlevel% equ 0 (
    echo   ✓ AiRepositoryImpl.kt 已使用 Regex^(^) 构造函数
) else (
    echo   ✗ AiRepositoryImpl.kt 未找到 Regex^(^) 构造函数
    goto :error
)

echo.
echo 检查 PrivacyEngine.kt 中的 Regex() 使用:
findstr /C:"Regex(" app\src\main\java\com\empathy\ai\domain\service\PrivacyEngine.kt | find /C "Regex(" > nul
if %errorlevel% equ 0 (
    echo   ✓ PrivacyEngine.kt 已使用 Regex^(^) 构造函数
) else (
    echo   ✗ PrivacyEngine.kt 未找到 Regex^(^) 构造函数
    goto :error
)

echo.
echo 检查是否还有残留的 .toRegex^(^):
findstr /C:".toRegex()" app\src\main\java\com\empathy\ai\data\repository\AiRepositoryImpl.kt > nul
if %errorlevel% equ 0 (
    echo   ✗ AiRepositoryImpl.kt 仍然包含 .toRegex^(^)
    goto :error
) else (
    echo   ✓ AiRepositoryImpl.kt 无残留的 .toRegex^(^)
)

findstr /C:".toRegex()" app\src\main\java\com\empathy\ai\domain\service\PrivacyEngine.kt > nul
if %errorlevel% equ 0 (
    echo   ✗ PrivacyEngine.kt 仍然包含 .toRegex^(^)
    goto :error
) else (
    echo   ✓ PrivacyEngine.kt 无残留的 .toRegex^(^)
)

echo.
echo ========================================
echo [2/5] 编译应用...
echo ========================================
echo.
call gradlew.bat :app:assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo ✗ 编译失败！
    goto :error
)

echo.
echo ✓ 编译成功！
echo.

echo ========================================
echo [3/5] 卸载旧版本...
echo ========================================
echo.
adb uninstall com.empathy.ai
echo.

echo ========================================
echo [4/5] 安装新版本...
echo ========================================
echo.
adb install app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo.
    echo ✗ 安装失败！请检查设备连接。
    goto :error
)

echo.
echo ✓ 安装成功！
echo.

echo ========================================
echo [5/5] 启动日志监控...
echo ========================================
echo.
echo 请按照以下步骤测试:
echo.
echo 1. 打开应用
echo 2. 打开悬浮窗
echo 3. 选择任意联系人
echo 4. 输入测试文本: "你好"
echo 5. 点击确认按钮
echo.
echo 监控日志中...
echo.
echo 成功标志: 看到 "AnalysisResult解析成功"
echo 失败标志: 看到 "PatternSyntaxException"
echo.
echo 按 Ctrl+C 停止监控
echo.
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /C:"AiRepositoryImpl" /C:"AnalysisResult" /C:"PatternSyntaxException"

goto :end

:error
echo.
echo ========================================
echo ✗ 验证失败！
echo ========================================
echo.
echo 请检查:
echo 1. 代码修改是否正确
echo 2. IDE是否自动格式化还原了修改
echo 3. 是否需要重新应用修复
echo.
echo 详细信息请查看: docs/05-FixBug/AI响应解析Bug验证指南.md
echo.
pause
exit /b 1

:end
echo.
echo ========================================
echo 验证脚本执行完成
echo ========================================
echo.
pause
