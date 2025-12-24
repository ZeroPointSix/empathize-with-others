package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ConversationLog领域模型单元测试
 */
class ConversationLogTest {

    @Test
    fun `创建ConversationLog成功`() {
        val log = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = "你好，最近怎么样？",
            aiResponse = "建议保持友好的问候",
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertEquals(1L, log.id)
        assertEquals("contact_123", log.contactId)
        assertEquals("你好，最近怎么样？", log.userInput)
        assertEquals("建议保持友好的问候", log.aiResponse)
        assertFalse(log.isSummarized)
    }

    @Test
    fun `创建ConversationLog时aiResponse可以为null`() {
        val log = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = "你好",
            aiResponse = null,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertNull(log.aiResponse)
    }

    @Test
    fun `创建ConversationLog时使用默认值`() {
        val log = ConversationLog(
            id = 0L,
            contactId = "contact_123",
            userInput = "你好",
            aiResponse = null,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertEquals(0L, log.id)
        assertFalse(log.isSummarized)
    }

    @Test
    fun `ConversationLog的copy方法正确工作`() {
        val original = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = "你好",
            aiResponse = null,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        val copied = original.copy(
            aiResponse = "AI回复",
            isSummarized = true
        )

        assertEquals(original.id, copied.id)
        assertEquals(original.contactId, copied.contactId)
        assertEquals(original.userInput, copied.userInput)
        assertEquals("AI回复", copied.aiResponse)
        assertTrue(copied.isSummarized)
    }

    @Test
    fun `ConversationLog的equals方法正确工作`() {
        val timestamp = System.currentTimeMillis()
        val log1 = ConversationLog(1L, "contact_123", "你好", "回复", timestamp, false)
        val log2 = ConversationLog(1L, "contact_123", "你好", "回复", timestamp, false)

        assertEquals(log1, log2)
    }

    @Test
    fun `不同的ConversationLog不相等`() {
        val timestamp = System.currentTimeMillis()
        val log1 = ConversationLog(1L, "contact_123", "你好", "回复", timestamp, false)
        val log2 = ConversationLog(2L, "contact_123", "你好", "回复", timestamp, false)

        assertFalse(log1 == log2)
    }

    @Test
    fun `ConversationLog的hashCode一致性`() {
        val timestamp = System.currentTimeMillis()
        val log1 = ConversationLog(1L, "contact_123", "你好", "回复", timestamp, false)
        val log2 = ConversationLog(1L, "contact_123", "你好", "回复", timestamp, false)

        assertEquals(log1.hashCode(), log2.hashCode())
    }

    @Test
    fun `ConversationLog支持长文本`() {
        val longText = "这是一段很长的文本".repeat(100)
        val log = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = longText,
            aiResponse = longText,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertEquals(longText, log.userInput)
        assertEquals(longText, log.aiResponse)
    }

    @Test
    fun `ConversationLog支持特殊字符`() {
        val specialText = "你好！@#\$%^&*()_+{}|:\"<>?~`-=[]\\;',./\n\t"
        val log = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = specialText,
            aiResponse = specialText,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertEquals(specialText, log.userInput)
        assertEquals(specialText, log.aiResponse)
    }

    @Test
    fun `ConversationLog支持emoji`() {
        val emojiText = "你好😀👍🎉"
        val log = ConversationLog(
            id = 1L,
            contactId = "contact_123",
            userInput = emojiText,
            aiResponse = emojiText,
            timestamp = System.currentTimeMillis(),
            isSummarized = false
        )

        assertEquals(emojiText, log.userInput)
        assertEquals(emojiText, log.aiResponse)
    }
}
