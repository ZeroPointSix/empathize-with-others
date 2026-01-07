# 状态管理架构

## 一、状态管理概览

Cherry Studio 采用**混合状态管理策略**，结合多种存储方案：

```
┌─────────────────────────────────────────────────────────────┐
│                      状态管理架构                             │
├─────────────────────────────────────────────────────────────┤
│  Redux Toolkit           │  持久化全局状态                    │
│  ├─ app                  │  应用初始化/引导状态               │
│  ├─ assistant            │  助手配置/市场                     │
│  ├─ topic                │  对话话题管理                      │
│  ├─ settings             │  用户偏好设置                      │
│  └─ runtime              │  临时运行时状态（不持久化）         │
├─────────────────────────────────────────────────────────────┤
│  SQLite + Drizzle ORM    │  持久化业务数据                    │
│  ├─ messages             │  消息记录                          │
│  ├─ message_blocks       │  消息块（文本/思考/工具等）        │
│  ├─ topics               │  对话话题                          │
│  ├─ assistants           │  助手配置                          │
│  └─ ...                  │  其他实体                          │
├─────────────────────────────────────────────────────────────┤
│  MMKV                    │  高性能键值存储                    │
│  ├─ sensitive_data       │  敏感数据（API Key 等）            │
│  ├─ cache                │  临时缓存                          │
│  └─ ...                  │  其他需要高性能访问的数据           │
└─────────────────────────────────────────────────────────────┘
```

## 二、Redux Store 结构

### 2.1 Store 配置 (`src/store/index.ts`)

```typescript
export const store = configureStore({
  reducer: {
    app: appSlice,
    assistant: assistantSlice,
    topic: topicSlice,
    settings: settingsSlice,
    runtime: runtimeSlice,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // 忽略某些不需要序列化的 action
        ignoredActions: ['topic/setCurrentTopic'],
      },
    }),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
```

### 2.2 各 Slice 职责

```typescript
// src/store/app.ts - 应用初始化状态
interface AppState {
  isInitialized: boolean
  onboardingCompleted: boolean
  currentScreen: string
  // ...
}

// src/store/assistant.ts - 助手管理
interface AssistantState {
  items: Assistant[]
  marketplace: Assistant[]
  selectedId: string | null
  // ...
}

// src/store/topic.ts - 对话话题
interface TopicState {
  items: Topic[]
  currentTopicId: string | null
  isLoading: boolean
  // ...
}

// src/store/settings.ts - 用户设置
interface SettingsState {
  theme: 'light' | 'dark' | 'system'
  language: string
  modelConfigs: Record<string, ModelConfig>
  // ...
}

// src/store/runtime.ts - 临时运行时状态（不持久化）
interface RuntimeState {
  htmlPreviewContent: string | null
  editingMessage: Message | null
  // ...
}
```

## 三、消息状态管理

### 3.1 消息状态枚举 (`types/message.ts`)

```typescript
// 助手消息状态
export enum AssistantMessageStatus {
  PENDING = 'pending',      // 等待处理
  PROCESSING = 'processing', // 正在处理
  SEARCHING = 'searching',   // 搜索中
  SUCCESS = 'success',       // 成功
  PAUSED = 'paused',        // 暂停
  ERROR = 'error'           // 错误
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

### 3.2 消息块类型

Cherry Studio 采用**块状消息架构**，一条消息包含多个块：

```typescript
export enum MessageBlockType {
  UNKNOWN = 'unknown',      // 占位块
  MAIN_TEXT = 'main_text',  // 主文本
  THINKING = 'thinking',    // 思考过程
  TRANSLATION = 'translation',
  IMAGE = 'image',
  CODE = 'code',
  TOOL = 'tool',           // 工具调用
  FILE = 'file',
  ERROR = 'error',
  CITATION = 'citation'    // 引用/搜索结果
}
```

### 3.3 消息数据结构

```typescript
// 消息
interface Message {
  id: string
  topic_id: string
  role: 'user' | 'assistant'
  status?: AssistantMessageStatus
  created_at: number
  metadata?: MessageMetadata
}

// 消息块
interface MessageBlock {
  id: string
  message_id: string
  type: MessageBlockType
  status: MessageBlockStatus
  content: string
  metadata?: BlockMetadata
}

// 消息元数据
interface MessageMetadata {
  model_id?: string
  provider_id?: string
  thinking_millsec?: number
  citations?: Citation[]
  // ...
}
```

## 四、数据库实时查询

### 4.1 Drizzle ORM 实时查询 (`hooks/useMessages.ts`)

```typescript
import { useLiveQuery } from 'drizzle-orm/expo'
import { db } from '@/db'
import { messagesSchema, messageBlocksSchema } from '@/db/schema'

export const useMessages = (topicId: string) => {
  // 实时查询消息
  const messagesQuery = db.select()
    .from(messagesSchema)
    .where(eq(messagesSchema.topic_id, topicId))
    .orderBy(desc(messagesSchema.created_at))

  const { data: rawMessages, pending } = useLiveQuery(messagesQuery, [topicId])

  // 实时查询消息块
  const blocksQuery = db.select({
    id: messageBlocksSchema.id,
    messageId: messageBlocksSchema.message_id,
    type: messageBlocksSchema.type,
    content: messageBlocksSchema.content,
    status: messageBlocksSchema.status,
    metadata: messageBlocksSchema.metadata,
  })
  .from(messageBlocksSchema)
  .innerJoin(
    messagesSchema,
    eq(messageBlocksSchema.message_id, messagesSchema.id)
  )
  .where(eq(messagesSchema.topic_id, topicId))

  const { data: rawBlocks } = useLiveQuery(blocksQuery, [topicId])

  // 组装消息与块的关系
  return { messages: processedMessages, blocks: rawBlocks, pending }
}
```

### 4.2 BlockManager 状态管理

`BlockManager` 负责消息块的智能更新策略：

```typescript
// src/services/messageStreaming/BlockManager.ts
class BlockManager {
  private deps: BlockManagerDeps
  private _lastBlockType: MessageBlockType | null = null

