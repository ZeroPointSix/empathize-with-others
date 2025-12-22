@echo off
REM 文档审查脚本 - 配合 Claude Code 使用
REM 用法: scripts\doc-review.bat [文档路径]
REM
REM 此脚本用于触发 Claude Code 的文档审查流程
REM 审查结果会保存到 文档/开发文档/DR/ 目录

setlocal enabledelayedexpansion

echo ========================================
echo 文档审查脚本
echo ========================================
echo.

if "%~1"=="" (
    echo 用法: scripts\doc-review.bat [文档路径]
    echo.
    echo 示例:
    echo   scripts\doc-review.bat "文档/开发文档/PRD/PRD-00012-事实流内容编辑功能需求.md"
    echo   scripts\doc-review.bat "文档/开发文档/TDD/TDD-00012-事实流内容编辑功能技术设计.md"
    echo.
    echo 支持的文档类型:
    echo   PRD  - 产品需求文档
    echo   FD   - 功能设计文档
    echo   TDD  - 技术设计文档
    echo   TD   - 任务清单
    echo   IMPL - 实现进度
    echo   BUG  - 问题分析
    echo.
    goto :eof
)

set "DOC_PATH=%~1"

REM 检查文件是否存在
if not exist "%DOC_PATH%" (
    echo ❌ 文件不存在: %DOC_PATH%
    goto :eof
)

REM 提取文档信息
for %%f in ("%DOC_PATH%") do (
    set "DOC_NAME=%%~nf"
    set "DOC_DIR=%%~dpf"
)

REM 解析文档类型和编号
for /f "tokens=1,2 delims=-" %%a in ("%DOC_NAME%") do (
    set "DOC_TYPE=%%a"
    set "DOC_NUM=%%b"
)

echo 📄 文档信息:
echo    路径: %DOC_PATH%
echo    类型: %DOC_TYPE%
echo    编号: %DOC_NUM%
echo.

REM 查找关联文档
echo 🔍 查找关联文档...
set "RELATED_DOCS="

if exist "文档\开发文档\PRD\PRD-%DOC_NUM%-*.md" (
    for %%f in ("文档\开发文档\PRD\PRD-%DOC_NUM%-*.md") do (
        echo    找到: %%f
        set "RELATED_DOCS=!RELATED_DOCS! %%f"
    )
)

if exist "文档\开发文档\FD\FD-%DOC_NUM%-*.md" (
    for %%f in ("文档\开发文档\FD\FD-%DOC_NUM%-*.md") do (
        echo    找到: %%f
        set "RELATED_DOCS=!RELATED_DOCS! %%f"
    )
)

if exist "文档\开发文档\TDD\TDD-%DOC_NUM%-*.md" (
    for %%f in ("文档\开发文档\TDD\TDD-%DOC_NUM%-*.md") do (
        echo    找到: %%f
        set "RELATED_DOCS=!RELATED_DOCS! %%f"
    )
)

if exist "文档\开发文档\TD\TD-%DOC_NUM%-*.md" (
    for %%f in ("文档\开发文档\TD\TD-%DOC_NUM%-*.md") do (
        echo    找到: %%f
        set "RELATED_DOCS=!RELATED_DOCS! %%f"
    )
)

echo.
echo ========================================
echo 准备审查
echo ========================================
echo.
echo 请在 Claude Code 中执行以下命令:
echo.
echo   /DocReview %DOC_PATH%
echo.
echo 或者直接告诉 Claude Code:
echo.
echo   "请审查文档 %DOC_PATH%，并生成 DR 报告"
echo.
echo ========================================

endlocal
