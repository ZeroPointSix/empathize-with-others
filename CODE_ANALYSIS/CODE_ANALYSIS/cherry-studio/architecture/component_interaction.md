# 组件交互架构

## 一、ChatScreen 组件结构

```
ChatScreen (src/screens/home/chat/ChatScreen.tsx)
│
├── ChatScreenHeader        // 顶部栏
│   ├── BackButton
│   ├── Title
│   └── Actions (更多菜单)
│
├── ChatContent             // 消息内容区域
│   └── Messages            // 消息列表组件
│       ├── LegendList      // 虚拟列表（消息组复用）
│       │   └── MessageGroup (按时间/角色分组)
│       │       └── Message (单条消息)
│       │           ├── MessageHeader      // 头像/名称
│       │           ├── MessageContent     // 消息内容容器
│       │           │   └── BlockRenderer  // 块渲染器
│       │           │       ├── TextBlock      // 文本块
│       │           │       ├── ThinkingBlock  // 思考块
│       │           │       ├── CodeBlock      // 代码块
│       │           │       ├── ImageBlock     // 图片块
│       │           │       ├── ToolBlock      // 工具调用块
│       │           │       └── CitationBlock  // 引用块
│       │           └── MessageFooter     // 操作按钮（复制/编辑等）
│       │
│       └── ScrollToBottomButton  // 滚动到底部按钮
│
└── MessageInputContainer   // 输入区域容器
    ├── InputArea           // 输入区域
    │   ├── TextInput       // 文本输入框
    │   ├── MentionInput    // @提及组件
    │   └── FilePreview     // 文件预览
    ├── Toolbar             // 工具栏
    │   ├── AttachmentButton
    │   ├── VoiceButton
    │   └── MoreButton
    └── ActionButtons       // 操作按钮
        ├── SendButton
        └── PauseButton     // 暂停生成按钮
```

## 二、消息输入交互流程

### 2.1 useMessageSend Hook (`src/componentsV2/features/ChatScreen/MessageInput/hooks/useMessageSend.ts`)

```typescript
interface UseMessageSendProps {
  topic: Topic
  assistant: Assistant
  text: string
  files: File[]
  mentions: Mention[]
  clearInputs: () => void
  restoreInputs: () => void
}

export const useMessageSend = (props: UseMessageSendProps) => {
  // 状态
  const [isGenerating, setIsGenerating] = useState(false)

  // 核心方法
  const sendMessage = useCallback(async (overrideText?: string) => {
    // 1. 验证输入
    if (!validateInput()) return

    // 2. 获取用户消息
    const { message, blocks } = getUserMessage(baseUserMessage)

    // 3. 处理 mentions (多模型)
    if (currentMentions.length > 0) {
      message.mentions = currentMentions
    }

    // 4. 调用消息服务
    await _sendMessage(message, blocks, assistant, topic.id)

    // 5. 清理输入
    clearInputs()
  }, [...])

  const onPause = useCallback(() => {
    // 暂停生成
    pauseGeneration()
  }, [])

  return {
    sendMessage,
    onPause,
    isGenerating,
  }
}
```

### 2.2 组件与 Hook 连接

```typescript
// MessageInputContainer.tsx
const MessageInputContainer: FC<MessageInputContainerProps> = ({
  topic,
  assistant,
}) => {
  const { text, setText } = useInputText()
  const { files, addFile, removeFile } = useFileAttachments()
  const { mentions, addMention, removeMention } = useMentions()
  const { sendMessage, onPause, isGenerating } = useMessageSend({
    topic,
    assistant,
    text,
    files,
    mentions,
    clearInputs,
    restoreInputs,
  })

  return (
    <View style={styles.container}>
      <InputArea>
        <TextInput value={text} onChangeText={setText} />
        <FilePreview files={files} onRemove={removeFile} />
      </InputArea>

      <Toolbar>
        <AttachmentButton onPress={addFile} />
        <VoiceButton />
        <MoreButton />
      </Toolbar>

      <ActionButtons>
        {isGenerating ? (
          <PauseButton onPress={onPause} />
        ) : (
          <SendButton onPress={sendMessage} />
        )}
      </ActionButtons>
    </View>
  )
}
```

