---
event: onFileSave
filePattern: "文档/开发文档/**/*.md"
description: 保存文档时自动触发审查流程
---

# 文档自动审查 Hook

当在 `文档/开发文档/` 目录下保存 Markdown 文档时，自动触发审查流程。

## 触发条件

- 文件路径匹配 `文档/开发文档/**/*.md`
- 文件类型为 PRD、FD、TDD、TD、IMPL、BUG 等开发文档

## 执行流程

### 1. 解析文档信息

从文件名提取：
- 文档类型（PRD/FD/TDD/TD/IMPL/BUG/CR/DR/RE）
- 文档编号（5位数字）
- 文档描述

### 2. 快速格式检查

检查以下基本格式：
- [ ] 文档命名是否符合 `类型-编号-描述.md` 格式
- [ ] 文档是否包含必要的元信息表格
- [ ] 章节结构是否完整

### 3. 关联文档查找

根据文档编号，自动查找相同编号的其他文档：
- PRD-00012 → 查找 FD-00012、TDD-00012、TD-00012
- TDD-00012 → 查找 PRD-00012、FD-00012、TD-00012

### 4. 一致性检查

对比关联文档，检查：
- 功能点是否一致
- 技术方案是否匹配需求
- 任务清单是否覆盖所有设计点

### 5. 生成审查摘要

在聊天中输出简要审查结果：
```
📄 文档审查: PRD-00012-事实流内容编辑功能需求.md

✅ 格式检查: 通过
✅ 关联文档: FD-00012, TDD-00012, TD-00012
⚠️ 发现 2 个问题:
   1. 缺少验收标准章节
   2. 与 TDD-00012 的接口定义不一致

💡 建议: 运行完整审查生成 DR 报告
```

### 6. 可选：完整审查

如果用户确认，执行完整审查并：
1. 生成 DR（文档审查报告）
2. 自动修复可修复的问题
3. 更新 WORKSPACE.md

## 自动修复范围

以下问题可以自动修复：
- 格式问题（表格对齐、标题层级）
- 缺失的标准章节（添加模板）
- 编号格式错误
- 路径引用错误

以下问题需要人工确认：
- 内容不一致
- 功能点遗漏
- 技术方案冲突

## 使用自动审查流水线

### 方式一：PowerShell 脚本（推荐）

```powershell
# 审查单个文档
powershell -ExecutionPolicy Bypass -File test-workflow\auto-review-pipeline.ps1 -DocPath "文档/开发文档/PRD/PRD-00012-xxx.md"

# 审查并自动修复
powershell -ExecutionPolicy Bypass -File test-workflow\auto-review-pipeline.ps1 -DocPath "文档/开发文档/PRD/PRD-00012-xxx.md" -AutoFix
```

### 方式二：dev.bat 统一入口

```batch
scripts\dev.bat doc 文档/开发文档/PRD/PRD-00012-xxx.md
```

### 方式三：在 Kiro 中自然语言触发

直接告诉 Kiro：
- "帮我审查 PRD-00012 文档"
- "检查一下刚写的 TDD 文档"
- "运行文档审查流水线"

## 配置选项

```yaml
# 可在 .kiro/settings/hooks.yaml 中配置
auto-doc-review:
  enabled: true
  auto-fix: false          # 是否自动修复格式问题
  generate-dr: false       # 是否自动生成 DR 报告
  notify-on-issues: true   # 发现问题时是否通知
```
