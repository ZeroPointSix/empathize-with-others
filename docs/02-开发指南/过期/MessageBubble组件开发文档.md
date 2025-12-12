# MessageBubble 组件开发文档

## 创建时间
2025-12-05

## 组件概述

MessageBubble 是一个聊天消息气泡组件，用于在聊天界面中展示发送和接收的消息。

## 文件位置
```
app/src/main/java/com/empathy/ai/presentation/ui/component/message/MessageBubble.kt
```

## 设计规范

### 1. 状态提升
- ✅ 组件完全无状态
- ✅ 所有数据通过参数传入
- ✅ 无内部 `remember` 或 `mutableStateOf`

### 2. 参数设计
```kotlin
@Composable
fun MessageBubble(
    text: String,              // 消息文本
    timestamp: String,         // 时间戳
    isFromUser: Boolean,       // 是否为用户发送
    modifier: Modifier = Modifier
)
```

### 3. 视觉设计

#### 布局特性
- **对齐方式**：
  - 用户消息：右对齐
  - 接收消息：左对齐
- **最大宽度**：280dp（避免消息过宽）
- **圆角设计**：
  - 顶部圆角：16dp
  - 底部圆角：发送消息右下角4dp，接收消息左下角4dp（模拟对话气泡效果）

#### 颜色方案
- **用户消息**：
  - 背景：`MaterialTheme.colorScheme.primaryContainer`
  - 文字：`MaterialTheme.colorScheme.onPrimaryContainer`
- **接收消息**：
  - 背景：`MaterialTheme.colorScheme.surfaceVariant`
  - 文字：`MaterialTheme.colorScheme.onSurfaceVariant`
- **时间戳**：
  - 颜色：`onSurfaceVariant` with 60% alpha

#### 间距
- 气泡内边距：水平12dp，垂直8dp
- 气泡与时间戳间距：4dp
- 时间戳水平边距：4dp

### 4. Material 3 主题集成
- ✅ 所有颜色从 `MaterialTheme.colorScheme` 获取
- ✅ 文字样式使用 `MaterialTheme.typography`
- ✅ 自动支持深色模式
- ✅ 支持动态颜色（Android 12+）

### 5. Modifier 链式调用
```kotlin
Box(
    modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(RoundedCornerShape(...))
        .background(color = ...)
        .then(Modifier.padding(...))  // ✅ 使用 then() 链式调用
)
```

## Preview 函数

组件包含 6 个预览函数，覆盖所有使用场景：

1. **MessageBubbleUserPreview** - 用户发送的单条消息
2. **MessageBubbleReceivedPreview** - 接收的单条消息
3. **MessageBubbleLongTextPreview** - 长文本消息（测试自动换行）
4. **MessageBubbleConversationPreview** - 完整对话场景
5. **MessageBubbleUserDarkPreview** - 深色模式下的用户消息
6. **MessageBubbleConversationDarkPreview** - 深色模式下的对话场景

## 参考组件

设计风格参考了 `AnalysisCard.kt`：
- 圆角卡片设计（16dp 圆角）
- Material 3 颜色系统
- 一致的间距规范
- 完整的 Preview 覆盖

## 编译检查

✅ **无诊断错误** - 代码通过 getDiagnostics 检查

## 使用示例

```kotlin
// 用户发送的消息
MessageBubble(
    text = "你好，最近怎么样？",
    timestamp = "14:30",
    isFromUser = true
)

// 接收的消息
MessageBubble(
    text = "挺好的，谢谢关心！",
    timestamp = "14:32",
    isFromUser = false
)

// 在列表中使用
LazyColumn {
    items(messages) { message ->
        MessageBubble(
            text = message.text,
            timestamp = message.timestamp,
            isFromUser = message.isFromUser,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
```

## 技术亮点

1. **响应式布局**：使用 `widthIn` 限制最大宽度，自动适应不同文本长度
2. **智能圆角**：根据消息方向动态调整圆角，增强对话感
3. **完全无状态**：便于测试和复用
4. **主题感知**：自动适配浅色/深色模式和动态颜色
5. **可访问性**：使用语义化的颜色对比度

## 后续优化建议

1. **消息状态**：可添加已读/未读、发送中、发送失败等状态指示
2. **长按菜单**：支持复制、删除、转发等操作
3. **富文本**：支持链接、表情、图片等多媒体内容
4. **动画效果**：添加消息进入动画
5. **可访问性**：添加 `contentDescription` 和语义化标签

## 开发总结

MessageBubble 组件严格遵循项目规范，实现了：
- ✅ 100% 状态提升
- ✅ Material 3 设计规范
- ✅ 深色模式支持
- ✅ 完整的 Preview 覆盖
- ✅ 零编译错误
- ✅ 与现有组件风格一致

组件已就绪，可直接用于 Phase 3 的聊天界面开发。
