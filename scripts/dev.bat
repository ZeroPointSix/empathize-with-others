@echo off
REM 开发者快捷命令入口
REM 用法: scripts\dev.bat [命令]
REM
REM 可用命令:
REM   build     - 快速构建 Debug APK
REM   test      - 运行所有单元测试
REM   test XXX  - 运行指定测试类
REM   ci        - 提交前完整检查
REM   clean     - 清理构建缓存
REM   stats     - 显示代码统计
REM   doc XXX   - 审查指定文档
REM   help      - 显示帮助信息

setlocal enabledelayedexpansion

if "%~1"=="" goto :help
if "%~1"=="help" goto :help
if "%~1"=="build" goto :build
if "%~1"=="test" goto :test
if "%~1"=="ci" goto :ci
if "%~1"=="clean" goto :clean
if "%~1"=="stats" goto :stats
if "%~1"=="doc" goto :doc

echo 未知命令: %~1
goto :help

:help
echo.
echo ========================================
echo 开发者快捷命令
echo ========================================
echo.
echo 用法: scripts\dev.bat [命令]
echo.
echo 可用命令:
echo   build     快速构建 Debug APK
echo   test      运行所有单元测试
echo   test XXX  运行指定测试类 (如: test EditFactUseCaseTest)
echo   ci        提交前完整检查 (编译+测试+构建)
echo   clean     清理构建缓存
echo   stats     显示代码统计
echo   doc XXX   审查指定文档 (如: doc PRD-00012)
echo   help      显示此帮助信息
echo.
echo 示例:
echo   scripts\dev.bat build
echo   scripts\dev.bat test EditFactUseCaseTest
echo   scripts\dev.bat ci
echo.
goto :eof

:build
echo.
echo [构建] 快速构建 Debug APK...
echo ----------------------------------------
call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 构建成功！
    echo 📦 APK: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ❌ 构建失败
)
goto :eof

:test
if "%~2"=="" (
    echo.
    echo [测试] 运行所有单元测试...
    echo ----------------------------------------
    call gradlew.bat testDebugUnitTest --build-cache --parallel
) else (
    echo.
    echo [测试] 运行测试类: %~2
    echo ----------------------------------------
    call gradlew.bat testDebugUnitTest --tests "*%~2*" --build-cache --parallel
)
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 测试通过！
) else (
    echo.
    echo ❌ 测试失败
    echo 📄 报告: app\build\reports\tests\testDebugUnitTest\index.html
)
goto :eof

:ci
echo.
echo [CI] 提交前完整检查...
echo ========================================
echo.

echo [1/3] 编译检查...
call gradlew.bat compileDebugKotlin --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 编译失败
    call gradlew.bat compileDebugKotlin --build-cache --parallel
    goto :eof
)
echo ✅ 编译通过

echo [2/3] 单元测试...
call gradlew.bat testDebugUnitTest --build-cache --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 测试失败
    echo 📄 报告: app\build\reports\tests\testDebugUnitTest\index.html
    goto :eof
)
echo ✅ 测试通过

echo [3/3] 构建 APK...
call gradlew.bat assembleDebug --build-cache -x lint -x test --parallel >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 构建失败
    goto :eof
)
echo ✅ 构建成功

echo.
echo ========================================
echo ✅ 所有检查通过！可以安全提交代码
echo ========================================
goto :eof

:clean
echo.
echo [清理] 清理构建缓存...
echo ----------------------------------------
call gradlew.bat clean --stop
echo ✅ 清理完成
goto :eof

:stats
echo.
echo [统计] 代码统计...
echo ========================================
echo.

REM 统计 Kotlin 文件
set /a MAIN_FILES=0
set /a TEST_FILES=0
for /r app\src\main %%f in (*.kt) do set /a MAIN_FILES+=1
for /r app\src\test %%f in (*.kt) do set /a TEST_FILES+=1

echo 📊 文件统计:
echo    源代码文件: %MAIN_FILES% 个
echo    测试文件:   %TEST_FILES% 个
echo    总计:       %MAIN_FILES% + %TEST_FILES% 个
echo.
echo 💡 详细统计请运行: scripts\simple_count.sh (需要 Git Bash)
goto :eof

:doc
if "%~2"=="" (
    echo.
    echo [文档] 请指定文档编号或路径
    echo.
    echo 用法:
    echo   scripts\dev.bat doc PRD-00012
    echo   scripts\dev.bat doc "文档/开发文档/PRD/PRD-00012-xxx.md"
    echo.
    goto :eof
)

set "DOC_INPUT=%~2"

REM 如果是编号格式，查找文档
echo %DOC_INPUT% | findstr /r "^[A-Z]*-[0-9]*$" >nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [文档] 查找编号为 %DOC_INPUT% 的文档...
    echo ----------------------------------------
    
    for /r "文档\开发文档" %%f in (%DOC_INPUT%-*.md) do (
        echo 找到: %%f
    )
    echo.
    echo 请指定完整路径运行审查
    goto :eof
)

REM 如果是完整路径
if exist "%DOC_INPUT%" (
    echo.
    echo [文档] 准备审查: %DOC_INPUT%
    echo ----------------------------------------
    powershell -ExecutionPolicy Bypass -File scripts\auto-doc-pipeline.ps1 -DocPath "%DOC_INPUT%"
) else (
    echo.
    echo ❌ 文件不存在: %DOC_INPUT%
)
goto :eof

endlocal
