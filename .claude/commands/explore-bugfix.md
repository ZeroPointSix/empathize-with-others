---
description: 启动 Bug 修复探索智能体 - 在独立工作树中安全地探索 Bug 修复方案
---

# Bug 修复探索

启动 **Bugfix Explorer** 智能体，在独立 Git 工作树中探索 Bug 修复方案。

## 使用场景

- 遇到复杂 Bug，不确定如何修复
- 想要尝试多种修复方案
- 担心直接修复会破坏现有代码
- 需要低成本的试错环境

## 执行流程

### 1. 确认工作环境

首先确认当前是否在探索工作树中：

```bash
git worktree list
```

如果不在探索工作树中，建议先创建：

```bash
git worktree add ../explore-bugfix-$(date +%Y%m%d) -b explore/bugfix-$(date +%Y%m%d)
cd ../explore-bugfix-$(date +%Y%m%d)
```

### 2. 读取项目规范

阅读以下文档了解项目规范：
- **CLAUDE.md** - 项目主文档
- **.kiro/steering/** - 项目 steering 文件
- **skills/multi-agent-explorer/references/project-standards.md** - 项目规范

### 3. 理解 Bug

请提供以下信息：
- Bug 描述
- 错误日志（如果有）
- 复现步骤（如果知道）

### 4. 开始探索

按照 `skills/multi-agent-explorer/agents/bugfix-explorer.md` 中定义的工作流程：

1. **理解问题** - 分析 Bug 描述和错误日志
2. **定位根因** - 追踪代码调用链，识别问题代码
3. **探索方案** - 提出至少 2 种修复方案
4. **实施修复** - 编写修复代码和测试
5. **生成报告** - 保存到 `文档/开发文档/MA/BUGFIX/`

## 工作原则

- ✅ **自主决策**：无需频繁询问用户，按最佳实践执行
- ✅ **大胆尝试**：这是沙盒环境，可以自由试错
- ✅ **记录一切**：记录所有尝试，包括失败的
- ✅ **报告优先**：即使失败也要生成报告

## 约束条件

- 必须遵循 Clean Architecture
- 修复必须有对应测试
- 报告保存到 `文档/开发文档/MA/BUGFIX/`

## 失败处理

如果长时间无法解决：
1. 停止尝试
2. 记录错误信息
3. 生成错误报告
4. 返回报告路径

---

**现在请告诉我你要修复的 Bug 是什么？**

