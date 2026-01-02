@echo off
REM ============================================================
REM 版本发布脚本 (Windows)
REM 
REM 用法:
REM   release.bat                    - 使用默认阶段(dev)发布
REM   release.bat --stage=beta       - 指定发布阶段
REM   release.bat --dry-run          - 预览模式
REM   release.bat --force            - 强制更新
REM   release.bat --help             - 显示帮助
REM
REM @see TDD-00024 图标和版本号自动更新技术设计
REM ============================================================

setlocal enabledelayedexpansion

REM 默认参数
set STAGE=dev
set DRY_RUN=
set FORCE=
set SHOW_HELP=

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :end_parse
if "%~1"=="--help" (
    set SHOW_HELP=1
    goto :end_parse
)
if "%~1"=="-h" (
    set SHOW_HELP=1
    goto :end_parse
)
if "%~1"=="--dry-run" (
    set DRY_RUN=--dry-run
    shift
    goto :parse_args
)
if "%~1"=="--force" (
    set FORCE=--force
    shift
    goto :parse_args
)

REM 解析 --stage=xxx 格式
echo %~1 | findstr /r "^--stage=" >nul
if %errorlevel%==0 (
    for /f "tokens=2 delims==" %%a in ("%~1") do set STAGE=%%a
    shift
    goto :parse_args
)

shift
goto :parse_args

:end_parse

REM 显示帮助
if defined SHOW_HELP (
    echo.
    echo 版本发布脚本 - 自动更新版本号和图标
    echo.
    echo 用法:
    echo   release.bat [选项]
    echo.
    echo 选项:
    echo   --stage=STAGE    发布阶段: dev, test, beta, production (默认: dev)
    echo   --dry-run        预览模式，不实际执行更新
    echo   --force          强制更新，忽略未提交的更改
    echo   --help, -h       显示此帮助信息
    echo.
    echo 示例:
    echo   release.bat                       使用默认阶段(dev)发布
    echo   release.bat --stage=beta          发布beta版本
    echo   release.bat --stage=production    发布正式版本
    echo   release.bat --dry-run             预览版本变更
    echo.
    exit /b 0
)

REM 验证发布阶段
if not "%STAGE%"=="dev" if not "%STAGE%"=="test" if not "%STAGE%"=="beta" if not "%STAGE%"=="production" (
    echo [错误] 无效的发布阶段: %STAGE%
    echo 有效值: dev, test, beta, production
    exit /b 1
)

REM 显示配置
echo.
echo ============================================================
echo                    版本发布脚本
echo ============================================================
echo.
echo 发布阶段: %STAGE%
if defined DRY_RUN echo 预览模式: 是
if defined FORCE echo 强制模式: 是
echo.

REM 确认执行（非预览模式）
if not defined DRY_RUN (
    echo [警告] 此操作将更新版本号和图标文件
    echo.
    set /p CONFIRM="确认继续? (y/N): "
    if /i not "!CONFIRM!"=="y" (
        echo 操作已取消
        exit /b 0
    )
    echo.
)

REM 检查Git状态
echo [1/4] 检查Git状态...
git status --porcelain >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 无法获取Git状态，请确保在Git仓库中运行
    exit /b 1
)

REM 检查未提交的更改
if not defined FORCE (
    for /f %%i in ('git status --porcelain') do (
        echo [警告] 存在未提交的更改
        echo 请先提交或暂存更改，或使用 --force 参数强制执行
        exit /b 1
    )
)

REM 分析Git提交
echo [2/4] 分析Git提交...
call gradlew.bat analyzeCommits --no-daemon
if %errorlevel% neq 0 (
    echo [错误] 分析Git提交失败
    exit /b 1
)

REM 构建参数
set GRADLE_PARAMS=--stage=%STAGE% %DRY_RUN% %FORCE%

REM 执行版本更新
echo.
echo [3/4] 更新版本号和图标...
call gradlew.bat updateVersionAndIcon %GRADLE_PARAMS% --no-daemon
if %errorlevel% neq 0 (
    echo [错误] 版本更新失败
    exit /b 1
)

REM 显示结果
echo.
echo [4/4] 显示当前版本...
call gradlew.bat showCurrentVersion --no-daemon

echo.
echo ============================================================
if defined DRY_RUN (
    echo                    预览完成
) else (
    echo                    发布完成!
)
echo ============================================================
echo.

REM 提示后续操作
if not defined DRY_RUN (
    echo 后续操作:
    echo   1. 检查更改: git diff
    echo   2. 提交更改: git add -A ^&^& git commit -m "chore: 更新版本号"
    echo   3. 推送更改: git push
    if "%STAGE%"=="production" (
        echo   4. 创建标签: git tag -a v版本号 -m "Release v版本号"
        echo   5. 推送标签: git push --tags
    )
    echo.
)

exit /b 0
