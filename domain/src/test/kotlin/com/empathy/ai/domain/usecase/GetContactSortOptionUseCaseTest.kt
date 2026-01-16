package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.repository.ContactSortPreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetContactSortOptionUseCaseTest {

    private lateinit var repository: ContactSortPreferencesRepository
    private lateinit var useCase: GetContactSortOptionUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetContactSortOptionUseCase(repository)
    }

    @Test
    fun `读取排序偏好成功`() = runTest {
        // Given
        coEvery { repository.getSortOption() } returns Result.success(ContactSortOption.LAST_INTERACTION)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(ContactSortOption.LAST_INTERACTION, result.getOrNull())
    }

    @Test
    fun `读取排序偏好失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("load failed")
        coEvery { repository.getSortOption() } returns Result.failure(error)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
