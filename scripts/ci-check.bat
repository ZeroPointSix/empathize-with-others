@echo off
REM CI 检查脚本 - 模拟 CI 环境的完整检查
REM 用法: scripts\ci-check.bat
REM 
REM 执行以下检查:
REM 1. 编译检查
REM 2. 单元测试
REM 3. Lint 检查（可选）
REM 4. 生成报告

setlocal enabledelayedexpansion

echo ========================================
echo CI 检查脚本
echo ========================================
echo.

set "START_TIME=%TIME%"
set "ERRORS=0"

REM 步骤 1: 编译检查
echo [1/3] 编译检查...
echo ----------------------------------------
call gradlew.bat compileDebugKotlin --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 编译失败
    set /a ERRORS+=1
    call gradlew.bat compileDebugKotlin --build-cache --parallel
) else (
    echo ✅ 编译通过
)
echo.

REM 步骤 2: 单元测试
echo [2/3] 单元测试...
echo ----------------------------------------
call gradlew.bat testDebugUnitTest --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 测试失败
    set /a ERRORS+=1
    echo 查看详情: app\build\reports\tests\testDebugUnitTest\index.html
) else (
    echo ✅ 测试通过
)
echo.

REM 步骤 3: 构建 APK
echo [3/3] 构建 Debug APK...
echo ----------------------------------------
call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ APK 构建失败
    set /a ERRORS+=1
) else (
    echo ✅ APK 构建成功
    
    REM 显示 APK 信息
    for %%f in (app\build\outputs\apk\debug\*.apk) do (
        echo    路径: %%f
        for %%s in ("%%f") do echo    大小: %%~zs bytes
    )
)
echo.

REM 结果汇总
echo ========================================
echo CI 检查结果
echo ========================================
set "END_TIME=%TIME%"
echo 开始时间: %START_TIME%
echo 结束时间: %END_TIME%
echo.

if %ERRORS% EQU 0 (
    echo ✅ 所有检查通过！可以安全提交代码。
    echo.
    echo 建议操作:
    echo   git add .
    echo   git commit -m "your message"
) else (
    echo ❌ 发现 %ERRORS% 个问题，请修复后再提交。
    echo.
    echo 查看详细报告:
    echo   - 测试报告: app\build\reports\tests\testDebugUnitTest\index.html
    echo   - 构建日志: 运行 gradlew.bat assembleDebug 查看详情
)
echo ========================================

endlocal
exit /b %ERRORS%
