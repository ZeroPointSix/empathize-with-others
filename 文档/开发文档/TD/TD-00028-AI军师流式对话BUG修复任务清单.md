# TD-00028: AI军师流式对话BUG修复任务清单

**创建日期**: 2026-01-04
**关联BUG**: BUG-00044, BUG-00048
**关联功能**: FD-00028 AI军师流式对话升级
**预计工时**: 10小时
**状态**: 进行中
**最后更新**: 2026-01-05

---

## 1. 任务概述

修复BUG-00044中列出的14个问题，按照TDD方法先编写测试，再修复代码。

---

## 2. 任务分组

### Phase 1: P0级问题修复（核心功能）

| 任务ID | 任务描述 | 关联BUG | 预计工时 | 状态 |
|--------|----------|---------|----------|------|
| T-001 | 修复流式响应完成后显示"..."问题 | P0-001 | 0.5h | ✅ 已完成 |
| T-002 | 修复重新生成按钮无反应问题 | P0-002 | 1h | ✅ 已完成 |
| T-003 | 修复已创建对话重新输入无反应问题 | P0-003 | 0.5h | ✅ 已完成 |
| T-004 | 修复滚动时自动跳回顶部问题 | P0-004 | 1h | ✅ 已完成 |
| T-005 | 修复输入时自动弹出"..."问题 | P0-005 | 0.5h | ✅ 已完成 |
| T-016 | 修复终止后重新生成消息角色错误 | BUG-00048 | 1h | ✅ 已完成 |

### Phase 2: P1级问题修复（用户体验）

| 任务ID | 任务描述 | 关联BUG | 预计工时 | 状态 |
|--------|----------|---------|----------|------|
| T-006 | 修复重试按钮无反应问题 | P1-001 | 0.5h | ✅ 已完成 |
| T-007 | 修复停止生成后显示"..."问题 | P1-002 | 1h | ✅ 已完成 |
| T-008 | 修复断网时卡住问题 | P1-003 | 1h | ⬜ 待开始 |
| T-009 | 改进API错误提示 | P1-004 | 0.5h | ✅ 已完成 |
| T-010 | 修复会话切换时请求迁移问题 | P1-005 | 0.5h | ✅ 已完成 |
| T-011 | 添加删除失败消息功能 | P1-006 | 0.5h | ✅ 已完成 |

### Phase 3: P2级问题修复（优化改进）

| 任务ID | 任务描述 | 关联BUG | 预计工时 | 状态 |
|--------|----------|---------|----------|------|
| T-012 | 完善流式光标动画 | P2-001 | 0.5h | ✅ 已存在 |
| T-013 | 考虑Markdown渲染支持 | P2-002 | 2h | ⬜ 待开始（可选）|

### Phase 4: 测试验证

| 任务ID | 任务描述 | 预计工时 | 状态 |
|--------|----------|----------|------|
| T-014 | 编写单元测试 | 1h | ✅ 已完成 |
| T-015 | 执行人工测试验证 | 1h | ⬜ 待开始 |

---

## 3. 已完成修改清单

### 3.1 AiAdvisorChatViewModel.kt

1. **sendMessageStreaming()** - 添加旧请求取消逻辑（T-003）
   ```kotlin
   // BUG-044-P0-003: 先取消旧的流式请求
   streamingJob?.cancel()
   streamingJob = null
   ```

2. **stopGeneration()** - 保存当前内容，避免空消息（T-007）
   ```kotlin
   // BUG-044-P1-002: 根据内容决定处理方式
   if (currentContent.isNotEmpty()) {
       aiAdvisorRepository.updateMessageContentAndStatus(...)
   } else {
       aiAdvisorRepository.deleteMessage(id)
   }
   ```

3. **retryMessage()** - 扩展支持CANCELLED状态（T-006）
   ```kotlin
   // BUG-044-P1-001: 支持FAILED和CANCELLED状态的重试
   if (conversation.sendStatus != SendStatus.FAILED &&
       conversation.sendStatus != SendStatus.CANCELLED) return
   ```

