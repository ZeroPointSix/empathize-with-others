package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.model.MessageType
import com.empathy.ai.domain.model.SendStatus
import com.empathy.ai.domain.repository.AiAdvisorRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GetAdvisorConversationsUseCase 单元测试
 *
 * 业务背景 (PRD-00026):
 *   AI军师对话流获取测试，验证会话内消息的历史记录查询
 *
 * 功能验证:
 *   - 按会话ID查询消息列表（Flow响应式流）
 *   - 消息按时间戳正序排列（时间线展示）
 *   - 空对话流的边界处理
 *   - 失败状态消息的包含（保留完整历史）
 *
 * 设计决策 (TDD-00026):
 *   - 使用Flow响应式流，实时反映消息变化
 *   - Repository层负责排序，UI层直接消费有序数据
 *   - 失败消息仍保留，用于审计和问题排查
 *
 * 任务追踪: FD-00026/阶段2-领域层实现
 */
class GetAdvisorConversationsUseCaseTest {

    private lateinit var useCase: GetAdvisorConversationsUseCase
    private lateinit var repository: AiAdvisorRepository

    private val testSessionId = "session-1"
    private val testContactId = "contact-1"

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetAdvisorConversationsUseCase(repository)
    }

    @Test
    fun `invoke should return conversations flow for session`() = runTest {
        // Given
        val conversations = listOf(
            createTestConversation("conv-1", MessageType.USER, "Hello"),
            createTestConversation("conv-2", MessageType.AI, "Hi there")
        )
        every { repository.getConversationsFlow(testSessionId) } returns flowOf(conversations)

        // When
        val result = useCase(testSessionId).first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Hello", result[0].content)
        assertEquals(MessageType.USER, result[0].messageType)
        assertEquals("Hi there", result[1].content)
        assertEquals(MessageType.AI, result[1].messageType)
    }

    @Test
    fun `invoke should return empty flow when no conversations`() = runTest {
        // Given
        every { repository.getConversationsFlow(testSessionId) } returns flowOf(emptyList())

        // When
        val result = useCase(testSessionId).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke should return conversations sorted by timestamp`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val conversations = listOf(
            createTestConversation("conv-1", MessageType.USER, "First", timestamp = now - 2000),
            createTestConversation("conv-2", MessageType.AI, "Second", timestamp = now - 1000),
            createTestConversation("conv-3", MessageType.USER, "Third", timestamp = now)
        )
        every { repository.getConversationsFlow(testSessionId) } returns flowOf(conversations)

        // When
        val result = useCase(testSessionId).first()

        // Then
        assertEquals(3, result.size)
        assertEquals("First", result[0].content)
        assertEquals("Second", result[1].content)
        assertEquals("Third", result[2].content)
    }

    @Test
    fun `invoke should include failed messages in flow`() = runTest {
        // Given
        val conversations = listOf(
            createTestConversation("conv-1", MessageType.USER, "Hello", sendStatus = SendStatus.SUCCESS),
            createTestConversation("conv-2", MessageType.AI, "", sendStatus = SendStatus.FAILED)
        )
        every { repository.getConversationsFlow(testSessionId) } returns flowOf(conversations)

        // When
        val result = useCase(testSessionId).first()

        // Then
        assertEquals(2, result.size)
        assertEquals(SendStatus.SUCCESS, result[0].sendStatus)
        assertEquals(SendStatus.FAILED, result[1].sendStatus)
    }

    private fun createTestConversation(
        id: String,
        messageType: MessageType,
        content: String,
        timestamp: Long = System.currentTimeMillis(),
        sendStatus: SendStatus = SendStatus.SUCCESS
    ): AiAdvisorConversation {
        return AiAdvisorConversation(
            id = id,
            contactId = testContactId,
            sessionId = testSessionId,
            messageType = messageType,
            content = content,
            timestamp = timestamp,
            createdAt = timestamp,
            sendStatus = sendStatus
        )
    }
}
