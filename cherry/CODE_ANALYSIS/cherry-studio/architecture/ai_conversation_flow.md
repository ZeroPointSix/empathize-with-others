# AI 对话流程架构分析

## 一、整体架构概览

Cherry Studio 采用**分层架构设计**，实现了用户输入到 AI 响应的完整对话流程：

```
┌─────────────────────────────────────────────────────────────────┐
│                      UI Layer (React Native)                     │
│  ChatScreen → MessageInputContainer → Messages                   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                       Service Layer                              │
│  MessagesService → OrchestrationService → ApiService             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                       AI Core Layer                              │
│  ModernAiProvider / LegacyAiProvider                             │
│  AiSdkToChunkAdapter → StreamProcessingService                   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                       Data Layer                                 │
│  SQLite (Drizzle ORM) + Redux (Runtime State) + MMKV             │
└─────────────────────────────────────────────────────────────────┘
```

## 二、AI 对话核心流程

### 2.1 完整数据流

```
用户输入 → useMessageSend.sendMessage()
         → MessagesService.getUserMessage() 创建用户消息
         → MessagesService.sendMessage() 保存并触发 AI 请求
         → fetchAndProcessAssistantResponseImpl()
         → OrchestrationService.transformMessagesAndFetch()
         → ConversationService.prepareMessagesForModel() 消息过滤转换
         → ApiService.fetchChatCompletion()
         → ModernAiProvider.completions()
         → AiSdkToChunkAdapter.processStream() 流式处理
         → StreamProcessingService.createStreamProcessor() 分发 Chunk
         → BlockManager 管理消息块状态
         → 数据库持久化 + UI 实时更新
```

### 2.2 关键文件路径

| 模块 | 文件路径 |
|------|----------|
| 消息发送入口 | `src/componentsV2/features/ChatScreen/MessageInput/hooks/useMessageSend.ts` |
| 消息服务 | `src/services/MessagesService.ts` |
| 编排服务 | `src/services/OrchestrationService.ts` |
| API 服务 | `src/services/ApiService.ts` |
| 新版 AI Provider | `src/aiCore/index_new.ts` |
| 流处理适配器 | `src/aiCore/chunk/AiSdkToChunkAdapter.ts` |
| Chunk 处理器 | `src/services/StreamProcessingService.ts` |
| Block 管理器 | `src/services/messageStreaming/BlockManager.ts` |

### 2.3 消息发送流程详解

**1. 用户输入处理** (`useMessageSend.ts:51-128`)

```typescript
const sendMessage = async (overrideText?: string) => {
  // 1. 准备输入数据
  const { message, blocks } = getUserMessage(baseUserMessage)

  // 2. 处理 mentions (多模型)
  if (currentMentions.length > 0) {
    message.mentions = currentMentions
  }

  // 3. 发送消息
  await _sendMessage(message, blocks, assistant, topic.id)
}
```

**2. 消息服务处理** (`MessagesService.ts:125-156`)

```typescript
export async function sendMessage(userMessage, userMessageBlocks, assistant, topicId) {
  // 保存用户消息到数据库
  await saveMessageAndBlocksToDB(userMessage, userMessageBlocks)

  // 多模型处理或单模型处理
  if (mentionedModels?.length > 0) {
    await multiModelResponses(topicId, assistant, userMessage, mentionedModels)
  } else {
    // 创建助手消息占位
    const assistantMessage = createAssistantMessage(assistant.id, topicId, {...})
    await fetchAndProcessAssistantResponseImpl(topicId, assistant, assistantMessage)
  }
}
```

## 三、流式响应 (Streaming) 实现

### 3.1 Chunk 类型系统 (`types/chunk.ts`)

定义了完整的流式事件生命周期：

```typescript
export enum ChunkType {
  // LLM 响应生命周期
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

  // 图像生成
  IMAGE_CREATED = 'image.created',
  IMAGE_DELTA = 'image.delta',
  IMAGE_COMPLETE = 'image.complete',

  // 搜索
  LLM_WEB_SEARCH_IN_PROGRESS = 'llm_websearch_in_progress',
  LLM_WEB_SEARCH_COMPLETE = 'llm_websearch_complete',

  // 错误与完成
  ERROR = 'error',
  BLOCK_COMPLETE = 'block_complete'
}
```

