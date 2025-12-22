@echo off
REM 智能测试脚本 - 支持多种测试模式
REM 用法: 
REM   scripts\smart-test.bat                    # 运行所有单元测试
REM   scripts\smart-test.bat EditFactUseCase    # 运行指定测试类
REM   scripts\smart-test.bat --changed          # 运行修改文件相关的测试
REM   scripts\smart-test.bat --failed           # 重新运行上次失败的测试

setlocal enabledelayedexpansion

echo ========================================
echo 智能测试脚本
echo ========================================

set "MODE=all"
set "TEST_CLASS="

REM 解析参数
if "%~1"=="" (
    set "MODE=all"
) else if "%~1"=="--changed" (
    set "MODE=changed"
) else if "%~1"=="--failed" (
    set "MODE=failed"
) else if "%~1"=="--all" (
    set "MODE=all"
) else (
    set "MODE=single"
    set "TEST_CLASS=%~1"
)

echo 测试模式: %MODE%
echo.

if "%MODE%"=="single" (
    echo 运行测试类: %TEST_CLASS%
    echo ----------------------------------------
    call gradlew.bat testDebugUnitTest --tests "*%TEST_CLASS%*" --build-cache --parallel
    goto :check_result
)

if "%MODE%"=="changed" (
    echo 检测修改的文件...
    echo ----------------------------------------
    
    REM 获取修改的 Kotlin 文件
    for /f "tokens=*" %%i in ('git diff --name-only HEAD -- "*.kt" 2^>nul') do (
        set "FILE=%%i"
        REM 提取文件名（不含路径和扩展名）
        for %%f in ("!FILE!") do set "BASENAME=%%~nf"
        
        REM 如果是源文件，查找对应测试
        echo !FILE! | findstr /i "src\\main" >nul
        if !errorlevel! equ 0 (
            echo 源文件: !BASENAME! - 查找测试...
            set "TEST_NAME=!BASENAME!Test"
            call gradlew.bat testDebugUnitTest --tests "*!TEST_NAME!*" --build-cache --parallel 2>nul
        )
        
        REM 如果是测试文件，直接运行
        echo !FILE! | findstr /i "src\\test" >nul
        if !errorlevel! equ 0 (
            echo 测试文件: !BASENAME! - 直接运行...
            call gradlew.bat testDebugUnitTest --tests "*!BASENAME!*" --build-cache --parallel 2>nul
        )
    )
    goto :check_result
)

if "%MODE%"=="failed" (
    echo 重新运行失败的测试...
    echo ----------------------------------------
    call gradlew.bat testDebugUnitTest --rerun-tasks --build-cache --parallel
    goto :check_result
)

if "%MODE%"=="all" (
    echo 运行所有单元测试...
    echo ----------------------------------------
    call gradlew.bat testDebugUnitTest --build-cache --parallel
    goto :check_result
)

:check_result
echo.
echo ========================================
if %ERRORLEVEL% EQU 0 (
    echo ✅ 测试通过！
) else (
    echo ❌ 测试失败，请检查上方错误信息
    echo.
    echo 测试报告位置:
    echo   app\build\reports\tests\testDebugUnitTest\index.html
)
echo ========================================

endlocal
