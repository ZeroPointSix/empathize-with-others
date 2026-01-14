package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ConversationTopic领域模型单元测试
 *
 * 测试对话主题模型的各种方法和属性
 */
class ConversationTopicTest {

    @Test
    fun `创建主题_默认值正确`() {
        // Given & When
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "讨论项目进度"
        )

        // Then
        assertTrue(topic.id.isNotBlank())
        assertEquals("contact-123", topic.contactId)
        assertEquals("讨论项目进度", topic.content)
        assertTrue(topic.isActive)
        assertTrue(topic.createdAt > 0)
        assertTrue(topic.updatedAt > 0)
    }

    @Test
    fun `getPreview_内容短于预览长度_返回完整内容`() {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "短内容"
        )

        // When
        val preview = topic.getPreview()

        // Then
        assertEquals("短内容", preview)
    }

    @Test
    fun `getPreview_内容等于预览长度_返回完整内容`() {
        // Given
        val content = "a".repeat(ConversationTopic.PREVIEW_LENGTH)
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = content
        )

        // When
        val preview = topic.getPreview()

        // Then
        assertEquals(content, preview)
    }

    @Test
    fun `getPreview_内容超过预览长度_返回截断内容加省略号`() {
        // Given
        val content = "a".repeat(ConversationTopic.PREVIEW_LENGTH + 10)
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = content
        )

        // When
        val preview = topic.getPreview()

        // Then
        assertEquals(ConversationTopic.PREVIEW_LENGTH + 3, preview.length) // 50 + "..."
        assertTrue(preview.endsWith("..."))
    }

    @Test
    fun `isValid_有效内容_返回true`() {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "有效的主题内容"
        )

        // When & Then
        assertTrue(topic.isValid())
    }

    @Test
    fun `isValid_空内容_返回false`() {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = ""
        )

        // When & Then
        assertFalse(topic.isValid())
    }

    @Test
    fun `isValid_只有空格_返回false`() {
        // Given
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = "   "
        )

        // When & Then
        assertFalse(topic.isValid())
    }

    @Test
    fun `isValid_超过最大长度_返回false`() {
        // Given
        val content = "a".repeat(ConversationTopic.MAX_CONTENT_LENGTH + 1)
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = content
        )

        // When & Then
        assertFalse(topic.isValid())
    }

    @Test
    fun `isValid_刚好等于最大长度_返回true`() {
        // Given
        val content = "a".repeat(ConversationTopic.MAX_CONTENT_LENGTH)
        val topic = ConversationTopic(
            contactId = "contact-123",
            content = content
        )

        // When & Then
        assertTrue(topic.isValid())
    }

    @Test
    fun `MAX_CONTENT_LENGTH_值为500`() {
        assertEquals(500, ConversationTopic.MAX_CONTENT_LENGTH)
    }

    @Test
    fun `PREVIEW_LENGTH_值为50`() {
        assertEquals(50, ConversationTopic.PREVIEW_LENGTH)
    }
}
