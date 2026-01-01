# 多智能体探索报告目录

> 本目录存放所有多智能体探索系统生成的报告。

## 目录结构

```
MA/
├── README.md           # 本文件
├── BUGFIX/             # Bug 修复探索报告
├── FEATURE/            # 功能开发探索报告
├── FREE/               # 自由探索报告
├── ARCH/               # 架构审查报告
├── TEST/               # 测试探索报告
├── MANAGE/             # 工作树管理报告
└── TASK/               # 任务永动机报告
```

## 报告命名规范

```
[类型]-[日期]-[简短描述].md

示例：
BUGFIX-20241230-悬浮窗崩溃修复.md
FEATURE-20241230-新增用户画像功能.md
ARCH-20241230-数据层架构审查.md
TEST-20241230-UseCase测试扩展.md
FREE-20241230-性能优化探索.md
MANAGE-20241230-工作树状态审查.md
```

## 报告状态

| 状态 | 说明 |
|------|------|
| 🔍探索中 | 智能体正在工作 |
| ✅可合并 | 探索成功，建议合并 |
| 📖仅参考 | 探索有价值，但不建议直接合并 |
| ❌已废弃 | 探索失败或已过期 |

## 使用说明

1. 所有探索报告由智能体自动生成
2. 报告保存到对应的子目录
3. 人类审查后决定是否采纳
4. 采纳的报告可以指导主 Agent 工作

## 相关文档

- [多智能体探索系统技能](../../../skills/multi-agent-explorer/SKILL.md)
- [报告模板](../../../skills/multi-agent-explorer/references/report-templates.md)
- [Git Worktree 指南](../../../skills/multi-agent-explorer/references/worktree-guide.md)



## 🆕 任务永动机 (Task Runner)

任务永动机是一个特殊的智能体，可以持续读取 `TASKS.md` 文件并自动执行任务。

### 使用方式

1. 复制任务模板到项目根目录：
   ```bash
   cp skills/multi-agent-explorer/templates/TASKS.template.md TASKS.md
   ```

2. 编辑 `TASKS.md`，添加你的任务

3. 启动任务永动机：
   ```
   /task-runner
   ```

4. 随时编辑 `TASKS.md` 添加新任务，AI 会自动读取

5. 上下文快满时 AI 会自动 handoff，恢复后执行：
   ```
   /pickup
   /task-runner
   ```

### 任务文件格式

```markdown
- [ ] 待执行任务
- [x] 已完成任务
- [-] 已跳过任务
- [!] 执行失败任务
```

