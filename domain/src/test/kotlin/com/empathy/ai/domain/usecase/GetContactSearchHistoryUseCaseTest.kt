package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactSearchHistoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetContactSearchHistoryUseCaseTest {

    private lateinit var repository: ContactSearchHistoryRepository
    private lateinit var useCase: GetContactSearchHistoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetContactSearchHistoryUseCase(repository)
    }

    @Test
    fun `读取搜索历史成功`() = runTest {
        // Given
        val history = listOf("张三", "合作")
        coEvery { repository.getHistory() } returns Result.success(history)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
    }

    @Test
    fun `读取搜索历史失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("load failed")
        coEvery { repository.getHistory() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
