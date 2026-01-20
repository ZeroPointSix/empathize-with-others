package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClearAllAdvisorDraftsUseCaseTest {

    private lateinit var repository: AiAdvisorPreferencesRepository
    private lateinit var useCase: ClearAllAdvisorDraftsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ClearAllAdvisorDraftsUseCase(repository)
    }

    @Test
    fun `清除全部草稿成功返回success`() = runTest {
        // Given
        every { repository.clearAllDrafts() } returns Unit

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `清除全部草稿异常返回failure`() = runTest {
        // Given
        val error = IllegalStateException("clear all failed")
        every { repository.clearAllDrafts() } throws error

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
