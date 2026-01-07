# FD-00030 AI军师Markdown渲染与会话隔离功能设计

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | FD-00030 |
| 创建日期 | 2026-01-07 |
| 更新日期 | 2026-01-07 |
| 状态 | 📝 待审查 |
| 关联PRD | PRD-00030 |
| 关联TDD | TDD-00030 |
| 关联文档 | PRD-00026、FD-00026、TDD-00026、PRD-00029、FD-00029、TDD-00029 |

### 版本历史

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2026-01-07 | Kiro | 初始版本 |
| 1.1 | 2026-01-07 | Kiro | 根据审查报告修改：修复DI模块配置错误、文件清单分类错误、补充性能指标、增强错误处理描述 |

---

## 1. 功能概述

### 1.1 功能目标

优化AI军师对话体验，实现两个核心功能：
1. **Markdown语法渲染** - AI回复支持基础Markdown格式显示，提升信息可读性
2. **会话上下文隔离** - 新会话只包含联系人画像信息，不包含历史对话，避免上下文混乱

### 1.2 核心功能点

| 功能点 | 描述 | 优先级 |
|-------|------|--------|
| Markdown粗体渲染 | `**文字**`显示为粗体 | P0 |
| Markdown斜体渲染 | `*文字*`显示为斜体 | P0 |
| Markdown列表渲染 | `- 项目`和`1. 项目`显示为列表 | P0 |
| Markdown行内代码 | `` `code` ``显示为灰色背景 | P0 |
| Markdown代码块 | 代码块显示为灰色背景区域 | P0 |
| 会话隔离 | 新会话只获取当前会话历史，不跨会话 | P0 |
| 联系人画像增强 | AI提示词包含联系人标签和事实流 | P0 |

### 1.3 问题背景

#### 问题1：AI回复格式单一

当前AI军师的回复内容以纯文本形式显示，无法展示结构化信息：

```
当前效果：
"建议你可以：1. 先了解对方的兴趣 2. 找共同话题 3. 保持真诚"

期望效果：
建议你可以：
1. 先了解对方的兴趣
2. 找共同话题
3. 保持真诚
```

#### 问题2：会话上下文混乱

当前实现中，AI获取的历史上下文是按**联系人ID**获取的，导致：

```
当前行为：
用户与张三的会话A：讨论约会建议
用户新建会话B：想讨论工作问题
→ AI仍然能看到会话A的约会相关内容
→ 上下文混乱，AI回复可能不相关

期望行为：
用户与张三的会话A：讨论约会建议
用户新建会话B：想讨论工作问题
→ AI只能看到张三的画像信息（姓名、标签、事实流等）
→ 会话B是全新的上下文，AI专注于当前话题
```

---

## 2. 系统架构设计

### 2.1 功能架构图

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
│  │   └── 历史获取逻辑修改：                              │  │
│  │       getRecentConversations(contactId, limit)        │  │
│  │       → getConversationsBySession(sessionId, limit)   │  │
│  │   └── buildPrompt方法增强：                           │  │
│  │       添加联系人画像信息（标签、事实流）              │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据流设计

#### Markdown渲染数据流

```
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
```

#### 会话隔离数据流

```
用户发送消息
    │
    ▼
┌──────────────────────────────────────┐
│ SendAdvisorMessageStreamingUseCase   │
└──────────────────────────────────────┘
    │
    ├── 获取联系人画像 ────────────────┐
    │   ContactRepository.getProfile() │
    │   BrainTagRepository.getTagsByContact()
    │   ContactRepository.getFactsByContact()
    │                                  │
    ├── 获取会话历史 ──────────────────┤
    │   AiAdvisorRepository            │
    │   .getConversationsBySession()   │ ← 按sessionId获取
    │                                  │
    └── 构建提示词 ────────────────────┘
        buildPrompt(画像 + 历史 + 消息)
            │
            ▼
    ┌──────────────────────────────────────┐
    │ AiRepository.generateTextStream()    │
    └──────────────────────────────────────┘
```

---

## 3. 功能详细设计

### 3.1 Markdown渲染功能

#### 3.1.1 支持的Markdown语法

