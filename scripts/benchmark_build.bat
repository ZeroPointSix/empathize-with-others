@echo ==========================================
@echo 共情AI助手 - 编译性能测试脚本
@echo ==========================================

echo 开始测试编译性能...
echo.

:: 清理构建缓存和项目
echo [1/4] 清理构建缓存...
call gradlew --stop
call gradlew clean
if %ERRORLEVEL% neq 0 (
    echo 清理失败，请检查错误信息
    exit /b 1
)

:: 测试完整构建
echo.
echo [2/4] 执行完整构建（首次编译）...
call gradlew assembleDebug --profile
if %ERRORLEVEL% neq 0 (
    echo 构建失败，请检查错误信息
    exit /b 1
)

:: 测试增量构建
echo.
echo [3/4] 执行增量构建（无改动）...
call gradlew assembleDebug --profile
if %ERRORLEVEL% neq 0 (
    echo 增量构建失败，请检查错误信息
    exit /b 1
)

:: 测试编译特定模块
echo.
echo [4/4] 执行单元测试编译...
call gradlew compileDebugUnitTestKotlin --profile
if %ERRORLEVEL% neq 0 (
    echo 测试编译失败，请检查错误信息
    exit /b 1
)

echo.
echo ==========================================
echo 编译性能测试完成！
echo ==========================================
echo 报告位置: app/build/reports/profile/
echo 请查看 HTML 报告获取详细性能分析
echo.

:: 打开报告目录
explorer "app\build\reports\profile"

pause