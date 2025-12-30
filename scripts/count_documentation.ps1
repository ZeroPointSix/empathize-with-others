# 项目文档统计脚本
# 统计项目中所有文档的行数、字数等信息

# 定义要统计的文档目录
$docDirs = @(
    '文档',
    'Rules',
    '.kiro',
    '.claude'
)

# 统计所有md文件
$totalLines = 0
$totalChars = 0
$totalWords = 0
$fileCount = 0
$files = @()

Write-Host "开始扫描项目文档..." -ForegroundColor Cyan

foreach ($dir in $docDirs) {
    if (Test-Path $dir) {
        $mdFiles = Get-ChildItem -Path $dir -Filter *.md -Recurse -File -ErrorAction SilentlyContinue
        Write-Host "  扫描 $dir - 发现 $($mdFiles.Count) 个文件" -ForegroundColor Gray

        foreach ($file in $mdFiles) {
            # 排除历史文档和临时文件
            if ($file.FullName -notmatch '历史文档' -and
                $file.FullName -notmatch 'temp_' -and
                $file.FullName -notmatch '过期' -and
                $file.FullName -notmatch 'archive' -and
                $file.FullName -notmatch 'extracted') {

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

                    $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '')
                    $files += [PSCustomObject]@{
                        File = $relativePath
                        Lines = $lines
                        Words = $words
                        Chars = $content.Length
                    }
                } catch {
                    Write-Host "    读取失败: $($file.Name)" -ForegroundColor Red
                }
            }
        }
    } else {
        Write-Host "  跳过 $dir - 目录不存在" -ForegroundColor Yellow
    }
}

# 输出报告
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       项目文档统计报告" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "【统计范围】" -ForegroundColor Yellow
Write-Host "  ✓ 文档/ 目录"
Write-Host "  ✓ Rules/ 目录"
Write-Host "  ✓ .kiro/ 目录"
Write-Host "  ✓ .claude/ 目录"
Write-Host "  ✗ 已排除历史文档、过期文档、临时文件"
Write-Host ""
Write-Host "【总体统计】" -ForegroundColor Green
Write-Host "  📄 文档文件总数: $fileCount 个"
Write-Host "  📏 总行数: $totalLines 行"
Write-Host "  📝 总字数: $totalWords 字"
Write-Host "  🔤 总字符数: $totalChars 字符"
Write-Host "  📊 平均每文件: $([math]::Round($totalLines / [math]::Max(1, $fileCount), 1)) 行 / $([math]::Round($totalWords / [math]::Max(1, $fileCount), 0)) 字"
Write-Host "  📚 估计总字数: $([math]::Round($totalWords / 10000, 2)) 万字"

Write-Host ""
Write-Host "【按目录统计】" -ForegroundColor Green

$grouped = $files | Group-Object {
    if ($_.File -match '^([^\\]+)') { $matches[1] } else { '其他' }
}

foreach ($group in $grouped | Sort-Object Name) {
    $dirLines = ($group.Group | Measure-Object -Property Lines -Sum).Sum
    $dirWords = ($group.Group | Measure-Object -Property Words -Sum).Sum
    $dirCount = $group.Count
    $percentLines = if ($totalLines -gt 0) { [math]::Round($dirLines / $totalLines * 100, 1) } else { 0 }
    Write-Host "  $($group.Name):" -ForegroundColor Cyan
    Write-Host "    文件: $dirCount 个 | 行数: $dirLines ($percentLines%) | 字数: $dirWords"
}

Write-Host ""
Write-Host "【最大文档 Top 15 - 按行数】" -ForegroundColor Green
$files | Sort-Object -Property Lines -Descending | Select-Object -First 15 | Format-Table @{
    Label = "文件"; Expression = {$_.File}; Width = 50
}, @{
    Label = "行数"; Expression = {$_.Lines}; Align = 'Right'
}, @{
    Label = "字数"; Expression = {$_.Words}; Align = 'Right'
}

Write-Host ""
Write-Host "【最大文档 Top 15 - 按字数】" -ForegroundColor Green
$files | Sort-Object -Property Words -Descending | Select-Object -First 15 | Format-Table @{
    Label = "文件"; Expression = {$_.File}; Width = 50
}, @{
    Label = "字数"; Expression = {$_.Words}; Align = 'Right'
}, @{
    Label = "行数"; Expression = {$_.Lines}; Align = 'Right'
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  统计完成！共处理 $fileCount 个文档文件" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