4. **switchSession()** - 切换前停止流式响应（T-010）
   ```kotlin
   // BUG-044-P1-005: 先停止当前流式响应
   stopGeneration()
   ```

### 3.2 AiAdvisorChatScreen.kt

1. **流式消息渲染条件** - 添加更严格的条件（T-001, T-005）
   ```kotlin
   // BUG-044-P0-001/P0-005修复：添加更严格的渲染条件
   if (uiState.isStreaming && uiState.currentStreamingMessageId != null)
   ```

2. **滚动逻辑** - 只在底部时自动滚动（T-004）
   ```kotlin
   // BUG-044-P0-004修复：只在用户在底部附近时才自动滚动
   val isNearBottom = lastVisibleIndex >= totalItems - 2
   if (isNearBottom) { ... }
   ```

### 3.3 AiAdvisorRepository.kt (接口)

新增方法：
- `updateMessageContentAndStatus()` - 更新消息内容和状态
- `deleteMessage()` - 删除消息

### 3.4 AiAdvisorRepositoryImpl.kt (实现)

实现新增的接口方法

### 3.5 AiAdvisorDao.kt

新增方法：
- `updateContentAndStatus()` - 更新消息内容和状态的SQL

### 3.6 SseStreamReader.kt

新增方法：
- `parseErrorMessage()` - 友好的错误信息解析（T-009）
  - 401: API密钥无效
  - 403: 访问被拒绝
  - 429: 请求过于频繁
  - 500/502/503/504: 服务器错误
  - 网络异常: 友好提示

---

## 4. 待完成任务

### T-008: 断网超时处理

需要在ViewModel中添加超时处理：
```kotlin
streamingJob = viewModelScope.launch {
    withTimeout(60_000) { // 60秒总超时
        sendAdvisorMessageStreamingUseCase(...)
            .collect { ... }
    }
}.also { job ->
    job.invokeOnCompletion { throwable ->
        if (throwable is TimeoutCancellationException) {
            _uiState.update {
                it.copy(
                    isStreaming = false,
                    error = "请求超时，请检查网络连接"
                )
            }
        }
    }
}
```

### T-013: Markdown渲染（可选）

考虑集成compose-markdown库或简单处理常见格式。

---

## 5. 验收标准

- [x] 所有P0级问题修复完成
- [x] 大部分P1级问题修复完成（10/11）
- [x] P2级光标动画已存在
- [x] 单元测试通过（RegenerateLastMessageTest）
- [ ] 人工测试验证通过（使用TE-00028-V7）

---

## 6. 修复进度

**完成率**: 12/14 核心任务 (85.7%)

| 优先级 | 完成数 | 总数 | 完成率 |
|--------|--------|------|--------|
| P0 | 6 | 6 | 100% |
| P1 | 5 | 6 | 83.3% |
| P2 | 1 | 2 | 50% |

---

## 7. BUG-00048 V3 修复记录（2026-01-05）

### 问题描述
终止AI生成后点击重新生成，被停止的内容"[用户已停止生成]"被错误地当作用户消息显示。

### 根因分析
1. `regenerateLastMessage()`中的消息查询逻辑没有使用时间戳排序
2. 查找用户消息时没有添加时间戳约束（必须在AI消息之前）
3. `lastUserInput`在某些场景下可能为空

### 修复方案
修改`AiAdvisorChatViewModel.regenerateLastMessage()`方法：
1. 使用`maxByOrNull { it.timestamp }`按时间戳找最后一条AI消息
2. 添加时间戳约束：`it.timestamp < lastAiMessage.timestamp`
3. 增加延迟时间到200ms，确保Flow更新完成

### 修改的文件
- `presentation/.../AiAdvisorChatViewModel.kt` - 改进消息查询逻辑

### 新增测试
- `presentation/src/test/.../RegenerateLastMessageTest.kt` - 9个测试用例

### 相关文档
- `文档/开发文档/BUG/BUG-00048-终止后重新生成消息角色错误问题分析.md` (V3)
- `文档/开发文档/TE/TE-00028-V7-BUG-00048-V3测试用例.md`

