---
description: 任务转 Issue - 将现有任务转换为可执行的、依赖有序的 GitHub Issues
tools: ['github/github-mcp-server/issue_write']
---

**说明：** 本命令用于将 FD 功能文档中的任务转换为 GitHub Issues，保持依赖关系和执行顺序。需要配置 GitHub MCP 服务器。

## 用户输入

```text
$ARGUMENTS
```

在继续之前，**必须**考虑用户输入（如果非空）。

## 执行流程

1. **查找 FD 功能文档**：
   - 定位 FD：`文档/开发文档/FD/FD-xxxxx-功能名称.md`
   - 从执行的脚本中提取**任务**的路径。

2. **获取 Git 远程**：

```bash
git config --get remote.origin.url
```

> [!CAUTION]
> 仅当远程是 GITHUB URL 时才继续下一步

3. 对于列表中的每个任务，使用 GitHub MCP 服务器在与 Git 远程对应的仓库中创建新 issue。

> [!CAUTION]
> 在任何情况下都不要在与远程 URL 不匹配的仓库中创建 issue

## Issue 格式

每个 Issue 应包含：

- **标题**: `[任务ID] 任务描述`
- **正文**: 
  - 任务详情
  - 文件路径
  - 依赖关系（如有）
  - 所属用户故事
- **标签**: 
  - `speckit` - 标识来源
  - 阶段标签（如 `phase-1-setup`、`phase-2-foundation`）
  - 用户故事标签（如 `US1`、`US2`）

## 文档信息

**相关文档**:
- 功能文档: `文档/开发文档/FD/FD-xxxxx-功能名称.md`
- 需求文档: `文档/开发文档/PRD/PRD-xxxxx-功能名称.md`
- 技术设计: `文档/开发文档/TDD/TDD-xxxxx-功能名称.md`
