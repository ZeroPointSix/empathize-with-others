@echo off
REM 快速构建脚本 - 跳过不必要的检查，加速开发迭代
REM 用法: scripts\quick-build.bat

echo ========================================
echo 快速构建 Debug APK
echo ========================================

REM 使用 --offline 可以跳过依赖检查（如果依赖没变化）
REM 使用 --build-cache 启用构建缓存
REM 使用 -x lint 跳过 lint 检查
REM 使用 -x test 跳过测试

call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 构建成功！
    echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
    echo ========================================
) else (
    echo.
    echo ========================================
    echo 构建失败，请检查错误信息
    echo ========================================
)
