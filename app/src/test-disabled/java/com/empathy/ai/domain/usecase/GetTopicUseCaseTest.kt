package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * GetTopicUseCase单元测试
 *
 * 测试获取对话主题用例的各种场景
 */
class GetTopicUseCaseTest {

    private lateinit var topicRepository: TopicRepository
    private lateinit var getTopicUseCase: GetTopicUseCase

    @Before
    fun setup() {
        topicRepository = mockk()
        getTopicUseCase = GetTopicUseCase(topicRepository)
    }

    @Test
    fun `获取活跃主题_存在主题_返回主题`() = runTest {
        // Given
        val contactId = "contact-123"
        val topic = ConversationTopic(
            contactId = contactId,
            content = "讨论项目进度",
            isActive = true
        )
        coEvery { topicRepository.getActiveTopic(contactId) } returns topic

        // When
        val result = getTopicUseCase(contactId)

        // Then
        assertEquals(topic, result)
    }

    @Test
    fun `获取活跃主题_不存在主题_返回null`() = runTest {
        // Given
        val contactId = "contact-123"
        coEvery { topicRepository.getActiveTopic(contactId) } returns null

        // When
        val result = getTopicUseCase(contactId)

        // Then
        assertNull(result)
    }

    @Test
    fun `联系人ID为空_返回null`() = runTest {
        // Given
        val contactId = ""

        // When
        val result = getTopicUseCase(contactId)

        // Then
        assertNull(result)
    }

    @Test
    fun `观察主题变化_存在主题_返回主题Flow`() = runTest {
        // Given
        val contactId = "contact-123"
        val topic = ConversationTopic(
            contactId = contactId,
            content = "讨论项目进度",
            isActive = true
        )
        every { topicRepository.observeActiveTopic(contactId) } returns flowOf(topic)

        // When
        val result = getTopicUseCase.observe(contactId).first()

        // Then
        assertEquals(topic, result)
    }

    @Test
    fun `观察主题变化_不存在主题_返回null Flow`() = runTest {
        // Given
        val contactId = "contact-123"
        every { topicRepository.observeActiveTopic(contactId) } returns flowOf(null)

        // When
        val result = getTopicUseCase.observe(contactId).first()

        // Then
        assertNull(result)
    }

    @Test
    fun `观察主题变化_联系人ID为空_返回null Flow`() = runTest {
        // Given
        val contactId = ""

        // When
        val result = getTopicUseCase.observe(contactId).first()

        // Then
        assertNull(result)
    }
}
