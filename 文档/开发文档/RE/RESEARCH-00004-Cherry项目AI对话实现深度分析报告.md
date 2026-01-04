# RESEARCH-00004-Cherry项目AI对话实现深度分析报告

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档编号 | RESEARCH-00004 |
| 创建日期 | 2026-01-04 |
| 调研人 | AI Assistant |
| 状态 | 调研完成 |
| 调研目的 | 深度分析Cherry Studio的AI对话实现，为PRD-00026 AI军师功能提供改进参考 |
| 关联任务 | PRD-00026 AI军师功能优化 |

---

## 1. 调研范围

### 1.1 调研主题
深度对比分析Cherry Studio与我们AI军师的AI对话实现架构，识别可借鉴的设计模式和改进点。

### 1.2 关注重点
- 消息架构设计（Block-based vs 简单消息）
- 流式响应处理机制
- 状态管理策略
- 缓存与性能优化
- 错误处理与重试机制

### 1.3 关联文档

| 文档类型 | 文档编号 | 文档名称 |
|----------|----------|----------|
| PRD | PRD-00026 | AI军师功能需求 |
| RE | RESEARCH-00003 | Cherry项目架构对比分析报告（初版） |

---

## 2. Cherry Studio AI对话核心架构

### 2.1 整体数据流

```
用户输入 → useMessageSend.sendMessage()
         → MessagesService.getUserMessage() 创建用户消息
         → MessagesService.sendMessage() 保存并触发AI请求
         → fetchAndProcessAssistantResponseImpl()
         → OrchestrationService.transformMessagesAndFetch()
         → ConversationService.prepareMessagesForModel() 消息过滤转换
         → ApiService.fetchChatCompletion()
         → ModernAiProvider.completions()
         → AiSdkToChunkAdapter.processStream() 流式处理
         → StreamProcessingService.createStreamProcessor() 分发Chunk
         → BlockManager 管理消息块状态
         → 数据库持久化 + UI实时更新
```

### 2.2 核心设计亮点

#### 2.2.1 Block-based消息架构

Cherry Studio将一条消息拆分为多个Block，支持复杂内容组合：

```typescript
// 消息块类型
export enum MessageBlockType {
  UNKNOWN = 'unknown',      // 占位块
  MAIN_TEXT = 'main_text',  // 主文本
  THINKING = 'thinking',    // 思考过程（DeepSeek等模型）
  TRANSLATION = 'translation',
  IMAGE = 'image',
  CODE = 'code',
  TOOL = 'tool',           // 工具调用（MCP）
  FILE = 'file',
  ERROR = 'error',
  CITATION = 'citation'    // 引用/搜索结果
}

// 消息块状态
export enum MessageBlockStatus {
  PENDING = 'pending',
  PROCESSING = 'processing',
  STREAMING = 'streaming',  // 流式接收中
  SUCCESS = 'success',
  ERROR = 'error',
  PAUSED = 'paused'
}
```

**优势**：
- 支持思考过程展示（DeepSeek R1等模型）
- 支持工具调用结果展示
- 支持引用/搜索结果展示
- 每个Block独立状态管理

#### 2.2.2 统一Chunk类型系统

```typescript
export enum ChunkType {
  // LLM响应生命周期
  LLM_RESPONSE_CREATED = 'llm_response_created',
  LLM_RESPONSE_COMPLETE = 'llm_response_complete',

  // 文本流
  TEXT_START = 'text.start',
  TEXT_DELTA = 'text.delta',
  TEXT_COMPLETE = 'text.complete',

  // 思考流
  THINKING_START = 'thinking.start',
  THINKING_DELTA = 'thinking.delta',
  THINKING_COMPLETE = 'thinking.complete',

  // 工具调用
  MCP_TOOL_PENDING = 'mcp_tool_pending',
  MCP_TOOL_IN_PROGRESS = 'mcp_tool_in_progress',
  MCP_TOOL_COMPLETE = 'mcp_tool_complete',

  // 错误与完成
  ERROR = 'error',
  BLOCK_COMPLETE = 'block_complete'
}
```

