package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactSearchHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClearContactSearchHistoryUseCaseTest {

    private lateinit var repository: ContactSearchHistoryRepository
    private lateinit var useCase: ClearContactSearchHistoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ClearContactSearchHistoryUseCase(repository)
    }

    @Test
    fun `清空搜索历史成功`() = runTest {
        // Given
        coEvery { repository.clearHistory() } returns Result.success(Unit)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.clearHistory() }
    }

    @Test
    fun `清空搜索历史失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("clear failed")
        coEvery { repository.clearHistory() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
