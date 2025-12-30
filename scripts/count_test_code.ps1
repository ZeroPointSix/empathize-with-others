# 统计项目测试代码行数的PowerShell脚本
# 统计test和androidTest目录下的代码

# 获取脚本所在目录的父目录作为项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# 输出文件路径
$OutputFile = Join-Path $ProjectRoot "test_code_stats.txt"

# 初始化统计结果
$TotalTestLines = 0
$TotalTestFiles = 0
$ModuleTestStats = @{}

# 要统计的模块列表
$Modules = @("app", "data", "domain", "presentation")

# 要统计的文件扩展名
$IncludeExtensions = @("*.kt", "*.java")

# 要包含的目录模式（只统计测试代码）
$IncludePatterns = @("*test*", "*androidTest*")

Write-Host "开始统计项目测试代码行数..." -ForegroundColor Green
Write-Host "项目根目录: $ProjectRoot" -ForegroundColor Cyan

# 遍历每个模块
foreach ($Module in $Modules) {
    $ModulePath = Join-Path $ProjectRoot $Module
    $ModuleTestLines = 0
    $ModuleTestFiles = 0
    $ModuleUnitTestLines = 0
    $ModuleUnitTestFiles = 0
    $ModuleAndroidTestLines = 0
    $ModuleAndroidTestFiles = 0
    
    if (Test-Path $ModulePath) {
        Write-Host "正在统计模块: $Module" -ForegroundColor Yellow
        
        # 获取模块中所有测试目录下的文件
        $TestFiles = Get-ChildItem -Path $ModulePath -Recurse -Include $IncludeExtensions | Where-Object {
            $FilePath = $_.FullName
            # 只包含测试相关的文件
            $ShouldInclude = $false
            foreach ($Pattern in $IncludePatterns) {
                if ($FilePath -like "*$Pattern*") {
                    $ShouldInclude = $true
                    break
                }
            }
            # 排除build目录下的文件
            if ($FilePath -like "*build*") {
                $ShouldInclude = $false
            }
            $ShouldInclude
        }
        
        # 统计每个文件的行数
        foreach ($File in $TestFiles) {
            try {
                $Lines = (Get-Content $File | Measure-Object -Line).Lines
                $ModuleTestLines += $Lines
                $ModuleTestFiles++
                
                # 区分单元测试和Android测试
                if ($File.FullName -like "*androidTest*") {
                    $ModuleAndroidTestLines += $Lines
                    $ModuleAndroidTestFiles++
                } elseif ($File.FullName -like "*test*") {
                    $ModuleUnitTestLines += $Lines
                    $ModuleUnitTestFiles++
                }
            } catch {
                Write-Warning "无法读取文件: $($File.FullName)"
            }
        }
        
        # 保存模块统计结果
        $ModuleTestStats[$Module] = @{
            TotalLines = $ModuleTestLines
            TotalFiles = $ModuleTestFiles
            UnitTestLines = $ModuleUnitTestLines
            UnitTestFiles = $ModuleUnitTestFiles
            AndroidTestLines = $ModuleAndroidTestLines
            AndroidTestFiles = $ModuleAndroidTestFiles
        }
        
        $TotalTestLines += $ModuleTestLines
        $TotalTestFiles += $ModuleTestFiles
        
        Write-Host "  模块 ${Module}: ${ModuleTestFiles} 个测试文件, ${ModuleTestLines} 行测试代码" -ForegroundColor Gray
        Write-Host "    单元测试: ${ModuleUnitTestFiles} 个文件, ${ModuleUnitTestLines} 行" -ForegroundColor Gray
        Write-Host "    Android测试: ${ModuleAndroidTestFiles} 个文件, ${ModuleAndroidTestLines} 行" -ForegroundColor Gray
    } else {
        Write-Warning "模块路径不存在: $ModulePath"
    }
}

# 读取生产代码统计结果（如果存在）
$ProductionStatsFile = Join-Path $ProjectRoot "production_code_stats.txt"
$ProductionLines = 0
$ProductionFiles = 0
$ProductionModuleStats = @{}

if (Test-Path $ProductionStatsFile) {
    Write-Host "读取生产代码统计结果..." -ForegroundColor Cyan
    $ProductionContent = Get-Content $ProductionStatsFile -Raw
    
    # 解析生产代码统计结果
    if ($ProductionContent -match "总计: (\d+) 个文件, (\d+) 行代码") {
        $ProductionFiles = [int]$matches[1]
        $ProductionLines = [int]$matches[2]
    }
    
    # 尝试解析各模块的生产代码统计
    foreach ($Module in $Modules) {
        $Pattern = "  ${Module}: (\d+) 个文件, (\d+) 行代码"
        if ($ProductionContent -match $Pattern) {
            $ProductionModuleStats[$Module] = @{
                Files = [int]$matches[1]
                Lines = [int]$matches[2]
            }
        }
    }
} else {
    Write-Warning "未找到生产代码统计文件，请先运行 count_production_code.ps1"
}

