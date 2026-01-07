# Claude Code 钩子系统

> **设计目的**: 通过自动化钩子增强多AI协作开发流程，确保规则前置和任务追踪
>
> **业务背景**: PRD-00029 AI军师UI架构优化 - 任务协调和规则同步需求
>
> **设计决策**: 采用 Claude Code 原生 Hook 机制而非自定义脚本调度，减少系统复杂度

本项目配置了自动钩子，以增强开发工作流程。

## 钩子列表

### 1. UserPromptSubmit 钩子 (开始前)
- **触发时机**: 用户提交提示词后、Claude处理前
- **执行脚本**: `.claude/hooks/read-rules.ps1`
- **设计权衡**: 在每次任务开始前强制读取Rules文档，确保多AI协作时规则一致性
- **功能**:
  - 读取 `Rules/` 目录下所有 `.md` 文档
  - 检查 `workspace-rules.md` 中是否有进行中的任务
  - 输出文档摘要供Claude参考

### 2. PostToolUse 钩子 (完成后)
- **触发时机**: 执行 `Edit`、`Write`、`Read` 等工具后
- **执行脚本**: `.claude/hooks/update-task-log.ps1`
- **设计权衡**: 仅在文件修改工具后触发，避免频繁日志写入; 使用Append模式确保历史可追溯
- **功能**:
  - 记录任务完成情况
  - 更新任务日志到 `.claude/logs/task-log.md`

## 文件结构

```
.claude/
├── settings.local.json    # 钩子配置
├── hooks/
│   ├── read-rules.ps1     # 开始前读取Rules文档
│   └── update-task-log.ps1 # 完成后更新任务日志
└── logs/
    ├── hook-read-rules.log    # Rules读取日志
    └── task-log.md            # 任务日志
```

## 配置说明

> **业务规则 (PRD-00029)**: 任务状态同步需要实时记录，避免信息丢失
>
> 在 `settings.local.json` 中的 `hooks` 节点配置：
> - `UserPromptSubmit`: 每次用户交互前触发，确保规则前置
> - `PostToolUse`: 仅文件修改操作后触发，避免日志膨胀

```json
{
  "hooks": {
    "UserPromptSubmit": [
      {
        "matcher": ".*",  // 匹配所有提示词，不遗漏任何任务
        "hooks": [{ "type": "command", "command": "..." }]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write|Read",  // 仅匹配文件操作，避免冗余日志
        "hooks": [{ "type": "command", "command": "..." }]
      }
    ]
  }
}
```

## 可用钩子类型

| 钩子名称 | 触发时机 |
|---------|---------|
| `UserPromptSubmit` | 用户提交提示词时 |
| `PreToolUse` | 工具执行前 |
| `PostToolUse` | 工具执行后 |
| `PostToolUseFailure` | 工具执行失败时 |
| `SessionStart` | 会话开始时 |
| `SessionEnd` | 会话结束时 |

## matcher 匹配规则

> **设计权衡**: 精确匹配 vs 宽泛匹配
> - `.*` (UserPromptSubmit): 宽泛匹配确保不遗漏任何任务上下文
> - `Edit|Write|Read` (PostToolUse): 精确匹配减少不必要的日志写入

| 模式 | 含义 | 使用场景 |
|------|------|---------|
| `.*` | 匹配所有 | UserPromptSubmit钩子，确保每次交互都被捕获 |
| `Edit\|Write\|Read` | 匹配指定工具 | PostToolUse钩子，仅记录文件修改操作 |
| `Bash*` | 前缀匹配 | 需要排除或特定处理Bash命令时使用 |

- `.*` - 匹配所有
- `Edit|Write|Read` - 匹配指定工具（用`|`分隔）
- `Bash*` - 匹配以Bash开头的工具

---
**维护者**: Claude | **最后更新**: 2026-01-07
**任务追踪**: FD-00029 (PRD-00029 AI军师UI架构优化)