  // 智能更新：根据块类型连续性自动判断节流或立即更新
  async smartBlockUpdate(blockId: string, changes: Partial<MessageBlock>, blockType: MessageBlockType, isComplete = false) {
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

  // 处理流式文本块
  async handleStreamingText(blockId: string, text: string) {
    await this.smartBlockUpdate(
      blockId,
      { content: text, status: MessageBlockStatus.STREAMING },
      MessageBlockType.MAIN_TEXT
    )
  }

  // 处理思考过程块
  async handleThinkingBlock(blockId: string, thinking: string, thinkingMs: number) {
    await this.smartBlockUpdate(
      blockId,
      {
        content: thinking,
        metadata: { thinking_millsec: thinkingMs },
        status: MessageBlockStatus.STREAMING
      },
      MessageBlockType.THINKING
    )
  }
}
```

## 五、异步操作处理

### 5.1 Redux Thunk 示例

```typescript
// src/store/topic/thunks.ts
export const fetchTopics = createAsyncThunk(
  'topic/fetchTopics',
  async (_, { dispatch }) => {
    const topics = await TopicService.getAllTopics()
    dispatch(setTopics(topics))
    return topics
  }
)

export const createTopic = createAsyncThunk(
  'topic/createTopic',
  async (payload: { title: string; assistantId: string }, { dispatch }) => {
    const topic = await TopicService.createTopic(payload)
    dispatch(addTopic(topic))
    return topic
  }
)

export const deleteTopic = createAsyncThunk(
  'topic/deleteTopic',
  async (topicId: string, { dispatch }) => {
    await TopicService.deleteTopic(topicId)
    dispatch(removeTopic(topicId))
  }
)
```

### 5.2 服务层异步操作

```typescript
// src/services/MessagesService.ts
export async function sendMessage(userMessage, userMessageBlocks, assistant, topicId) {
  // 1. 保存用户消息到数据库
  await saveMessageAndBlocksToDB(userMessage, userMessageBlocks)

  // 2. 多模型处理或单模型处理
  if (mentionedModels?.length > 0) {
    await multiModelResponses(topicId, assistant, userMessage, mentionedModels)
  } else {
    // 3. 创建助手消息占位
    const assistantMessage = createAssistantMessage(assistant.id, topicId, {...})
    await fetchAndProcessAssistantResponseImpl(topicId, assistant, assistantMessage)
  }
}
```

## 六、状态流转图

### 6.1 消息状态流转

```
用户发送消息
    │
    ▼
┌─────────────┐
│  保存用户消息 │  →  状态: SUCCESS
└─────────────┘
    │
    ▼
┌─────────────────────┐
│ 创建助手消息占位      │  →  状态: PENDING
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│ 开始流式响应          │  →  状态: PROCESSING
└─────────────────────┘
    │
    ├─→ 思考块  ──→ THINKING
    │
    ├─→ 文本块  ──→ STREAMING
    │
    ├─→ 工具调用 ──→ PROCESSING
    │
    └─→ 完成     ──→ SUCCESS / ERROR
```

### 6.2 消息块状态流转

```
┌──────────┐     ┌─────────────┐     ┌──────────┐
│ PENDING  │ ──→ │ PROCESSING  │ ──→ │ STREAMING│
└──────────┘     └─────────────┘     └──────────┘
                                              │
                    ┌─────────────────────────┘
                    ▼
              ┌──────────┐     ┌──────────┐
              │ SUCCESS  │ ←── │ COMPLETE │
              └──────────┘     └──────────┘

错误路径:
PENDING/PROCESSING/STREAMING → ERROR
```

## 七、状态持久化策略

### 7.1 Redux 持久化

```typescript
// 使用 redux-persist 进行状态持久化
import AsyncStorage from '@react-native-async-storage/async-storage'

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
  whitelist: ['app', 'assistant', 'topic', 'settings'],
  blacklist: ['runtime'],  // runtime 不持久化
}

export const persistedStore = persistReducer(persistConfig, rootReducer)
```

### 7.2 数据库持久化

```typescript
// 消息和消息块通过 Drizzle ORM 持久化到 SQLite
// schema 定义
export const messagesSchema = sqliteTable('messages', {
  id: text('id').primaryKey(),
  topic_id: text('topic_id').notNull(),
  role: text('role').notNull(),
  status: text('status'),
  created_at: integer('created_at').notNull(),
  metadata: text('metadata', { mode: 'json' }),
})

export const messageBlocksSchema = sqliteTable('message_blocks', {
  id: text('id').primaryKey(),
  message_id: text('message_id').notNull(),
  type: text('type').notNull(),
  status: text('status').notNull(),
  content: text('content').notNull(),
  metadata: text('metadata', { mode: 'json' }),
})
```

## 八、架构优势

1. **分层存储**: 根据数据特性和访问模式选择合适的存储方案
2. **实时同步**: useLiveQuery 实现数据库变更实时推送 UI
3. **智能节流**: BlockManager 根据块类型智能调整更新频率
4. **状态分离**: 持久化状态与临时状态分离，减少不必要的 IO
5. **类型安全**: 完整的 TypeScript 类型定义
6. **可测试性**: Redux + 服务层的架构易于单元测试
