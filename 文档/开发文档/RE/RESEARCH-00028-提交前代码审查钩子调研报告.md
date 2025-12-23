# RESEARCH-00028-提交前代码审查钩子调研报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | RESEARCH-00028 |
| 创建日期 | 2025-12-23 |
| 调研人 | Kiro |
| 状态 | 调研完成 |
| 调研目的 | 为创建"提交前代码审查"钩子提供前置知识 |
| 关联任务 | 用户请求创建Git提交前自动触发的代码审查钩子 |

---

## 1. 调研范围

### 1.1 调研主题
用户需要创建一个钩子，在每次Git提交（git commit）前自动触发，分析已修改的代码是否符合项目规范和业界最佳实践。

### 1.2 用户原始需求
> "在我们每一次提交前面自动触发，就是说在我们给提交的时候，不是有getDeFine吗？我们已经应该已经是修改了，主要是观察代码编写的如何，分析一下代码是否符合我们当前的项目规范，是否符合业界公认的最佳实践，主要是观察分析我们已经修改的代码就可以了。"

### 1.3 关注重点
- Kiro钩子系统支持的事件类型
- 如何实现Git提交前的代码审查
- 现有代码审查机制和规范

### 1.4 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| 命令 | - | .kiro/commands/CodeReview.md |
| 命令 | - | .kiro/commands/Research.md |
| 规范 | - | Rules/开发文档规范.md |
| 配置 | - | .kiro/settings/hooks.yaml |

---

## 2. Kiro钩子系统分析

### 2.1 支持的事件类型

根据现有钩子文件分析，Kiro钩子系统支持以下事件类型：

| 事件类型 | 说明 | 适用场景 |
|----------|------|----------|
| `promptSubmit` | 用户发送消息时触发 | 消息预处理、意图分析 |
| `agentStop` | Agent执行完成时触发 | 后处理、总结 |
| `fileEdited` | 文件编辑保存时触发 | 文档审查、代码检查 |
| `fileCreated` | 文件创建时触发 | 新文件检查 |
| `fileDeleted` | 文件删除时触发 | 清理检查 |
| `userTriggered` | 用户手动触发 | 按需执行 |

### 2.2 钩子动作类型

| 动作类型 | 说明 | 适用事件 |
|----------|------|----------|
| `askAgent` | 向Agent发送提示词 | fileEdited, fileCreated, fileDeleted, userTriggered |
| `runCommand` | 执行Shell命令 | promptSubmit, agentStop |

### 2.3 关键发现

⚠️ **重要限制**：Kiro钩子系统**不支持**直接监听Git事件（如pre-commit）。

- `promptSubmit` 是在用户向Kiro发送消息时触发，不是Git提交时
- `runCommand` 只能在 `promptSubmit` 和 `agentStop` 事件中使用
- 没有原生的 `gitCommit` 或 `preCommit` 事件类型

---

## 3. 现有钩子文件分析

### 3.1 已创建的钩子

| 文件名 | 事件类型 | 动作类型 | 功能 |
|--------|----------|----------|------|
| pre-commit-code-review.kiro.hook | promptSubmit | askAgent | 提交前代码审查（配置不完整） |
| prd-research-trigger.kiro.hook | promptSubmit | askAgent | PRD文档调研触发 |
| auto-doc-review.kiro.hook | fileEdited | askAgent | 文档自动审查 |

### 3.2 pre-commit-code-review.kiro.hook 问题分析

当前配置：
```json
{
  "enabled": true,
  "name": "提交前代码审查",
  "description": "在每次提交代码前自动触发...",
  "version": "1",
  "when": {
    "type": "promptSubmit"
  },
  "then": {
    "type": "askAgent",
    "prompt": ""  // ❌ 提示词为空
  }
}
```

**问题**：
1. `prompt` 字段为空，无法执行任何操作
2. `promptSubmit` 事件不是Git提交事件
3. 缺少文件模式匹配配置

---

## 4. 可行方案分析

### 4.1 方案一：使用 `userTriggered` 事件（推荐）

创建一个手动触发的钩子，用户在提交前点击按钮执行代码审查。

**优点**：
- 用户可控，按需执行
- 可以获取Git暂存区的文件列表
- 符合Kiro钩子系统设计

**实现方式**：
```json
{
  "enabled": true,
  "name": "提交前代码审查",
  "when": {
    "type": "userTriggered"
  },
  "then": {
    "type": "askAgent",
    "prompt": "执行提交前代码审查..."
  }
}
```

### 4.2 方案二：使用 `promptSubmit` + 关键词触发

当用户发送包含特定关键词的消息时触发代码审查。

**触发关键词**：
- "提交代码"
- "git commit"
- "代码审查"
- "检查代码"

**实现方式**：
在 `prompt` 中添加条件判断逻辑。

### 4.3 方案三：结合Git Hooks（外部方案）

在 `.git/hooks/pre-commit` 中调用Kiro或脚本进行检查。

**优点**：
- 真正的Git提交前触发
- 可以阻止不合规的提交

**缺点**：
- 不在Kiro钩子系统内
- 需要额外配置

---

## 5. 代码审查内容分析

### 5.1 项目规范检查点

根据 `.kiro/steering/` 和 `Rules/` 目录的规范文档：

| 检查项 | 规范来源 | 说明 |
|--------|----------|------|
| 架构合规性 | structure.md | Clean Architecture + MVVM |
| 命名规范 | structure.md | UseCase、ViewModel、Screen等命名 |
| 层级依赖 | structure.md | Domain层不依赖Android |
| 代码风格 | tech.md | Kotlin代码规范 |
| 文档规范 | 开发文档规范.md | 文档编号和格式 |