**优势**：
- 统一的流式事件协议
- 便于扩展新的内容类型
- 清晰的生命周期管理

#### 2.2.3 智能节流更新策略

```typescript
class BlockManager {
  async smartBlockUpdate(blockId, changes, blockType, isComplete = false) {
    const isBlockTypeChanged = this._lastBlockType !== blockType

    if (isBlockTypeChanged || isComplete) {
      // 块类型改变或完成时，立即写入数据库
      await messageBlockDatabase.updateOneBlock({ id: blockId, changes })
    } else {
      // 同类型流式内容，使用节流更新
      await this.deps.throttledBlockUpdate(blockId, changes)
    }

    this._lastBlockType = blockType
  }
}
```

**优势**：
- 减少数据库写入频率
- 保证UI流畅性
- 块类型切换时立即响应

---

## 3. 我们AI军师当前实现分析

### 3.1 相关文件清单

| 文件路径 | 类型 | 说明 |
|----------|------|------|
| `domain/model/AiAdvisorConversation.kt` | Model | 简单消息模型 |
| `domain/model/AiAdvisorSession.kt` | Model | 会话模型 |
| `domain/usecase/SendAdvisorMessageUseCase.kt` | UseCase | 发送消息核心逻辑 |
| `data/repository/AiAdvisorRepositoryImpl.kt` | Repository | Room数据访问 |
| `presentation/viewmodel/AiAdvisorChatViewModel.kt` | ViewModel | UI状态管理 |
| `presentation/ui/screen/advisor/AiAdvisorChatScreen.kt` | UI | Compose界面 |

### 3.2 当前消息模型

```kotlin
data class AiAdvisorConversation(
    val id: String,
    val contactId: String,
    val sessionId: String,
    val messageType: MessageType,  // USER 或 AI
    val content: String,           // 纯文本内容
    val timestamp: Long,
    val createdAt: Long,
    val sendStatus: SendStatus     // PENDING, SUCCESS, FAILED
)
```

**局限性**：
- ❌ 不支持Block架构，无法展示思考过程
- ❌ 不支持流式响应，只能等待完整响应
- ❌ 不支持工具调用结果展示
- ❌ 不支持引用/搜索结果

### 3.3 当前发送流程

```kotlin
// SendAdvisorMessageUseCase.kt
suspend operator fun invoke(contactId, sessionId, userMessage): Result<AiAdvisorConversation> {
    // 1. 保存用户消息
    aiAdvisorRepository.saveMessage(userConversation)

    // 2. 获取AI服务商
    val provider = aiProviderRepository.getDefaultProvider()

    // 3. 获取联系人画像
    val contact = contactRepository.getProfile(contactId)

    // 4. 获取对话历史
    val history = aiAdvisorRepository.getRecentConversations(contactId, HISTORY_LIMIT)

    // 5. 构建提示词
    val prompt = buildPrompt(contact, history, userMessage)

    // 6. 调用AI获取回复（阻塞等待完整响应）
    val aiResponse = aiRepository.generateText(provider, prompt, systemInstruction)

    // 7. 保存AI回复
    aiAdvisorRepository.saveMessage(aiConversation)

    return Result.success(aiConversation)
}
```

**局限性**：
- ❌ 阻塞等待完整响应，用户体验差
- ❌ 无法展示AI思考过程
- ❌ 无法中途取消请求
- ❌ 无法实时显示生成进度

---

## 4. 核心差距对比

### 4.1 消息架构对比

| 维度 | Cherry Studio | 我们AI军师 | 差距 |
|------|--------------|-----------|------|
| 消息结构 | Block-based（多块组合） | 单一文本 | 🔴 重大差距 |
| 思考过程 | ✅ THINKING Block | ❌ 不支持 | 🔴 重大差距 |
| 工具调用 | ✅ TOOL Block | ❌ 不支持 | 🟡 中等差距 |
| 引用展示 | ✅ CITATION Block | ❌ 不支持 | 🟡 中等差距 |
| 代码高亮 | ✅ CODE Block | ❌ 不支持 | 🟢 小差距 |