# 生成统计报告
$Report = @"
========================================
项目测试代码统计报告
========================================
统计时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
项目根目录: $ProjectRoot

模块测试代码统计:
"@

foreach ($Module in $Modules) {
    if ($ModuleTestStats.ContainsKey($Module)) {
        $Stats = $ModuleTestStats[$Module]
        $Report += "`n  ${Module}: $($Stats.TotalFiles) 个测试文件, $($Stats.TotalLines) 行测试代码"
        $Report += "`n    单元测试: $($Stats.UnitTestFiles) 个文件, $($Stats.UnitTestLines) 行"
        $Report += "`n    Android测试: $($Stats.AndroidTestFiles) 个文件, $($Stats.AndroidTestLines) 行"
    }
}

$Report += @"

测试代码总计: $TotalTestFiles 个文件, $TotalTestLines 行代码
"@

# 添加测试代码与生产代码的比例分析
if ($ProductionLines -gt 0) {
    $TestToProductionRatio = [math]::Round(($TotalTestLines / $ProductionLines) * 100, 2)
    $TestToProductionFileRatio = [math]::Round(($TotalTestFiles / $ProductionFiles) * 100, 2)
    
    $Report += @"
生产代码总计: $ProductionFiles 个文件, $ProductionLines 行代码
测试代码与生产代码比例:
  文件数比例: $TestToProductionFileRatio% (测试文件/生产文件)
  代码行数比例: $TestToProductionRatio% (测试代码/生产代码)
"@
    
    # 添加各模块的比例分析
    $Report += "`n各模块测试代码与生产代码比例:`n"
    foreach ($Module in $Modules) {
        if ($ModuleTestStats.ContainsKey($Module) -and $ProductionModuleStats.ContainsKey($Module)) {
            $TestStats = $ModuleTestStats[$Module]
            $ProdStats = $ProductionModuleStats[$Module]
            
            if ($ProdStats.Lines -gt 0) {
                $ModuleRatio = [math]::Round(($TestStats.TotalLines / $ProdStats.Lines) * 100, 2)
                $ModuleFileRatio = [math]::Round(($TestStats.TotalFiles / $ProdStats.Files) * 100, 2)
                $Report += "  ${Module}: 代码行数比例 ${ModuleRatio}%, 文件数比例 ${ModuleFileRatio}%`n"
            }
        }
    }
}

$Report += @"

========================================
详细测试文件列表:
========================================

"@