### 5.2 业界最佳实践检查点

| 检查项 | 说明 |
|--------|------|
| 空值处理 | null安全、空列表处理 |
| 错误处理 | Result包装、异常捕获 |
| 并发安全 | 协程使用、线程安全 |
| 性能考虑 | 避免主线程阻塞 |
| 安全性 | 敏感数据处理 |
| 可测试性 | 依赖注入、接口抽象 |

### 5.3 现有代码审查命令

`.kiro/commands/CodeReview.md` 提供了代码审查指令：

```markdown
## 常见审查类型参考
- 文档审查: 检查文档是否符合规范
- 架构审查: 检查代码是否符合Clean Architecture + MVVM
- 代码质量审查: 检查未实现方法、错误处理、硬编码等
- 功能完整性审查: 验证声明的功能是否真正实现
- 测试覆盖审查: 检查关键功能是否有测试
- 安全审查: 检查敏感数据处理、权限使用等
```

---

## 6. 问题与风险

### 6.1 🔴 阻塞问题 (P0)

#### P0-001: Kiro不支持Git事件监听
- **问题描述**: Kiro钩子系统没有原生的Git事件类型（如pre-commit）
- **影响范围**: 无法实现真正的"Git提交前自动触发"
- **建议解决方案**: 使用 `userTriggered` 事件作为替代方案

### 6.2 🟡 风险问题 (P1)

#### P1-001: 当前钩子配置不完整
- **问题描述**: pre-commit-code-review.kiro.hook 的 prompt 字段为空
- **潜在影响**: 钩子无法执行任何有意义的操作
- **建议措施**: 重新配置钩子，添加完整的审查提示词

### 6.3 🟢 优化建议 (P2)

#### P2-001: 建议结合Git Hooks
- **当前状态**: 仅依赖Kiro钩子系统
- **优化建议**: 在 `.git/hooks/pre-commit` 中添加脚本，调用 `scripts/full-check.bat`
- **预期收益**: 实现真正的提交前自动检查

---

## 7. 关键发现总结

### 7.1 核心结论

1. **Kiro钩子系统不支持Git事件**：无法直接监听git commit
2. **最佳替代方案**：使用 `userTriggered` 事件创建手动触发的代码审查钩子
3. **审查内容明确**：项目有完整的规范文档可供参考
4. **现有工具可用**：CodeReview命令和full-check.bat脚本可以复用

### 7.2 技术要点

| 要点 | 说明 | 重要程度 |
|------|------|----------|
| 事件类型限制 | Kiro不支持Git事件 | 高 |
| userTriggered | 推荐使用手动触发 | 高 |
| 审查提示词 | 需要详细的审查指令 | 高 |
| Git Hooks | 可作为补充方案 | 中 |

### 7.3 注意事项
- ⚠️ `promptSubmit` 不是Git提交事件，是用户发送消息事件
- ⚠️ `runCommand` 只能在 `promptSubmit` 和 `agentStop` 中使用
- ⚠️ 当前钩子配置需要修复

---

## 8. 后续任务建议

### 8.1 推荐的实现方案

**方案：创建 `userTriggered` 类型的代码审查钩子**

用户在准备提交代码前，手动点击钩子按钮，触发代码审查。

### 8.2 钩子配置建议

```json
{
  "enabled": true,
  "name": "提交前代码审查",
  "description": "在提交代码前手动触发，分析已修改的代码是否符合项目规范和业界最佳实践",
  "version": "1",
  "when": {
    "type": "userTriggered"
  },
  "then": {
    "type": "askAgent",
    "prompt": "请执行提交前代码审查：\n\n1. **获取变更文件**：运行 `git diff --cached --name-only` 获取暂存区的文件列表\n\n2. **读取变更内容**：运行 `git diff --cached` 获取具体的代码变更\n\n3. **审查内容**：\n   - 架构合规性：检查是否符合Clean Architecture + MVVM\n   - 命名规范：检查类名、方法名、变量名是否符合规范\n   - 层级依赖：检查Domain层是否有Android依赖\n   - 空值处理：检查null安全和空列表处理\n   - 错误处理：检查Result包装和异常捕获\n   - 代码风格：检查Kotlin代码规范\n\n4. **输出格式**：\n   - ✅ 合规项\n   - ⚠️ 警告项\n   - ❌ 不合规项\n   - 📋 改进建议\n\n5. **参考规范**：\n   - .kiro/steering/structure.md\n   - .kiro/steering/tech.md\n   - Rules/开发文档规范.md"
  }
}
```

### 8.3 预估工作量

| 任务 | 预估时间 | 复杂度 | 依赖 |
|------|----------|--------|------|
| 删除错误钩子 | 1分钟 | 低 | 无 |
| 创建新钩子 | 5分钟 | 低 | 无 |
| 测试验证 | 5分钟 | 低 | 新钩子 |

---

## 9. 附录

### 9.1 参考资料
- [Kiro Hooks文档](.kiro/settings/hooks.yaml)
- [CodeReview命令](.kiro/commands/CodeReview.md)
- [项目结构规范](.kiro/steering/structure.md)
- [技术栈规范](.kiro/steering/tech.md)

### 9.2 术语表

| 术语 | 解释 |
|------|------|
| Kiro Hook | Kiro的钩子系统，用于自动化触发Agent操作 |
| userTriggered | 用户手动触发的钩子事件类型 |
| promptSubmit | 用户发送消息时触发的钩子事件类型 |
| Git pre-commit | Git的提交前钩子，在commit前执行 |

---

**文档版本**: 1.0  
**最后更新**: 2025-12-23
