# TDD-00030: AI军师Markdown渲染与会话隔离技术设计

## 1. 文档信息

| 项目 | 内容 |
|------|------|
| 文档类型 | TDD (Technical Design Document) |
| 文档编号 | TDD-00030 |
| 功能名称 | AI军师Markdown渲染与会话隔离技术设计 |
| 版本 | 1.0 |
| 创建日期 | 2026-01-07 |
| 最后更新 | 2026-01-07 |
| 作者 | Kiro |
| 审核人 | - |
| 审核状态 | 🔄 待审核 |
| 关联文档 | PRD-00030, TDD-00026, TDD-00029 |

### 1.1 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-01-07 | Kiro | 初始版本 |

### 1.2 参考标准

| 标准文档 | 版本 | 说明 |
|---------|------|------|
| Clean Architecture | - | 架构模式标准 |
| MVVM Pattern | - | UI架构模式 |
| Kotlin Coding Conventions | 2.0.21 | 代码规范 |
| CommonMark Spec | 0.30 | Markdown语法标准 |

---

## 2. 架构概述

### 2.1 架构目标

本次技术设计实现两个核心功能：

1. **Markdown语法渲染** - AI回复支持基础Markdown格式显示
2. **会话上下文隔离** - 新会话只包含联系人画像信息，不包含历史对话

**核心目标**：
- 使用compose-markdown库实现Markdown渲染
- 修改历史获取逻辑，从按contactId改为按sessionId
- 增强联系人画像信息构建，提供更丰富的上下文

### 2.2 技术栈

| 技术领域 | 技术选择 | 版本 | 用途 |
|---------|----------|------|------|
| UI框架 | Jetpack Compose | BOM 2024.12.01 | 声明式UI |
| Markdown渲染 | compose-markdown | 0.5.4 | Markdown解析和渲染 |
| 依赖注入 | Hilt | 2.52 | 依赖管理 |
| 状态管理 | StateFlow | 1.9.0 | UI状态 |
| 数据流 | Kotlin Flow | 1.9.0 | 响应式数据流 |

### 2.3 设计原则

- **最小修改原则**：只修改必要的文件，保持现有架构稳定
- **向后兼容**：确保现有功能不受影响
- **单一职责**：Markdown渲染和会话隔离独立实现
- **可测试性**：所有修改都有对应的单元测试

---

## 3. 整体架构设计

### 3.1 功能架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Markdown渲染功能                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  :presentation 模块                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AiAdvisorChatScreen.kt                                │  │
│  │   └── ChatBubble组件                                  │  │
│  │       └── Text() → Markdown()  ← 修改点              │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ StreamingMessageBubble.kt                             │  │
│  │   └── MainTextBubble组件                              │  │
│  │       └── Text() → Markdown()  ← 修改点              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    会话隔离功能                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  :domain 模块                                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SendAdvisorMessageStreamingUseCase.kt                 │  │
│  │   └── 第147行修改：                                   │  │
│  │       getRecentConversations(contactId, limit)        │  │
│  │       → getConversationsBySession(sessionId, limit)   │  │
│  │   └── buildPrompt方法增强：                           │  │
│  │       添加联系人画像信息（标签、事实流）              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 数据流图

