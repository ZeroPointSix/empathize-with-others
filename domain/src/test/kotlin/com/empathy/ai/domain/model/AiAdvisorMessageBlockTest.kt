package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiAdvisorMessageBlock消息块模型测试
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class AiAdvisorMessageBlockTest {

    @Test
    fun `createMainTextBlock应创建MAIN_TEXT类型Block`() {
        val block = AiAdvisorMessageBlock.createMainTextBlock("msg-123")

        assertEquals("msg-123", block.messageId)
        assertEquals(MessageBlockType.MAIN_TEXT, block.type)
        assertEquals(MessageBlockStatus.PENDING, block.status)
        assertEquals("", block.content)
        assertNull(block.metadata)
        assertNotNull(block.id)
        assertTrue(block.createdAt > 0)
    }

    @Test
    fun `createMainTextBlock应支持自定义内容和状态`() {
        val block = AiAdvisorMessageBlock.createMainTextBlock(
            messageId = "msg-123",
            content = "Hello World",
            status = MessageBlockStatus.SUCCESS
        )

        assertEquals("Hello World", block.content)
        assertEquals(MessageBlockStatus.SUCCESS, block.status)
    }

    @Test
    fun `createThinkingBlock应创建THINKING类型Block`() {
        val block = AiAdvisorMessageBlock.createThinkingBlock("msg-456")

        assertEquals("msg-456", block.messageId)
        assertEquals(MessageBlockType.THINKING, block.type)
        assertEquals(MessageBlockStatus.PENDING, block.status)
        assertEquals("", block.content)
    }

    @Test
    fun `createThinkingBlock应支持自定义内容和状态`() {
        val block = AiAdvisorMessageBlock.createThinkingBlock(
            messageId = "msg-456",
            content = "Let me think...",
            status = MessageBlockStatus.STREAMING
        )

        assertEquals("Let me think...", block.content)
        assertEquals(MessageBlockStatus.STREAMING, block.status)
    }

    @Test
    fun `createErrorBlock应创建ERROR类型Block`() {
        val block = AiAdvisorMessageBlock.createErrorBlock("msg-789", "Network error")

        assertEquals("msg-789", block.messageId)
        assertEquals(MessageBlockType.ERROR, block.type)
        assertEquals(MessageBlockStatus.ERROR, block.status)
        assertEquals("Network error", block.content)
    }

    @Test
    fun `withContent应返回更新内容后的新实例`() {
        val original = AiAdvisorMessageBlock.createMainTextBlock("msg-123")
        val updated = original.withContent("New content")

        assertEquals("New content", updated.content)
        assertEquals("", original.content) // 原实例不变
        assertEquals(original.id, updated.id)
        assertEquals(original.messageId, updated.messageId)
    }

    @Test
    fun `withStatus应返回更新状态后的新实例`() {
        val original = AiAdvisorMessageBlock.createMainTextBlock("msg-123")
        val updated = original.withStatus(MessageBlockStatus.SUCCESS)

        assertEquals(MessageBlockStatus.SUCCESS, updated.status)
        assertEquals(MessageBlockStatus.PENDING, original.status) // 原实例不变
    }

    @Test
    fun `withContentAndStatus应同时更新内容和状态`() {
        val original = AiAdvisorMessageBlock.createMainTextBlock("msg-123")
        val updated = original.withContentAndStatus("Final content", MessageBlockStatus.SUCCESS)

        assertEquals("Final content", updated.content)
        assertEquals(MessageBlockStatus.SUCCESS, updated.status)
        assertEquals("", original.content)
        assertEquals(MessageBlockStatus.PENDING, original.status)
    }

    @Test
    fun `每次创建Block应生成唯一ID`() {
        val block1 = AiAdvisorMessageBlock.createMainTextBlock("msg-123")
        val block2 = AiAdvisorMessageBlock.createMainTextBlock("msg-123")

        assertNotEquals(block1.id, block2.id)
    }

    @Test
    fun `BlockMetadata应正确存储思考耗时`() {
        val metadata = BlockMetadata(thinkingMs = 1500L)
        assertEquals(1500L, metadata.thinkingMs)
        assertNull(metadata.tokenCount)
    }

    @Test
    fun `BlockMetadata应正确存储Token数量`() {
        val metadata = BlockMetadata(tokenCount = 100)
        assertNull(metadata.thinkingMs)
        assertEquals(100, metadata.tokenCount)
    }

    @Test
    fun `BlockMetadata应支持同时存储多个字段`() {
        val metadata = BlockMetadata(thinkingMs = 2000L, tokenCount = 150)
        assertEquals(2000L, metadata.thinkingMs)
        assertEquals(150, metadata.tokenCount)
    }

    @Test
    fun `Block应支持metadata字段`() {
        val metadata = BlockMetadata(thinkingMs = 1000L)
        val block = AiAdvisorMessageBlock(
            id = "block-1",
            messageId = "msg-1",
            type = MessageBlockType.THINKING,
            status = MessageBlockStatus.SUCCESS,
            content = "Thinking content",
            metadata = metadata
        )

        assertEquals(metadata, block.metadata)
        assertEquals(1000L, block.metadata?.thinkingMs)
    }
}
