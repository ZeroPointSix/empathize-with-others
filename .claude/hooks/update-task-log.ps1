#!/usr/bin/env pwsh
# ============================================================
# 钩子脚本：任务完成后更新任务日志
# ============================================================
# 触发时机：每次工具调用完成或代码编写后
# 功能：记录任务完成情况并更新日志

$ErrorActionPreference = "Continue"

param(
    [string]$TaskDescription = "未记录的任务",
    [string]$Status = "completed",
    [string]$FilesChanged = "",
    [string]$Notes = ""
)

function Update-TaskLog {
    param(
        [string]$TaskDescription,
        [string]$Status,
        [string]$FilesChanged,
        [string]$Notes
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logPath = "$PSScriptRoot\..\logs\task-log.md"

    # 确保logs目录存在
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $logPath)

    # 构建日志条目
    $logEntry = @()
    $logEntry += "## 任务记录 - $timestamp"
    $logEntry += ""
    $logEntry += "**状态**: $Status"
    $logEntry += "**任务**: $TaskDescription"
    $logEntry += "**时间**: $timestamp"

    if ($FilesChanged) {
        $logEntry += ""
        $logEntry += "**变更文件**:"
        $files = $FilesChanged -split ','
        foreach ($file in $files) {
            if ($file.Trim()) {
                $logEntry += "  - $($file.Trim())"
            }
        }
    }

    if ($Notes) {
        $logEntry += ""
        $logEntry += "**备注**: $Notes"
    }

    $logEntry += ""
    $logEntry += "---"
    $logEntry += ""

    # 写入日志
    $logEntry | Out-File -FilePath $logPath -Encoding UTF8 -Append

    return $logEntry
}

# 如果没有提供任务描述，从环境变量读取
if (-not $TaskDescription -or $TaskDescription -eq "未记录的任务") {
    $TaskDescription = $env:CLAUDE_TASK_DESCRIPTION ?? "工具执行完成"
}

if (-not $FilesChanged) {
    $FilesChanged = $env:CLAUDE_FILES_CHANGED ?? ""
}

if (-not $Notes) {
    $Notes = $env:CLAUDE_TASK_NOTES ?? ""
}

# 执行日志更新
$output = @()
$output += "=" * 60
$output += "【钩子触发】任务完成 - 更新任务日志"
$output += "=" * 60
$output += ""
$output += "📝 任务: $TaskDescription"
$output += "📊 状态: $Status"
if ($FilesChanged) {
    $output += "📁 变更: $FilesChanged"
}
if ($Notes) {
    $output += "📋 备注: $Notes"
}
$output += ""

# 调用更新函数
$result = Update-TaskLog -TaskDescription $TaskDescription -Status $Status -FilesChanged $FilesChanged -Notes $Notes

$output += "✅ 任务日志已更新"
$output += ""
$output += "=" * 60

$output | ForEach-Object { Write-Host $_ }

# 输出日志路径
$logPath = "$PSScriptRoot\..\logs\task-log.md"
Write-Host ""
Write-Host "📁 任务日志: $logPath"
