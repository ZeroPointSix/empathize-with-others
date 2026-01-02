---
name: Multi-Agent Explorer
description: This skill should be used when the user asks to "create worktree agent", "parallel development", "multi-agent exploration", "git worktree workflow", "sandbox development", "bug exploration", "feature exploration", "code review agent", "test exploration", "architecture review", or needs guidance on multi-worktree parallel development with multiple AI agents. Provides comprehensive framework for running multiple AI agents in separate git worktrees for exploratory, low-risk development tasks.
version: 1.0.0
---

# Multi-Agent Explorer - 多智能体并行探索系统

## 概述

Multi-Agent Explorer 是一套基于 Git Worktree 的多智能体并行开发框架。通过在独立的工作树中运行多个 AI 智能体，实现低风险的探索性开发任务，包括 Bug 修复探索、功能开发、代码审查、测试扩展等。

**核心理念**：
- 辅助性质：所有探索工作树都是辅助性的，不直接影响主分支
- 沙盒环境：智能体可以自由试错，无需担心破坏主代码
- 报告驱动：所有探索结果以报告形式输出，供主 Agent 参考
- 人类审查：最终是否合并由人类决定

## 适用场景

### 1. Bug 修复探索 (bugfix-explorer)
- 在独立工作树中探索 Bug 修复方案
- 允许多次试错，记录所有尝试
- 生成修复报告供主 Agent 参考

### 2. 功能开发探索 (feature-explorer)
- 根据 PRD 文档进行功能开发
- 自主规划和实现，无需频繁确认
- 生成开发报告和代码变更清单

### 3. 自由探索 (free-explorer)
- 完全自主的创新探索
- 可以自由提出和实现新功能
- 记录所有创意和实验结果

### 4. 代码架构审查 (architecture-reviewer)
- 分析项目架构优缺点
- 提出改进建议
- 生成架构审查报告

### 5. 测试扩展 (test-explorer)
- 扩展测试用例覆盖
- 探索边界情况和特殊场景
- 生成测试报告

### 6. 工作树管理 (worktree-manager)
- 管理所有探索工作树
- 审查其他智能体的工作
- 协调合并决策

## 工作流程

### 第一步：创建工作树

```bash
# 创建探索分支和工作树
git worktree add ../explore-bugfix-20241230 -b explore/bugfix-20241230

# 进入工作树目录
cd ../explore-bugfix-20241230
```

### 第二步：启动智能体

在新工作树中启动对应的智能体：

```bash
# 使用 slash 命令快速启动
/explore-bugfix    # Bug 修复探索
/explore-feature   # 功能开发探索
/explore-free      # 自由探索
/explore-arch      # 架构审查
/explore-test      # 测试扩展
/explore-manage    # 工作树管理
```

### 第三步：自主工作

智能体将：
1. 创建决策日志文件 `DECISION_JOURNAL.md`
2. 读取项目规范（CLAUDE.md、steering 文件）
3. 理解任务目标
4. 自主规划和执行
5. **实时更新决策日志**（每30分钟至少一次）
6. 记录所有尝试和结果
7. 生成探索报告

### 第四步：生成报告

所有探索结果保存到 `文档/开发文档/MA/` 目录：

```
文档/开发文档/MA/
├── BUGFIX/           # Bug 修复探索报告
│   ├── BUGFIX-YYYYMMDD-xxx.md           # 最终报告
│   └── BUGFIX-YYYYMMDD-xxx-JOURNAL.md   # 决策日志
├── FEATURE/          # 功能开发探索报告
├── FREE/             # 自由探索报告
├── ARCH/             # 架构审查报告
├── TEST/             # 测试探索报告
├── MANAGE/           # 管理审查报告
├── TASK/             # 任务永动机报告
└── LEARNINGS.md      # 跨任务经验积累
```

### 第五步：人类审查

1. 查看探索报告
2. 决定是否采纳
3. 如果采纳，可以：
   - 直接合并工作树的代码
   - 让主 Agent 参考报告重新实现

## 智能体列表

