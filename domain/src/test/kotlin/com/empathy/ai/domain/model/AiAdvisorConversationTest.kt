package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiAdvisorConversation 单元测试
 */
class AiAdvisorConversationTest {

    @Test
    fun `创建用户消息应设置正确的消息类型`() {
        val conversation = AiAdvisorConversation(
            id = "test-id",
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.USER,
            content = "Hello",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.SUCCESS
        )

        assertEquals(MessageType.USER, conversation.messageType)
        assertEquals("Hello", conversation.content)
        assertEquals(SendStatus.SUCCESS, conversation.sendStatus)
    }

    @Test
    fun `创建AI消息应设置正确的消息类型`() {
        val conversation = AiAdvisorConversation(
            id = "test-id",
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.AI,
            content = "AI Response",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.PENDING
        )

        assertEquals(MessageType.AI, conversation.messageType)
        assertEquals("AI Response", conversation.content)
        assertEquals(SendStatus.PENDING, conversation.sendStatus)
    }

    @Test
    fun `不同ID的对话应不相等`() {
        val conversation1 = AiAdvisorConversation(
            id = "id-1",
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.USER,
            content = "Hello",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.SUCCESS
        )

        val conversation2 = AiAdvisorConversation(
            id = "id-2",
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.USER,
            content = "Hello",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.SUCCESS
        )

        assertNotEquals(conversation1, conversation2)
    }

    @Test
    fun `CANCELLED状态应正确设置`() {
        val conversation = AiAdvisorConversation(
            id = "test-id",
            contactId = "contact-1",
            sessionId = "session-1",
            messageType = MessageType.AI,
            content = "",
            timestamp = System.currentTimeMillis(),
            sendStatus = SendStatus.CANCELLED
        )

        assertEquals(SendStatus.CANCELLED, conversation.sendStatus)
    }
}