## 三、消息列表渲染架构

### 3.1 Messages 组件 (`src/screens/home/messages/Messages.tsx`)

```typescript
const Messages: FC<MessagesProps> = ({ assistant, topic }) => {
  // 实时数据库查询
  const { messages, blocks, pending } = useMessages(topic.id)

  // 按时间/角色分组
  const groupedMessages = useMemo(() => {
    return groupMessagesByTimeAndRole(messages)
  }, [messages])

  return (
    <View style={styles.container}>
      {pending && <LoadingIndicator />}

      <LegendList
        data={groupedMessages}
        renderItem={({ item }) => (
          <MessageGroup
            assistant={assistant}
            item={item}
            messageBlocks={blocks}
          />
        )}
        // 列表优化配置
        maintainScrollAtEnd        // 自动滚动到底部
        recycleItems               // 列表项复用优化
        estimatedItemSize={100}    // 预估高度
        // 事件处理
        onRefresh={refreshMessages}
        onEndReached={loadMoreMessages}
      />
    </View>
  )
}
```

### 3.2 MessageGroup 组件

```typescript
const MessageGroup: FC<MessageGroupProps> = ({ assistant, item, messageBlocks }) => {
  // 获取该组的消息
  const groupMessages = item.messages

  return (
    <View style={styles.groupContainer}>
      {groupMessages.map((message) => (
        <Message
          key={message.id}
          message={message}
          assistant={assistant}
          blocks={getBlocksForMessage(message.id, messageBlocks)}
        />
      ))}
    </View>
  )
}
```

### 3.3 Message 组件

```typescript
const Message: FC<MessageProps> = ({ message, assistant, blocks }) => {
  const isUser = message.role === 'user'

  return (
    <View style={[styles.messageContainer, isUser && styles.userMessage]}>
      {/* 头像 */}
      <Avatar source={isUser ? userAvatar : assistant.avatar} />

      {/* 消息内容 */}
      <View style={styles.contentContainer}>
        {/* 用户名 */}
        {!isUser && <Text style={styles.name}>{assistant.name}</Text>}

        {/* 消息块渲染 */}
        <View style={styles.blocksContainer}>
          {blocks.map((block) => (
            <BlockRenderer key={block.id} block={block} />
          ))}
        </View>

        {/* 状态指示器 */}
        {message.status && (
          <MessageStatusIndicator status={message.status} />
        )}
      </View>

      {/* 操作按钮 */}
      <MessageActions message={message} />
    </View>
  )
}
```

## 四、块渲染器架构

### 4.1 BlockRenderer 组件

```typescript
const BlockRenderer: FC<BlockRendererProps> = ({ block }) => {
  switch (block.type) {
    case MessageBlockType.MAIN_TEXT:
      return <TextBlock content={block.content} />

    case MessageBlockType.THINKING:
      return <ThinkingBlock
        content={block.content}
        thinkingMs={block.metadata?.thinking_millsec}
      />

    case MessageBlockType.CODE:
      return <CodeBlock
        content={block.content}
        language={block.metadata?.language}
      />

    case MessageBlockType.IMAGE:
      return <ImageBlock uri={block.content} />

    case MessageBlockType.TOOL:
      return <ToolBlock
        toolName={block.metadata?.toolName}
        parameters={block.metadata?.parameters}
        result={block.metadata?.result}
      />

    case MessageBlockType.CITATION:
      return <CitationBlock citations={block.metadata?.citations} />

    default:
      return <TextBlock content={block.content} />
  }
}
```

### 4.2 文本块 (TextBlock)

```typescript
const TextBlock: FC<TextBlockProps> = ({ content }) => {
  // 支持 Markdown 渲染
  return (
    <MarkdownView
      content={content}
      style={styles.markdown}
      onLinkPress={handleLinkPress}
    />
  )
}
```

### 4.3 思考块 (ThinkingBlock)