# 添加每个模块的详细测试文件列表
foreach ($Module in $Modules) {
    if ($ModuleTestStats.ContainsKey($Module)) {
        $Report += "`n=== ${Module} 模块测试代码 ===`n"
        $ModulePath = Join-Path $ProjectRoot $Module
        
        $TestFiles = Get-ChildItem -Path $ModulePath -Recurse -Include $IncludeExtensions | Where-Object {
            $FilePath = $_.FullName
            $ShouldInclude = $false
            foreach ($Pattern in $IncludePatterns) {
                if ($FilePath -like "*$Pattern*") {
                    $ShouldInclude = $true
                    break
                }
            }
            if ($FilePath -like "*build*") {
                $ShouldInclude = $false
            }
            $ShouldInclude
        } | Sort-Object FullName
        
        foreach ($File in $TestFiles) {
            try {
                $Lines = (Get-Content $File | Measure-Object -Line).Lines
                $RelativePath = $File.FullName.Replace($ProjectRoot, "").TrimStart("\", "/")
                $TestType = "单元测试"
                if ($File.FullName -like "*androidTest*") {
                    $TestType = "Android测试"
                }
                $Report += "  ${Lines} 行 [${TestType}]: ${RelativePath}`n"
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
Write-Host "测试代码统计完成!" -ForegroundColor Green
Write-Host "总计: $TotalTestFiles 个测试文件, $TotalTestLines 行测试代码" -ForegroundColor Green

if ($ProductionLines -gt 0) {
    $TestToProductionRatio = [math]::Round(($TotalTestLines / $ProductionLines) * 100, 2)
    Write-Host "测试代码与生产代码比例: ${TestToProductionRatio}%" -ForegroundColor Yellow
}

Write-Host "详细报告已保存到: $OutputFile" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Green

# 显示模块统计表格
Write-Host "`n模块测试代码统计表格:" -ForegroundColor Yellow
Write-Host "+--------------+------------+------------+------------+------------+" -ForegroundColor Gray
Write-Host "| 模块名称     | 测试文件数 | 测试代码行 | 单元测试数 | Android测试数|" -ForegroundColor Gray
Write-Host "+--------------+------------+------------+------------+------------+" -ForegroundColor Gray

foreach ($Module in $Modules) {
    if ($ModuleTestStats.ContainsKey($Module)) {
        $Stats = $ModuleTestStats[$Module]
        $ModuleName = $Module.PadRight(12)
        $TestFileCount = $Stats.TotalFiles.ToString().PadRight(10)
        $TestLineCount = $Stats.TotalLines.ToString().PadRight(10)
        $UnitTestCount = $Stats.UnitTestFiles.ToString().PadRight(10)
        $AndroidTestCount = $Stats.AndroidTestFiles.ToString().PadRight(10)
        Write-Host "| ${ModuleName} | ${TestFileCount} | ${TestLineCount} | ${UnitTestCount} | ${AndroidTestCount} |" -ForegroundColor White
    }
}

Write-Host "+--------------+------------+------------+------------+------------+" -ForegroundColor Gray
$TotalTestFilesStr = $TotalTestFiles.ToString().PadRight(10)
$TotalTestLinesStr = $TotalTestLines.ToString().PadRight(10)
$TotalUnitTestFiles = ($ModuleTestStats.Values | ForEach-Object { $_.UnitTestFiles } | Measure-Object -Sum).Sum.ToString().PadRight(10)
$TotalAndroidTestFiles = ($ModuleTestStats.Values | ForEach-Object { $_.AndroidTestFiles } | Measure-Object -Sum).Sum.ToString().PadRight(10)
Write-Host "| 总计         | ${TotalTestFilesStr} | ${TotalTestLinesStr} | ${TotalUnitTestFiles} | ${TotalAndroidTestFiles} |" -ForegroundColor Green
Write-Host "+--------------+------------+------------+------------+------------+" -ForegroundColor Gray

# 显示测试代码与生产代码的比例分析表格
if ($ProductionLines -gt 0) {
    Write-Host "`n测试代码与生产代码比例分析:" -ForegroundColor Yellow
    Write-Host "+--------------+--------------+--------------+--------------+--------------+" -ForegroundColor Gray
    Write-Host "| 模块名称     | 生产代码行数 | 测试代码行数 | 代码行数比例 | 文件数比例   |" -ForegroundColor Gray
    Write-Host "+--------------+--------------+--------------+--------------+--------------+" -ForegroundColor Gray
    
    foreach ($Module in $Modules) {
        if ($ModuleTestStats.ContainsKey($Module) -and $ProductionModuleStats.ContainsKey($Module)) {
            $TestStats = $ModuleTestStats[$Module]
            $ProdStats = $ProductionModuleStats[$Module]
            
            $ModuleName = $Module.PadRight(12)
            $ProdLineCount = $ProdStats.Lines.ToString().PadRight(12)
            $TestLineCount = $TestStats.TotalLines.ToString().PadRight(12)
            
            if ($ProdStats.Lines -gt 0) {
                $ModuleRatio = [math]::Round(($TestStats.TotalLines / $ProdStats.Lines) * 100, 2)
                $ModuleFileRatio = [math]::Round(($TestStats.TotalFiles / $ProdStats.Files) * 100, 2)
                $RatioStr = "${ModuleRatio}%".PadRight(12)
                $FileRatioStr = "${ModuleFileRatio}%".PadRight(12)
                Write-Host "| ${ModuleName} | ${ProdLineCount} | ${TestLineCount} | ${RatioStr} | ${FileRatioStr} |" -ForegroundColor White
            } else {
                $RatioStr = "N/A".PadRight(12)
                $FileRatioStr = "N/A".PadRight(12)
                Write-Host "| ${ModuleName} | ${ProdLineCount} | ${TestLineCount} | ${RatioStr} | ${FileRatioStr} |" -ForegroundColor White
            }
        }
    }
    
    Write-Host "+--------------+--------------+--------------+--------------+--------------+" -ForegroundColor Gray
    $TotalProdLinesStr = $ProductionLines.ToString().PadRight(12)
    $TotalTestLinesStr = $TotalTestLines.ToString().PadRight(12)
    $TotalRatio = [math]::Round(($TotalTestLines / $ProductionLines) * 100, 2)
    $TotalFileRatio = [math]::Round(($TotalTestFiles / $ProductionFiles) * 100, 2)
    $TotalRatioStr = "${TotalRatio}%".PadRight(12)
    $TotalFileRatioStr = "${TotalFileRatio}%".PadRight(12)
    Write-Host "| 总计         | ${TotalProdLinesStr} | ${TotalTestLinesStr} | ${TotalRatioStr} | ${TotalFileRatioStr} |" -ForegroundColor Green
    Write-Host "+--------------+--------------+--------------+--------------+--------------+" -ForegroundColor Gray
}

return $TotalTestLines