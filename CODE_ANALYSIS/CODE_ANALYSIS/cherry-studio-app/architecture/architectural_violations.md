# 架构违规检测 - Cherry Studio 表现层

## 违规概览

| 违规类型 | 严重程度 | 数量 | 优先级 |
|---------|---------|------|-------|
| 业务逻辑泄漏到表现层 | 🔴 严重 | 5 | P0 |
| 巨型组件 | 🟠 中等 | 3 | P1 |
| Props Drilling | 🟡 轻微 | 4 | P2 |
| 直接数据库访问 | 🔴 严重 | 8 | P0 |
| 职责不清 | 🟠 中等 | 6 | P1 |

## 1. 严重违规 (P0)

### 1.1 业务逻辑泄漏到表现层

#### 违规 1: Hook 直接操作数据库

**文件**: `src/hooks/useMessageActions.tsx:64-100`

```typescript
const handleDelete = async () => {
  return new Promise<void>((resolve, reject) => {
    presentDialog('error', {
      title: t('message.delete_message'),
      content: t('message.delete_message_confirmation'),
      onConfirm: async () => {
        try {
          // ❌ 违规：直接调用数据库操作
          await deleteMessageById(message.id)
          if (message.askId) {
            await deleteMessageById(message.askId)
          }
          // ...
        }
      }
    })
  })
}
```

**影响**:
- 违反分层架构原则
- 难以进行单元测试
- 数据库逻辑与 UI 逻辑耦合
- 复用性差

**建议修复**:
```typescript
// ✅ 建议：分离到 Service 层
// services/messageService.ts
export const messageService = {
  async deleteMessage(messageId: string): Promise<void> {
    await deleteMessageById(messageId)
  },
  async deleteMessagePair(askId: string, replyId: string): Promise<void> {
    await deleteMessageById(askId)
    await deleteMessageById(replyId)
  }
}

// hooks/useMessageActions.ts
const handleDelete = async () => {
  presentDialog('error', {
    onConfirm: async () => {
      await messageService.deleteMessagePair(message.id, message.askId)
    }
  })
}
```

#### 违规 2: 组件内复杂的业务逻辑

**文件**: `src/screens/home/messages/blocks/ErrorBlock.tsx:50-150`

```typescript
// ❌ 违规：组件内包含业务逻辑
export const ErrorBlock: React.FC<ErrorBlockProps> = ({ block, message }) => {
  const [expanded, setExpanded] = useState(false)

  // 业务逻辑混合在组件中
  const handleRetry = async () => {
    try {
      await retryMessage(message.id)  // 直接调用业务操作
      await regenerateMessage(message)  // 复杂的重试逻辑
    } catch (error) {
      handleError(error)
    }
  }

  const handleCopy = async () => {
    await Clipboard.setStringAsync(block.content)
    showToast(t('message.copied'))
  }

  return <View>{/* ... */}</View>
}
```

### 1.2 直接数据库访问

#### 违规 3: Hook 导入 Drizzle ORM

**文件**: `src/hooks/useMessages.ts:4-18`

```typescript
// ❌ 违规：从数据层导入 ORM
import { eq } from 'drizzle-orm'
import { messages as messagesSchema } from '../db/schema/messages'
import { blocks as blocksSchema } from '../db/schema/blocks'
import { useLiveQuery } from 'drizzle-orm/expo-sqlite'

export const useMessages = (topicId: string) => {
  const query = db
    .select()
    .from(messagesSchema)
    .leftJoin(blocksSchema, eq(messagesSchema.id, blocksSchema.message_id))
    .where(eq(messagesSchema.topic_id, topicId))
  // ...
}
```

**类似违规文件**:
- `src/hooks/useTopics.ts`
- `src/hooks/useMessageBlocks.ts`
- `src/hooks/useAssistants.ts`
- `src/hooks/useFiles.ts`