```typescript
const ThinkingBlock: FC<ThinkingBlockProps> = ({ content, thinkingMs }) => {
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <View style={styles.thinkingContainer}>
      <TouchableOpacity
        style={styles.thinkingHeader}
        onPress={() => setIsExpanded(!isExpanded)}
      >
        <Icon name="brain" size={16} />
        <Text style={styles.thinkingLabel}>
          {thinkingMs ? `${formatDuration(thinkingMs)}` : '思考中...'}
        </Text>
        <Icon
          name={isExpanded ? 'chevron-up' : 'chevron-down'}
          size={16}
        />
      </TouchableOpacity>

      {isExpanded && (
        <Text style={styles.thinkingContent}>{content}</Text>
      )}
    </View>
  )
}
```

## 五、组件交互图

### 5.1 消息发送时序图

```
用户输入消息
    │
    ▼
┌──────────────────┐
│ TextInput        │  ──onChangeText──→  useInputText (text state)
└──────────────────┘
                                              │
    ┌─────────────────────────────────────────┘
    ▼
┌──────────────────┐
│ SendButton       │  ──onPress──→ useMessageSend.sendMessage()
└──────────────────┘
                                              │
    ┌─────────────────────────────────────────┐
    │                                         │
    ▼                                         ▼
┌──────────────────┐                 ┌──────────────────┐
│ getUserMessage() │                 │ MessagesService  │
│ 准备消息数据结构  │ ──调用────────→ │ 保存消息到数据库  │
└──────────────────┘                 └──────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ fetchAndProcessAssistant │
                              │     ResponseImpl         │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ ModernAiProvider         │
                              │     completions()        │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ AiSdkToChunkAdapter      │
                              │     processStream()      │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ StreamProcessingService  │
                              │     createProcessor()    │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ BlockManager             │
                              │     smartBlockUpdate()   │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ useMessages (useLiveQuery│
                              │     数据库实时查询        │
                              └──────────────────────────┘
                                              │
                                              ▼
                              ┌──────────────────────────┐
                              │ Messages (LegendList)    │
                              │     重新渲染消息列表      │
                              └──────────────────────────┘
```

### 5.2 数据流方向

```
                    持久化层 (SQLite)
                          ▲
                          │ useLiveQuery 实时推送
                          │
┌──────────────┐         │         ┌──────────────┐
│   UI 组件    │ ───────┼────────→ │  Redux Store │
│  (React)     │         │         │              │
└──────┬───────┘         │         └──────────────┘
       │                 │
       │ 用户交互         │
       │                 │
       ▼                 │
┌──────────────┐         │
│ Service 层   │ ────────+
│              │  读写操作
└──────────────┘
       │
       ▼
┌──────────────┐
│ AI Core 层   │
│              │
└──────────────┘
       │
       ▼
┌──────────────┐
│ 外部 API     │
│ (LLM 服务)   │
└──────────────┘
```

## 六、关键组件路径

| 组件 | 路径 |
|------|------|
| ChatScreen | `src/screens/home/chat/ChatScreen.tsx` |
| Messages | `src/screens/home/messages/Messages.tsx` |
| MessageGroup | `src/screens/home/messages/MessageGroup.tsx` |
| Message | `src/screens/home/messages/Message.tsx` |
| BlockRenderer | `src/screens/home/messages/blocks/BlockRenderer.tsx` |
| MessageInputContainer | `src/componentsV2/features/ChatScreen/MessageInput/MessageInputContainer.tsx` |
| useMessageSend | `src/componentsV2/features/ChatScreen/MessageInput/hooks/useMessageSend.ts` |
| useMessages | `src/hooks/useMessages.ts` |

## 七、架构特点

1. **组件职责分离**: 每个组件只关注单一职责
2. **数据驱动渲染**: 使用 useLiveQuery 实现数据与 UI 自动同步
3. **虚拟列表优化**: LegendList 支持大量消息的高效渲染
4. **块状消息架构**: 灵活的块组合支持多种内容类型
5. **Hook 抽取逻辑**: 业务逻辑从组件中分离到可复用 Hook
6. **列表项复用**: recycleItems 机制减少内存占用