| 语法 | 示例 | 渲染效果 | 优先级 |
|------|------|----------|--------|
| **粗体** | `**重要**` | 加粗显示 | P0 |
| *斜体* | `*强调*` | 斜体显示 | P0 |
| 无序列表 | `- 项目` | 项目符号列表 | P0 |
| 有序列表 | `1. 步骤` | 编号列表 | P0 |
| 行内代码 | `` `code` `` | 灰色背景 | P0 |
| 代码块 | ` ```代码``` ` | 灰色背景区域 | P0 |
| 标题 | `# 标题` | 大号加粗 | P1 |
| 链接 | `[文字](url)` | 可点击链接 | P2 |
| 分隔线 | `---` | 水平分隔线 | P2 |

#### 3.1.2 不支持的语法（明确排除）

| 语法 | 排除原因 |
|------|----------|
| 表格 | 复杂度高，移动端显示效果差 |
| 图片 | 安全风险，需要额外处理 |
| LaTeX公式 | 使用场景少，依赖重 |
| 代码高亮 | 依赖重，基础灰色背景足够 |
| HTML标签 | 安全风险 |

#### 3.1.3 样式配置

> 💡 **实现参考**: 具体代码实现请参见 [4.3 Markdown样式配置组件](#43-markdown样式配置组件)

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

#### 3.1.4 渲染效果示例

**输入**：
```markdown
建议你可以：

1. **先了解对方的兴趣** - 这是建立关系的基础
2. *找共同话题* - 让对话更自然
3. 保持真诚

如果对方说`我很忙`，可以这样回复：

```
好的，那你忙完了告诉我
```
```

**渲染效果**：

建议你可以：

1. **先了解对方的兴趣** - 这是建立关系的基础
2. *找共同话题* - 让对话更自然
3. 保持真诚

如果对方说`我很忙`，可以这样回复：

```
好的，那你忙完了告诉我
```



### 3.2 会话隔离功能

#### 3.2.1 上下文构成

| 会话类型 | 联系人画像 | 当前会话历史 | 其他会话历史 |
|----------|------------|--------------|--------------|
| 新建会话 | ✅ 包含 | ❌ 无（空） | ❌ 不包含 |
| 继续会话 | ✅ 包含 | ✅ 包含 | ❌ 不包含 |

#### 3.2.2 联系人画像信息

新会话时，AI可获取的联系人信息：

| 信息类型 | 说明 | 示例 | 来源 |
|----------|------|------|------|
| 姓名 | 联系人名称 | "张三" | ContactRepository |
| 关系标签 | 关系类型 | "亲密"、"熟悉" | ContactProfile |
| 标签画像 | BrainTag列表 | "喜欢旅游"、"工作压力大" | BrainTagRepository |
| 事实流 | 重要事实记录（最近10条） | "上周刚换工作" | ContactRepository |

#### 3.2.3 Prompt模板设计

**完整Prompt输出格式**：

```
【联系人画像】
姓名: 张三
标签: 喜欢旅游, 工作压力大, 性格外向
重要事实:
- 上周刚换了新工作
- 最近在学习摄影
- 喜欢周末去爬山
- 养了一只猫叫小花
- 下个月要去日本旅行

【当前会话历史】
用户: 你好，我想问一下关于工作的事情
AI军师: 好的，请问您想了解什么方面的工作问题？
用户: 我想知道怎么和新同事相处

【当前问题】
有什么建议吗？
```

**Prompt构建规则**：

| 规则 | 说明 |
|------|------|
| 画像信息位置 | 始终放在最前面，为AI提供背景上下文 |
| 标签格式 | 以逗号分隔，最多显示10个 |
| 事实流数量 | 最多显示5条，按时间倒序（最新的在前） |
| 会话历史顺序 | 按时间正序排列 |
| 当前问题位置 | 放在最后，作为AI需要回答的内容 |

#### 3.2.4 会话隔离逻辑

```
发送消息时构建AI提示词：
    ↓
获取联系人画像信息（姓名、标签、事实流、总结）
    ↓
获取当前会话(sessionId)的对话历史  ← 关键修改点
    ↓
构建提示词 = 联系人画像 + 当前会话历史 + 用户消息
    ↓
发送给AI
```

**关键代码变更**：

```kotlin
// 修改前（按contactId获取，跨会话）
val history = aiAdvisorRepository.getRecentConversations(contactId, limit)

// 修改后（按sessionId获取，会话隔离）
val history = aiAdvisorRepository.getConversationsBySession(sessionId, limit)
```

---

## 4. UI组件设计

### 4.1 ChatBubble组件修改

#### 4.1.1 修改前

```kotlin
// AI消息使用普通Text
Text(
    text = conversation.content,
    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    color = iOSTextPrimary,
    fontSize = 16.sp,
    lineHeight = 22.sp
)
```

#### 4.1.2 修改后

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

### 4.2 StreamingMessageBubble组件修改

#### 4.2.1 MainTextBubble修改

```kotlin
@Composable
private fun MainTextBubble(
    content: String,
    isStreaming: Boolean
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        ),
        color = iOSCardBackground,
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (content.isEmpty() && isStreaming) {
                // 等待内容时显示占位
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "正在生成",
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = iOSTextPrimary.copy(alpha = 0.5f)
                    )
                    StreamingCursor()
                }
            } else {
                Row {
                    // 使用Markdown渲染
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
            }
        }
    }
}
```

### 4.3 Markdown样式配置组件

```kotlin
/**
 * AI军师Markdown样式配置
 *
 * 提供统一的Markdown渲染样式，确保AI消息显示一致
 */
object AiAdvisorMarkdownStyle {
    
    /**
     * 获取Markdown颜色配置
     */
    @Composable
    fun colors(
        textColor: Color = iOSTextPrimary
    ) = markdownColors(
        text = textColor,
        codeBackground = Color(0xFFF5F5F5),
        inlineCodeBackground = Color(0xFFE8E8E8),
        linkText = iOSBlue,
        dividerColor = iOSSeparator
    )
    
    /**
     * 获取Markdown排版配置
     */
    @Composable
    fun typography() = markdownTypography(
        text = TextStyle(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = iOSTextPrimary
        ),
        code = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = iOSTextPrimary
        ),
        h1 = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = iOSTextPrimary
        ),
        h2 = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = iOSTextPrimary
        ),
        h3 = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
    )
}
```

---

## 5. 业务逻辑设计

### 5.1 SendAdvisorMessageStreamingUseCase修改

#### 5.1.1 新增依赖

```kotlin
class SendAdvisorMessageStreamingUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    private val contactRepository: ContactRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val brainTagRepository: BrainTagRepository  // 🆕 新增
)
```

#### 5.1.2 历史获取逻辑修改

```kotlin
// 修改前（第138行）
val historyResult = aiAdvisorRepository.getRecentConversations(contactId, DEFAULT_HISTORY_LIMIT)

// 修改后
val historyResult = aiAdvisorRepository.getConversationsBySession(sessionId, DEFAULT_HISTORY_LIMIT)
```

#### 5.1.3 buildPrompt方法增强

```kotlin
/**
 * 构建AI军师提示词（增强版）
 *
 * 包含联系人画像信息：姓名、标签、事实流
 */
private suspend fun buildPrompt(
    contactId: String,
    contactName: String?,
    history: List<AiAdvisorConversation>,
    userMessage: String
): String {
    val sb = StringBuilder()

    // 联系人画像信息
    sb.appendLine("【联系人画像】")
    sb.appendLine("姓名: ${contactName ?: "未知"}")
    
    // 获取联系人标签
    val brainTags = brainTagRepository.getTagsByContact(contactId).getOrNull()
    if (!brainTags.isNullOrEmpty()) {
        val tagContent = brainTags.take(10).joinToString(", ") { it.content }
        sb.appendLine("标签: $tagContent")
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

### 5.2 Repository接口扩展

#### 5.2.1 AiAdvisorRepository新增方法

```kotlin
interface AiAdvisorRepository {
    /**
     * 按会话ID获取对话历史
     * 
     * @param sessionId 会话ID
     * @param limit 最大返回数量
     * @return 对话列表，按时间正序
     */
    suspend fun getConversationsBySession(sessionId: String, limit: Int): Result<List<AiAdvisorConversation>>
    
    // ... 其他现有方法 ...
}
```

#### 5.2.2 AiAdvisorDao新增查询

```kotlin
@Dao
interface AiAdvisorDao {
    /**
     * 按会话ID获取对话历史
     */
    @Query("""
        SELECT * FROM ai_advisor_conversations 
        WHERE session_id = :sessionId 
        ORDER BY timestamp ASC 
        LIMIT :limit
    """)
    suspend fun getConversationsBySession(sessionId: String, limit: Int): List<AiAdvisorConversationEntity>
    
    // ... 其他现有方法 ...
}
```

---

## 6. 调用链设计

### 6.1 Markdown渲染调用链

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

### 6.2 流式Markdown渲染调用链

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

### 6.3 会话隔离调用链

```
AiAdvisorChatViewModel.sendMessage()
    ↓
SendAdvisorMessageStreamingUseCase(contactId, sessionId, message)
    ↓ (获取联系人画像)
ContactRepository.getProfile(contactId)
BrainTagRepository.getTagsByContact(contactId)
ContactRepository.getFactsByContact(contactId, limit=5)
    ↓ (获取当前会话历史)
AiAdvisorRepository.getConversationsBySession(sessionId, limit)
    ↓ (构建增强提示词)
buildPrompt(contactId, contactName, brainTags, facts, history, userMessage)
    ↓ (调用AI)
AiRepository.generateTextStream(provider, prompt, systemInstruction)
```

---

## 7. 错误处理设计

### 7.1 Markdown渲染错误处理

| 错误场景 | 处理方式 | 降级效果 |
|----------|----------|----------|
| Markdown语法解析失败 | 降级为普通文本显示 | 保留完整内容，丢失格式 |
| 不支持的Markdown语法 | 原样显示文本 | 显示原始Markdown源码 |
| Markdown库加载失败 | 降级为普通Text | 显示纯文本内容 |
| 流式渲染中断 | 显示已接收内容 | 用户可刷新重试 |
| 内存不足导致渲染失败 | 清理缓存并重试 | 显示部分内容 |
| 渲染性能问题 | 添加防抖处理 | 限制recomposition频率 |

```kotlin
/**
 * 安全的Markdown渲染
 * 
 * 解析失败时降级为普通文本
 */
@Composable
fun SafeMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    fallbackToText: Boolean = true
) {
    try {
        Markdown(
            content = content,
            modifier = modifier,
            colors = AiAdvisorMarkdownStyle.colors(),
            typography = AiAdvisorMarkdownStyle.typography()
        )
    } catch (e: Exception) {
        if (fallbackToText) {
            Text(
                text = content,
                modifier = modifier,
                fontSize = 16.sp,
                color = iOSTextPrimary
            )
        }
    }
}
```

### 7.2 会话隔离错误处理

| 错误场景 | 处理方式 | 用户影响 |
|----------|----------|----------|
| getConversationsBySession失败 | 返回空列表，继续发送消息 | AI无历史上下文，但可正常对话 |
| 联系人画像获取失败 | 使用默认值，继续发送消息 | AI缺少联系人信息，回复可能不够个性化 |
| 标签获取失败 | 跳过标签信息，继续构建Prompt | AI缺少标签画像，回复可能不够精准 |
| 事实流获取失败 | 跳过事实流信息，继续构建Prompt | AI缺少事实记录，回复可能不够贴切 |
| 数据库查询超时 | 设置超时时间，返回空结果 | 降级为无上下文对话 |

```kotlin
// 错误处理示例
val brainTags = brainTagRepository.getTagsByContact(contactId).getOrNull() ?: emptyList()
val facts = contactRepository.getFactsByContact(contactId, limit = 5).getOrNull() ?: emptyList()
```

---

## 8. 文件清单

### 8.1 新增文件

| 模块 | 文件路径 | 说明 |
|------|---------|------|
| :presentation | `ui/component/markdown/AiAdvisorMarkdownStyle.kt` | Markdown样式配置组件 |
| :presentation | `ui/component/markdown/SafeMarkdown.kt` | 安全Markdown渲染组件（带降级处理） |

### 8.2 修改文件

| 模块 | 文件路径 | 修改内容 |
|------|---------|----------|
| :presentation | `ui/screen/advisor/AiAdvisorChatScreen.kt` | ChatBubble组件：AI消息使用Markdown渲染 |
| :presentation | `ui/screen/advisor/component/StreamingMessageBubble.kt` | MainTextBubble组件：使用Markdown渲染 |
| :domain | `usecase/SendAdvisorMessageStreamingUseCase.kt` | 1. 新增BrainTagRepository依赖<br>2. 修改历史获取逻辑（按sessionId）<br>3. 增强buildPrompt方法（添加联系人画像） |
| :domain | `repository/AiAdvisorRepository.kt` | 新增`getConversationsBySession`方法签名 |
| :data | `repository/AiAdvisorRepositoryImpl.kt` | 实现`getConversationsBySession`方法 |
| :data | `local/dao/AiAdvisorDao.kt` | 新增`getConversationsBySession`查询 |
| :app | `di/AiAdvisorModule.kt` | 添加BrainTagRepository注入到SendAdvisorMessageStreamingUseCase |
| :app | `build.gradle.kts` | 添加compose-markdown依赖 |
| :presentation | `build.gradle.kts` | 添加compose-markdown依赖 |

### 8.3 依赖变更

**gradle/libs.versions.toml**：

```toml
[versions]
compose-markdown = "0.5.4"

