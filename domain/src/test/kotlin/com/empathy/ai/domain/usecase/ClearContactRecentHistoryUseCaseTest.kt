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

class ClearContactRecentHistoryUseCaseTest {

    private lateinit var repository: ContactRecentHistoryRepository
    private lateinit var useCase: ClearContactRecentHistoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ClearContactRecentHistoryUseCase(repository)
    }

    @Test
    fun `清空最近联系人成功`() = runTest {
        // Given
        coEvery { repository.clearRecentContacts() } returns Result.success(Unit)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.clearRecentContacts() }
    }

    @Test
    fun `清空最近联系人失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("clear failed")
        coEvery { repository.clearRecentContacts() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
