# QUICK-REF-BUG-00048-V3快速参考

## 问题
终止AI生成后重新生成，被停止的内容被错误地当作用户消息显示。

## 根因
`regenerateLastMessage()`中的消息查询逻辑缺陷：
- 使用列表顺序而不是时间戳排序
- 用户消息没有时间戳约束

## 修复
修改`AiAdvisorChatViewModel.regenerateLastMessage()`：

```kotlin
// 1. 使用时间戳找最后一条AI消息
val lastAiMessage = conversations
    .filter { it.messageType == MessageType.AI }
    .maxByOrNull { it.timestamp }

// 2. 添加时间戳约束找用户消息
val userInputToUse = _uiState.value.lastUserInput.ifEmpty {
    conversations
        .filter { 
            it.messageType == MessageType.USER && 
            it.sendStatus == SendStatus.SUCCESS &&
            it.timestamp < lastAiMessage.timestamp  // 关键
        }
        .maxByOrNull { it.timestamp }
        ?.content ?: ""
}

// 3. 增加延迟时间
kotlinx.coroutines.delay(200)
```

## 文件
- **修改**：`presentation/.../AiAdvisorChatViewModel.kt`
- **测试**：`presentation/src/test/.../RegenerateLastMessageTest.kt`
- **文档**：`BUG-00048-V3`, `TE-00028-V7`

## 测试
- ✅ 单元测试：9个用例全部通过
- ⬜ 人工测试：10个场景待执行

## 验证
- [x] 编译通过
- [x] 单元测试通过
- [x] APK构建成功
- [x] 应用启动成功
- [ ] 人工测试验证

## 关键改进
1. **时间戳排序**：避免并发问题
2. **时间戳约束**：确保消息顺序正确
3. **优先级机制**：lastUserInput > 时间戳查找

---

**版本**: 1.0 | **日期**: 2026-01-05 | **状态**: ✅ 已完成