[libraries]
compose-markdown = { module = "com.mikepenz:multiplatform-markdown-renderer", version.ref = "compose-markdown" }
```

**presentation/build.gradle.kts**：

```kotlin
dependencies {
    implementation(libs.compose.markdown)
}
```

---

## 9. 测试计划

### 9.1 单元测试

#### 9.1.1 SendAdvisorMessageStreamingUseCaseTest

```kotlin
@Test
fun `buildPrompt includes contact profile information`() = runTest {
    // Given
    val contactId = "contact_123"
    val contactName = "张三"
    val brainTags = listOf(
        BrainTag(id = "1", contactId = contactId, content = "喜欢旅游"),
        BrainTag(id = "2", contactId = contactId, content = "工作压力大")
    )
    val facts = listOf(
        Fact(id = "1", contactId = contactId, content = "上周换了新工作"),
        Fact(id = "2", contactId = contactId, content = "最近在学摄影")
    )
    
    coEvery { brainTagRepository.getTagsByContact(contactId) } returns Result.success(brainTags)
    coEvery { contactRepository.getFactsByContact(contactId, 5) } returns Result.success(facts)
    
    // When
    val prompt = useCase.buildPrompt(contactId, contactName, emptyList(), "你好")
    
    // Then
    assertTrue(prompt.contains("【联系人画像】"))
    assertTrue(prompt.contains("姓名: 张三"))
    assertTrue(prompt.contains("喜欢旅游"))
    assertTrue(prompt.contains("工作压力大"))
    assertTrue(prompt.contains("上周换了新工作"))
    assertTrue(prompt.contains("最近在学摄影"))
}

