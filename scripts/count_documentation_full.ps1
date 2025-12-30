# 项目文档完整统计脚本
# 统计项目中所有文档的行数、字数等信息

# 统计结果累加器
$totalLines = 0
$totalChars = 0
$totalWords = 0
$fileCount = 0
$files = @()
$excludedCount = 0

Write-Host "开始全面扫描项目文档..." -ForegroundColor Cyan

# 定义排除规则
$excludePatterns = @(
    '历史文档',
    'temp_',
    '过期',
    'archive',
    'extracted',
    'META-INF',
    '\.git',
    'build\/',
    '\.gradle',
    'node_modules'
)

# 定义必须包含的特殊文档
$specialFiles = @(
    'CLAUDE.md',
    'WORKSPACE.md',
    'README.md'
)

# 1. 扫描所有根目录和子目录的 md 文件
Write-Host ""
Write-Host "[1/4] 扫描所有 .md 文件..." -ForegroundColor Yellow

$allMdFiles = Get-ChildItem -Path . -Filter *.md -Recurse -File -ErrorAction SilentlyContinue
Write-Host "  发现 $($allMdFiles.Count) 个 .md 文件" -ForegroundColor Gray

foreach ($file in $allMdFiles) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
    $shouldInclude = $true

    # 检查是否应该排除
    foreach ($pattern in $excludePatterns) {
        if ($relativePath -match $pattern) {
            $shouldInclude = $false
            $excludedCount++
            break
        }
    }

    if ($shouldInclude) {
        try {
            $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction Stop

            # 统计行数
            $lines = (Get-Content $file.FullName -Encoding UTF8 -ErrorAction Stop).Count

            # 统计字数（中文字符+英文单词）
            $chineseChars = ([regex]::Matches($content, '[\u4e00-\u9fa5]')).Count
            $englishWords = ([regex]::Matches($content, '[a-zA-Z]+')).Count
            $words = $chineseChars + $englishWords

            $totalLines += $lines
            $totalChars += $content.Length
            $totalWords += $words
            $fileCount++

            # 获取文件所属目录/模块
            $module = if ($relativePath -match '^([^\\]+)') { $matches[1] } else { '根目录' }

            $files += [PSCustomObject]@{
                File = $relativePath
                Module = $module
                Lines = $lines
                Words = $words
                Chars = $content.Length
            }
        } catch {
            # 忽略读取失败的文件
        }
    }
}

Write-Host "  有效文件: $fileCount 个 (已排除 $excludedCount 个)" -ForegroundColor Green

# 2. 扫描可能的其他文档格式
Write-Host ""
Write-Host "[2/4] 扫描其他文档格式 (.txt, .rst, .adoc)..." -ForegroundColor Yellow

$otherExtensions = @('*.txt', '*.rst', '*.adoc')
foreach ($ext in $otherExtensions) {
    $otherFiles = Get-ChildItem -Path . -Filter $ext -Recurse -File -ErrorAction SilentlyContinue
    foreach ($file in $otherFiles) {
        $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
        $shouldInclude = $true

        foreach ($pattern in $excludePatterns) {
            if ($relativePath -match $pattern) {
                $shouldInclude = $false
                break
            }
        }

        # 只统计看起来像文档的文件（排除代码文件）
        if ($shouldInclude -and
            $relativePath -notmatch '\.(kt|java|xml|gradle|json|pro)' -and
            $relativePath -notmatch '\/src\/' -and
            $relativePath -notmatch '\/build\/') {

            try {
                $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction Stop

                # 统计行数
                $lines = (Get-Content $file.FullName -Encoding UTF8 -ErrorAction Stop).Count

                # 统计字数
                $chineseChars = ([regex]::Matches($content, '[\u4e00-\u9fa5]')).Count
                $englishWords = ([regex]::Matches($content, '[a-zA-Z]+')).Count
                $words = $chineseChars + $englishWords

                $totalLines += $lines
                $totalChars += $content.Length
                $totalWords += $words
                $fileCount++

                $module = if ($relativePath -match '^([^\\]+)') { $matches[1] } else { '根目录' }

                $files += [PSCustomObject]@{
                    File = $relativePath
                    Module = $module
                    Lines = $lines
                    Words = $words
                    Chars = $content.Length
                }
            } catch {
                # 忽略读取失败的文件
            }
        }
    }
}

# 3. 按模块分组统计
Write-Host ""
Write-Host "[3/4] 按模块/目录分组统计..." -ForegroundColor Yellow

$grouped = $files | Group-Object { $_.Module }

# 输出报告
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       项目文档完整统计报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "【统计范围】" -ForegroundColor Yellow
Write-Host "  ✓ 所有 .md 文件（包括根目录、各模块）"
Write-Host "  ✓ 文档格式文件 (.txt, .rst, .adoc)"
Write-Host "  ✗ 已排除以下内容:"
foreach ($pattern in $excludePatterns) {
    Write-Host "    - $pattern"
}
Write-Host ""
Write-Host "【总体统计】" -ForegroundColor Green
Write-Host "  📄 文档文件总数: $fileCount 个"
Write-Host "  📏 总行数: $([math]::Round($totalLines, 0)) 行"
Write-Host "  📝 总字数: $([math]::Round($totalWords, 0)) 字"
Write-Host "  🔤 总字符数: $([math]::Round($totalChars, 0)) 字符"
Write-Host "  📊 平均每文件: $([math]::Round($totalLines / [math]::Max(1, $fileCount), 1)) 行"
Write-Host "  📊 平均每文件: $([math]::Round($totalWords / [math]::Max(1, $fileCount), 0)) 字"
Write-Host "  📚 估计总字数: $([math]::Round($totalWords / 10000, 2)) 万字"

