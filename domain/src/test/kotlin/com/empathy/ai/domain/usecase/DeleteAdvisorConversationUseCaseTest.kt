package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * DeleteAdvisorConversationUseCase单元测试
 */
class DeleteAdvisorConversationUseCaseTest {

    private lateinit var useCase: DeleteAdvisorConversationUseCase
    private lateinit var repository: AiAdvisorRepository

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteAdvisorConversationUseCase(repository)
    }

    @Test
    fun `invoke should call repository deleteConversation`() = runTest {
        // Given
        val conversationId = "conv-1"
        coEvery { repository.deleteConversation(conversationId) } returns Result.success(Unit)

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.deleteConversation(conversationId) }
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val conversationId = "conv-1"
        coEvery {
            repository.deleteConversation(conversationId)
        } returns Result.failure(Exception("Delete failed"))

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Delete failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should handle empty conversationId`() = runTest {
        // Given
        val conversationId = ""
        coEvery { repository.deleteConversation(conversationId) } returns Result.success(Unit)

        // When
        val result = useCase(conversationId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.deleteConversation(conversationId) }
    }
}
