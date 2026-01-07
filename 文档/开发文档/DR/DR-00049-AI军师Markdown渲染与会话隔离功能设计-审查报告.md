# FD-00030 文档审查报告

> **审查日期**: 2026-01-07
> **审查人**: Code Reviewer
> **审查状态**: 🔄 需修改后通过

---

## 📄 文档信息

| 项目 | 内容 |
|------|------|
| **文档编号** | FD-00030 |
| **文档类型** | FD (功能设计文档) |
| **文档标题** | AI军师Markdown渲染与会话隔离功能设计 |
| **存放路径** | `文档/开发文档/FD/FD-00030-AI军师Markdown渲染与会话隔离功能设计.md` |
| **版本** | 1.0 |
| **创建日期** | 2026-01-07 |
| **关联文档** | PRD-00030, TDD-00030, FD-00026, TDD-00026, FD-00029, TDD-00029 |

---

## 📊 质量评分

| 评估维度 | 评分 | 说明 |
|----------|------|------|
| **格式规范性** | 9/10 | 文档命名、格式符合规范，章节结构清晰 |
| **内容完整性** | 8/10 | 功能设计完整，但缺少性能指标量化 |
| **文档质量** | 7/10 | 存在重复内容，错误处理描述可更详细 |
| **开发可行性** | 8/10 | 技术方案可行，代码示例充分 |
| **架构符合性** | 9/10 | 符合Clean Architecture + MVVM模式 |
| **功能集成性** | 7/10 | 存在DI模块配置错误，文件清单分类不准确 |
| **整体评分** | **7.7/10** | 良好，需修复严重问题后通过 |

---

## ✅ 优点

### 1. **功能架构设计清晰**
- 功能架构图直观展示了Markdown渲染和会话隔离两个功能的分层结构
- 数据流设计详细，从AI返回文本到最终渲染的调用链清晰

### 2. **Markdown渲染设计完整**
- 支持的Markdown语法分类明确（粗体、斜体、列表、代码等）
- 样式配置详细，包括代码块和行内代码的具体样式参数
- 提供了`SafeMarkdown`降级处理方案，增强了容错能力

### 3. **会话隔离逻辑设计合理**
- 上下文构成表格清晰区分新建会话和继续会话的差异
- Prompt模板设计完整，包含联系人画像、会话历史、当前问题的标准格式
- 关键代码变更示例明确，便于开发实现

### 4. **测试计划覆盖全面**
- 单元测试、UI测试、集成测试均有覆盖
- 测试用例设计具体，包含边界条件测试（如空标签、标签数量限制）
- 提供了完整的测试代码示例

### 5. **风险评估充分**
- 识别了主要风险点（Markdown库兼容性、流式渲染性能、联系人画像获取失败等）
- 针对每种风险提供了缓解措施

---

## ⚠️ 需要改进项

### IMP-001: 缺少性能指标量化

**问题描述**: 文档未明确提供性能指标要求，如Markdown渲染时间、内存占用等。

**建议改进**:
```markdown
### 性能指标要求

| 指标 | 目标值 | 说明 |
|------|--------|------|
| Markdown单条消息渲染时间 | < 50ms | 使用Compose性能追踪 |
| 流式渲染帧率 | > 30fps | 监控recomposition次数 |
| 联系人画像加载时间 | < 100ms | 在buildPrompt方法中添加计时日志 |
| 会话历史加载时间 | < 200ms | 在getConversationsBySession调用处添加计时日志 |
```

### IMP-002: 错误处理描述可更详细

**问题描述**: 错误处理设计较为简单，未详细说明Markdown解析失败的场景和应对策略。

**建议改进**:
```markdown
### 7.1 Markdown渲染错误处理

| 错误场景 | 处理方式 | 降级效果 |
|----------|----------|----------|
| Markdown语法解析失败 | 降级为普通文本显示 | 保留完整内容，丢失格式 |
| 不支持的Markdown语法 | 原样显示文本 | 显示原始Markdown源码 |
| Markdown库加载失败 | 降级为普通Text | 显示纯文本内容 |
| 流式渲染中断 | 显示已接收内容 | 用户可刷新重试 |
| 内存不足导致渲染失败 | 清理缓存并重试 | 显示部分内容 |
```

