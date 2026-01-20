package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetContactRecentHistoryUseCaseTest {

    private lateinit var repository: ContactRecentHistoryRepository
    private lateinit var useCase: GetContactRecentHistoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetContactRecentHistoryUseCase(repository)
    }

    @Test
    fun `读取最近联系人成功`() = runTest {
        // Given
        val history = listOf("id-1", "id-2")
        coEvery { repository.getRecentContactIds() } returns Result.success(history)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
    }

    @Test
    fun `读取最近联系人失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("load failed")
        coEvery { repository.getRecentContactIds() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
