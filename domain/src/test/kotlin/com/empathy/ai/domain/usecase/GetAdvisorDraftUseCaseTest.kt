package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetAdvisorDraftUseCaseTest {

    private lateinit var repository: AiAdvisorPreferencesRepository
    private lateinit var useCase: GetAdvisorDraftUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetAdvisorDraftUseCase(repository)
    }

    @Test
    fun `获取草稿成功返回内容`() = runTest {
        // Given
        val sessionId = "session-1"
        every { repository.getDraft(sessionId) } returns "draft"

        // When
        val result = useCase(sessionId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("draft", result.getOrNull())
    }

    @Test
    fun `获取草稿异常返回failure`() = runTest {
        // Given
        val sessionId = "session-1"
        val error = IllegalStateException("read failed")
        every { repository.getDraft(sessionId) } throws error

        // When
        val result = useCase(sessionId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