### IMP-003: 文档存在重复内容

**问题描述**: Markdown样式配置在多处重复描述（第209-229行和第466-522行），增加文档维护成本。

**建议改进**: 将样式配置统一到`AiAdvisorMarkdownStyle`组件部分，并在调用处引用。

---

## ❌ 严重问题

### CRIT-001: DI模块配置错误

**问题描述**: 文档第778行提到需要更新`FloatingWindowModule`添加BrainTagRepository依赖，但根据项目架构（[`structure.md`](.kiro/steering/structure.md:572)），`SendAdvisorMessageStreamingUseCase`应该在`AiAdvisorModule`中注册，而不是`FloatingWindowModule`。

**错误引用**:
```
| :app | `di/FloatingWindowModule.kt` | 添加BrainTagRepository注入到UseCase |
```

**正确方案**:
```kotlin
// 文件位置: app/src/main/java/com/empathy/ai/di/AiAdvisorModule.kt

@Module
@InstallIn(SingletonComponent::class)
object AiAdvisorModule {

    @Provides
    @Singleton
    fun provideSendAdvisorMessageStreamingUseCase(
        aiAdvisorRepository: AiAdvisorRepository,
        aiRepository: AiRepository,
        contactRepository: ContactRepository,
        aiProviderRepository: AiProviderRepository,
        brainTagRepository: BrainTagRepository  // 🆕 新增依赖
    ): SendAdvisorMessageStreamingUseCase {
        return SendAdvisorMessageStreamingUseCase(
            aiAdvisorRepository,
            aiRepository,
            contactRepository,
            aiProviderRepository,
            brainTagRepository
        )
    }
}
```

**修复优先级**: 🔴 **P0（必须修复）**

---

### CRIT-002: 文件清单分类错误

**问题描述**: 文档第767-769行将已存在的文件错误标记为"新增"。

**错误分类**:
```
| :domain | `repository/AiAdvisorRepository.kt` | 新增`getConversationsBySession`方法 |
| :data | `repository/AiAdvisorRepositoryImpl.kt` | 实现`getConversationsBySession`方法 |
| :data | `local/dao/AiAdvisorDao.kt` | 新增`getConversationsBySession`查询 |
```

**正确分类**:
```
| :domain | `repository/AiAdvisorRepository.kt` | 修改 - 新增`getConversationsBySession`方法签名 |
| :data | `repository/AiAdvisorRepositoryImpl.kt` | 修改 - 实现`getConversationsBySession`方法 |
| :data | `local/dao/AiAdvisorDao.kt` | 修改 - 新增`getConversationsBySession`查询 |
```

**修复优先级**: 🔴 **P0（必须修复）**

---

### CRIT-003: 文件清单中AiAdvisorMarkdownStyle和SafeMarkdown组件位置描述不准确

**问题描述**: 文档第765-766行描述的新增组件路径不够完整。

**当前描述**:
```
| :presentation | `ui/screen/advisor/component/AiAdvisorMarkdownStyle.kt` | Markdown样式配置组件 |
| :presentation | `ui/screen/advisor/component/SafeMarkdown.kt` | 安全Markdown渲染组件（带降级处理） |
```

**建议改进**: 建议在`ui/component/markdown/`目录下创建专门的Markdown组件包，与项目现有的`presentation/src/main/kotlin/com/empathy/ai/presentation/ui/component/`结构保持一致。

**修复优先级**: 🟡 **P1（建议修复）**

---

## 🔗 前置文档一致性

### 与 PRD-00030 一致性检查

