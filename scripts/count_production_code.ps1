# 统计项目生产代码行数的PowerShell脚本
# 排除测试代码和非核心文件

# 获取脚本所在目录的父目录作为项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# 输出文件路径
$OutputFile = Join-Path $ProjectRoot "production_code_stats.txt"

# 初始化统计结果
$TotalLines = 0
$TotalFiles = 0
$ModuleStats = @{}

# 要统计的模块列表
$Modules = @("app", "data", "domain", "presentation")

# 要统计的文件扩展名
$IncludeExtensions = @("*.kt", "*.java")

# 要排除的目录模式
$ExcludePatterns = @(
    "*test*", "*androidTest*", "*build*", "*.git*", 
    "*generated*", "*intermediates*", "*outputs*"
)

Write-Host "开始统计项目生产代码行数..." -ForegroundColor Green
Write-Host "项目根目录: $ProjectRoot" -ForegroundColor Cyan

# 遍历每个模块
foreach ($Module in $Modules) {
    $ModulePath = Join-Path $ProjectRoot $Module
    $ModuleLines = 0
    $ModuleFiles = 0
    
    if (Test-Path $ModulePath) {
        Write-Host "正在统计模块: $Module" -ForegroundColor Yellow
        
        # 获取模块中所有符合条件的文件
        $Files = Get-ChildItem -Path $ModulePath -Recurse -Include $IncludeExtensions | Where-Object {
            $FilePath = $_.FullName
            # 排除包含测试或构建相关路径的文件
            $ShouldExclude = $false
            foreach ($Pattern in $ExcludePatterns) {
                if ($FilePath -like "*$Pattern*") {
                    $ShouldExclude = $true
                    break
                }
            }
            -not $ShouldExclude
        }
        
        # 统计每个文件的行数
        foreach ($File in $Files) {
            try {
                $Lines = (Get-Content $File | Measure-Object -Line).Lines
                $ModuleLines += $Lines
                $ModuleFiles++
            } catch {
                Write-Warning "无法读取文件: $($File.FullName)"
            }
        }
        
        # 保存模块统计结果
        $ModuleStats[$Module] = @{
            Lines = $ModuleLines
            Files = $ModuleFiles
        }
        
        $TotalLines += $ModuleLines
        $TotalFiles += $ModuleFiles
        
        Write-Host "  模块 ${Module}: ${ModuleFiles} 个文件, ${ModuleLines} 行代码" -ForegroundColor Gray
    } else {
        Write-Warning "模块路径不存在: $ModulePath"
    }
}

# 生成统计报告
$Report = @"
========================================
项目生产代码统计报告
========================================
统计时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
项目根目录: $ProjectRoot

模块统计:
"@

foreach ($Module in $Modules) {
    if ($ModuleStats.ContainsKey($Module)) {
        $Stats = $ModuleStats[$Module]
        $Report += "`n  ${Module}: $($Stats.Files) 个文件, $($Stats.Lines) 行代码"
    }
}

$Report += @"

总计: $TotalFiles 个文件, $TotalLines 行代码

========================================
详细统计:
========================================

"@

# 添加每个模块的详细文件列表
foreach ($Module in $Modules) {
    if ($ModuleStats.ContainsKey($Module)) {
        $Report += "`n=== ${Module} 模块 ===`n"
        $ModulePath = Join-Path $ProjectRoot $Module
        
        $Files = Get-ChildItem -Path $ModulePath -Recurse -Include $IncludeExtensions | Where-Object {
            $FilePath = $_.FullName
            $ShouldExclude = $false
            foreach ($Pattern in $ExcludePatterns) {
                if ($FilePath -like "*$Pattern*") {
                    $ShouldExclude = $true
                    break
                }
            }
            -not $ShouldExclude
        } | Sort-Object FullName
        
        foreach ($File in $Files) {
            try {
                $Lines = (Get-Content $File | Measure-Object -Line).Lines
                $RelativePath = $File.FullName.Replace($ProjectRoot, "").TrimStart("\", "/")
                $Report += "  ${Lines} 行: ${RelativePath}`n"
            } catch {
                $Report += "  读取失败: $($File.FullName)`n"
            }
        }
    }
}

# 保存报告到文件
$Report | Out-File -FilePath $OutputFile -Encoding UTF8

# 在控制台显示摘要
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "统计完成!" -ForegroundColor Green
Write-Host "总计: $TotalFiles 个生产代码文件, $TotalLines 行代码" -ForegroundColor Green
Write-Host "详细报告已保存到: $OutputFile" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Green

# 显示模块统计表格
Write-Host "`n模块统计表格:" -ForegroundColor Yellow
Write-Host "+--------------+----------+----------+" -ForegroundColor Gray
Write-Host "| 模块名称     | 文件数   | 代码行数 |" -ForegroundColor Gray
Write-Host "+--------------+----------+----------+" -ForegroundColor Gray

foreach ($Module in $Modules) {
    if ($ModuleStats.ContainsKey($Module)) {
        $Stats = $ModuleStats[$Module]
        $ModuleName = $Module.PadRight(12)
        $FileCount = $Stats.Files.ToString().PadRight(8)
        $LineCount = $Stats.Lines.ToString().PadRight(8)
        Write-Host "| ${ModuleName} | ${FileCount} | ${LineCount} |" -ForegroundColor White
    }
}

Write-Host "+--------------+----------+----------+" -ForegroundColor Gray
$TotalFilesStr = $TotalFiles.ToString().PadRight(8)
$TotalLinesStr = $TotalLines.ToString().PadRight(8)
Write-Host "| 总计         | ${TotalFilesStr} | ${TotalLinesStr} |" -ForegroundColor Green
Write-Host "+--------------+----------+----------+" -ForegroundColor Gray

return $TotalLines