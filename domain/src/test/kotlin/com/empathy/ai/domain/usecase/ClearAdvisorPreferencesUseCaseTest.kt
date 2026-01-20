package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClearAdvisorPreferencesUseCaseTest {

    private lateinit var repository: AiAdvisorPreferencesRepository
    private lateinit var useCase: ClearAdvisorPreferencesUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ClearAdvisorPreferencesUseCase(repository)
    }

    @Test
    fun `清除偏好成功返回success`() = runTest {
        every { repository.clear() } returns Unit

        val result = useCase()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `清除偏好异常返回failure`() = runTest {
        val error = IllegalStateException("clear failed")
        every { repository.clear() } throws error

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
