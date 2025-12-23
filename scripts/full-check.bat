@echo off
REM 完整检查脚本 - 一键完成编译+测试+检查
REM 用法: scripts\full-check.bat
REM 
REM 执行流程:
REM 1. 清理旧构建（可选）
REM 2. 编译检查
REM 3. 单元测试
REM 4. Lint检查
REM 5. 构建APK
REM 6. 生成汇总报告

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════╗
echo ║       完整检查脚本 v1.0                ║
echo ╚════════════════════════════════════════╝
echo.

set "START_TIME=%TIME%"
set "STEP_ERRORS=0"
set "TOTAL_STEPS=4"

REM 检查是否需要清理
if "%1"=="clean" (
    echo [0/4] 清理旧构建...
    call gradlew.bat clean >nul 2>&1
    echo      ✓ 清理完成
    echo.
)

REM ========================================
REM 步骤 1: 编译检查
REM ========================================
echo [1/4] 编译检查...
set "STEP_START=%TIME%"
call gradlew.bat compileDebugKotlin --build-cache --parallel 2>&1 | findstr /i "error:"
set "COMPILE_RESULT=%ERRORLEVEL%"

REM 重新运行获取真实结果
call gradlew.bat compileDebugKotlin --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ❌ 编译失败
    echo      → 运行 gradlew compileDebugKotlin 查看详情
    set /a STEP_ERRORS+=1
    goto :summary
) else (
    echo      ✅ 编译通过
)
echo.

REM ========================================
REM 步骤 2: 单元测试
REM ========================================
echo [2/4] 单元测试...
call gradlew.bat testDebugUnitTest --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ❌ 测试失败
    echo      → 报告: app\build\reports\tests\testDebugUnitTest\index.html
    set /a STEP_ERRORS+=1
) else (
    REM 统计测试数量
    echo      ✅ 测试通过
)
echo.

REM ========================================
REM 步骤 3: Lint检查
REM ========================================
echo [3/4] Lint检查...
call gradlew.bat lintDebug --build-cache >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ⚠️  Lint发现问题
    echo      → 报告: app\build\reports\lint-results-debug.html
    REM Lint警告不阻断流程
) else (
    echo      ✅ Lint通过
)
echo.

REM ========================================
REM 步骤 4: 构建APK
REM ========================================
echo [4/4] 构建Debug APK...
call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo      ❌ APK构建失败
    set /a STEP_ERRORS+=1
) else (
    echo      ✅ APK构建成功
    for %%f in (app\build\outputs\apk\debug\*.apk) do (
        for %%s in ("%%f") do (
            set /a "SIZE_KB=%%~zs/1024"
            echo      → 大小: !SIZE_KB! KB
        )
        echo      → 路径: %%f
    )
)
echo.

:summary
REM ========================================
REM 结果汇总
REM ========================================
echo ╔════════════════════════════════════════╗
echo ║              检查结果                  ║
echo ╚════════════════════════════════════════╝
echo.
echo   开始: %START_TIME%
echo   结束: %TIME%
echo.

if %STEP_ERRORS% EQU 0 (
    echo   ✅ 全部通过！代码可以安全提交。
    echo.
    echo   下一步建议:
    echo     scripts\device-test.bat  - 真机测试
    echo     git add . ^&^& git commit  - 提交代码
) else (
    echo   ❌ 发现 %STEP_ERRORS% 个问题，请修复后重试。
    echo.
    echo   调试建议:
    echo     gradlew compileDebugKotlin  - 查看编译错误
    echo     gradlew testDebugUnitTest   - 查看测试详情
)
echo.
echo ════════════════════════════════════════

endlocal
exit /b %STEP_ERRORS%