| 需求项 | PRD描述 | FD实现 | 一致性 |
|--------|---------|--------|--------|
| Markdown粗体渲染 | `**文字**`显示为粗体 | 使用Markdown组件渲染粗体 | ✅ 一致 |
| Markdown斜体渲染 | `*文字*`显示为斜体 | 使用Markdown组件渲染斜体 | ✅ 一致 |
| Markdown列表渲染 | `- 项目`显示为列表 | 使用Markdown组件渲染列表 | ✅ 一致 |
| Markdown行内代码 | `` `code` ``显示为灰色背景 | 配置inlineCodeBackground | ✅ 一致 |
| Markdown代码块 | 代码块显示灰色背景区域 | 配置codeBackground | ✅ 一致 |
| 会话隔离 | 新会话只获取当前会话历史 | 使用getConversationsBySession | ✅ 一致 |
| 联系人画像增强 | 包含标签和事实流 | buildPrompt方法增强 | ✅ 一致 |

**结论**: FD-00030 与 PRD-00030 完全一致，所有功能点都有对应的设计方案。

---

### 与 TDD-00030 一致性检查

| 设计项 | TDD描述 | FD实现 | 一致性 |
|--------|---------|--------|--------|
| 技术选型 | compose-markdown 0.5.4 | compose-markdown 0.5.4 | ✅ 一致 |
| ChatBubble修改 | Text → Markdown | Text → Markdown | ✅ 一致 |
| StreamingMessageBubble修改 | Text → Markdown | Text → Markdown | ✅ 一致 |
| UseCase依赖 | 新增BrainTagRepository | 新增BrainTagRepository | ✅ 一致 |
| 历史获取逻辑 | 按sessionId获取 | 按sessionId获取 | ✅ 一致 |
| buildPrompt增强 | 添加联系人画像 | 添加联系人画像 | ✅ 一致 |
| DI模块 | AiAdvisorModule | **错误引用FloatingWindowModule** | ❌ 不一致 |

**结论**: FD-00030 与 TDD-00030 基本一致，存在DI模块引用不一致问题（见CRIT-001）。

---

## 🔗 功能集成完整性检查

### 功能组件清单

| 组件类型 | 组件名称 | 是否需要修改 | 集成状态 |
|---------|---------|-------------|----------|
| **UI组件** | `AiAdvisorChatScreen.kt` | 是 | ✅ 已有设计方案 |
| **UI组件** | `ChatBubble` | 是 | ✅ 已有设计方案 |
| **UI组件** | `StreamingMessageBubble.kt` | 是 | ✅ 已有设计方案 |
| **UI组件** | `MainTextBubble` | 是 | ✅ 已有设计方案 |
| **新增组件** | `AiAdvisorMarkdownStyle.kt` | 是 | ✅ 已有设计方案 |
| **新增组件** | `SafeMarkdown.kt` | 是 | ✅ 已有设计方案 |
| **UseCase** | `SendAdvisorMessageStreamingUseCase` | 是 | ✅ 已有设计方案 |
| **Repository** | `AiAdvisorRepository` | 是（修改） | ✅ 方法已定义 |
| **Repository** | `AiAdvisorRepositoryImpl` | 是（修改） | ✅ 方法已定义 |
| **DAO** | `AiAdvisorDao` | 是（修改） | ✅ 查询已定义 |
| **Repository** | `BrainTagRepository` | 否 | ✅ 已存在 |
| **Repository** | `ContactRepository` | 否 | ✅ 已存在 |

---

### 集成点验证

| 集成点 | 相关文件 | 状态 | 说明 |
|--------|---------|------|------|
| **UI组件修改** | `AiAdvisorChatScreen.kt` | ✅ | ChatBubble组件使用Markdown渲染 |
| **UI组件修改** | `StreamingMessageBubble.kt` | ✅ | MainTextBubble组件使用Markdown渲染 |
| **新增组件** | `AiAdvisorMarkdownStyle.kt` | ✅ | 统一Markdown样式配置 |
| **新增组件** | `SafeMarkdown.kt` | ✅ | 带降级处理的Markdown渲染 |
| **UseCase修改** | `SendAdvisorMessageStreamingUseCase.kt` | ✅ | 新增依赖，修改历史获取逻辑 |
| **Repository修改** | `AiAdvisorRepository.kt` | ✅ | 新增getConversationsBySession方法 |
| **Repository实现** | `AiAdvisorRepositoryImpl.kt` | ✅ | 实现getConversationsBySession方法 |
| **DAO修改** | `AiAdvisorDao.kt` | ✅ | 新增getConversationsBySession查询 |
| **DI模块修改** | `AiAdvisorModule.kt` | ⚠️ | **需修正为AiAdvisorModule而非FloatingWindowModule** |
| **DI模块修改** | `FloatingWindowModule.kt` | ❌ | 不应在此处修改 |

