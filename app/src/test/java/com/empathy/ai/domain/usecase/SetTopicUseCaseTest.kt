package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SetTopicUseCase单元测试
 *
 * 测试设置对话主题用例的各种场景
 */
class SetTopicUseCaseTest {

    private lateinit var topicRepository: TopicRepository
    private lateinit var setTopicUseCase: SetTopicUseCase

    @Before
    fun setup() {
        topicRepository = mockk()
        setTopicUseCase = SetTopicUseCase(topicRepository)
    }

    @Test
    fun `设置主题成功_返回创建的主题`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "讨论项目进度"
        coEvery { topicRepository.setTopic(any()) } returns Result.success(Unit)

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isSuccess)
        val topic = result.getOrNull()!!
        assertEquals(contactId, topic.contactId)
        assertEquals(content, topic.content)
        assertTrue(topic.isActive)
        coVerify { topicRepository.setTopic(any()) }
    }

    @Test
    fun `联系人ID为空_返回失败`() = runTest {
        // Given
        val contactId = ""
        val content = "讨论项目进度"

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `主题内容为空_返回失败`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = ""

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("主题内容不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `主题内容只有空格_返回失败`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "   "

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `主题内容超过最大长度_返回失败`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "a".repeat(ConversationTopic.MAX_CONTENT_LENGTH + 1)

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("不能超过") == true)
    }

    @Test
    fun `主题内容刚好等于最大长度_返回成功`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "a".repeat(ConversationTopic.MAX_CONTENT_LENGTH)
        coEvery { topicRepository.setTopic(any()) } returns Result.success(Unit)

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `主题内容前后有空格_自动去除空格`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "  讨论项目进度  "
        coEvery { topicRepository.setTopic(any()) } returns Result.success(Unit)

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("讨论项目进度", result.getOrNull()?.content)
    }

    @Test
    fun `仓库保存失败_返回失败`() = runTest {
        // Given
        val contactId = "contact-123"
        val content = "讨论项目进度"
        val error = RuntimeException("数据库错误")
        coEvery { topicRepository.setTopic(any()) } returns Result.failure(error)

        // When
        val result = setTopicUseCase(contactId, content)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
