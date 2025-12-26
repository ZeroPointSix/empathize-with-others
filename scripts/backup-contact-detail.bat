@echo off
REM ============================================================
REM TD-00020 联系人详情页UI优化 - 代码备份脚本
REM 
REM 功能: 备份现有联系人详情页相关代码
REM 用途: 在Phase 8页面重写前执行，确保可回滚
REM 
REM @see TDD-00020 13.4 代码备份策略
REM ============================================================

setlocal enabledelayedexpansion

REM 设置变量
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_DIR=backup\contact-detail-%TIMESTAMP%
set PRESENTATION_DIR=presentation\src\main\kotlin\com\empathy\ai\presentation

echo ============================================================
echo TD-00020 联系人详情页UI优化 - 代码备份脚本
echo ============================================================
echo.
echo 备份时间: %date% %time%
echo 备份目录: %BACKUP_DIR%
echo.

REM 创建备份目录
echo [1/6] 创建备份目录...
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%BACKUP_DIR%\overview" mkdir "%BACKUP_DIR%\overview"
if not exist "%BACKUP_DIR%\factstream" mkdir "%BACKUP_DIR%\factstream"
if not exist "%BACKUP_DIR%\persona" mkdir "%BACKUP_DIR%\persona"
if not exist "%BACKUP_DIR%\vault" mkdir "%BACKUP_DIR%\vault"
if not exist "%BACKUP_DIR%\contact" mkdir "%BACKUP_DIR%\contact"
echo    完成!
echo.

REM 备份概览页
echo [2/6] 备份概览页 (OverviewTab)...
set OVERVIEW_SRC=%PRESENTATION_DIR%\ui\screen\contact\overview
if exist "%OVERVIEW_SRC%\OverviewTab.kt" (
    copy "%OVERVIEW_SRC%\OverviewTab.kt" "%BACKUP_DIR%\overview\OverviewTabLegacy.kt" >nul
    echo    已备份: OverviewTab.kt -^> OverviewTabLegacy.kt
) else (
    echo    警告: OverviewTab.kt 不存在
)
if exist "%OVERVIEW_SRC%\OverviewUiState.kt" (
    copy "%OVERVIEW_SRC%\OverviewUiState.kt" "%BACKUP_DIR%\overview\OverviewUiStateLegacy.kt" >nul
    echo    已备份: OverviewUiState.kt -^> OverviewUiStateLegacy.kt
)
echo.

REM 备份事实流页
echo [3/6] 备份事实流页 (FactStreamTab)...
set FACTSTREAM_SRC=%PRESENTATION_DIR%\ui\screen\contact\factstream
if exist "%FACTSTREAM_SRC%\FactStreamTab.kt" (
    copy "%FACTSTREAM_SRC%\FactStreamTab.kt" "%BACKUP_DIR%\factstream\FactStreamTabLegacy.kt" >nul
    echo    已备份: FactStreamTab.kt -^> FactStreamTabLegacy.kt
) else (
    echo    警告: FactStreamTab.kt 不存在
)
if exist "%FACTSTREAM_SRC%\FactStreamUiState.kt" (
    copy "%FACTSTREAM_SRC%\FactStreamUiState.kt" "%BACKUP_DIR%\factstream\FactStreamUiStateLegacy.kt" >nul
    echo    已备份: FactStreamUiState.kt -^> FactStreamUiStateLegacy.kt
)
echo.

REM 备份画像库页
echo [4/6] 备份画像库页 (PersonaTab)...
set PERSONA_SRC=%PRESENTATION_DIR%\ui\screen\contact\persona
if exist "%PERSONA_SRC%\PersonaTab.kt" (
    copy "%PERSONA_SRC%\PersonaTab.kt" "%BACKUP_DIR%\persona\PersonaTabLegacy.kt" >nul
    echo    已备份: PersonaTab.kt -^> PersonaTabLegacy.kt
) else (
    echo    警告: PersonaTab.kt 不存在
)
if exist "%PERSONA_SRC%\PersonaUiState.kt" (
    copy "%PERSONA_SRC%\PersonaUiState.kt" "%BACKUP_DIR%\persona\PersonaUiStateLegacy.kt" >nul
    echo    已备份: PersonaUiState.kt -^> PersonaUiStateLegacy.kt
)
echo.

REM 备份资料库页
echo [5/6] 备份资料库页 (DataVaultTab)...
set VAULT_SRC=%PRESENTATION_DIR%\ui\screen\contact\vault
if exist "%VAULT_SRC%\DataVaultTab.kt" (
    copy "%VAULT_SRC%\DataVaultTab.kt" "%BACKUP_DIR%\vault\DataVaultTabLegacy.kt" >nul
    echo    已备份: DataVaultTab.kt -^> DataVaultTabLegacy.kt
) else (
    echo    警告: DataVaultTab.kt 不存在
)
if exist "%VAULT_SRC%\DataVaultUiState.kt" (
    copy "%VAULT_SRC%\DataVaultUiState.kt" "%BACKUP_DIR%\vault\DataVaultUiStateLegacy.kt" >nul
    echo    已备份: DataVaultUiState.kt -^> DataVaultUiStateLegacy.kt
)
echo.

REM 备份新建联系人页
echo [6/6] 备份新建联系人页 (CreateContactScreen)...
set CONTACT_SRC=%PRESENTATION_DIR%\ui\screen\contact
if exist "%CONTACT_SRC%\CreateContactScreen.kt" (
    copy "%CONTACT_SRC%\CreateContactScreen.kt" "%BACKUP_DIR%\contact\CreateContactScreenLegacy.kt" >nul
    echo    已备份: CreateContactScreen.kt -^> CreateContactScreenLegacy.kt
) else (
    echo    警告: CreateContactScreen.kt 不存在
)
echo.

REM 记录Git commit hash
echo 记录Git信息...
for /f "tokens=*" %%i in ('git rev-parse HEAD 2^>nul') do set GIT_HASH=%%i
if defined GIT_HASH (
    echo Git Commit: %GIT_HASH% > "%BACKUP_DIR%\backup-info.txt"
    echo Backup Time: %date% %time% >> "%BACKUP_DIR%\backup-info.txt"
    echo. >> "%BACKUP_DIR%\backup-info.txt"
    echo Backed up files: >> "%BACKUP_DIR%\backup-info.txt"
    dir /b /s "%BACKUP_DIR%\*.kt" >> "%BACKUP_DIR%\backup-info.txt" 2>nul
    echo    Git Commit Hash: %GIT_HASH%
) else (
    echo    警告: 无法获取Git commit hash
)
echo.

REM 验证备份
echo ============================================================
echo 备份验证
echo ============================================================
set FILE_COUNT=0
for /r "%BACKUP_DIR%" %%f in (*.kt) do set /a FILE_COUNT+=1
echo 备份文件数量: %FILE_COUNT%
echo 备份目录位置: %BACKUP_DIR%
echo.

if %FILE_COUNT% GTR 0 (
    echo [成功] 备份完成!
    echo.
    echo 如需回滚，请执行以下步骤:
    echo 1. 将 %BACKUP_DIR% 中的文件复制回原位置
    echo 2. 移除 "Legacy" 后缀
    echo 3. 重新构建项目
) else (
    echo [警告] 未找到需要备份的文件
    echo 请确认源文件路径是否正确
)

echo.
echo ============================================================
echo 备份脚本执行完毕
echo ============================================================

endlocal
pause