### 4.2 流式响应对比

| 维度 | Cherry Studio | 我们AI军师 | 差距 |
|------|--------------|-----------|------|
| 流式支持 | ✅ 完整流式 | ❌ 阻塞等待 | 🔴 重大差距 |
| 实时显示 | ✅ 逐字显示 | ❌ 一次性显示 | 🔴 重大差距 |
| 取消请求 | ✅ 支持暂停/取消 | ❌ 不支持 | 🟡 中等差距 |
| 进度指示 | ✅ 状态枚举 | ⚠️ 简单状态 | 🟡 中等差距 |

### 4.3 状态管理对比

| 维度 | Cherry Studio | 我们AI军师 | 差距 |
|------|--------------|-----------|------|
| 消息状态 | 6种状态（含STREAMING） | 3种状态 | 🟡 中等差距 |
| 块状态 | 独立Block状态 | 无 | 🔴 重大差距 |
| 实时同步 | useLiveQuery | Flow | ✅ 相当 |
| 乐观更新 | ✅ 完整实现 | ⚠️ 部分实现 | 🟢 小差距 |

### 4.4 缓存策略对比

| 维度 | Cherry Studio | 我们AI军师 | 差距 |
|------|--------------|-----------|------|
| 多级缓存 | 永久+LRU+TTL | 无缓存 | 🔴 重大差距 |
| 节流更新 | ✅ 智能节流 | ❌ 无 | 🟡 中等差距 |
| 内存管理 | ✅ LRU驱逐 | ❌ 无 | 🟡 中等差距 |

---

## 5. 改进建议

### 5.1 🔴 P0 - Block-based消息架构（必须实现）

**目标**：支持思考过程、工具调用等复杂内容展示

**新增模型**：

```kotlin
// 消息块类型
enum class MessageBlockType {
    MAIN_TEXT,    // 主文本
    THINKING,     // 思考过程
    TOOL,         // 工具调用
    CODE,         // 代码块
    CITATION,     // 引用
    ERROR         // 错误
}

// 消息块状态
enum class MessageBlockStatus {
    PENDING,      // 等待
    PROCESSING,   // 处理中
    STREAMING,    // 流式接收中
    SUCCESS,      // 成功
    ERROR,        // 错误
    PAUSED        // 暂停
}

// 消息块
data class AiAdvisorMessageBlock(
    val id: String,
    val messageId: String,
    val type: MessageBlockType,
    val status: MessageBlockStatus,
    val content: String,
    val metadata: Map<String, Any>? = null,  // 如thinking_millsec
    val createdAt: Long
)
```

**数据库迁移**：

```kotlin
// Migration 13 to 14
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS ai_advisor_message_blocks (
                id TEXT PRIMARY KEY NOT NULL,
                message_id TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                content TEXT NOT NULL,
                metadata TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY (message_id) REFERENCES ai_advisor_conversations(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX idx_blocks_message_id ON ai_advisor_message_blocks(message_id)")
    }
}
```

### 5.2 🔴 P0 - 流式响应支持（必须实现）

**目标**：实现逐字显示、实时思考过程展示

**新增Chunk类型**：

```kotlin
// 流式事件类型
sealed class StreamChunk {
    // 响应生命周期
    object ResponseCreated : StreamChunk()
    object ResponseComplete : StreamChunk()

    // 文本流
    object TextStart : StreamChunk()
    data class TextDelta(val text: String) : StreamChunk()
    data class TextComplete(val fullText: String) : StreamChunk()

    // 思考流
    object ThinkingStart : StreamChunk()
    data class ThinkingDelta(val text: String, val thinkingMs: Long? = null) : StreamChunk()
    data class ThinkingComplete(val fullThinking: String, val totalMs: Long) : StreamChunk()

    // 错误
    data class Error(val error: Throwable) : StreamChunk()
}
```

**流式UseCase**：