```
┌─────────────────────────────────────────────────────────────┐
│                    Markdown渲染数据流                        │
└─────────────────────────────────────────────────────────────┘

  AI返回Markdown文本
         │
         ▼
  ┌──────────────┐
  │ AiRepository │ ← 流式返回文本
  └──────────────┘
         │
         ▼
  ┌──────────────┐
  │ ViewModel    │ ← 更新streamingContent
  └──────────────┘
         │
         ▼
  ┌──────────────┐
  │ Markdown()   │ ← compose-markdown组件
  │   组件       │    解析并渲染Markdown
  └──────────────┘
         │
         ▼
  ┌──────────────┐
  │ 渲染后的UI   │ ← 粗体、斜体、列表等
  └──────────────┘


┌─────────────────────────────────────────────────────────────┐
│                    会话隔离数据流                            │
└─────────────────────────────────────────────────────────────┘

  用户发送消息
         │
         ▼
  ┌──────────────────────────────────────┐
  │ SendAdvisorMessageStreamingUseCase   │
  └──────────────────────────────────────┘
         │
    ┌────┴────────────────────┐
    ▼                         ▼
┌────────────────┐    ┌────────────────┐
│ 获取联系人画像 │    │ 获取会话历史   │
│ (contactId)    │    │ (sessionId)    │ ← 修改点
└────────────────┘    └────────────────┘
    │                         │
    └────────┬────────────────┘
             ▼
  ┌──────────────────────────────────────┐
  │ buildPrompt()                        │
  │ = 联系人画像 + 当前会话历史 + 用户消息│
  └──────────────────────────────────────┘
             │
             ▼
  ┌──────────────────────────────────────┐
  │ AiRepository.generateTextStream()    │
  └──────────────────────────────────────┘
```



---

## 4. 详细技术设计

### 4.1 Markdown渲染实现

#### 4.1.1 依赖配置

**文件位置**：`gradle/libs.versions.toml`

```toml
[versions]
compose-markdown = "0.5.4"

[libraries]
compose-markdown = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "compose-markdown" }
```

**文件位置**：`presentation/build.gradle.kts`

```kotlin
dependencies {
    // Markdown渲染
    implementation(libs.compose.markdown)
}
```

#### 4.1.2 ChatBubble组件修改

**文件位置**：`presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

**修改前**（ChatBubble组件内，约第520行）：

```kotlin
Text(
    text = conversation.content,
    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    color = when {
        isFailed || isCancelled -> iOSRed
        isUser -> Color.White
        else -> iOSTextPrimary
    },
    fontSize = 16.sp,
    lineHeight = 22.sp
)
```

**修改后**：

```kotlin
// 用户消息使用普通Text，AI消息使用Markdown渲染
if (isUser) {
    Text(
        text = conversation.content,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        color = Color.White,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
} else {
    // AI消息使用Markdown渲染
    // PRD-00030: 支持粗体、斜体、列表、行内代码、代码块
    Markdown(
        content = conversation.content,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        colors = markdownColors(
            text = when {
                isFailed || isCancelled -> iOSRed
                else -> iOSTextPrimary
            },
            codeBackground = Color(0xFFF5F5F5),
            inlineCodeBackground = Color(0xFFE8E8E8)
        ),
        typography = markdownTypography(
            text = TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = iOSTextPrimary
            ),
            code = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = iOSTextPrimary
            )
        )
    )
}
```

#### 4.1.3 MainTextBubble组件修改

**文件位置**：`presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/component/StreamingMessageBubble.kt`

**修改前**（MainTextBubble组件内，约第130行）：

```kotlin
Row {
    Text(
        text = content,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = iOSTextPrimary
    )
    if (isStreaming) {
        StreamingCursor()
    }
}
```

**修改后**：

```kotlin
Row {
    // PRD-00030: 流式消息也使用Markdown渲染
    Markdown(
        content = content,
        colors = markdownColors(
            text = iOSTextPrimary,
            codeBackground = Color(0xFFF5F5F5),
            inlineCodeBackground = Color(0xFFE8E8E8)
        ),
        typography = markdownTypography(
            text = TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = iOSTextPrimary
            ),
            code = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = iOSTextPrimary
            )
        )
    )
    if (isStreaming) {
        StreamingCursor()
    }
}
```

#### 4.1.4 Markdown样式配置

**代码块样式**：

| 属性 | 值 | 说明 |
|------|-----|------|
| 背景色 | `#F5F5F5` | 浅灰色背景 |
| 圆角 | 8dp | 圆角矩形 |
| 内边距 | 12dp | 内部间距 |
| 字体 | Monospace | 等宽字体 |
| 字号 | 14sp | 略小于正文 |