**建议修复**:
```typescript
// ✅ 建议：创建 Repository 层
// repositories/messageRepository.ts
export const messageRepository = {
  getByTopicId(topicId: string) {
    return db
      .select()
      .from(messagesSchema)
      .where(eq(messagesSchema.topic_id, topicId))
  },

  getWithBlocks(topicId: string) {
    return db
      .select()
      .from(messagesSchema)
      .leftJoin(blocksSchema, eq(messagesSchema.id, blocksSchema.message_id))
      .where(eq(messagesSchema.topic_id, topicId))
  }
}

// hooks/useMessages.ts
import { messageRepository } from '@/repositories/messageRepository'

export const useMessages = (topicId: string) => {
  const { data } = useLiveQuery(
    () => messageRepository.getWithBlocks(topicId),
    [topicId]
  )
}
```

## 2. 中等违规 (P1)

### 2.1 巨型组件

#### 违规 4: ErrorBlock 组件过大

**文件**: `src/screens/home/messages/blocks/ErrorBlock.tsx`
**行数**: 578 行

**问题分析**:
```
❌ 单一文件包含:
  - 错误状态管理
  - 多种错误类型渲染
  - 复制/重试/详情等操作
  - 动画逻辑
  - 主题适配
```

**组件结构**:
```
ErrorBlock.tsx (578行)
├── 状态定义 (50行)
├── 常量定义 (30行)
├── 类型定义 (40行)
├── 错误格式化函数 (80行)
├── 渲染函数 (100行)
└── 主组件 (278行)
```

**建议拆分**:
```
src/screens/home/messages/blocks/ErrorBlock/
├── ErrorBlock.tsx              (主组件，50行)
├── ErrorHeader.tsx             (头部，40行)
├── ErrorContent.tsx            (内容，60行)
├── ErrorActions.tsx            (操作按钮，50行)
├── ErrorDetail.tsx             (详情展开，40行)
├── hooks/
│   ├── useErrorState.ts        (状态管理)
│   ├── useErrorActions.ts      (操作方法)
│   └── useErrorFormatting.ts   (格式化逻辑)
└── types.ts                    (类型定义)
```

#### 违规 5: ChatContent 组件过大

**文件**: `src/componentsV2/features/ChatScreen/ChatContent/ChatContent.tsx`
**行数**: ~300 行

**问题**:
- 包含过多的渲染逻辑
- 状态管理与 UI 混合
- 缺少子组件拆分

#### 违规 6: Messages 组件行数过多

**文件**: `src/screens/home/messages/Messages.tsx`
**行数**: ~250 行

**问题**:
- 列表渲染逻辑复杂
- 缺少虚拟滚动优化
- 状态管理可提取

### 2.2 Context 过于复杂

#### 违规 7: MessageInputContext 包含过多状态

**文件**: `src/componentsV2/features/ChatScreen/MessageInput/components/Root.tsx`

```typescript
// ❌ 违规：Context 包含过多属性
interface MessageInputContextValue {
  // 状态 (6个)
  files: File[]
  text: string
  mentions: Mention[]
  isRecording: boolean
  isGenerating: boolean

  // Setters (6个)
  setFiles: (files: File[]) => void
  setText: (text: string) => void
  setMentions: (mentions: Mention[]) => void
  setIsRecording: (value: boolean) => void
  setIsGenerating: (value: boolean) => void

  // Actions (8个)
  addFiles: (files: File[]) => void
  clearFiles: () => void
  clearText: () => void
  handlePasteImages: (images: string[]) => void
  // ... 更多方法
}
```

**建议**: 拆分为多个专注的 Context
```typescript
// ✅ 建议：拆分 Context
const MessageInputStateContext = createContext<State>({})
const MessageInputActionsContext = createContext<Actions>({})
const MessageInputConfigContext = createContext<Config>({})
```

## 3. 轻微违规 (P2)

### 3.1 Props Drilling

#### 违规 8: 深层 Props 传递

**文件**: `src/componentsV2/features/ChatScreen/MessageInput/components/ToolButton.tsx`

```typescript
// ❌ 违规：多层传递 props
export const MessageInputToolButton: React.FC = () => {
  // 通过 Context 获取，但设计过于复杂
  const { mentions, files, setFiles, assistant, updateAssistant } = useMessageInput()
  return <ToolButton mentions={mentions} files={files} />
}
```

