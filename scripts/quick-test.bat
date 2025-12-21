@echo off
REM 快速测试脚本 - 只运行单元测试
REM 用法: scripts\quick-test.bat [测试类名]
REM 示例: scripts\quick-test.bat ContactDetailViewModelTest

echo ========================================
echo 运行单元测试
echo ========================================

if "%1"=="" (
    REM 运行所有单元测试
    call gradlew.bat testDebugUnitTest --build-cache --parallel
) else (
    REM 运行指定的测试类
    echo 运行测试: %1
    call gradlew.bat testDebugUnitTest --tests "*%1*" --build-cache
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 测试通过！
    echo ========================================
) else (
    echo.
    echo ========================================
    echo 测试失败，请检查错误信息
    echo 报告位置: app\build\reports\tests\testDebugUnitTest\index.html
    echo ========================================
)
