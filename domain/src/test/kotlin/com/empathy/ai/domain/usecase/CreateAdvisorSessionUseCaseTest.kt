package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CreateAdvisorSessionUseCase单元测试
 */
class CreateAdvisorSessionUseCaseTest {

    private lateinit var useCase: CreateAdvisorSessionUseCase
    private lateinit var repository: AiAdvisorRepository

    private val testContactId = "contact-1"

    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateAdvisorSessionUseCase(repository)
    }

    @Test
    fun `invoke should create new session with default title`() = runTest {
        // Given
        coEvery { repository.createSession(any()) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testContactId, result.getOrNull()?.contactId)
        assertEquals("新对话", result.getOrNull()?.title)
        coVerify { repository.createSession(any()) }
    }

    @Test
    fun `invoke should create session with custom title`() = runTest {
        // Given
        val customTitle = "Custom Session Title"
        coEvery { repository.createSession(any()) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId, customTitle)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(customTitle, result.getOrNull()?.title)
    }

    @Test
    fun `invoke should return session with generated id`() = runTest {
        // Given
        coEvery { repository.createSession(any()) } returns Result.success(Unit)

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.id?.isNotEmpty() == true)
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        coEvery {
            repository.createSession(any())
        } returns Result.failure(Exception("Database error"))

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    private fun createTestSession(
        id: String,
        contactId: String,
        title: String = "Test Session"
    ): AiAdvisorSession {
        val now = System.currentTimeMillis()
        return AiAdvisorSession(
            id = id,
            contactId = contactId,
            title = title,
            createdAt = now,
            updatedAt = now,
            messageCount = 0,
            isActive = true
        )
    }
}