---

## 8. BUG-00048 V4 修复记录（2026-01-05）

### 问题描述
V3修复后问题仍然存在。深度分析发现根因是：
1. `lastUserInput`未持久化，应用重启/ViewModel重建后丢失
2. 回退查询逻辑存在时序问题（Flow更新延迟）

### 根因分析（深度）
```
问题：重新生成时消息角色错误
│
├── 框架机制层
│   ├── Flow更新延迟（Room Flow通知有延迟）
│   └── StateFlow状态管理（lastUserInput未持久化）
│
├── 模块交互层
│   ├── ViewModel与Repository的异步通信
│   └── 删除操作与查询操作的时序问题
│
└── 边界条件层
    ├── lastUserInput为空的场景（应用重启、ViewModel重建）
    └── conversations列表未及时更新
```

### 修复方案（方案C：双重保障机制）

1. **添加`relatedUserMessageId`字段**：在AI消息中存储关联的用户消息ID
2. **三重保障获取用户输入**：
   - 优先级1：内存中的`lastUserInput`
   - 优先级2：通过`relatedUserMessageId`查找关联用户消息
   - 优先级3：时间戳回退查找（兼容旧数据）

### 修改的文件

| 文件 | 修改内容 |
|------|----------|
| `domain/model/AiAdvisorConversation.kt` | 添加`relatedUserMessageId`字段 |
| `data/local/entity/AiAdvisorConversationEntity.kt` | 添加`related_user_message_id`列 |
| `data/di/DatabaseModule.kt` | 添加MIGRATION_14_15迁移脚本 |
| `data/local/AppDatabase.kt` | 版本号从14升级到15 |
| `domain/usecase/SendAdvisorMessageStreamingUseCase.kt` | 添加relatedUserMessageId参数 |
| `presentation/viewmodel/AiAdvisorChatViewModel.kt` | 添加getUserInputForRegenerate方法 |

### 新增测试
- `presentation/src/test/.../RegenerateLastMessageV4Test.kt` - 10个测试用例

### 相关文档
- `文档/开发文档/BUG/BUG-00048-V4-终止后重新生成消息角色错误深度分析.md`
- `文档/开发文档/TE/TE-00028-V9-BUG-00048-V4测试用例.md`

### 状态
🟢 代码修改已完成，待构建验证和人工测试

---

**文档版本**: v1.3
**创建日期**: 2026-01-04
**最后更新**: 2026-01-05

---

## 3. 详细任务说明

### T-001: 修复流式响应完成后显示"..."问题

**问题描述**: AI流式回复完成后，UI刷新只显示"..."

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

**修改内容**:
```kotlin
// 修改流式消息显示条件
// 原: if (uiState.isStreaming)
// 改: if (uiState.isStreaming || uiState.streamingContent.isNotEmpty())
```

**测试用例**:
- `AiAdvisorChatViewModelTest.kt` - 测试流式完成后状态

---

### T-002: 修复重新生成按钮无反应问题

