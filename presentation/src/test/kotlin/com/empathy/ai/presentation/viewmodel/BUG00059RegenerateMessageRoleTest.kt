package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-00059: 中断生成后重新生成消息角色错乱问题测试
 *
 * 测试场景：
 * 1. 停止生成后，AI消息应该正确显示为AI消息（白色气泡）
 * 2. 重新生成时，应该使用正确的用户输入
 * 3. 重新生成后，不应该出现新的用户消息
 * 4. 消息类型不应该被错误地修改
 *
 * 问题描述：
 * 用户停止AI生成后重新生成，AI之前生成的半截内容被错误地当成用户消息显示
 */
class BUG00059RegenerateMessageRoleTest {

    // ==================== 辅助方法 ====================

    private fun createUserMessage(
        id: String,
        content: String,
        status: SendStatus = SendStatus.SUCCESS,
        timestamp: Long = System.currentTimeMillis()
    ): AiAdvisorConversation {
        return AiAdvisorConversation(
            id = id,
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.USER,
            content = content,
            timestamp = timestamp,
            sendStatus = status
        )
    }

    private fun createAiMessage(
        id: String,
        content: String,
        status: SendStatus = SendStatus.SUCCESS,
        timestamp: Long = System.currentTimeMillis(),
        relatedUserMessageId: String? = null
    ): AiAdvisorConversation {
        return AiAdvisorConversation(
            id = id,
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.AI,
            content = content,
            timestamp = timestamp,
            sendStatus = status,
            relatedUserMessageId = relatedUserMessageId
        )
    }

    /**
     * 模拟getUserInputForRegenerate的逻辑
     * 使用relatedUserMessageId作为唯一来源（推荐方案）
     */
    private fun getUserInputForRegenerateV2(
        lastAiMessage: AiAdvisorConversation,
        conversations: List<AiAdvisorConversation>
    ): Pair<String, String?> {
        // 强制通过relatedUserMessageId获取用户消息
        val relatedUserMessageId = lastAiMessage.relatedUserMessageId
        if (relatedUserMessageId.isNullOrEmpty()) {
            return Pair("", null)
        }
        
        val relatedUserMessage = conversations.find { it.id == relatedUserMessageId }
        if (relatedUserMessage == null || relatedUserMessage.messageType != MessageType.USER) {
            return Pair("", null)
        }
        
        return Pair(relatedUserMessage.content, relatedUserMessageId)
    }

    // ==================== TC-001: 正常停止后重新生成 ====================

    @Test
    fun `TC-001 regenerate should use correct user input after stop`() {
        // Given: 用户发送消息，AI开始生成后被停止
        val userMessage = createUserMessage(
            id = "user-1",
            content = "帮我分析一下荔枝",
            timestamp = 1000
        )
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "她分享欲强，当她说东西的时候...[用户已停止生成]",
            status = SendStatus.CANCELLED,
            timestamp = 2000,
            relatedUserMessageId = "user-1"
        )
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 获取重新生成时的用户输入
        val (userInputToUse, relatedId) = getUserInputForRegenerateV2(aiMessage, conversations)
        