@Test
fun `getConversationsBySession returns only current session history`() = runTest {
    // Given
    val sessionId = "session_123"
    val conversations = listOf(
        AiAdvisorConversation(id = "1", sessionId = sessionId, content = "消息1"),
        AiAdvisorConversation(id = "2", sessionId = sessionId, content = "消息2")
    )
    
    coEvery { aiAdvisorRepository.getConversationsBySession(sessionId, 10) } returns 
        Result.success(conversations)
    
    // When
    val result = aiAdvisorRepository.getConversationsBySession(sessionId, 10)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(2, result.getOrNull()?.size)
}

@Test
fun `buildPrompt handles empty brainTags gracefully`() = runTest {
    // Given
    val contactId = "contact_123"
    coEvery { brainTagRepository.getTagsByContact(contactId) } returns Result.success(emptyList())
    coEvery { contactRepository.getFactsByContact(contactId, 5) } returns Result.success(emptyList())
    
    // When
    val prompt = useCase.buildPrompt(contactId, "张三", emptyList(), "你好")
    
    // Then
    assertTrue(prompt.contains("【联系人画像】"))
    assertTrue(prompt.contains("姓名: 张三"))
    assertFalse(prompt.contains("标签:"))  // 无标签时不显示标签行
}

@Test
fun `buildPrompt limits brainTags to 10`() = runTest {
    // Given
    val contactId = "contact_123"
    val brainTags = (1..15).map { 
        BrainTag(id = "$it", contactId = contactId, content = "标签$it") 
    }
    
    coEvery { brainTagRepository.getTagsByContact(contactId) } returns Result.success(brainTags)
    coEvery { contactRepository.getFactsByContact(contactId, 5) } returns Result.success(emptyList())
    
    // When
    val prompt = useCase.buildPrompt(contactId, "张三", emptyList(), "你好")
    
    // Then
    // 只包含前10个标签
    assertTrue(prompt.contains("标签1"))
    assertTrue(prompt.contains("标签10"))
    assertFalse(prompt.contains("标签11"))
}
```

#### 9.1.2 AiAdvisorDaoTest

```kotlin
@Test
fun `getConversationsBySession returns conversations for specific session`() = runTest {
    // Given
    val sessionId1 = "session_1"
    val sessionId2 = "session_2"
    
    dao.insertConversation(AiAdvisorConversationEntity(
        id = "1", sessionId = sessionId1, content = "会话1消息"
    ))
    dao.insertConversation(AiAdvisorConversationEntity(
        id = "2", sessionId = sessionId2, content = "会话2消息"
    ))
    dao.insertConversation(AiAdvisorConversationEntity(
        id = "3", sessionId = sessionId1, content = "会话1消息2"
    ))
    
    // When
    val result = dao.getConversationsBySession(sessionId1, 10)
    
    // Then
    assertEquals(2, result.size)
    assertTrue(result.all { it.sessionId == sessionId1 })
}

