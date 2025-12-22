package com.empathy.ai.domain.usecase

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
 * ClearTopicUseCase单元测试
 *
 * 测试清除对话主题用例的各种场景
 */
class ClearTopicUseCaseTest {

    private lateinit var topicRepository: TopicRepository
    private lateinit var clearTopicUseCase: ClearTopicUseCase

    @Before
    fun setup() {
        topicRepository = mockk()
        clearTopicUseCase = ClearTopicUseCase(topicRepository)
    }

    @Test
    fun `清除主题成功_返回成功`() = runTest {
        // Given
        val contactId = "contact-123"
        coEvery { topicRepository.clearTopic(contactId) } returns Result.success(Unit)

        // When
        val result = clearTopicUseCase(contactId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { topicRepository.clearTopic(contactId) }
    }

    @Test
    fun `联系人ID为空_返回失败`() = runTest {
        // Given
        val contactId = ""

        // When
        val result = clearTopicUseCase(contactId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `联系人ID只有空格_返回失败`() = runTest {
        // Given
        val contactId = "   "

        // When
        val result = clearTopicUseCase(contactId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `仓库清除失败_返回失败`() = runTest {
        // Given
        val contactId = "contact-123"
        val error = RuntimeException("数据库错误")
        coEvery { topicRepository.clearTopic(contactId) } returns Result.failure(error)

        // When
        val result = clearTopicUseCase(contactId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
