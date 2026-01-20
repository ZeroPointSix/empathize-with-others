package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClearAdvisorDraftUseCaseTest {

    private lateinit var repository: AiAdvisorPreferencesRepository
    private lateinit var useCase: ClearAdvisorDraftUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ClearAdvisorDraftUseCase(repository)
    }

    @Test
    fun `清除草稿成功返回success`() = runTest {
        // Given
        val sessionId = "session-1"
        every { repository.clearDraft(sessionId) } returns Unit

        // When
        val result = useCase(sessionId)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `清除草稿异常返回failure`() = runTest {
        // Given
        val sessionId = "session-1"
        val error = IllegalStateException("clear failed")
        every { repository.clearDraft(sessionId) } throws error

        // When
        val result = useCase(sessionId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