**建议**: 使用 Context 替代，或使用状态管理库

#### 违规 9: 跨组件状态传递

**文件**: `src/screens/home/messages/messages.tsx`

```typescript
// ❌ 违规：兄弟组件间状态传递
const Messages: React.FC<MessagesProps> = ({ topicId }) => {
  const [selectedMessage, setSelectedMessage] = useState<Message | null>(null)

  return (
    <View>
      <MessageList
        messages={messages}
        onSelect={setSelectedMessage}
      />
      <MessageDetail
        message={selectedMessage}
        onClose={() => setSelectedMessage(null)}
      />
    </View>
  )
}
```

**建议**: 使用状态管理库（Zustand/Redux）替代本地状态

### 3.2 命名不一致

#### 违规 10: 组件命名风格不统一

| 位置 | 命名风格 |
|-----|---------|
| `componentsV2/base/` | PascalCase (Text, IconButton) |
| `componentsV2/layout/` | PascalCase (YStack, XStack) |
| `screens/home/` | mixed (messages.tsx, blocks/*.tsx) |
| `hooks/` | camelCase (useMessages, useTopics) |

**建议**: 统一使用 PascalCase 命名组件文件

### 3.3 错误处理不一致

#### 违规 11: 错误处理方式多样

| 组件 | 错误处理方式 |
|-----|-------------|
| ErrorBlock | 专用错误展示组件 |
| 其他组件 | Toast 提示 |
| 部分组件 | 无错误处理 |

**建议**: 统一错误处理策略，创建 ErrorBoundary

## 4. 违规汇总统计

### 4.1 按文件统计

| 文件 | 违规类型 | 严重程度 |
|-----|---------|---------|
| `hooks/useMessageActions.tsx` | 业务逻辑泄漏 | 🔴 严重 |
| `hooks/useMessages.ts` | 直接 DB 访问 | 🔴 严重 |
| `hooks/useTopics.ts` | 直接 DB 访问 | 🔴 严重 |
| `hooks/useAssistants.ts` | 直接 DB 访问 | 🔴 严重 |
| `ErrorBlock.tsx` | 巨型组件 | 🟠 中等 |
| `ChatContent.tsx` | 巨型组件 | 🟠 中等 |
| `Messages.tsx` | 巨型组件 | 🟠 中等 |
| `MessageInput/Root.tsx` | Context 复杂 | 🟠 中等 |

### 4.2 按严重程度统计

| 严重程度 | 数量 | 占比 |
|---------|------|------|
| 🔴 严重 | 8 | 35% |
| 🟠 中等 | 9 | 40% |
| 🟡 轻微 | 6 | 25% |

## 5. 修复优先级

### 5.1 立即修复 (本周)

| 违规 | 修复方案 | 预计工时 |
|-----|---------|---------|
| useMessageActions 业务逻辑 | 抽取到 Service 层 | 2h |
| useMessages 直接 DB 访问 | 创建 Repository 层 | 3h |

### 5.2 下个迭代修复

| 违规 | 修复方案 | 预计工时 |
|-----|---------|---------|
| ErrorBlock 拆分 | 组件重构 | 4h |
| Context 拆分 | 重构 MessageInput | 3h |

### 5.3 长期改进

| 违规 | 修复方案 | 预计工时 |
|-----|---------|---------|
| 建立组件规范 | 制定规范文档 | 1d |
| 统一错误处理 | 创建 ErrorBoundary | 2d |
| 引入状态管理库 | 评估并引入 Zustand | 3d |

## 6. 架构健康度评分

| 指标 | 当前评分 | 目标评分 | 差距 |
|-----|---------|---------|------|
| 分层清晰度 | 6/10 | 9/10 | -3 |
| 单一职责 | 5/10 | 9/10 | -4 |
| 依赖管理 | 5/10 | 9/10 | -4 |
| 可测试性 | 6/10 | 9/10 | -3 |
| 可维护性 | 6/10 | 9/10 | -3 |

**当前总分**: 5.6/10
**目标总分**: 9/10