**问题描述**: 点击"重新生成"按钮没有任何反应

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/component/StreamingMessageBubble.kt`
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
1. 确保 `RegenerateButton` 组件正确实现
2. 验证 `onRegenerate` 回调正确传递
3. 添加日志验证执行

**测试用例**:
- `AiAdvisorChatViewModelTest.kt` - 测试regenerateLastMessage方法

---

### T-003: 修复已创建对话重新输入无反应问题

**问题描述**: 在已有对话中发送新消息卡在"正在生成"

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
```kotlin
private fun sendMessageStreaming(message: String, sessionId: String) {
    // 先取消旧的流式请求
    streamingJob?.cancel()
    streamingJob = null
    // ...
}
```

**测试用例**:
- `AiAdvisorChatViewModelTest.kt` - 测试连续发送消息

---

### T-004: 修复滚动时自动跳回顶部问题

**问题描述**: 滚动查看历史时自动跳回顶部

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

**修改内容**:
```kotlin
// 只在用户在底部时自动滚动
LaunchedEffect(uiState.conversations.size, uiState.streamingContent) {
    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val totalItems = uiState.conversations.size + (if (uiState.isStreaming) 1 else 0)
    val isNearBottom = lastVisibleIndex >= totalItems - 2
    
    if (isNearBottom && (uiState.conversations.isNotEmpty() || uiState.isStreaming)) {
        // 滚动到底部
    }
}
```

---

### T-005: 修复输入时自动弹出"..."问题

**问题描述**: 输入时自动弹出"..."消息

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

**修改内容**:
```kotlin
// 添加更严格的渲染条件
if (uiState.isStreaming && uiState.currentStreamingMessageId != null) {
    item(key = "streaming_message") { ... }
}
```

---

### T-006: 修复重试按钮无反应问题

**问题描述**: 失败消息的重试按钮无反应

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
```kotlin
fun retryMessage(conversation: AiAdvisorConversation) {
    // 扩展条件，包括CANCELLED状态
    if (conversation.sendStatus != SendStatus.FAILED && 
        conversation.sendStatus != SendStatus.CANCELLED) return
    // ...
}
```

---

### T-007: 修复停止生成后显示"..."问题

**问题描述**: 停止生成后显示空的"..."消息

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
```kotlin
fun stopGeneration() {
    val currentContent = _uiState.value.streamingContent
    // 如果有内容，保存并标记为取消
    // 如果没有内容，删除消息
}
```

---

### T-008: 修复断网时卡住问题

**问题描述**: 断网时卡在"思考中..."界面

**修改文件**:
- `data/src/main/kotlin/com/empathy/ai/data/di/NetworkModule.kt`
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
1. 配置合理的超时时间
2. 添加超时处理逻辑

---

### T-009: 改进API错误提示

**问题描述**: API错误只显示HTTP错误码

**修改文件**:
- `data/src/main/kotlin/com/empathy/ai/data/remote/SseStreamReader.kt`

**修改内容**:
```kotlin
private fun parseErrorMessage(response: Response?, throwable: Throwable?): String {
    return when {
        response?.code == 401 -> "API密钥无效或已过期，请在设置中检查API密钥配置"
        response?.code == 429 -> "请求过于频繁，请稍后再试"
        // ...
    }
}
```

---

### T-010: 修复会话切换时请求迁移问题

**问题描述**: 切换会话时旧请求继续执行

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/viewmodel/AiAdvisorChatViewModel.kt`

**修改内容**:
```kotlin
fun switchSession(sessionId: String) {
    stopGeneration() // 先停止当前流式响应
    _uiState.update { it.copy(currentSessionId = sessionId) }
    loadConversations(sessionId)
}
```

---

### T-011: 添加删除失败消息功能

**问题描述**: 失败消息没有删除按钮

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/AiAdvisorChatScreen.kt`

**修改内容**:
确保删除按钮正确显示和回调正确传递

---

### T-012: 完善流式光标动画

**问题描述**: 流式光标动画未显示

**修改文件**:
- `presentation/src/main/kotlin/com/empathy/ai/presentation/ui/screen/advisor/component/StreamingMessageBubble.kt`

**修改内容**:
确保 `StreamingCursor` 组件正确实现闪烁动画

---

## 4. 执行顺序

```
Phase 1 (P0) → Phase 2 (P1) → Phase 3 (P2) → Phase 4 (测试)
```

每个任务按照TDD流程：
1. 编写失败的测试
2. 编写最少代码使测试通过
3. 重构优化

---

## 5. 验收标准

- [ ] 所有P0级问题修复完成
- [ ] 所有P1级问题修复完成
- [ ] P2级问题至少完成光标动画
- [ ] 单元测试通过
- [ ] 人工测试验证通过（使用TE-00028-V2）

---

## 6. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 修改影响其他功能 | 高 | 编写回归测试 |
| 状态管理复杂 | 中 | 添加详细日志 |
| 测试覆盖不足 | 中 | 补充关键测试用例 |

---

**文档版本**: v1.0
**创建日期**: 2026-01-04
