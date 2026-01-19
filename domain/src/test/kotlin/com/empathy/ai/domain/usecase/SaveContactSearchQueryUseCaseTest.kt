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

class SaveContactSearchQueryUseCaseTest {

    private lateinit var repository: ContactSearchHistoryRepository
    private lateinit var useCase: SaveContactSearchQueryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SaveContactSearchQueryUseCase(repository)
    }

    @Test
    fun `保存搜索词成功返回历史`() = runTest {
        // Given
        val history = listOf("张三", "合作")
        coEvery { repository.saveQuery("张三") } returns Result.success(history)

        // When
        val result = useCase("张三")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
        coVerify { repository.saveQuery("张三") }
    }

    @Test
    fun `保存搜索词失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("save failed")
        coEvery { repository.saveQuery("合作") } returns Result.failure(error)

        // When
        val result = useCase("合作")

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
