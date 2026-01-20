package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordContactVisitUseCaseTest {

    private lateinit var repository: ContactRecentHistoryRepository
    private lateinit var useCase: RecordContactVisitUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = RecordContactVisitUseCase(repository)
    }

    @Test
    fun `记录联系人访问成功返回历史`() = runTest {
        // Given
        val history = listOf("id-1", "id-2")
        coEvery { repository.recordContactVisit("id-1") } returns Result.success(history)

        // When
        val result = useCase("id-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
        coVerify { repository.recordContactVisit("id-1") }
    }

    @Test
    fun `记录联系人访问失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("save failed")
        coEvery { repository.recordContactVisit("id-2") } returns Result.failure(error)

        // When
        val result = useCase("id-2")

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