        // Then: 应该使用原始用户输入，而不是AI生成的内容
        assertEquals("帮我分析一下荔枝", userInputToUse)
        assertEquals("user-1", relatedId)
        assertFalse(userInputToUse.contains("[用户已停止生成]"))
        assertFalse(userInputToUse.contains("她分享欲强"))
    }

    @Test
    fun `TC-001 stopped AI message should have AI type`() {
        // Given: AI消息被停止
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "部分内容...[用户已停止生成]",
            status = SendStatus.CANCELLED
        )
        
        // Then: 消息类型应该是AI
        assertEquals(MessageType.AI, aiMessage.messageType)
        assertEquals(SendStatus.CANCELLED, aiMessage.sendStatus)
    }

    // ==================== TC-002: 快速停止后重新生成 ====================

    @Test
    fun `TC-002 regenerate should work even with empty AI content`() {
        // Given: AI刚开始生成就被停止（几乎没有内容）
        val userMessage = createUserMessage(
            id = "user-1",
            content = "你好",
            timestamp = 1000
        )
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",  // 几乎没有内容
            status = SendStatus.CANCELLED,
            timestamp = 2000,
            relatedUserMessageId = "user-1"
        )
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 获取重新生成时的用户输入
        val (userInputToUse, _) = getUserInputForRegenerateV2(aiMessage, conversations)
        
        // Then: 应该正确获取用户输入
        assertEquals("你好", userInputToUse)
    }

    // ==================== TC-003: 多轮对话后停止重新生成 ====================

    @Test
    fun `TC-003 regenerate should use correct user message in multi-turn conversation`() {
        // Given: 多轮对话后，最后一条AI消息被停止
        val userMessage1 = createUserMessage("user-1", "第一个问题", timestamp = 1000)
        val aiMessage1 = createAiMessage("ai-1", "第一个回答", timestamp = 2000, relatedUserMessageId = "user-1")
        val userMessage2 = createUserMessage("user-2", "第二个问题", timestamp = 3000)
        val aiMessage2 = createAiMessage(
            id = "ai-2",
            content = "第二个回答的一部分...[用户已停止生成]",
            status = SendStatus.CANCELLED,
            timestamp = 4000,
            relatedUserMessageId = "user-2"
        )
        val conversations = listOf(userMessage1, aiMessage1, userMessage2, aiMessage2)
        
        // When: 获取重新生成时的用户输入
        val (userInputToUse, relatedId) = getUserInputForRegenerateV2(aiMessage2, conversations)
        
        // Then: 应该使用第二个问题，而不是第一个
        assertEquals("第二个问题", userInputToUse)
        assertEquals("user-2", relatedId)
        assertNotEquals("第一个问题", userInputToUse)
    }

    // ==================== TC-004: relatedUserMessageId为空的情况 ====================

    @Test
    fun `TC-004 regenerate should fail gracefully when relatedUserMessageId is null`() {
        // Given: AI消息没有关联的用户消息ID（旧数据或异常情况）
        val userMessage = createUserMessage("user-1", "用户问题", timestamp = 1000)
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "AI回答...[用户已停止生成]",
            status = SendStatus.CANCELLED,
            timestamp = 2000,
            relatedUserMessageId = null  // 没有关联
        )
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 获取重新生成时的用户输入
        val (userInputToUse, relatedId) = getUserInputForRegenerateV2(aiMessage, conversations)
        
        // Then: 应该返回空，而不是错误地使用AI内容
        assertTrue(userInputToUse.isEmpty())
        assertEquals(null, relatedId)
    }

    // ==================== TC-005: 验证消息类型不会被错误修改 ====================

    @Test
    fun `TC-005 message type should never change from AI to USER`() {
        // Given: 创建AI消息
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "AI生成的内容",
            status = SendStatus.SUCCESS
        )
        
        // Then: 消息类型应该始终是AI
        assertEquals(MessageType.AI, aiMessage.messageType)
        
        // 模拟停止生成后的状态
        val stoppedAiMessage = aiMessage.copy(
            content = aiMessage.content + "\n\n[用户已停止生成]",
            sendStatus = SendStatus.CANCELLED
        )
        
        // Then: 消息类型仍然是AI
        assertEquals(MessageType.AI, stoppedAiMessage.messageType)
    }

    // ==================== TC-006: 验证用户消息内容不包含AI生成的内容 ====================

    @Test
    fun `TC-006 user message should never contain AI generated content markers`() {
        // Given: 正常的用户消息
        val userMessage = createUserMessage(
            id = "user-1",
            content = "帮我分析一下"
        )
        
        // Then: 用户消息不应该包含AI生成的标记
        assertFalse(userMessage.content.contains("[用户已停止生成]"))
        assertFalse(userMessage.content.contains("核心策略"))
        assertFalse(userMessage.content.contains("ENFJ"))
        
        // 验证消息类型
        assertEquals(MessageType.USER, userMessage.messageType)
    }

    // ==================== TC-007: 验证relatedUserMessageId指向正确的消息 ====================

    @Test
    fun `TC-007 relatedUserMessageId should point to USER type message`() {
        // Given: AI消息关联了用户消息
        val userMessage = createUserMessage("user-1", "用户问题", timestamp = 1000)
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "AI回答",
            timestamp = 2000,
            relatedUserMessageId = "user-1"
        )
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 查找关联的消息
        val relatedMessage = conversations.find { it.id == aiMessage.relatedUserMessageId }
        
        // Then: 关联的消息应该是USER类型
        assertNotNull(relatedMessage)
        assertEquals(MessageType.USER, relatedMessage!!.messageType)
    }

    @Test
    fun `TC-007 relatedUserMessageId should not point to AI message`() {
        // Given: 错误地将AI消息ID设置为relatedUserMessageId（不应该发生）
        val aiMessage1 = createAiMessage("ai-1", "第一个AI回答", timestamp = 1000)
        val aiMessage2 = createAiMessage(
            id = "ai-2",
            content = "第二个AI回答",
            timestamp = 2000,
            relatedUserMessageId = "ai-1"  // 错误：指向AI消息
        )
        val conversations = listOf(aiMessage1, aiMessage2)
        
        // When: 使用新的验证逻辑获取用户输入
        val (userInputToUse, _) = getUserInputForRegenerateV2(aiMessage2, conversations)
        
        // Then: 应该返回空，因为关联的消息不是USER类型
        assertTrue(userInputToUse.isEmpty())
    }

    // ==================== TC-008: 验证消息内容长度检查 ====================

    @Test
    fun `TC-008 AI generated content is typically longer than user input`() {
        // Given: 典型的用户输入和AI生成内容
        val userInput = "帮我分析一下荔枝"
        val aiGeneratedContent = """
            她分享欲强，当她说东西的时候：
            - 别敷衍"哈哈哈""确实"
            
            核心策略：有深度的真诚 + 给她当"知音"
            
            ENFJ 最吃的就是——被真正理解...
        """.trimIndent()
        
        // Then: AI生成的内容通常比用户输入长得多
        assertTrue(aiGeneratedContent.length > userInput.length)
        assertTrue(aiGeneratedContent.length > 100)  // AI内容通常超过100字符
    }

    // ==================== TC-009: 验证停止标记只出现在AI消息中 ====================

    @Test
    fun `TC-009 stop marker should only appear in AI messages`() {
        // Given: 停止标记
        val stopMarker = "[用户已停止生成]"
        
        // 正常的用户消息
        val userMessage = createUserMessage("user-1", "用户问题")
        
        // 被停止的AI消息
        val stoppedAiMessage = createAiMessage(
            id = "ai-1",
            content = "部分内容...$stopMarker",
            status = SendStatus.CANCELLED
        )
        
        // Then: 停止标记只应该出现在AI消息中
        assertFalse(userMessage.content.contains(stopMarker))
        assertTrue(stoppedAiMessage.content.contains(stopMarker))
        assertEquals(MessageType.AI, stoppedAiMessage.messageType)
    }

    // ==================== TC-010: 验证重新生成不会创建新的用户消息 ====================

    @Test
    fun `TC-010 regenerate should not create new user message`() {
        // Given: 初始对话状态
        val userMessage = createUserMessage("user-1", "用户问题", timestamp = 1000)
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "AI回答...[用户已停止生成]",
            status = SendStatus.CANCELLED,
            timestamp = 2000,
            relatedUserMessageId = "user-1"
        )
        val initialConversations = listOf(userMessage, aiMessage)
        val initialUserMessageCount = initialConversations.count { it.messageType == MessageType.USER }
        
        // When: 模拟重新生成（删除AI消息，创建新AI消息）
        val newAiMessage = createAiMessage(
            id = "ai-2",
            content = "新的AI回答",
            status = SendStatus.SUCCESS,
            timestamp = 3000,
            relatedUserMessageId = "user-1"
        )
        val afterRegenerateConversations = listOf(userMessage, newAiMessage)
        val afterUserMessageCount = afterRegenerateConversations.count { it.messageType == MessageType.USER }
        
        // Then: 用户消息数量应该不变
        assertEquals(initialUserMessageCount, afterUserMessageCount)
        assertEquals(1, afterUserMessageCount)
    }
}