### 3.2 AI SDK 到 Chunk 适配器

`AiSdkToChunkAdapter` 将 AI SDK 的流转换为 Cherry Studio 的 Chunk 格式：

```typescript
// src/aiCore/chunk/AiSdkToChunkAdapter.ts
class AiSdkToChunkAdapter {
  async processStream(aiSdkResult: any): Promise<string> {
    if (aiSdkResult.fullStream) {
      await this.readFullStream(aiSdkResult.fullStream)
    }
    return await aiSdkResult.text
  }

  private convertAndEmitChunk(chunk: TextStreamPart<any>, final) {
    switch (chunk.type) {
      case 'text-delta':
        this.onChunk({ type: ChunkType.TEXT_DELTA, text: processedText })
        break
      case 'reasoning-delta':
        this.onChunk({ type: ChunkType.THINKING_DELTA, text: final.reasoningContent })
        break
      case 'tool-call':
        this.toolCallHandler.handleToolCall(chunk)
        break
      case 'finish':
        this.onChunk({ type: ChunkType.BLOCK_COMPLETE, response: {...} })
        break
    }
  }
}
```

### 3.3 Stream 处理服务

`StreamProcessingService` 将 Chunk 分发到对应的回调：

```typescript
// src/services/StreamProcessingService.ts
export function createStreamProcessor(callbacks: StreamProcessorCallbacks) {
  return (chunk: Chunk) => {
    switch (data.type) {
      case ChunkType.TEXT_DELTA:
        callbacks.onTextChunk?.(data.text)
        break
      case ChunkType.THINKING_DELTA:
        callbacks.onThinkingChunk?.(data.text, data.thinking_millsec)
        break
      case ChunkType.MCP_TOOL_COMPLETE:
        data.responses.forEach(toolResp => callbacks.onToolCallComplete!(toolResp))
        break
      case ChunkType.ERROR:
        callbacks.onError?.(data.error)
        break
      case ChunkType.BLOCK_COMPLETE:
        callbacks.onComplete?.(AssistantMessageStatus.SUCCESS, data.response)
        break
    }
  }
}
```

## 四、关键代码路径总结

| 功能 | 核心文件 |
|------|----------|
| **用户输入** | `src/componentsV2/features/ChatScreen/MessageInput/hooks/useMessageSend.ts` |
| **消息创建** | `src/services/MessagesService.ts` - `getUserMessage()`, `sendMessage()` |
| **消息转换** | `src/services/ConversationService.ts` - `prepareMessagesForModel()` |
| **AI 请求** | `src/services/ApiService.ts` - `fetchChatCompletion()` |
| **Provider 核心** | `src/aiCore/index_new.ts` - `ModernAiProvider.completions()` |
| **流式适配** | `src/aiCore/chunk/AiSdkToChunkAdapter.ts` - `processStream()` |
| **Chunk 分发** | `src/services/StreamProcessingService.ts` - `createStreamProcessor()` |
| **块状态管理** | `src/services/messageStreaming/BlockManager.ts` |
| **回调处理** | `src/services/messageStreaming/callbacks/baseCallbacks.ts` |
| **消息展示** | `src/screens/home/messages/Messages.tsx` |
| **类型定义** | `src/types/message.ts`, `src/types/chunk.ts` |

## 五、架构亮点

1. **渐进式重构**: Legacy/Modern 双 Provider 并存，支持平滑迁移
2. **块状消息架构**: 消息拆分为多个 Block，支持复杂内容组合（思考+文本+工具+引用）
3. **实时数据流**: Drizzle `useLiveQuery` 实现数据库变更实时同步到 UI
4. **智能节流更新**: BlockManager 根据块类型变化自动切换即时/节流写入策略
5. **统一 Chunk 协议**: 所有流式事件通过 ChunkType 枚举标准化，便于扩展
6. **多模型支持**: 通过 mentions 机制实现单消息多模型并行响应