**行内代码样式**：

| 属性 | 值 | 说明 |
|------|-----|------|
| 背景色 | `#E8E8E8` | 灰色背景 |
| 圆角 | 4dp | 小圆角 |
| 内边距 | 水平4dp, 垂直2dp | 紧凑间距 |
| 字体 | Monospace | 等宽字体 |

---

### 4.2 会话隔离实现

#### 4.2.1 SendAdvisorMessageStreamingUseCase修改

**文件位置**：`domain/src/main/kotlin/com/empathy/ai/domain/usecase/SendAdvisorMessageStreamingUseCase.kt`

**修改1：新增依赖注入**

```kotlin
// 修改前
class SendAdvisorMessageStreamingUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    private val contactRepository: ContactRepository,
    private val aiProviderRepository: AiProviderRepository
)

// 修改后 - 新增BrainTagRepository依赖
class SendAdvisorMessageStreamingUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    private val contactRepository: ContactRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val brainTagRepository: BrainTagRepository  // 🆕 新增
)
```

**修改2：历史获取逻辑（第147行）**

```kotlin
// 修改前
val historyResult = aiAdvisorRepository.getRecentConversations(contactId, DEFAULT_HISTORY_LIMIT)

// 修改后 - 按sessionId获取，实现会话隔离
val historyResult = aiAdvisorRepository.getConversationsBySession(sessionId, DEFAULT_HISTORY_LIMIT)
```

**修改3：buildPrompt方法增强**

```kotlin
// 修改前
private fun buildPrompt(
    contactName: String?,
    history: List<AiAdvisorConversation>,
    userMessage: String
): String {
    val sb = StringBuilder()

    // 联系人信息
    if (!contactName.isNullOrBlank()) {
        sb.appendLine("【联系人】$contactName")
        sb.appendLine()
    }
    // ... 其他代码
}

// 修改后 - 增强联系人画像信息
private suspend fun buildPrompt(
    contactId: String,
    contactName: String?,
    history: List<AiAdvisorConversation>,
    userMessage: String
): String {
    val sb = StringBuilder()

    // 联系人画像信息（PRD-00030增强）
    sb.appendLine("【联系人画像】")
    sb.appendLine("姓名: ${contactName ?: "未知"}")
    
    // 获取联系人标签
    val brainTags = brainTagRepository.getTagsByContact(contactId).getOrNull()
    if (!brainTags.isNullOrEmpty()) {
        sb.appendLine("标签: ${brainTags.joinToString(", ") { it.content }}")
    }
    
    // 获取联系人事实流（最近5条）
    val facts = contactRepository.getFactsByContact(contactId, limit = 5).getOrNull()
    if (!facts.isNullOrEmpty()) {
        sb.appendLine("重要事实:")
        facts.forEach { fact ->
            sb.appendLine("- ${fact.content}")
        }
    }
    sb.appendLine()

    // 对话历史（当前会话）
    if (history.isNotEmpty()) {
        sb.appendLine("【当前会话历史】")
        history.sortedBy { it.timestamp }.forEach { conv ->
            val role = if (conv.messageType == MessageType.USER) "用户" else "AI军师"
            sb.appendLine("$role: ${conv.content}")
        }
        sb.appendLine()
    }

    // 当前问题
    sb.appendLine("【当前问题】")
    sb.appendLine(userMessage)

    return sb.toString()
}
```

#### 4.2.2 invoke方法调用修改

```kotlin
// 修改buildPrompt调用
val prompt = buildPrompt(contactId, contact?.name, history, userMessage)
```

---

## 5. Repository接口确认

### 5.1 AiAdvisorRepository

**文件位置**：`domain/src/main/kotlin/com/empathy/ai/domain/repository/AiAdvisorRepository.kt`

需要确认以下方法存在：

