#!/usr/bin/env pwsh
# ============================================================
# 钩子脚本：开始任务前读取所有Rules文档
# ============================================================
# 触发时机：用户提交提示词后、Claude处理前
# 功能：读取Rules目录下所有.md文件并输出摘要

$ErrorActionPreference = "Continue"

function Read-RulesDocuments {
    param(
        [string]$RulesPath = "$PSScriptRoot\..\Rules"
    )

    $output = @()
    $output += "=" * 60
    $output += "【钩子触发】开始任务前 - Rules文档读取"
    $output += "=" * 60
    $output += ""

    # 检查Rules目录是否存在
    if (-not (Test-Path $RulesPath)) {
        $output += "⚠️ Rules目录不存在: $RulesPath"
        return $output
    }

    # 获取所有.md文件
    $mdFiles = Get-ChildItem -Path $RulesPath -Filter "*.md" -File -ErrorAction SilentlyContinue | Sort-Object Name

    if ($mdFiles.Count -eq 0) {
        $output += "⚠️ Rules目录中没有找到.md文件"
        return $output
    }

    $output += "📚 发现 $($mdFiles.Count) 个规则文档:"
    $output += ""

    foreach ($file in $mdFiles) {
        try {
            $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
            if ($content) {
                # 获取前3行作为摘要
                $lines = $content -split "`n" | Where-Object { $_ -match '\S' } | Select-Object -First 3
                $summary = $lines -join " | "

                $output += "📄 $($file.Name)"
                $output += "   摘要: $summary"
                $output += ""
            }
        } catch {
            $output += "⚠️ 读取 $($file.Name) 时出错: $_"
        }
    }

    # 读取workspace-rules检查是否有进行中的任务
    $workspaceRulesPath = Join-Path $RulesPath "workspace-rules.md"
    if (Test-Path $workspaceRulesPath) {
        $output += "---"
        $output += "🔍 检查 workspace-rules.md 中的任务状态:"
        $output += ""
        try {
            $workspaceContent = Get-Content -Path $workspaceRulesPath -Raw -ErrorAction SilentlyContinue
            if ($workspaceContent) {
                # 查找进行中的任务标记
                $inProgressTasks = $workspaceContent -split "`n" | Where-Object { $_ -match '进行中|执行中|ING|in progress' }
                if ($inProgressTasks) {
                    $output += "⚠️ 发现进行中的任务:"
                    foreach ($task in $inProgressTasks) {
                        $output += "  - $task"
                    }
                    $output += ""
                    $output += "💡 建议：暂停当前操作，询问用户是否要继续新任务"
                } else {
                    $output += "✅ workspace-rules.md 中没有进行中的任务"
                }
            }
        } catch {
            $output += "⚠️ 读取 workspace-rules.md 时出错: $_"
        }
    }

    $output += ""
    $output += "=" * 60
    $output += "【钩子完成】Rules文档读取完毕，准备开始任务"
    $output += "=" * 60

    return $output
}

# 执行读取
$result = Read-RulesDocuments
$result | ForEach-Object { Write-Host $_ }

# 输出到文件供Claude参考
$outputFile = "$PSScriptRoot\..\logs\hook-read-rules.log"
$null = New-Item -ItemType Directory -Force -Path (Split-Path $outputFile)
$result | Out-File -FilePath $outputFile -Encoding UTF8

Write-Host ""
Write-Host "📁 详细日志已保存到: $outputFile"