Write-Host ""
Write-Host "【按模块/目录统计】" -ForegroundColor Green
Write-Host ""

$moduleStats = @()
foreach ($group in $grouped | Sort-Object Name) {
    $moduleLines = ($group.Group | Measure-Object -Property Lines -Sum).Sum
    $moduleWords = ($group.Group | Measure-Object -Property Words -Sum).Sum
    $moduleCount = $group.Count
    $percentFiles = if ($fileCount -gt 0) { [math]::Round($moduleCount / $fileCount * 100, 1) } else { 0 }
    $percentLines = if ($totalLines -gt 0) { [math]::Round($moduleLines / $totalLines * 100, 1) } else { 0 }

    $moduleStats += [PSCustomObject]@{
        Module = $group.Name
        Files = $moduleCount
        Lines = $moduleLines
        Words = $moduleWords
        FilePercent = $percentFiles
        LinePercent = $percentLines
    }
}

# 显示模块统计
$moduleStats | Sort-Object Lines -Descending | Format-Table @{
    Label = "模块/目录"; Expression = {$_.Module}; Width = 25
}, @{
    Label = "文件数"; Expression = {$_.Files}; Align = 'Right'
}, @{
    Label = "占比"; Expression = {"$($_.FilePercent)%"}; Align = 'Right'
}, @{
    Label = "行数"; Expression = {$_.Lines}; Align = 'Right'
}, @{
    Label = "占比"; Expression = {"$($_.LinePercent)%"}; Align = 'Right'
}, @{
    Label = "字数"; Expression = {$_.Words}; Align = 'Right'
}

Write-Host ""
Write-Host "【最大文档 Top 20 - 按行数】" -ForegroundColor Green
$files | Sort-Object -Property Lines -Descending | Select-Object -First 20 | ForEach-Object {
    $percent = if ($totalLines -gt 0) { [math]::Round($_.Lines / $totalLines * 100, 2) } else { 0 }
    [PSCustomObject]@{
        文件 = $_.File
        行数 = $_.Lines
        占比 = "$percent%"
        字数 = $_.Words
    }
} | Format-Table -AutoSize

Write-Host ""
Write-Host "【最大文档 Top 20 - 按字数】" -ForegroundColor Green
$files | Sort-Object -Property Words -Descending | Select-Object -First 20 | ForEach-Object {
    $percent = if ($totalWords -gt 0) { [math]::Round($_.Words / $totalWords * 100, 2) } else { 0 }
    [PSCustomObject]@{
        文件 = $_.File
        字数 = $_.Words
        占比 = "$percent%"
        行数 = $_.Lines
    }
} | Format-Table -AutoSize

# 4. 按文档类型分类统计
Write-Host ""
Write-Host "【按文档类型分类】" -ForegroundColor Green

$typeGroups = @{
    'PRD文档' = $files | Where-Object { $_.File -match 'PRD' }
    'TDD文档' = $files | Where-Object { $_.File -match 'TDD' }
    'FD文档' = $files | Where-Object { $_.File -match 'FD' }
    'BUG文档' = $files | Where-Object { $_.File -match 'BUG' }
    '项目分析' = $files | Where-Object { $_.File -match '分析报告' -or $_.File -match 'Analysis' }
    '项目文档' = $files | Where-Object { $_.File -match '^文档\\' -and $_.File -notmatch 'PRD|TDD|FD|BUG' }
    '配置文档' = $files | Where-Object { $_.Module -eq '.claude' -or $_.Module -eq '.kiro' -or $_.Module -eq 'Rules' }
    '模块文档' = $files | Where-Object { $_.File -match 'CLAUDE\.md$' -or $_.File -match 'WORKSPACE' }
    '其他文档' = $files | Where-Object {
        $_.File -notmatch 'PRD|TDD|FD|BUG' -and
        $_.File -notmatch '分析报告' -and
        $_.File -notmatch '^文档\\' -and
        $_.Module -ne '.claude' -and $_.Module -ne '.kiro' -and $_.Module -ne 'Rules' -and
        $_.File -notmatch 'CLAUDE\.md$' -and $_.File -notmatch 'WORKSPACE'
    }
}

foreach ($type in $typeGroups.Keys) {
    $typeFiles = $typeGroups[$type]
    if ($typeFiles.Count -gt 0) {
        $typeLines = ($typeFiles | Measure-Object -Property Lines -Sum).Sum
        $typeWords = ($typeFiles | Measure-Object -Property Words -Sum).Sum
        Write-Host "  $type`: $($typeFiles.Count) 个文件, $typeLines 行, $typeWords 字"
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  统计完成！共处理 $fileCount 个文档文件" -ForegroundColor Cyan
Write-Host "  总计约 $([math]::Round($totalWords / 10000, 2)) 万字，$totalLines 行" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
