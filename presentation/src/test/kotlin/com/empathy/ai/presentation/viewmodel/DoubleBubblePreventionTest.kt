package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * BUG-00048 V4 双气泡预防测试
 * 
 * 测试场景：验证shouldShowStreamingBubble的逻辑
 */
class DoubleBubblePreventionTest {

    /**
     * TC-DB-001: 消息已在列表中且有内容
     * 
     * 场景：流式完成后，消息已进入conversations列表
     * 预期：不显示流式气泡
     */
    @Test
    fun `shouldShowStreamingBubble should be false when message is in list with content`() {
        // Given: 消息已在列表中且有内容
        val currentStreamingMessageId = "ai-msg-001"
        val conversations = listOf(
            createAiMessage("ai-msg-001", "完整的AI回复内容", SendStatus.SUCCESS)
        )
        val streamingContent = "完整的AI回复内容"
        val isStreaming = false
        
        // When: 检查消息是否已在列表中（使用V4增强的检测逻辑）
        val messageAlreadyInList = conversations.any { conv ->
            conv.id == currentStreamingMessageId && 
            (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
        }
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 不应该显示流式气泡
        assertTrue(messageAlreadyInList)
        assertFalse(shouldShowStreamingBubble)
    }

    /**
     * TC-DB-002: 消息不在列表中，流式进行中
     * 
     * 场景：流式正在进行
     * 预期：显示流式气泡
     */
    @Test
    fun `shouldShowStreamingBubble should be true when streaming and message not in list`() {
        // Given: 流式进行中，消息不在列表中
        val currentStreamingMessageId = "ai-msg-001"
        val conversations = emptyList<AiAdvisorConversation>()
        val streamingContent = "正在生成的内容"
        val isStreaming = true
        
        // When: 检查是否应该显示流式气泡
        val messageAlreadyInList = conversations.any { conv ->
            conv.id == currentStreamingMessageId && 
            (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
        }
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 应该显示流式气泡
        assertFalse(messageAlreadyInList)
        assertTrue(shouldShowStreamingBubble)
    }

    /**
     * TC-DB-003: 消息在列表中但状态为PENDING且内容为空
     * 
     * 场景：消息刚创建，还没有内容
     * 预期：显示流式气泡（因为列表中的消息没有内容且状态为PENDING）
     */
    @Test
    fun `shouldShowStreamingBubble should be true when message in list but empty and pending`() {
        // Given: 消息在列表中但内容为空且状态为PENDING
        val currentStreamingMessageId = "ai-msg-001"
        val conversations = listOf(
            createAiMessage("ai-msg-001", "", SendStatus.PENDING)
        )
        val streamingContent = "正在生成的内容"
        val isStreaming = true
        
        // When: 检查是否应该显示流式气泡（V4增强逻辑）
        val messageAlreadyInList = conversations.any { conv ->
            conv.id == currentStreamingMessageId && 
            (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
        }
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 应该显示流式气泡（因为content为空且状态为PENDING）
        assertFalse(messageAlreadyInList)
        assertTrue(shouldShowStreamingBubble)
    }

    /**
     * TC-DB-004: 流式完成但streamingContent还没清空
     * 
     * 场景：Completed触发后，等待清空streamingContent期间
     * 预期：如果消息已在列表中，不显示流式气泡
     */
    @Test
    fun `shouldShowStreamingBubble should be false when completed and message in list`() {
        // Given: 流式完成，消息已在列表中，但streamingContent还没清空
        val currentStreamingMessageId = "ai-msg-001"
        val conversations = listOf(
            createAiMessage("ai-msg-001", "完整的AI回复内容", SendStatus.SUCCESS)
        )
        val streamingContent = "完整的AI回复内容" // 还没清空
        val isStreaming = false // 已完成
        
        // When: 检查是否应该显示流式气泡（V4增强逻辑）
        val messageAlreadyInList = conversations.any { conv ->
            conv.id == currentStreamingMessageId && 
            (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
        }
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 不应该显示流式气泡（关键：避免双气泡）
        assertTrue(messageAlreadyInList)
        assertFalse(shouldShowStreamingBubble)
    }

    /**
     * TC-DB-005: 消息状态为CANCELLED但内容为空
     * 
     * 场景：停止生成后，消息状态为CANCELLED
     * 预期：不显示流式气泡（因为状态不是PENDING）
     */
    @Test
    fun `shouldShowStreamingBubble should be false when message is cancelled`() {
        // Given: 消息状态为CANCELLED
        val currentStreamingMessageId = "ai-msg-001"
        val conversations = listOf(
            createAiMessage("ai-msg-001", "", SendStatus.CANCELLED)
        )
        val streamingContent = ""
        val isStreaming = false
        
        // When: 检查是否应该显示流式气泡（V4增强逻辑）
        val messageAlreadyInList = conversations.any { conv ->
            conv.id == currentStreamingMessageId && 
            (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
        }
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 不应该显示流式气泡（因为状态为CANCELLED，不是PENDING）
        assertTrue(messageAlreadyInList)
        assertFalse(shouldShowStreamingBubble)
    }

    /**
     * TC-DB-006: currentStreamingMessageId为null
     * 
     * 场景：没有正在流式的消息
     * 预期：不显示流式气泡
     */
    @Test
    fun `shouldShowStreamingBubble should be false when currentStreamingMessageId is null`() {
        // Given: currentStreamingMessageId为null
        val currentStreamingMessageId: String? = null
        val conversations = emptyList<AiAdvisorConversation>()
        val streamingContent = ""
        val isStreaming = false
        
        // When: 检查是否应该显示流式气泡
        val messageAlreadyInList = currentStreamingMessageId?.let { messageId ->
            conversations.any { conv ->
                conv.id == messageId && 
                (conv.content.isNotEmpty() || conv.sendStatus != SendStatus.PENDING)
            }
        } ?: false
        
        val shouldShowStreamingBubble = !messageAlreadyInList && (
            isStreaming || 
            (streamingContent.isNotEmpty() && currentStreamingMessageId != null)
        )
        
        // Then: 不应该显示流式气泡
        assertFalse(shouldShowStreamingBubble)
    }

    // 辅助方法
    private fun createAiMessage(
        id: String, 
        content: String, 
        status: SendStatus,
        timestamp: Long = System.currentTimeMillis()
    ): AiAdvisorConversation {
        return AiAdvisorConversation(
            id = id,
            contactId = "contact-001",
            sessionId = "session-001",
            messageType = MessageType.AI,
            content = content,
            timestamp = timestamp,
            sendStatus = status
        )
    }
}