```kotlin
interface AiAdvisorRepository {
    /**
     * 按会话ID获取对话历史
     * PRD-00030: 会话隔离功能需要此方法
     * 
     * @param sessionId 会话ID
     * @param limit 最大返回数量
     * @return 对话列表，按时间正序
     */
    suspend fun getConversationsBySession(sessionId: String, limit: Int): Result<List<AiAdvisorConversation>>
    
    // ... 其他现有方法 ...
}
```

### 5.2 BrainTagRepository

**文件位置**：`domain/src/main/kotlin/com/empathy/ai/domain/repository/BrainTagRepository.kt`

需要确认以下方法存在：

```kotlin
interface BrainTagRepository {
    /**
     * 获取联系人的所有标签
     * 
     * @param contactId 联系人ID
     * @return 标签列表
     */
    suspend fun getTagsByContact(contactId: String): Result<List<BrainTag>>
    
    // ... 其他现有方法 ...
}
```

### 5.3 ContactRepository

**文件位置**：`domain/src/main/kotlin/com/empathy/ai/domain/repository/ContactRepository.kt`

需要确认以下方法存在（如不存在需新增）：

```kotlin
interface ContactRepository {
    /**
     * 获取联系人的事实流
     * PRD-00030: 联系人画像增强需要此方法
     * 
     * @param contactId 联系人ID
     * @param limit 最大返回数量
     * @return 事实列表
     */
    suspend fun getFactsByContact(contactId: String, limit: Int): Result<List<ContactFact>>
    
    // ... 其他现有方法 ...
}
```



---

## 6. DI模块集成

### 6.1 现有DI模块复用

本次修改无需新增DI模块，只需更新现有模块配置。

### 6.2 AiAdvisorModule修改

**文件位置**：`app/src/main/java/com/empathy/ai/di/AiAdvisorModule.kt`

```kotlin
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
            brainTagRepository  // 🆕 新增依赖
        )
    }
    
    // ... 其他现有配置 ...
}
```

### 6.3 依赖关系图

```
┌─────────────────────────────────────────────────────────────┐
│                SendAdvisorMessageStreamingUseCase           │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│AiAdvisor     │    │Contact       │    │BrainTag      │
│Repository    │    │Repository    │    │Repository    │ ← 🆕 新增
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│AiAdvisorDao  │    │ContactDao    │    │BrainTagDao   │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## 7. 调用链设计

### 7.1 Markdown渲染调用链

```
AiAdvisorChatScreen
    ↓ (ChatBubble组件)
conversation.content (Markdown文本)
    ↓
Markdown(content = conversation.content, ...)
    ↓ (compose-markdown库解析)
解析Markdown语法 → 生成Compose UI节点
    ↓
渲染为格式化UI（粗体、斜体、列表、代码块等）
```

### 7.2 流式Markdown渲染调用链

```
AiAdvisorChatViewModel
    ↓ (收集流式响应)
uiState.streamingContent (累积的Markdown文本)
    ↓
StreamingMessageBubbleSimple
    ↓ (MainTextBubble组件)
Markdown(content = content, ...)
    ↓ (实时渲染)
流式显示格式化内容
```

### 7.3 会话隔离调用链

```
AiAdvisorChatViewModel.sendMessage()
    ↓
SendAdvisorMessageStreamingUseCase(contactId, sessionId, message)
    ↓ (获取联系人画像)
ContactRepository.getProfile(contactId)
BrainTagRepository.getTagsByContact(contactId)
ContactRepository.getFactsByContact(contactId, limit=5)
    ↓ (获取当前会话历史 - 🆕 修改点)
AiAdvisorRepository.getConversationsBySession(sessionId, limit)
    ↓ (构建增强提示词)
buildPrompt(contactId, contactName, brainTags, facts, history, userMessage)
    ↓ (调用AI)