@Test
fun `getConversationsBySession respects limit`() = runTest {
    // Given
    val sessionId = "session_1"
    (1..20).forEach { i ->
        dao.insertConversation(AiAdvisorConversationEntity(
            id = "$i", sessionId = sessionId, content = "消息$i"
        ))
    }
    
    // When
    val result = dao.getConversationsBySession(sessionId, 10)
    
    // Then
    assertEquals(10, result.size)
}

@Test
fun `getConversationsBySession returns empty list for non-existent session`() = runTest {
    // When
    val result = dao.getConversationsBySession("non_existent", 10)
    
    // Then
    assertTrue(result.isEmpty())
}
```

### 9.2 UI测试

#### 9.2.1 Markdown渲染测试

| 测试场景 | 验证点 |
|---------|-------|
| 粗体渲染 | `**文字**`显示为粗体样式 |
| 斜体渲染 | `*文字*`显示为斜体样式 |
| 无序列表渲染 | `- 项目`显示为项目符号列表 |
| 有序列表渲染 | `1. 步骤`显示为编号列表 |
| 行内代码渲染 | `` `code` ``显示为灰色背景 |
| 代码块渲染 | 代码块显示为灰色背景区域 |
| 混合格式渲染 | 多种格式混合正确渲染 |
| 降级处理 | 解析失败时显示纯文本 |

#### 9.2.2 会话隔离测试

| 测试场景 | 验证点 |
|---------|-------|
| 新会话无历史 | 新建会话时AI不包含其他会话的历史 |
| 联系人画像包含 | AI提示词包含联系人姓名、标签、事实流 |
| 当前会话历史 | 继续会话时AI包含当前会话的历史 |
| 跨会话隔离 | 切换会话后AI不包含前一个会话的历史 |

### 9.3 集成测试

| 测试场景 | 验证点 |
|---------|-------|
| 完整对话流程 | 发送消息 → AI回复（Markdown格式） → 正确渲染 |
| 新会话流程 | 创建新会话 → 发送消息 → AI只获取联系人画像 |
| 继续会话流程 | 选择历史会话 → 发送消息 → AI获取当前会话历史 |
| 流式渲染 | 流式响应过程中Markdown实时渲染 |

---

## 10. 性能指标

### 10.1 性能指标要求

| 指标 | 目标值 | 测量方法 | 说明 |
|------|--------|----------|------|
| Markdown单条消息渲染时间 | < 50ms | 使用Compose性能追踪 | 单条消息从接收到渲染完成 |
| 流式渲染帧率 | > 30fps | 监控recomposition次数 | 流式响应过程中保持流畅 |
| 联系人画像加载时间 | < 100ms | 在buildPrompt方法中添加计时日志 | 包含标签和事实流获取 |
| 会话历史加载时间 | < 200ms | 在getConversationsBySession调用处添加计时日志 | 100条历史记录以内 |
| 内存占用增量 | < 5MB | 使用Android Profiler | Markdown渲染组件的额外内存占用 |

### 10.2 性能优化策略

| 优化点 | 策略 | 说明 |
|--------|------|------|
| 流式渲染防抖 | 使用`debounce`限制渲染频率 | 避免频繁recomposition |
| Markdown解析缓存 | 使用`remember`缓存解析结果 | 相同内容不重复解析 |
| 数据库查询优化 | 为session_id添加索引 | 加速会话历史查询 |
| 联系人画像并行获取 | 使用`async`并行获取标签和事实流 | 减少总加载时间 |

---

## 11. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| Markdown库兼容性问题 | 高 | 低 | 使用成熟的compose-markdown库（0.5.4版本），添加降级处理 |
| 流式渲染性能问题 | 中 | 中 | 添加防抖处理，限制渲染频率，使用remember缓存 |
| 联系人画像获取失败 | 低 | 低 | 使用默认值继续，不阻塞消息发送 |
| 会话历史查询性能 | 低 | 低 | 添加数据库索引，限制查询数量 |
| Markdown解析异常 | 中 | 低 | 添加try-catch，降级为纯文本显示 |
| 复杂Markdown格式显示异常 | 中 | 中 | 明确支持的语法范围，不支持的语法原样显示 |

---

## 12. 验收标准

| 序号 | 验收项 | 标准 |
|------|--------|------|
| 1 | **Markdown粗体** | `**文字**`正确显示为粗体 |
| 2 | **Markdown斜体** | `*文字*`正确显示为斜体 |
| 3 | **Markdown无序列表** | `- 项目`正确显示为项目符号列表 |
| 4 | **Markdown有序列表** | `1. 步骤`正确显示为编号列表 |
| 5 | **Markdown行内代码** | `` `code` ``正确显示为灰色背景 |
| 6 | **Markdown代码块** | 代码块正确显示为灰色背景区域（#F5F5F5） |
| 7 | **流式Markdown渲染** | 流式响应过程中Markdown实时渲染，帧率>30fps |
| 8 | **降级处理** | Markdown解析失败时降级为纯文本显示 |
| 9 | **会话隔离** | 新会话只获取当前会话历史，不跨会话 |
| 10 | **联系人画像** | AI提示词包含联系人姓名、标签（最多10个）、事实流（最多5条） |
| 11 | **画像获取容错** | 标签或事实流获取失败时不阻塞消息发送 |
| 12 | **性能达标** | Markdown渲染<50ms，画像加载<100ms，历史加载<200ms |
| 13 | **单元测试覆盖** | 核心UseCase和DAO测试覆盖率>80% |

---

## 13. 关联文档

- [PRD-00030-AI军师Markdown渲染与会话隔离需求](../PRD/PRD-00030-AI军师Markdown渲染与会话隔离需求.md)
- [TDD-00030-AI军师Markdown渲染与会话隔离技术设计](../TDD/TDD-00030-AI军师Markdown渲染与会话隔离技术设计.md)
- [PRD-00029-AI军师UI优化需求](../PRD/PRD-00029-AI军师UI优化需求.md)
- [FD-00029-AI军师UI架构优化功能设计](./FD-00029-AI军师UI架构优化功能设计.md)
- [TDD-00029-AI军师UI架构优化技术设计](../TDD/TDD-00029-AI军师UI架构优化技术设计.md)
- [PRD-00026-AI军师对话功能需求](../PRD/PRD-00026-AI军师对话功能需求.md)

---

**文档版本**: 1.1
**最后更新**: 2026-01-07
**更新内容**: 根据审查报告修改：
1. 修复DI模块配置错误（FloatingWindowModule → AiAdvisorModule）
2. 修复文件清单分类错误（已存在文件从"新增"改为"修改"）
3. 补充性能指标量化表格（第10章）
4. 增强错误处理描述（添加降级效果和用户影响列）
5. 明确组件包结构（ui/component/markdown/）
6. 更新验收标准（添加性能达标项）