```kotlin
class SendAdvisorMessageStreamingUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val aiRepository: AiRepository,
    // ...
) {
    /**
     * 发送消息并返回流式响应
     */
    operator fun invoke(
        contactId: String,
        sessionId: String,
        userMessage: String
    ): Flow<StreamChunk> = flow {
        // 1. 保存用户消息
        val userConversation = AiAdvisorConversation.createUserMessage(...)
        aiAdvisorRepository.saveMessage(userConversation)

        // 2. 创建AI消息占位
        val aiMessage = AiAdvisorConversation.createAiMessage(
            sendStatus = SendStatus.PENDING
        )
        aiAdvisorRepository.saveMessage(aiMessage)

        emit(StreamChunk.ResponseCreated)

        // 3. 调用流式API
        aiRepository.generateTextStream(provider, prompt, systemInstruction)
            .collect { chunk ->
                when (chunk) {
                    is AiStreamChunk.TextDelta -> {
                        emit(StreamChunk.TextDelta(chunk.text))
                        // 更新Block内容（节流）
                        updateBlockThrottled(aiMessage.id, chunk.text)
                    }
                    is AiStreamChunk.ThinkingDelta -> {
                        emit(StreamChunk.ThinkingDelta(chunk.text, chunk.thinkingMs))
                        updateThinkingBlock(aiMessage.id, chunk.text)
                    }
                    is AiStreamChunk.Complete -> {
                        emit(StreamChunk.ResponseComplete)
                        finalizeMessage(aiMessage.id)
                    }
                    is AiStreamChunk.Error -> {
                        emit(StreamChunk.Error(chunk.error))
                        markMessageFailed(aiMessage.id)
                    }
                }
            }
    }
}
```

### 5.3 🟡 P1 - 智能节流更新

**目标**：减少数据库写入，提升性能

```kotlin
class BlockUpdateManager @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var lastBlockType: MessageBlockType? = null
    private val pendingUpdates = mutableMapOf<String, String>()
    private var throttleJob: Job? = null

    /**
     * 智能更新Block
     * - 块类型变化时立即写入
     * - 同类型内容使用节流（300ms）
     */
    suspend fun smartUpdate(
        blockId: String,
        content: String,
        blockType: MessageBlockType,
        isComplete: Boolean = false
    ) {
        val isTypeChanged = lastBlockType != null && lastBlockType != blockType

        if (isTypeChanged || isComplete) {
            // 立即写入
            flushPendingUpdates()
            aiAdvisorRepository.updateBlock(blockId, content)
        } else {
            // 节流更新
            pendingUpdates[blockId] = content
            scheduleFlush()
        }

        lastBlockType = blockType
    }

    private fun scheduleFlush() {
        throttleJob?.cancel()
        throttleJob = CoroutineScope(ioDispatcher).launch {
            delay(300) // 300ms节流
            flushPendingUpdates()
        }
    }

    private suspend fun flushPendingUpdates() {
        pendingUpdates.forEach { (blockId, content) ->
            aiAdvisorRepository.updateBlock(blockId, content)
        }
        pendingUpdates.clear()
    }
}
```

### 5.4 🟡 P1 - 消息状态增强

**目标**：更细粒度的状态管理

```kotlin
// 增强的消息状态
enum class AdvisorMessageStatus {
    PENDING,      // 等待处理
    PROCESSING,   // 正在处理
    STREAMING,    // 流式接收中
    SEARCHING,    // 搜索中（如果支持联网搜索）
    SUCCESS,      // 成功
    PAUSED,       // 暂停
    ERROR         // 错误
}

// ViewModel状态增强
data class AiAdvisorChatUiState(
    // ... 现有字段
    val streamingMessageId: String? = null,  // 正在流式接收的消息ID
    val streamingContent: String = "",        // 流式内容缓冲
    val thinkingContent: String = "",         // 思考过程缓冲
    val thinkingTimeMs: Long = 0,             // 思考耗时
    val canCancel: Boolean = false            // 是否可取消
)
```

### 5.5 🟢 P2 - 缓存层优化

**目标**：减少数据库查询，提升响应速度