AiRepository.generateTextStream(provider, prompt, systemInstruction)
```

---

## 8. 文件变更清单

### 8.1 新增文件

| 文件路径 | 模块 | 说明 |
|---------|------|------|
| 无 | - | 本需求不需要新增文件 |

### 8.2 修改文件

| 文件路径 | 模块 | 修改内容 |
|---------|------|----------|
| `gradle/libs.versions.toml` | gradle | 添加compose-markdown版本和依赖声明 |
| `presentation/build.gradle.kts` | :presentation | 添加compose-markdown依赖 |
| `AiAdvisorChatScreen.kt` | :presentation | ChatBubble组件使用Markdown渲染AI消息 |
| `StreamingMessageBubble.kt` | :presentation | MainTextBubble组件使用Markdown渲染 |
| `SendAdvisorMessageStreamingUseCase.kt` | :domain | 1. 新增BrainTagRepository依赖<br>2. 修改历史获取为按sessionId<br>3. 增强buildPrompt方法 |
| `AiAdvisorModule.kt` | :app | 更新UseCase依赖注入配置 |

### 8.3 需确认文件

| 文件路径 | 模块 | 确认内容 |
|---------|------|----------|
| `AiAdvisorRepository.kt` | :domain | 确认getConversationsBySession方法存在 |
| `BrainTagRepository.kt` | :domain | 确认getTagsByContact方法存在 |
| `ContactRepository.kt` | :domain | 确认getFactsByContact方法存在（如不存在需新增） |

---

## 9. 测试计划

### 9.1 单元测试

| 测试类 | 测试内容 | 优先级 |
|--------|----------|--------|
| `SendAdvisorMessageStreamingUseCaseTest` | 会话隔离逻辑测试 | P0 |
| `MarkdownRenderingTest` | Markdown渲染效果测试 | P0 |

#### 9.1.1 会话隔离测试用例

```kotlin
@Test
fun `新会话应只获取当前会话历史`() {
    // Given
    val contactId = "contact-1"
    val sessionId = "session-new"
    val userMessage = "你好"
    
    // 模拟：contactId下有多个会话的历史
    coEvery { 
        aiAdvisorRepository.getConversationsBySession(sessionId, any()) 
    } returns Result.success(emptyList())  // 新会话无历史
    
    // When
    useCase(contactId, sessionId, userMessage).collect { }
    
    // Then
    coVerify { 
        aiAdvisorRepository.getConversationsBySession(sessionId, any())
    }
    coVerify(exactly = 0) { 
        aiAdvisorRepository.getRecentConversations(any(), any())
    }
}

@Test
fun `buildPrompt应包含联系人画像信息`() {
    // Given
    val contactId = "contact-1"
    val brainTags = listOf(
        BrainTag(id = "1", contactId = contactId, content = "喜欢旅游"),
        BrainTag(id = "2", contactId = contactId, content = "工作压力大")
    )
    
    coEvery { brainTagRepository.getTagsByContact(contactId) } returns Result.success(brainTags)
    
    // When
    val prompt = buildPrompt(contactId, "张三", emptyList(), "你好")
    
    // Then
    assertThat(prompt).contains("【联系人画像】")
    assertThat(prompt).contains("姓名: 张三")
    assertThat(prompt).contains("标签: 喜欢旅游, 工作压力大")
}
```

### 9.2 UI测试

| 测试类 | 测试内容 | 优先级 |
|--------|----------|--------|
| `ChatBubbleMarkdownTest` | AI消息Markdown渲染测试 | P1 |
| `StreamingMessageMarkdownTest` | 流式消息Markdown渲染测试 | P1 |

#### 9.2.1 Markdown渲染测试用例

```kotlin
@Test
fun `AI消息应正确渲染粗体文本`() {
    composeTestRule.setContent {
        ChatBubble(
            conversation = AiAdvisorConversation(
                id = "1",
                content = "这是**粗体**文本",
                messageType = MessageType.AI,
                // ...
            ),
            // ...
        )
    }
    
    // 验证粗体文本被正确渲染
    composeTestRule.onNodeWithText("粗体").assertExists()
}

