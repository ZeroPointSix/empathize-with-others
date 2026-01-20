package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveAdvisorDraftUseCaseTest {

    private lateinit var repository: AiAdvisorPreferencesRepository
    private lateinit var useCase: SaveAdvisorDraftUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SaveAdvisorDraftUseCase(repository)
    }

    @Test
    fun `保存草稿成功返回success`() = runTest {
        // Given
        val sessionId = "session-1"
        val draft = "draft"
        every { repository.setDraft(sessionId, draft) } returns Unit

        // When
        val result = useCase(sessionId, draft)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `保存草稿异常返回failure`() = runTest {
        // Given
        val sessionId = "session-1"
        val draft = "draft"
        val error = IllegalStateException("save failed")
        every { repository.setDraft(sessionId, draft) } throws error

        // When
        val result = useCase(sessionId, draft)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
