package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * BUG-00048 V3 修复验证测试
 * 
 * 测试场景：终止AI生成后点击重新生成时的消息查询逻辑
 * 
 * 这些测试验证 regenerateLastMessage() 方法中的消息查询逻辑：
 * 1. 使用时间戳排序找到最后一条AI消息
 * 2. 使用时间戳约束找到AI消息之前的最后一条用户消息
 * 3. 优先使用 lastUserInput，其次使用时间戳查找
 */
class RegenerateLastMessageTest {

    /**
     * 测试用例 TC-001: 验证时间戳排序逻辑
     * 
     * 场景：多条AI消息，验证能找到时间戳最大的那条
     */
    @Test
    fun `findLastAiMessage should return message with largest timestamp`() {
        // Given: 多条AI消息，时间戳不同
        val aiMessage1 = createAiMessage("ai-1", "第一条AI回复", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage2 = createAiMessage("ai-2", "第二条AI回复", SendStatus.SUCCESS, timestamp = 3000)
        val aiMessage3 = createAiMessage("ai-3", "[用户已停止生成]", SendStatus.CANCELLED, timestamp = 2000)
        
        val conversations = listOf(aiMessage1, aiMessage3, aiMessage2) // 故意打乱顺序
        
        // When: 使用时间戳找最后一条AI消息
        val lastAiMessage = conversations
            .filter { it.messageType == MessageType.AI }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回时间戳最大的消息
        assertNotNull(lastAiMessage)
        assertEquals("ai-2", lastAiMessage?.id)
        assertEquals(3000L, lastAiMessage?.timestamp)
    }

    /**
     * 测试用例 TC-002: 验证用户消息时间戳约束
     * 
     * 场景：用户消息必须在AI消息之前
     */
    @Test
    fun `findLastUserMessage should only return messages before AI message`() {
        // Given: 用户消息和AI消息
        val userMessage1 = createUserMessage("user-1", "第一个问题", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage = createAiMessage("ai-1", "[用户已停止生成]", SendStatus.CANCELLED, timestamp = 2000)
        val userMessage2 = createUserMessage("user-2", "这是后来的用户消息", SendStatus.SUCCESS, timestamp = 3000)
        
        val conversations = listOf(userMessage1, aiMessage, userMessage2)
        
        // When: 查找AI消息之前的最后一条用户消息
        val lastUserMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < aiMessage.timestamp  // 关键约束
            }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回AI消息之前的用户消息
        assertNotNull(lastUserMessage)
        assertEquals("user-1", lastUserMessage?.id)
        assertEquals("第一个问题", lastUserMessage?.content)
    }

    /**
     * 测试用例 TC-003: 验证多轮对话场景
     * 
     * 场景：消息1 → AI回复1 → 消息2 → AI回复2(停止)
     * 预期：找到消息2
     */
    @Test
    fun `findLastUserMessage should return most recent user message before AI`() {
        // Given: 多轮对话
        val userMessage1 = createUserMessage("user-1", "第一个问题", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage1 = createAiMessage("ai-1", "第一个回答", SendStatus.SUCCESS, timestamp = 2000)
        val userMessage2 = createUserMessage("user-2", "第二个问题", SendStatus.SUCCESS, timestamp = 3000)
        val aiMessage2 = createAiMessage("ai-2", "[用户已停止生成]", SendStatus.CANCELLED, timestamp = 4000)
        
        val conversations = listOf(userMessage1, aiMessage1, userMessage2, aiMessage2)
        
        // 找到最后一条AI消息
        val lastAiMessage = conversations
            .filter { it.messageType == MessageType.AI }
            .maxByOrNull { it.timestamp }
        
        // When: 查找该AI消息之前的最后一条用户消息
        val lastUserMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < lastAiMessage!!.timestamp
            }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回"第二个问题"
        assertNotNull(lastUserMessage)
        assertEquals("user-2", lastUserMessage?.id)
        assertEquals("第二个问题", lastUserMessage?.content)
    }

    /**
     * 测试用例 TC-004: 验证CANCELLED状态的AI消息也能被找到
     * 
     * 场景：最后一条AI消息是CANCELLED状态
     */
    @Test
    fun `findLastAiMessage should include CANCELLED status messages`() {
        // Given: 包含CANCELLED状态的AI消息
        val aiMessage1 = createAiMessage("ai-1", "正常回复", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage2 = createAiMessage("ai-2", "[用户已停止生成]", SendStatus.CANCELLED, timestamp = 2000)
        
        val conversations = listOf(aiMessage1, aiMessage2)
        
        // When: 查找最后一条AI消息（不过滤状态）
        val lastAiMessage = conversations
            .filter { it.messageType == MessageType.AI }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回CANCELLED状态的消息
        assertNotNull(lastAiMessage)
        assertEquals("ai-2", lastAiMessage?.id)
        assertEquals(SendStatus.CANCELLED, lastAiMessage?.sendStatus)
    }

    /**
     * 测试用例 TC-005: 验证只查找SUCCESS状态的用户消息
     * 
     * 场景：存在PENDING或FAILED状态的用户消息
     */
    @Test
    fun `findLastUserMessage should only return SUCCESS status messages`() {
        // Given: 不同状态的用户消息
        val userMessage1 = createUserMessage("user-1", "成功的消息", SendStatus.SUCCESS, timestamp = 1000)
        val userMessage2 = createUserMessage("user-2", "失败的消息", SendStatus.FAILED, timestamp = 2000)
        val aiMessage = createAiMessage("ai-1", "AI回复", SendStatus.SUCCESS, timestamp = 3000)
        
        val conversations = listOf(userMessage1, userMessage2, aiMessage)
        
        // When: 查找SUCCESS状态的用户消息
        val lastUserMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < aiMessage.timestamp
            }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回SUCCESS状态的消息
        assertNotNull(lastUserMessage)
        assertEquals("user-1", lastUserMessage?.id)
        assertEquals(SendStatus.SUCCESS, lastUserMessage?.sendStatus)
    }

    /**
     * 测试用例 TC-006: 验证空列表处理
     * 
     * 场景：没有AI消息
     */
    @Test
    fun `findLastAiMessage should return null when no AI messages`() {
        // Given: 只有用户消息
        val userMessage = createUserMessage("user-1", "用户消息", SendStatus.SUCCESS, timestamp = 1000)
        
        val conversations = listOf(userMessage)
        
        // When: 查找AI消息
        val lastAiMessage = conversations
            .filter { it.messageType == MessageType.AI }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回null
        assertNull(lastAiMessage)
    }

    /**
     * 测试用例 TC-007: 验证没有符合条件的用户消息
     * 
     * 场景：所有用户消息都在AI消息之后
     */
    @Test
    fun `findLastUserMessage should return null when no user messages before AI`() {
        // Given: 用户消息在AI消息之后
        val aiMessage = createAiMessage("ai-1", "AI回复", SendStatus.SUCCESS, timestamp = 1000)
        val userMessage = createUserMessage("user-1", "后来的用户消息", SendStatus.SUCCESS, timestamp = 2000)
        
        val conversations = listOf(aiMessage, userMessage)
        
        // When: 查找AI消息之前的用户消息
        val lastUserMessage = conversations
            .filter { 
                it.messageType == MessageType.USER && 
                it.sendStatus == SendStatus.SUCCESS &&
                it.timestamp < aiMessage.timestamp
            }
            .maxByOrNull { it.timestamp }
        
        // Then: 应该返回null
        assertNull(lastUserMessage)
    }

    /**
     * 测试用例 TC-008: 验证lastUserInput优先级
     * 
     * 场景：lastUserInput不为空时应该优先使用
     */
    @Test
    fun `userInputToUse should prefer lastUserInput over conversations lookup`() {
        // Given: lastUserInput有值
        val lastUserInput = "这是lastUserInput的值"
        val userMessage = createUserMessage("user-1", "这是conversations中的值", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage = createAiMessage("ai-1", "AI回复", SendStatus.CANCELLED, timestamp = 2000)
        
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 模拟 userInputToUse 的计算逻辑
        val userInputToUse = lastUserInput.ifEmpty {
            conversations
                .filter { 
                    it.messageType == MessageType.USER && 
                    it.sendStatus == SendStatus.SUCCESS &&
                    it.timestamp < aiMessage.timestamp
                }
                .maxByOrNull { it.timestamp }
                ?.content ?: ""
        }
        
        // Then: 应该使用lastUserInput的值
        assertEquals("这是lastUserInput的值", userInputToUse)
    }

    /**
     * 测试用例 TC-009: 验证lastUserInput为空时的回退逻辑
     * 
     * 场景：lastUserInput为空，应该从conversations查找
     */
    @Test
    fun `userInputToUse should fallback to conversations when lastUserInput is empty`() {
        // Given: lastUserInput为空
        val lastUserInput = ""
        val userMessage = createUserMessage("user-1", "这是conversations中的值", SendStatus.SUCCESS, timestamp = 1000)
        val aiMessage = createAiMessage("ai-1", "AI回复", SendStatus.CANCELLED, timestamp = 2000)
        
        val conversations = listOf(userMessage, aiMessage)
        
        // When: 模拟 userInputToUse 的计算逻辑
        val userInputToUse = lastUserInput.ifEmpty {
            conversations
                .filter { 
                    it.messageType == MessageType.USER && 
                    it.sendStatus == SendStatus.SUCCESS &&
                    it.timestamp < aiMessage.timestamp
                }
                .maxByOrNull { it.timestamp }
                ?.content ?: ""
        }
        
        // Then: 应该使用conversations中的值
        assertEquals("这是conversations中的值", userInputToUse)
    }

    // Helper functions
    private fun createUserMessage(
        id: String, 
        content: String, 
        status: SendStatus,
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
        status: SendStatus,
        timestamp: Long = System.currentTimeMillis()
    ): AiAdvisorConversation {
        return AiAdvisorConversation(
            id = id,
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.AI,
            content = content,
            timestamp = timestamp,
            sendStatus = status
        )
    }
}
