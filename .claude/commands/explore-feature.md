---
description: 启动功能开发探索智能体 - 在独立工作树中根据 PRD 文档探索功能实现
---

# 功能开发探索

启动 **Feature Explorer** 智能体，在独立 Git 工作树中根据 PRD 文档探索功能实现。

## 使用场景

- 有 PRD 文档，想要探索实现方案
- 想要验证功能可行性
- 需要低风险的开发环境
- 让 AI 先试着实现，看看效果

## 执行流程

### 1. 读取项目规范

阅读以下文档了解项目规范：
- **CLAUDE.md** - 项目主文档
- **.kiro/steering/** - 项目 steering 文件
- **skills/multi-agent-explorer/references/project-standards.md** - 项目规范

### 2. 创建决策日志

**🔴 重要：在开始任何工作前，必须创建决策日志文件**

```bash
# 复制决策日志模板
cp skills/multi-agent-explorer/templates/DECISION_JOURNAL.template.md DECISION_JOURNAL.md
```

决策日志是你最重要的输出之一。即使无法完成任务，详细的决策日志也是成功，因为它为后续智能体铺平了道路。

### 3. 理解需求

请提供以下信息：
- PRD 文档路径或内容
- 功能描述
- 验收标准

### 4. 开始探索

按照 `skills/multi-agent-explorer/agents/feature-explorer.md` 中定义的工作流程：

1. **需求分析** - 理解 PRD 和用户故事
2. **技术设计** - 设计数据模型和接口
3. **实现开发** - 按 Clean Architecture 分层实现
4. **测试验证** - 编写测试，验证功能
5. **生成报告** - 保存到 `文档/开发文档/MA/FEATURE/`

## 开发顺序

标准的功能开发顺序：

1. **Domain 层**：Model → Repository 接口 → UseCase
2. **Data 层**：Entity → DAO → Repository 实现
3. **Presentation 层**：UiState/UiEvent → ViewModel → Screen
4. **DI 配置**：在对应 Module 中注册依赖

## 工作原则

- ✅ **自主决策**：无需频繁询问用户，按最佳实践执行
- ✅ **大胆尝试**：这是沙盒环境，可以自由试错
- ✅ **记录一切**：记录所有设计决策
- ✅ **报告优先**：即使失败也要生成报告
- ✅ **🔴 决策日志**：实时更新决策日志，记录每个决策点

## 决策日志要求

**每30分钟至少更新一次决策日志**，记录：
1. 遇到什么问题？
2. 考虑了哪些方案？
3. 为什么选择这个方案？
4. 放弃的方案为什么不选？
5. 学到了什么教训？

## 约束条件

- 必须遵循 Clean Architecture
- 每个 UseCase 必须有对应测试
- 报告保存到 `文档/开发文档/MA/FEATURE/`

## 失败处理

如果长时间无法完成：
1. 停止尝试
2. 记录已完成的部分
3. 分析阻塞原因
4. 生成部分完成报告

---

**现在请告诉我你要开发的功能是什么？或者提供 PRD 文档路径。**