---

### 调用链完整性

#### Markdown渲染调用链

```
AiAdvisorChatScreen
    ↓ (ChatBubble组件)
Markdown(content = conversation.content, ...)
    ↓ (compose-markdown库解析)
解析Markdown语法 → 生成Compose UI节点
    ↓
渲染为格式化UI（粗体、斜体、列表、代码块等）
```

**状态**: ✅ 完整

#### 会话隔离调用链

```
AiAdvisorChatViewModel.sendMessage()
    ↓
SendAdvisorMessageStreamingUseCase(contactId, sessionId, message)
    ↓ (获取联系人画像)
ContactRepository.getProfile(contactId)
BrainTagRepository.getTagsByContact(contactId)
ContactRepository.getFactsByContact(contactId, limit=5)
    ↓ (获取当前会话历史 - 关键修改点)
AiAdvisorRepository.getConversationsBySession(sessionId, limit)
    ↓ (构建增强提示词)
buildPrompt(contactId, contactName, brainTags, facts, history, userMessage)
    ↓ (调用AI)
AiRepository.generateTextStream(provider, prompt, systemInstruction)
```

**状态**: ✅ 完整

---

### 导航集成状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 新增页面 | ❌ 不需要 | 本需求不涉及新页面 |
| 导航变更 | ✅ 不需要 | AI军师对话界面已存在 |

**结论**: 本需求不涉及导航系统变更，无需检查NavGraph注册。

---

### 数据库集成状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 新增Entity | ❌ 不需要 | 使用现有的AiAdvisorConversationEntity |
| 新增DAO方法 | ✅ 已有设计 | getConversationsBySession |
| 数据库Migration | ❌ 不需要 | 不修改表结构 |
| Entity注册 | ✅ 无需修改 | 使用现有Entity |

---

### 字符串资源状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 新增字符串 | ❌ 不需要 | 本需求为后端逻辑变更 |

---

## 📋 改进建议汇总

### 优先级 P0（必须修复）

| 编号 | 问题 | 建议操作 |
|------|------|----------|
| CRIT-001 | DI模块配置错误 | 将FloatingWindowModule改为AiAdvisorModule |
| CRIT-002 | 文件清单分类错误 | 将"新增"改为"修改" |

### 优先级 P1（建议修复）

| 编号 | 问题 | 建议操作 |
|------|------|----------|
| IMP-001 | 缺少性能指标 | 添加性能指标量化表格 |
| IMP-002 | 错误处理描述不详细 | 补充错误场景和处理方式表格 |
| IMP-003 | 文档内容重复 | 统一样式配置描述 |
| CRIT-003 | 组件路径描述不准确 | 明确组件包结构 |

---

## 📋 审查结论

**审查结果**: 🔴 **需修改后通过**

FD-00030文档整体质量良好，功能设计完整，技术方案可行，与PRD-00030和TDD-00030保持高度一致。但存在以下**必须修复**的严重问题：

1. **DI模块配置错误**（CRIT-001）: 文档错误引用`FloatingWindowModule`作为`SendAdvisorMessageStreamingUseCase`的DI模块，实际应在`AiAdvisorModule`中配置。

2. **文件清单分类错误**（CRIT-002）: 将已存在的文件错误标记为"新增"，应改为"修改"。

请在修复上述P0优先级问题后，重新提交审查。

---

**审查人**: Code Reviewer  
**审查日期**: 2026-01-07  
**下次审查**: 修复P0问题后