```kotlin
class ConversationCacheManager @Inject constructor() {
    // LRU缓存：最近访问的会话
    private val sessionCache = object : LinkedHashMap<String, List<AiAdvisorConversation>>(
        10, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<AiAdvisorConversation>>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    // TTL缓存：带过期时间
    private val cacheTimestamps = mutableMapOf<String, Long>()
    private val CACHE_TTL = 5 * 60 * 1000L // 5分钟

    fun get(sessionId: String): List<AiAdvisorConversation>? {
        val timestamp = cacheTimestamps[sessionId] ?: return null
        if (System.currentTimeMillis() - timestamp > CACHE_TTL) {
            invalidate(sessionId)
            return null
        }
        return sessionCache[sessionId]
    }

    fun put(sessionId: String, conversations: List<AiAdvisorConversation>) {
        sessionCache[sessionId] = conversations
        cacheTimestamps[sessionId] = System.currentTimeMillis()
    }

    fun invalidate(sessionId: String) {
        sessionCache.remove(sessionId)
        cacheTimestamps.remove(sessionId)
    }

    companion object {
        private const val MAX_CACHE_SIZE = 10
    }
}
```

---

## 6. 实施路线图

### 6.1 Phase 1: 基础流式支持（1-2周）

| 任务 | 优先级 | 预估时间 |
|------|--------|----------|
| 新增AiRepository.generateTextStream接口 | P0 | 2天 |
| 实现OkHttp SSE流式响应 | P0 | 2天 |
| 新增StreamChunk类型定义 | P0 | 0.5天 |
| 实现SendAdvisorMessageStreamingUseCase | P0 | 2天 |
| ViewModel流式状态管理 | P0 | 1天 |
| UI流式显示（打字机效果） | P0 | 1天 |

### 6.2 Phase 2: Block架构（1-2周）

| 任务 | 优先级 | 预估时间 |
|------|--------|----------|
| 新增MessageBlock模型 | P0 | 0.5天 |
| 数据库迁移（Migration 13→14） | P0 | 1天 |
| 新增MessageBlockDao | P0 | 1天 |
| 实现BlockManager | P1 | 2天 |
| UI Block渲染组件 | P1 | 2天 |
| 思考过程展示UI | P1 | 1天 |

### 6.3 Phase 3: 性能优化（1周）

| 任务 | 优先级 | 预估时间 |
|------|--------|----------|
| 实现智能节流更新 | P1 | 1天 |
| 实现LRU缓存 | P2 | 1天 |
| 实现TTL缓存 | P2 | 0.5天 |
| 性能测试与调优 | P2 | 1天 |

---

## 7. 关键发现总结

### 7.1 核心结论

1. **Block架构是关键差距**：Cherry Studio的Block-based消息架构是支持思考过程、工具调用等高级功能的基础，我们必须实现类似架构。

2. **流式响应是用户体验关键**：阻塞等待完整响应的体验远不如逐字显示，特别是对于长回复。

3. **智能节流是性能关键**：Cherry Studio的BlockManager智能判断何时立即写入、何时节流，值得借鉴。

4. **状态管理需要细化**：6种消息状态比我们的3种更能准确反映AI响应的生命周期。

### 7.2 技术要点

| 要点 | 说明 | 重要程度 |
|------|------|----------|
| Block-based架构 | 消息拆分为多个独立Block | 高 |
| 流式响应 | SSE/WebSocket实时推送 | 高 |
| ChunkType枚举 | 统一的流式事件协议 | 高 |
| 智能节流 | 根据块类型变化决定写入策略 | 中 |
| 多级缓存 | 永久+LRU+TTL三级缓存 | 中 |
| 乐观更新 | 先更新UI，后持久化 | 中 |

### 7.3 注意事项

- ⚠️ 流式响应需要后端API支持SSE或WebSocket
- ⚠️ Block架构需要数据库迁移，注意向后兼容
- ⚠️ 节流更新需要处理应用意外退出时的数据丢失
- ⚠️ 思考过程展示需要AI模型支持（如DeepSeek R1）

