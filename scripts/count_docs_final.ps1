# 项目文档最终统计脚本（去重）

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       项目文档最终统计报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 只统计顶层目录，避免重复
$topDocDirs = @(
    '文档',
    '历史文档',
    'Rules',
    '.kiro',
    '.claude',
    'skills',
    '.Roo',
    '.kilocode'
)

$totalLines = 0
$totalWords = 0
$totalChars = 0
$fileCount = 0
$files = @()

Write-Host "[扫描文档目录...]" -ForegroundColor Yellow
Write-Host ""

foreach ($dir in $topDocDirs) {
    if (Test-Path $dir) {
        $mdFiles = Get-ChildItem -Path $dir -Filter *.md -Recurse -File -ErrorAction SilentlyContinue
        Write-Host "  $dir : $($mdFiles.Count) 个md文件" -ForegroundColor Cyan

        foreach ($file in $mdFiles) {
            # 排除 build、temp、META-INF 等
            if ($file.FullName -notmatch 'build\\' -and
                $file.FullName -notmatch 'temp_' -and
                $file.FullName -notmatch 'META-INF' -and
                $file.FullName -notmatch 'extracted' -and
                $file.FullName -notmatch 'node_modules') {

                try {
                    $content = Get-Content $file.FullName -Raw -Encoding UTF8 -ErrorAction Stop
                    $lines = (Get-Content $file.FullName -Encoding UTF8 -ErrorAction Stop).Count

                    $chineseChars = ([regex]::Matches($content, '[\u4e00-\u9fa5]')).Count
                    $englishWords = ([regex]::Matches($content, '[a-zA-Z]+')).Count
                    $words = $chineseChars + $englishWords

                    $totalLines += $lines
                    $totalChars += $content.Length
                    $totalWords += $words
                    $fileCount++

                    $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
                    $files += [PSCustomObject]@{
                        File = $relativePath
                        Dir = $dir
                        Lines = $lines
                        Words = $words
                    }
                } catch { }
            }
        }
    }
}

# 扫描根目录和各模块的 CLAUDE.md 等文档
Write-Host ""
Write-Host "[扫描根目录和模块文档...]" -ForegroundColor Yellow

$rootDocs = Get-ChildItem -Path . -Filter *.md -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'CLAUDE|README|WORKSPACE|CHANGELOG' }
foreach ($file in $rootDocs) {
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $lines = (Get-Content $file.FullName -Encoding UTF8).Count
        $chineseChars = ([regex]::Matches($content, '[\u4e00-\u9fa5]')).Count
        $englishWords = ([regex]::Matches($content, '[a-zA-Z]+')).Count
        $words = $chineseChars + $englishWords

        $totalLines += $lines
        $totalChars += $content.Length
        $totalWords += $words
        $fileCount++

        $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
        $files += [PSCustomObject]@{
            File = $relativePath
            Dir = '根目录'
            Lines = $lines
            Words = $words
        }
    } catch { }
}

# 扫描各模块的 CLAUDE.md
$modules = @('domain', 'data', 'presentation', 'app')
foreach ($module in $modules) {
    if (Test-Path $module) {
        $moduleDoc = Join-Path $module 'CLAUDE.md'
        if (Test-Path $moduleDoc) {
            try {
                $content = Get-Content $moduleDoc -Raw -Encoding UTF8
                $lines = (Get-Content $moduleDoc -Encoding UTF8).Count
                $chineseChars = ([regex]::Matches($content, '[\u4e00-\u9fa5]')).Count
                $englishWords = ([regex]::Matches($content, '[a-zA-Z]+')).Count
                $words = $chineseChars + $englishWords

                $totalLines += $lines
                $totalChars += $content.Length
                $totalWords += $words
                $fileCount++

                $relativePath = $moduleDoc.Replace((Get-Location).Path + '\', '')
                $files += [PSCustomObject]@{
                    File = $relativePath
                    Dir = $module
                    Lines = $lines
                    Words = $words
                }
            } catch { }
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       项目文档最终统计报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[统计范围]" -ForegroundColor Yellow
Write-Host "  ✓ 文档/ 目录（含项目文档、开发文档）"
Write-Host "  ✓ 历史文档/ 目录"
Write-Host "  ✓ Rules/ 目录"
Write-Host "  ✓ .kiro/ 目录"
Write-Host "  ✓ .claude/ 目录"
Write-Host "  ✓ skills/ 目录"
Write-Host "  ✓ .Roo/ 目录"
Write-Host "  ✓ .kilocode/ 目录"
Write-Host "  ✓ 根目录文档 (CLAUDE.md, README.md等)"
Write-Host "  ✓ 各模块 CLAUDE.md"
Write-Host "  ✗ 已排除: build、temp、META-INF等临时文件"
Write-Host ""
Write-Host "[总体统计]" -ForegroundColor Green
Write-Host "  📄 文档文件总数: $fileCount 个"
Write-Host "  📏 总行数: $([math]::Round($totalLines, 0)) 行"
Write-Host "  📝 总字数: $([math]::Round($totalWords, 0)) 字"
Write-Host "  🔤 总字符数: $([math]::Round($totalChars, 0)) 字符"
Write-Host "  📊 平均每文件: $([math]::Round($totalLines / [math]::Max(1, $fileCount), 1)) 行"
Write-Host "  📊 平均每文件: $([math]::Round($totalWords / [math]::Max(1, $fileCount), 0)) 字"
Write-Host "  📚 估计总字数: $([math]::Round($totalWords / 10000, 2)) 万字"
Write-Host ""

# 按目录统计
Write-Host "[按目录统计]" -ForegroundColor Green
$grouped = $files | Group-Object Dir
foreach ($group in $grouped | Sort-Object { ($_.Group | Measure-Object Lines -Sum).Sum } -Descending) {
    $dirLines = ($group.Group | Measure-Object -Property Lines -Sum).Sum
    $dirWords = ($group.Group | Measure-Object -Property Words -Sum).Sum
    $percent = if ($totalLines -gt 0) { [math]::Round($dirLines / $totalLines * 100, 1) } else { 0 }
    Write-Host "  $($group.Name.PadRight(15)) : $($group.Count.ToString().PadLeft(3)) 文件 | $($dirLines.ToString().PadLeft(6)) 行 ($percent%) | $($dirWords.ToString().PadLeft(6)) 字"
}

Write-Host ""
Write-Host "[最大文档 Top 15 - 按行数]" -ForegroundColor Green
$files | Sort-Object Lines -Descending | Select-Object -First 15 | ForEach-Object {
    $percent = if ($totalLines -gt 0) { [math]::Round($_.Lines / $totalLines * 100, 2) } else { 0 }
    [PSCustomObject]@{
        文件 = $_.File
        行数 = $_.Lines
        占比 = "$percent%"
        字数 = $_.Words
    }
} | Format-Table -AutoSize

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  统计完成！共处理 $fileCount 个文档文件" -ForegroundColor Cyan
Write-Host "  总计约 $([math]::Round($totalWords / 10000, 2)) 万字，$totalLines 行" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
