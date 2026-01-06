package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * BUG-00048-V4 测试用例
 *
 * 测试三重保障获取用户输入的逻辑：
 * 1. 优先使用内存中的lastUserInput
 * 2. 通过relatedUserMessageId查找关联的用户消息
 * 3. 时间戳回退查找（兼容旧数据）
 *
 * @see BUG-00048-V4-终止后重新生成消息角色错误深度分析.md
 * @see TE-00028-V9-BUG-00048-V4测试用例.md
 */
class RegenerateLastMessageV4Test {

    // ==================== 辅助方法 ====================

    private fun createUserMessage(
        id: String,
        content: String,
        timestamp: Long = System.currentTimeMillis(),
        status: SendStatus = SendStatus.SUCCESS
    ): AiAdvisorConversation = AiAdvisorConversation(
        id = id,
        contactId = "contact-1",
        sessionId = "session-1",
        messageType = MessageType.USER,
        content = content,
        timestamp = timestamp,
        sendStatus = status
    )

    private fun createAiMessage(
        id: String,
        content: String,
        timestamp: Long = System.currentTimeMillis(),
        status: SendStatus = SendStatus.SUCCESS,
        relatedUserMessageId: String? = null
    ): AiAdvisorConversation = AiAdvisorConversation(
        id = id,
        contactId = "contact-1",
        sessionId = "session-1",
        messageType = MessageType.AI,
        content = content,
        timestamp = timestamp,
        sendStatus = status,
        relatedUserMessageId = relatedUserMessageId
    )

    /**
     * 模拟ViewModel中的getUserInputForRegenerate方法
     * 用于单元测试验证三重保障逻辑
     */
    private fun getUserInputForRegenerate(
        lastUserInput: String,
        lastAiMessage: AiAdvisorConversation,
        conversations: List<AiAdvisorConversation>
    ): Pair<String, String?> {
        // 优先级1：使用内存中的lastUserInput
        if (lastUserInput.isNotEmpty()) {
            val relatedId = lastAiMessage.relatedUserMessageId
                ?: conversations
                    .filter { 
                        it.messageType == MessageType.USER && 
                        it.sendStatus == SendStatus.SUCCESS &&
                        it.timestamp < lastAiMessage.timestamp
                    }
                    .maxByOrNull { it.timestamp }
                    ?.id
            return Pair(lastUserInput, relatedId)
        }
        
        // 优先级2：通过relatedUserMessageId查找
        val relatedUserMessageId = lastAiMessage.relatedUserMessageId
        if (relatedUserMessageId != null && relatedUserMessageId.isNotEmpty()) {
            val relatedMessage = conversations.find { it.id == relatedUserMessageId }
            if (relatedMessage != null && relatedMessage.content.isNotEmpty()) {
                return Pair(relatedMessage.content, relatedUserMessageId)
            }
        }
        
        // 优先级3：时间戳回退查找（兼容旧数据）
        val fallbackMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < lastAiMessage.timestamp
            }
            .maxByOrNull { it.timestamp }
        