@Test
fun `AI消息应正确渲染代码块`() {
    composeTestRule.setContent {
        ChatBubble(
            conversation = AiAdvisorConversation(
                id = "1",
                content = "```\ncode block\n```",
                messageType = MessageType.AI,
                // ...
            ),
            // ...
        )
    }
    
    // 验证代码块被正确渲染（灰色背景）
    composeTestRule.onNodeWithText("code block").assertExists()
}
```

### 9.3 集成测试

| 测试场景 | 测试内容 | 优先级 |
|----------|----------|--------|
| 新会话隔离 | 新建会话时AI无法获取之前会话历史 | P0 |
| 画像信息传递 | AI提示词包含联系人标签和事实流 | P0 |
| Markdown流式渲染 | 流式响应时Markdown实时渲染 | P0 |

---

## 10. 任务分解

### 10.1 Phase 1: Markdown渲染（预计1天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T030-01 | 添加compose-markdown依赖到libs.versions.toml | 0.5h | - |
| T030-02 | 添加compose-markdown依赖到presentation模块 | 0.5h | T030-01 |
| T030-03 | 修改ChatBubble组件使用Markdown渲染 | 1h | T030-02 |
| T030-04 | 修改MainTextBubble组件使用Markdown渲染 | 1h | T030-02 |
| T030-05 | 编写Markdown渲染单元测试 | 1h | T030-03, T030-04 |

### 10.2 Phase 2: 会话隔离（预计1.5天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T030-06 | 确认AiAdvisorRepository.getConversationsBySession方法存在 | 0.5h | - |
| T030-07 | 确认BrainTagRepository.getTagsByContact方法存在 | 0.5h | - |
| T030-08 | 确认/新增ContactRepository.getFactsByContact方法 | 1h | - |
| T030-09 | 修改SendAdvisorMessageStreamingUseCase添加BrainTagRepository依赖 | 0.5h | T030-07 |
| T030-10 | 修改历史获取逻辑为按sessionId | 0.5h | T030-06 |
| T030-11 | 增强buildPrompt方法添加联系人画像信息 | 1h | T030-07, T030-08 |
| T030-12 | 更新AiAdvisorModule依赖注入配置 | 0.5h | T030-09 |
| T030-13 | 编写会话隔离单元测试 | 2h | T030-10, T030-11 |

### 10.3 Phase 3: 集成测试（预计0.5天）

| 任务ID | 任务描述 | 预计工时 | 依赖 |
|--------|----------|----------|------|
| T030-14 | 新会话隔离集成测试 | 1h | T030-10 |
| T030-15 | Markdown流式渲染集成测试 | 1h | T030-04 |
| T030-16 | 端到端功能验证 | 1h | T030-14, T030-15 |

---

## 11. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| compose-markdown库兼容性问题 | 高 | 低 | 使用稳定版本0.5.4，已验证与Compose BOM 2024.12.01兼容 |
| getConversationsBySession方法不存在 | 高 | 中 | 提前确认Repository接口，必要时新增方法 |
| 流式Markdown渲染性能问题 | 中 | 低 | 监控渲染性能，必要时添加防抖处理 |
| 联系人画像信息过长 | 中 | 低 | 限制事实流数量为5条，标签数量不限制 |

---

## 12. 关联文档

- [PRD-00030-AI军师Markdown渲染与会话隔离需求](../PRD/PRD-00030-AI军师Markdown渲染与会话隔离需求.md)
- [TDD-00026-AI军师对话功能技术设计](./TDD-00026-AI军师对话功能技术设计.md)
- [TDD-00029-AI军师UI架构优化技术设计](./TDD-00029-AI军师UI架构优化技术设计.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-01-07  
**更新内容**: 初始版本

