package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.repository.ContactSortPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveContactSortOptionUseCaseTest {

    private lateinit var repository: ContactSortPreferencesRepository
    private lateinit var useCase: SaveContactSortOptionUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = SaveContactSortOptionUseCase(repository)
    }

    @Test
    fun `保存排序偏好成功`() = runTest {
        // Given
        coEvery { repository.setSortOption(ContactSortOption.NAME) } returns Result.success(Unit)

        // When
        val result = useCase(ContactSortOption.NAME)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.setSortOption(ContactSortOption.NAME) }
    }

    @Test
    fun `保存排序偏好失败返回failure`() = runTest {
        // Given
        val error = IllegalStateException("save failed")
        coEvery { repository.setSortOption(ContactSortOption.RELATIONSHIP_SCORE) } returns Result.failure(error)

        // When
        val result = useCase(ContactSortOption.RELATIONSHIP_SCORE)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