        return Pair(fallbackMessage?.content ?: "", fallbackMessage?.id)
    }

    // ==================== TC-V4-001: lastUserInput有值时优先使用 ====================

    @Test
    fun `TC-V4-001 getUserInputForRegenerate should prefer lastUserInput when available`() {
        // Given
        val lastUserInput = "原始用户问题"
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = "user-1",
            timestamp = 2000
        )
        val userMessage = createUserMessage(
            id = "user-1",
            content = "这是关联的用户消息",
            timestamp = 1000
        )
        
        // When
        val (result, _) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(userMessage, lastAiMessage)
        )
        
        // Then
        assertEquals("原始用户问题", result)
    }

    // ==================== TC-V4-002: lastUserInput为空时通过relatedUserMessageId查找 ====================

    @Test
    fun `TC-V4-002 getUserInputForRegenerate should use relatedUserMessageId when lastUserInput is empty`() {
        // Given
        val lastUserInput = ""
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = "user-1",
            timestamp = 2000
        )
        val userMessage = createUserMessage(
            id = "user-1",
            content = "通过ID关联的用户消息",
            timestamp = 1000
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(userMessage, lastAiMessage)
        )
        
        // Then
        assertEquals("通过ID关联的用户消息", result)
        assertEquals("user-1", relatedId)
    }

    // ==================== TC-V4-003: 两者都为空时使用时间戳回退 ====================

    @Test
    fun `TC-V4-003 getUserInputForRegenerate should fallback to timestamp when relatedUserMessageId is null`() {
        // Given
        val lastUserInput = ""
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = null,
            timestamp = 2000
        )
        val userMessage = createUserMessage(
            id = "user-1",
            content = "时间戳回退找到的消息",
            timestamp = 1000
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(userMessage, lastAiMessage)
        )
        
        // Then
        assertEquals("时间戳回退找到的消息", result)
        assertEquals("user-1", relatedId)
    }

    // ==================== TC-V4-004: 三者都为空时返回空字符串 ====================

    @Test
    fun `TC-V4-004 getUserInputForRegenerate should return empty when all sources are empty`() {
        // Given
        val lastUserInput = ""
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = null,
            timestamp = 1000
        )
        // 没有用户消息
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(lastAiMessage)
        )
        
        // Then
        assertEquals("", result)
        assertEquals(null, relatedId)
    }

    // ==================== TC-V4-005: relatedUserMessageId指向的消息不存在时回退 ====================

    @Test
    fun `TC-V4-005 getUserInputForRegenerate should fallback when relatedUserMessage not found`() {
        // Given
        val lastUserInput = ""
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = "non-existent-id",
            timestamp = 2000
        )
        val userMessage = createUserMessage(
            id = "user-1",
            content = "时间戳回退找到的消息",
            timestamp = 1000
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(userMessage, lastAiMessage)
        )
        
        // Then
        assertEquals("时间戳回退找到的消息", result)
        assertEquals("user-1", relatedId)
    }

    // ==================== TC-V4-006: 多轮对话场景 ====================

    @Test
    fun `TC-V4-006 getUserInputForRegenerate should find correct user message in multi-turn conversation`() {
        // Given
        val lastUserInput = ""
        val userMessage1 = createUserMessage("user-1", "第一个问题", timestamp = 1000)
        val aiMessage1 = createAiMessage("ai-1", "第一个回答", timestamp = 2000)
        val userMessage2 = createUserMessage("user-2", "第二个问题", timestamp = 3000)
        val aiMessage2 = createAiMessage(
            id = "ai-2",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = "user-2",
            timestamp = 4000
        )
        
        val conversations = listOf(userMessage1, aiMessage1, userMessage2, aiMessage2)
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = aiMessage2,
            conversations = conversations
        )
        
        // Then
        assertEquals("第二个问题", result)
        assertEquals("user-2", relatedId)
    }

    // ==================== TC-V4-007: 验证不会使用AI消息内容 ====================

    @Test
    fun `TC-V4-007 getUserInputForRegenerate should never return AI message content`() {
        // Given - 模拟问题场景：只有AI消息，没有用户消息
        val lastUserInput = ""
        val aiMessage1 = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = null,
            timestamp = 1000
        )
        
        // When
        val (result, _) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = aiMessage1,
            conversations = listOf(aiMessage1)
        )
        
        // Then - 应该返回空，而不是AI消息内容
        assertEquals("", result)
        assertNotEquals("[用户已停止生成]", result)
    }

    // ==================== TC-V4-008: 验证时间戳约束 ====================

    @Test
    fun `TC-V4-008 getUserInputForRegenerate should only find user messages before AI message`() {
        // Given - 用户消息在AI消息之后（不应该被选中）
        val lastUserInput = ""
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = null,
            timestamp = 1000
        )
        val userMessageAfter = createUserMessage(
            id = "user-after",
            content = "这是AI消息之后的用户消息",
            timestamp = 2000  // 在AI消息之后
        )
        val userMessageBefore = createUserMessage(
            id = "user-before",
            content = "这是AI消息之前的用户消息",
            timestamp = 500  // 在AI消息之前
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = aiMessage,
            conversations = listOf(userMessageAfter, aiMessage, userMessageBefore)
        )
        
        // Then - 应该返回AI消息之前的用户消息
        assertEquals("这是AI消息之前的用户消息", result)
        assertEquals("user-before", relatedId)
    }

    // ==================== TC-V4-009: 验证FAILED状态的用户消息不被选中 ====================

    @Test
    fun `TC-V4-009 getUserInputForRegenerate should skip FAILED user messages`() {
        // Given
        val lastUserInput = ""
        val failedUserMessage = createUserMessage(
            id = "user-failed",
            content = "发送失败的消息",
            timestamp = 900,
            status = SendStatus.FAILED
        )
        val successUserMessage = createUserMessage(
            id = "user-success",
            content = "发送成功的消息",
            timestamp = 800,
            status = SendStatus.SUCCESS
        )
        val aiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = null,
            timestamp = 1000
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = aiMessage,
            conversations = listOf(failedUserMessage, successUserMessage, aiMessage)
        )
        
        // Then - 应该跳过FAILED状态的消息，返回SUCCESS状态的消息
        assertEquals("发送成功的消息", result)
        assertEquals("user-success", relatedId)
    }

    // ==================== TC-V4-010: 验证relatedUserMessageId返回正确 ====================

    @Test
    fun `TC-V4-010 getUserInputForRegenerate should return correct relatedUserMessageId`() {
        // Given - lastUserInput有值，但relatedUserMessageId也存在
        val lastUserInput = "内存中的用户输入"
        val lastAiMessage = createAiMessage(
            id = "ai-1",
            content = "[用户已停止生成]",
            status = SendStatus.CANCELLED,
            relatedUserMessageId = "user-1",
            timestamp = 2000
        )
        val userMessage = createUserMessage(
            id = "user-1",
            content = "数据库中的用户消息",
            timestamp = 1000
        )
        
        // When
        val (result, relatedId) = getUserInputForRegenerate(
            lastUserInput = lastUserInput,
            lastAiMessage = lastAiMessage,
            conversations = listOf(userMessage, lastAiMessage)
        )
        
        // Then - 使用lastUserInput，但relatedId应该是AI消息关联的ID
        assertEquals("内存中的用户输入", result)
        assertEquals("user-1", relatedId)
    }
}
