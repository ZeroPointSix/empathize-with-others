# 文档自动审查流水线
# 用法: .\scripts\auto-doc-pipeline.ps1 -DocPath "文档/开发文档/PRD/PRD-00012-xxx.md"
# 
# 功能:
# 1. 解析文档信息
# 2. 查找关联文档
# 3. 执行格式检查
# 4. 生成审查提示（供 AI 工具使用）
# 5. 可选：调用 Claude Code CLI 执行审查

param(
    [Parameter(Mandatory=$true)]
    [string]$DocPath,
    
    [switch]$AutoFix,        # 自动修复格式问题
    [switch]$GenerateDR,     # 生成 DR 报告
    [switch]$Verbose         # 详细输出
)

# 颜色输出函数
function Write-Success { param($msg) Write-Host "✅ $msg" -ForegroundColor Green }
function Write-Warning { param($msg) Write-Host "⚠️ $msg" -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host "❌ $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "📄 $msg" -ForegroundColor Cyan }

Write-Host ""
Write-Host "========================================" -ForegroundColor Blue
Write-Host "文档自动审查流水线" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# 检查文件是否存在
if (-not (Test-Path $DocPath)) {
    Write-Error "文件不存在: $DocPath"
    exit 1
}

# 解析文档信息
$fileName = [System.IO.Path]::GetFileNameWithoutExtension($DocPath)
$parts = $fileName -split '-'

if ($parts.Count -lt 2) {
    Write-Error "文件名格式不正确，应为: 类型-编号-描述.md"
    exit 1
}

$docType = $parts[0]
$docNum = $parts[1]
$docDesc = ($parts[2..($parts.Count-1)] -join '-')

Write-Info "文档信息:"
Write-Host "   类型: $docType"
Write-Host "   编号: $docNum"
Write-Host "   描述: $docDesc"
Write-Host ""

# 查找关联文档
Write-Host "🔍 查找关联文档..." -ForegroundColor Cyan
$relatedDocs = @()
$docTypes = @("PRD", "FD", "TDD", "TD", "IMPL", "BUG", "CR", "DR")

foreach ($type in $docTypes) {
    $pattern = "文档/开发文档/$type/$type-$docNum-*.md"
    $found = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue
    foreach ($f in $found) {
        if ($f.FullName -ne (Resolve-Path $DocPath).Path) {
            Write-Host "   找到: $($f.Name)" -ForegroundColor Gray
            $relatedDocs += $f.FullName
        }
    }
}

if ($relatedDocs.Count -eq 0) {
    Write-Warning "未找到关联文档"
} else {
    Write-Success "找到 $($relatedDocs.Count) 个关联文档"
}
Write-Host ""

# 格式检查
Write-Host "📋 格式检查..." -ForegroundColor Cyan
$content = Get-Content $DocPath -Raw -Encoding UTF8
$issues = @()

# 检查文档信息表格
if ($content -notmatch "文档编号") {
    $issues += "缺少文档信息表格"
}

# 检查必要章节（根据文档类型）
$requiredSections = @{
    "PRD" = @("需求背景", "功能需求", "验收标准")
    "FD" = @("功能概述", "业务流程", "界面设计")
    "TDD" = @("技术架构", "数据模型", "接口设计")
    "TD" = @("任务清单", "依赖关系")
    "BUG" = @("问题描述", "复现步骤", "根因分析")
}

if ($requiredSections.ContainsKey($docType)) {
    foreach ($section in $requiredSections[$docType]) {
        if ($content -notmatch $section) {
            $issues += "缺少章节: $section"
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Success "格式检查通过"
} else {
    Write-Warning "发现 $($issues.Count) 个格式问题:"
    foreach ($issue in $issues) {
        Write-Host "   - $issue" -ForegroundColor Yellow
    }
}
Write-Host ""

# 生成审查提示
Write-Host "========================================" -ForegroundColor Blue
Write-Host "审查提示" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

$prompt = @"
请审查以下文档并生成 DR（文档审查报告）:

**主文档**: $DocPath

**关联文档**:
$($relatedDocs | ForEach-Object { "- $_" } | Out-String)

**审查要求**:
1. 检查文档格式是否符合 Rules/开发文档规范.md
2. 检查与关联文档的一致性
3. 检查技术方案是否符合项目架构（.kiro/steering/structure.md）
4. 生成 DR 报告保存到 文档/开发文档/DR/

**发现的格式问题**:
$($issues | ForEach-Object { "- $_" } | Out-String)

请执行完整审查并：
1. 生成 DR-$docNum-xxx文档审查报告.md
2. 列出需要修复的问题
3. 如果可以自动修复，请直接修复
"@

Write-Host $prompt
Write-Host ""
Write-Host "========================================" -ForegroundColor Blue

# 保存审查提示到临时文件（供其他工具使用）
$promptFile = ".kiro/temp/doc-review-prompt.txt"
$promptDir = [System.IO.Path]::GetDirectoryName($promptFile)
if (-not (Test-Path $promptDir)) {
    New-Item -ItemType Directory -Path $promptDir -Force | Out-Null
}
$prompt | Out-File -FilePath $promptFile -Encoding UTF8

Write-Host ""
Write-Success "审查提示已保存到: $promptFile"
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Cyan
Write-Host "1. 复制上述提示到 Kiro/Claude Code/Roo"
Write-Host "2. 或者直接说: '请审查文档 $DocPath'"
Write-Host ""