| 智能体 | 用途 | Slash 命令 | 报告目录 | 决策日志 |
|--------|------|------------|----------|----------|
| bugfix-explorer | Bug 修复探索 | /explore-bugfix | MA/BUGFIX/ | ✅ 必须 |
| feature-explorer | 功能开发探索 | /explore-feature | MA/FEATURE/ | ✅ 必须 |
| free-explorer | 自由创新探索 | /explore-free | MA/FREE/ | ✅ 必须 |
| architecture-reviewer | 架构审查 | /explore-arch | MA/ARCH/ | ✅ 必须 |
| test-explorer | 测试扩展 | /explore-test | MA/TEST/ | ✅ 必须 |
| worktree-manager | 工作树管理 | /explore-manage | MA/MANAGE/ | 审查他人 |
| **task-runner** | **任务永动机** | **/task-runner** | **MA/TASK/** | ✅ 必须 |

## 🆕 任务永动机 (Task Runner)

任务永动机是一个特殊的智能体，可以持续读取任务文件并自动执行任务。

### 核心特点

- **持续运行**：不断循环读取任务文件，执行未完成的任务
- **动态任务**：用户可以随时向任务文件添加新任务
- **自动 Handoff**：上下文快满时自动执行 handoff，保存进度
- **自动恢复**：通过 pickup 恢复后可以继续执行

### 工作流程

```
┌─────────────────────────────────────────────────────────┐
│                    任务永动机循环                         │
├─────────────────────────────────────────────────────────┤
│  1. 读取 TASKS.md                                        │
│  2. 找到第一个未完成任务 ([ ])                            │
│  3. 执行任务                                             │
│  4. 标记完成 ([x])                                       │
│  5. 记录结果到任务报告                                    │
│  6. 检查上下文长度                                        │
│     ├─ 如果快满 → 执行 handoff → 结束                    │
│     └─ 如果正常 → 回到步骤 1                             │
└─────────────────────────────────────────────────────────┘
```

### 快速开始

1. 复制任务模板：`cp skills/multi-agent-explorer/templates/TASKS.template.md TASKS.md`
2. 编辑任务文件：添加你的任务
3. 启动永动机：`/task-runner`
4. 随时添加任务：编辑 TASKS.md 添加新任务
5. 自动 handoff：上下文快满时自动交接
6. 恢复执行：`/pickup` 然后 `/task-runner`

### 任务文件格式

```markdown
# 任务清单

## 待执行任务

- [ ] 任务1：修复登录页面的崩溃问题
- [ ] 任务2：实现用户头像上传功能
- [ ] 任务3：编写 UserRepository 的单元测试
- [x] 任务4：已完成的任务（会被跳过）

## 执行日志

（由 AI 自动填写）
```

详见 `templates/TASKS.template.md` 获取完整模板。

## 核心原则

### 0. 🔴 决策日志原则（最重要）
- **每个智能体必须维护决策日志**
- 实时记录遇到的问题、考虑的方案、做出的决策
- 即使任务失败，决策日志也是宝贵的学习资料
- 详见 `references/decision-journal-guide.md`

### 0.1 🆕 影响范围评估原则
- **在开始任何修改前，必须先评估影响范围**
- 列出将要修改、新增、删除的文件
- 分析与其他任务的冲突，确定并行性
- 详见 `references/impact-analysis-guide.md`

```
╔══════════════════════════════════════════════════════════════════════════════╗
║  🧠 决策日志的价值                                                            ║
║                                                                              ║
║  能力较弱的智能体可能无法完成复杂任务                                          ║
║  但他们的探索过程、遇到的问题、做出的决策                                      ║
║  都是后续智能体和人类开发者的宝贵学习资料                                      ║
║                                                                              ║
║  失败的任务 + 详细的决策日志 > 成功的任务 + 空白的记录                         ║
╚══════════════════════════════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════════════════════════════╗
║  🎯 影响范围评估的价值                                                        ║
║                                                                              ║
║  在多智能体并行开发中，最大的风险是文件冲突                                     ║
║  通过预先评估影响范围，我们可以：                                              ║
║  • 识别可以并行执行的任务                                                     ║
║  • 识别必须串行执行的任务                                                     ║
║  • 提前规划任务分配                                                           ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

### 1. 自主性原则
- 智能体无需频繁询问用户
- 按照最佳实践自主决策
- 用户只负责最终审查

### 2. 安全性原则
- 所有工作在独立工作树中进行
- 不直接影响主分支
- 失败的探索可以直接丢弃

### 3. 报告优先原则
- 即使任务失败也要生成报告
- 记录所有尝试和错误
- 为主 Agent 提供参考

### 4. 规范遵循原则
- 必须遵循项目的 Clean Architecture
- 必须遵循项目的编码规范
- 必须遵循项目的测试要求

### 5. ⚠️ 报告质量原则（重要）
- **报告必须详细、全面、自包含**
- **宁长勿短**：简短的报告 = 没有价值的报告
- **自包含**：删除分支后，仅凭报告能完全理解所有内容
- **必须嵌入代码**：修改前+修改后+修改原因
- 详见 `references/report-quality-standards.md`

### 6. 🔴 诚实性原则（极其重要）
- **诚实比成功更重要**
- **失败的诚实报告 > 虚假的成功报告**
- **禁止夸大效果**：不要说"完美"、"非常好"
- **禁止隐藏问题**：发现的所有问题都要报告
- **禁止声称未验证的结论**：没验证就不要说"成功"
- **禁止魔改需求**：不要为了"完成"而改变需求
- 详见 `references/honesty-verification.md`

### 7. 📚 经验积累原则
- 有价值的经验应写入 `文档/开发文档/MA/LEARNINGS.md`
- 只积累经过验证的经验
- 标记验证状态：✅ 已验证 / ⚠️ 待验证

## 失败处理

当智能体遇到长时间无法解决的问题时：

1. **停止尝试**：不要无限循环尝试
2. **记录错误**：详细记录错误信息和尝试过程
3. **生成报告**：生成错误报告，包含：
   - 问题描述
   - 尝试的方案
   - 失败原因分析
   - 建议的解决方向
4. **返回报告**：将报告保存到对应目录

## 技能调用

智能体可以调用以下技能：

### 代码分析类
- `code-architecture-analyzer` - 架构分析
- `code-quality-analyzer` - 质量分析
- `code-dependency-tracer` - 依赖追踪
- `code-pattern-detector` - 模式检测

### 开发类
- `debugging-strategies` - 调试策略
- `test-driven-development` - 测试驱动开发
- `refactoring` - 代码重构
- `jetpack-compose` - Compose 开发

### 审查类
- `code-review` - 代码审查
- `verification` - 完成验证

### 文档类
- `documentation` - 文档编写
- `planning` - 计划编写

## 报告模板

详见 `references/report-templates.md`

## 项目规范

详见 `references/project-standards.md`

## 快速开始

1. 创建工作树：`git worktree add ../explore-xxx -b explore/xxx`
2. 进入工作树：`cd ../explore-xxx`
3. 启动智能体：使用对应的 slash 命令
4. 等待完成：智能体自主工作
5. 查看报告：检查 `文档/开发文档/MA/` 目录

## 注意事项

⚠️ **重要提醒**：
- 探索工作树使用较低成本的模型
- 探索结果仅供参考，不保证正确性
- 最终代码质量由人类审查把关
- 如果探索失败，报告比代码更重要

