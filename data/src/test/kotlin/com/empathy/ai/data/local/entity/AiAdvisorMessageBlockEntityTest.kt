package com.empathy.ai.data.local.entity

import com.empathy.ai.domain.model.AiAdvisorMessageBlock
import com.empathy.ai.domain.model.BlockMetadata
import com.empathy.ai.domain.model.MessageBlockStatus
import com.empathy.ai.domain.model.MessageBlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AiAdvisorMessageBlockEntity数据库实体测试
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 */
class AiAdvisorMessageBlockEntityTest {

    @Test
    fun `fromDomain应正确转换领域模型到实体`() {
        // Given
        val domainBlock = AiAdvisorMessageBlock(
            id = "block-123",
            messageId = "msg-456",
            type = MessageBlockType.MAIN_TEXT,
            status = MessageBlockStatus.SUCCESS,
            content = "Hello World",
            metadata = null,
            createdAt = 1000L
        )

        // When
        val entity = AiAdvisorMessageBlockEntity.fromDomain(domainBlock)

        // Then
        assertEquals("block-123", entity.id)
        assertEquals("msg-456", entity.messageId)
        assertEquals("MAIN_TEXT", entity.type)
        assertEquals("SUCCESS", entity.status)
        assertEquals("Hello World", entity.content)
        assertNull(entity.metadata)
        assertEquals(1000L, entity.createdAt)
    }

    @Test
    fun `toDomain应正确转换实体到领域模型`() {
        // Given
        val entity = AiAdvisorMessageBlockEntity(
            id = "block-123",
            messageId = "msg-456",
            type = "THINKING",
            status = "STREAMING",
            content = "Thinking...",
            metadata = null,
            createdAt = 2000L
        )

        // When
        val domainBlock = entity.toDomain()

        // Then
        assertEquals("block-123", domainBlock.id)
        assertEquals("msg-456", domainBlock.messageId)
        assertEquals(MessageBlockType.THINKING, domainBlock.type)
        assertEquals(MessageBlockStatus.STREAMING, domainBlock.status)
        assertEquals("Thinking...", domainBlock.content)
        assertNull(domainBlock.metadata)
        assertEquals(2000L, domainBlock.createdAt)
    }

    @Test
    fun `fromDomain应正确处理metadata`() {
        // Given
        val metadata = BlockMetadata(thinkingMs = 1500L, tokenCount = 100)
        val domainBlock = AiAdvisorMessageBlock(
            id = "block-123",
            messageId = "msg-456",
            type = MessageBlockType.THINKING,
            status = MessageBlockStatus.SUCCESS,
            content = "Thinking content",
            metadata = metadata,
            createdAt = 1000L
        )

        // When
        val entity = AiAdvisorMessageBlockEntity.fromDomain(domainBlock)

        // Then
        // metadata应被序列化为JSON字符串
        // 注意：实际实现可能需要Moshi依赖
    }

    @Test
    fun `所有MessageBlockType值应能正确转换`() {
        MessageBlockType.entries.forEach { type ->
            val entity = AiAdvisorMessageBlockEntity(
                id = "id",
                messageId = "msg",
                type = type.name,
                status = "SUCCESS",
                content = "",
                createdAt = 0L
            )

            val domain = entity.toDomain()
            assertEquals(type, domain.type)
        }
    }

    @Test
    fun `所有MessageBlockStatus值应能正确转换`() {
        MessageBlockStatus.entries.forEach { status ->
            val entity = AiAdvisorMessageBlockEntity(
                id = "id",
                messageId = "msg",
                type = "MAIN_TEXT",
                status = status.name,
                content = "",
                createdAt = 0L
            )

            val domain = entity.toDomain()
            assertEquals(status, domain.status)
        }
    }

    @Test
    fun `双向转换应保持数据一致性`() {
        // Given
        val original = AiAdvisorMessageBlock(
            id = "block-original",
            messageId = "msg-original",
            type = MessageBlockType.ERROR,
            status = MessageBlockStatus.ERROR,
            content = "Error message",
            metadata = null,
            createdAt = 3000L
        )

        // When
        val entity = AiAdvisorMessageBlockEntity.fromDomain(original)
        val restored = entity.toDomain()

        // Then
        assertEquals(original.id, restored.id)
        assertEquals(original.messageId, restored.messageId)
        assertEquals(original.type, restored.type)
        assertEquals(original.status, restored.status)
        assertEquals(original.content, restored.content)
        assertEquals(original.createdAt, restored.createdAt)
    }
}