---

## 8. 附录

### 8.1 参考资料

- Cherry Studio CODE_ANALYSIS文档
- [AI SDK Streaming Documentation](https://sdk.vercel.ai/docs/ai-sdk-core/streaming)
- [OkHttp SSE Support](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-event-source/)

### 8.2 术语表

| 术语 | 解释 |
|------|------|
| Block | 消息块，一条消息可包含多个Block |
| Chunk | 流式响应的数据片段 |
| SSE | Server-Sent Events，服务器推送事件 |
| LRU | Least Recently Used，最近最少使用缓存策略 |
| TTL | Time To Live，缓存过期时间 |
| 节流 | Throttle，限制操作频率 |

---

**文档版本**: 1.0  
**最后更新**: 2026-01-04


---

## 9. 代码实现示例

### 9.1 流式响应接口设计

```kotlin
// domain/repository/AiRepository.kt - 新增流式接口
interface AiRepository {
    // 现有接口
    suspend fun generateText(
        provider: AiProvider,
        prompt: String,
        systemInstruction: String
    ): Result<String>

    // 🆕 新增流式接口
    fun generateTextStream(
        provider: AiProvider,
        prompt: String,
        systemInstruction: String
    ): Flow<AiStreamChunk>
}

// domain/model/AiStreamChunk.kt - 流式数据块
sealed class AiStreamChunk {
    /** 响应开始 */
    object Started : AiStreamChunk()

    /** 文本增量 */
    data class TextDelta(val text: String) : AiStreamChunk()

    /** 思考过程增量（DeepSeek R1等模型） */
    data class ThinkingDelta(
        val text: String,
        val thinkingMs: Long? = null
    ) : AiStreamChunk()

    /** 思考完成 */
    data class ThinkingComplete(
        val fullThinking: String,
        val totalMs: Long
    ) : AiStreamChunk()

    /** 响应完成 */
    data class Complete(
        val fullText: String,
        val usage: TokenUsage? = null
    ) : AiStreamChunk()

    /** 错误 */
    data class Error(val error: Throwable) : AiStreamChunk()
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
```

### 9.2 OkHttp SSE实现

```kotlin
// data/remote/SseStreamReader.kt
class SseStreamReader(
    private val okHttpClient: OkHttpClient
) {
    /**
     * 发起SSE流式请求
     */
    fun stream(
        url: String,
        requestBody: ChatRequestDto,
        headers: Map<String, String>
    ): Flow<AiStreamChunk> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .post(requestBody.toJson().toRequestBody("application/json".toMediaType()))
            .build()

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(request, object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    trySend(AiStreamChunk.Started)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data == "[DONE]") {
                        // OpenAI格式的结束标记
                        return
                    }

                    try {
                        val chunk = parseChunk(data)
                        chunk?.let { trySend(it) }
                    } catch (e: Exception) {
                        trySend(AiStreamChunk.Error(e))
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    channel.close()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    trySend(AiStreamChunk.Error(t ?: Exception("SSE connection failed")))
                    channel.close()
                }
            })

        awaitClose {
            eventSource.cancel()
        }
    }

    private fun parseChunk(data: String): AiStreamChunk? {
        val json = JSONObject(data)
        val choices = json.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null

        val choice = choices.getJSONObject(0)
        val delta = choice.optJSONObject("delta") ?: return null

        // 检查是否有思考内容（DeepSeek R1格式）
        val reasoning = delta.optString("reasoning_content", "")
        if (reasoning.isNotEmpty()) {
            return AiStreamChunk.ThinkingDelta(reasoning)
        }

        // 普通文本内容
        val content = delta.optString("content", "")
        if (content.isNotEmpty()) {
            return AiStreamChunk.TextDelta(content)
        }

        // 检查是否完成
        val finishReason = choice.optString("finish_reason", "")
        if (finishReason == "stop") {
            val usage = json.optJSONObject("usage")?.let {
                TokenUsage(
                    promptTokens = it.optInt("prompt_tokens"),
                    completionTokens = it.optInt("completion_tokens"),
                    totalTokens = it.optInt("total_tokens")
                )
            }
            return AiStreamChunk.Complete("", usage)
        }

        return null
    }
}
```

### 9.3 流式ViewModel实现

```kotlin
// presentation/viewmodel/AiAdvisorChatViewModel.kt - 流式版本
@HiltViewModel
class AiAdvisorChatViewModel @Inject constructor(
    // ... 现有依赖
    private val sendAdvisorMessageStreamingUseCase: SendAdvisorMessageStreamingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAdvisorChatUiState())
    val uiState: StateFlow<AiAdvisorChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    /**
     * 发送消息（流式版本）
     */
    fun sendMessageStreaming() {
        val currentState = _uiState.value
        val message = currentState.inputText.trim()
        if (message.isEmpty() || currentState.isSending) return

        val sessionId = currentState.currentSessionId ?: return

        // 取消之前的流式任务
        streamingJob?.cancel()

        streamingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    inputText = "",
                    error = null,
                    streamingContent = "",
                    thinkingContent = "",
                    canCancel = true
                )
            }

            sendAdvisorMessageStreamingUseCase(contactId, sessionId, message)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            canCancel = false,
                            error = error.message ?: "发送失败"
                        )
                    }
                }
                .collect { chunk ->
                    handleStreamChunk(chunk)
                }
        }
    }

    private fun handleStreamChunk(chunk: AiStreamChunk) {
        when (chunk) {
            is AiStreamChunk.Started -> {
                _uiState.update { it.copy(streamingMessageId = "pending") }
            }

            is AiStreamChunk.TextDelta -> {
                _uiState.update { state ->
                    state.copy(
                        streamingContent = state.streamingContent + chunk.text
                    )
                }
            }

            is AiStreamChunk.ThinkingDelta -> {
                _uiState.update { state ->
                    state.copy(
                        thinkingContent = state.thinkingContent + chunk.text,
                        thinkingTimeMs = chunk.thinkingMs ?: state.thinkingTimeMs
                    )
                }
            }

            is AiStreamChunk.ThinkingComplete -> {
                _uiState.update { state ->
                    state.copy(
                        thinkingContent = chunk.fullThinking,
                        thinkingTimeMs = chunk.totalMs
                    )
                }
            }

            is AiStreamChunk.Complete -> {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        canCancel = false,
                        streamingContent = "",
                        thinkingContent = "",
                        streamingMessageId = null
                    )
                }
            }

            is AiStreamChunk.Error -> {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        canCancel = false,
                        error = chunk.error.message ?: "未知错误"
                    )
                }
            }
        }
    }

    /**
     * 取消流式请求
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        _uiState.update {
            it.copy(
                isSending = false,
                canCancel = false,
                streamingContent = "",
                thinkingContent = ""
            )
        }
    }
}
```

### 9.4 流式UI组件

```kotlin
// presentation/ui/screen/advisor/component/StreamingMessageBubble.kt
@Composable
fun StreamingMessageBubble(
    content: String,
    thinkingContent: String,
    thinkingTimeMs: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 思考过程展示（可折叠）
        if (thinkingContent.isNotEmpty()) {
            ThinkingSection(
                content = thinkingContent,
                timeMs = thinkingTimeMs
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 主文本内容（带打字机效果）
        if (content.isNotEmpty()) {
            TypewriterText(
                text = content,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 等待中的动画
            LoadingDots()
        }
    }
}

@Composable
private fun ThinkingSection(
    content: String,
    timeMs: Long
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "思考过程",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (timeMs > 0) {
                        Text(
                            text = " (${timeMs / 1000.0}s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier
) {
    // 简单实现：直接显示全部文本
    // 高级实现可以添加逐字动画
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(8.dp)
                    .alpha(alpha)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            )
        }
    }
}
```

### 9.5 数据库Entity更新

```kotlin
// data/local/entity/AiAdvisorMessageBlockEntity.kt
@Entity(
    tableName = "ai_advisor_message_blocks",
    foreignKeys = [
        ForeignKey(
            entity = AiAdvisorConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("message_id")]
)
data class AiAdvisorMessageBlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "type")
    val type: String,  // MessageBlockType.name

    @ColumnInfo(name = "status")
    val status: String,  // MessageBlockStatus.name

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,  // JSON格式

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): AiAdvisorMessageBlock = AiAdvisorMessageBlock(
        id = id,
        messageId = messageId,
        type = MessageBlockType.valueOf(type),
        status = MessageBlockStatus.valueOf(status),
        content = content,
        metadata = metadata?.let { parseMetadata(it) },
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(block: AiAdvisorMessageBlock): AiAdvisorMessageBlockEntity =
            AiAdvisorMessageBlockEntity(
                id = block.id,
                messageId = block.messageId,
                type = block.type.name,
                status = block.status.name,
                content = block.content,
                metadata = block.metadata?.let { serializeMetadata(it) },
                createdAt = block.createdAt
            )

        private fun parseMetadata(json: String): Map<String, Any> {
            return try {
                Moshi.Builder().build()
                    .adapter<Map<String, Any>>(
                        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                    )
                    .fromJson(json) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }

        private fun serializeMetadata(metadata: Map<String, Any>): String {
            return Moshi.Builder().build()
                .adapter<Map<String, Any>>(
                    Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                )
                .toJson(metadata)
        }
    }
}
```

---

## 10. 测试策略

### 10.1 流式响应单元测试

```kotlin
@Test
fun `流式响应正确解析文本增量`() = runTest {
    // Given
    val sseData = """data: {"choices":[{"delta":{"content":"Hello"}}]}"""

    // When
    val chunk = sseStreamReader.parseChunk(sseData)

    // Then
    assertThat(chunk).isInstanceOf(AiStreamChunk.TextDelta::class.java)
    assertThat((chunk as AiStreamChunk.TextDelta).text).isEqualTo("Hello")
}

@Test
fun `流式响应正确解析思考内容`() = runTest {
    // Given
    val sseData = """data: {"choices":[{"delta":{"reasoning_content":"Let me think..."}}]}"""

    // When
    val chunk = sseStreamReader.parseChunk(sseData)

    // Then
    assertThat(chunk).isInstanceOf(AiStreamChunk.ThinkingDelta::class.java)
    assertThat((chunk as AiStreamChunk.ThinkingDelta).text).isEqualTo("Let me think...")
}

@Test
fun `ViewModel正确累积流式内容`() = runTest {
    // Given
    val viewModel = AiAdvisorChatViewModel(...)

    // When
    viewModel.handleStreamChunk(AiStreamChunk.TextDelta("Hello"))
    viewModel.handleStreamChunk(AiStreamChunk.TextDelta(" World"))

    // Then
    assertThat(viewModel.uiState.value.streamingContent).isEqualTo("Hello World")
}
```

### 10.2 Block管理测试

```kotlin
@Test
fun `智能节流在块类型变化时立即写入`() = runTest {
    // Given
    val blockManager = BlockUpdateManager(repository, testDispatcher)

    // When
    blockManager.smartUpdate("block1", "thinking...", MessageBlockType.THINKING)
    blockManager.smartUpdate("block1", "response", MessageBlockType.MAIN_TEXT)

    // Then
    verify(repository, times(2)).updateBlock(any(), any())
}

@Test
fun `智能节流在同类型时延迟写入`() = runTest {
    // Given
    val blockManager = BlockUpdateManager(repository, testDispatcher)

    // When
    blockManager.smartUpdate("block1", "Hello", MessageBlockType.MAIN_TEXT)
    blockManager.smartUpdate("block1", "Hello World", MessageBlockType.MAIN_TEXT)

    // Then - 应该只有一次写入（节流后）
    advanceTimeBy(350) // 超过300ms节流时间
    verify(repository, times(1)).updateBlock(any(), any())
}
```

---

**文档版本**: 1.1  
**最后更新**: 2026-01-04  
**更新内容**: 添加代码实现示例和测试策略
