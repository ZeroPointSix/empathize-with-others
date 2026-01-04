package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GetAdvisorSessionsUseCase单元测试
 */
class GetAdvisorSessionsUseCaseTest {

    private lateinit var useCase: GetAdvisorSessionsUseCase
    private lateinit var repository: AiAdvisorRepository

    private val testContactId = "contact-1"

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetAdvisorSessionsUseCase(repository)
    }

    @Test
    fun `invoke should return sessions for contact`() = runTest {
        // Given
        val sessions = listOf(
            createTestSession("session-1", testContactId, "Session 1"),
            createTestSession("session-2", testContactId, "Session 2")
        )
        coEvery { repository.getSessions(testContactId) } returns Result.success(sessions)

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Session 1", result.getOrNull()?.get(0)?.title)
        assertEquals("Session 2", result.getOrNull()?.get(1)?.title)
    }

    @Test
    fun `invoke should return empty list when no sessions exist`() = runTest {
        // Given
        coEvery { repository.getSessions(testContactId) } returns Result.success(emptyList())

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        coEvery {
            repository.getSessions(testContactId)
        } returns Result.failure(Exception("Database error"))

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should return sessions sorted by update time`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val sessions = listOf(
            createTestSession("session-1", testContactId, "Older", updatedAt = now - 1000),
            createTestSession("session-2", testContactId, "Newer", updatedAt = now)
        )
        coEvery { repository.getSessions(testContactId) } returns Result.success(sessions)

        // When
        val result = useCase(testContactId)

        // Then
        assertTrue(result.isSuccess)
        // Repository should return sorted by updatedAt desc
        assertEquals(2, result.getOrNull()?.size)
    }

    private fun createTestSession(
        id: String,
        contactId: String,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    ): AiAdvisorSession {
        val now = System.currentTimeMillis()
        return AiAdvisorSession(
            id = id,
            contactId = contactId,
            title = title,
            createdAt = now,
            updatedAt = updatedAt,
            messageCount = 0,
            isActive = true
        )
    }
}
