package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteContactUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var aiAdvisorRepository: AiAdvisorRepository
    private lateinit var clearAdvisorDraftUseCase: ClearAdvisorDraftUseCase
    private lateinit var useCase: DeleteContactUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        aiAdvisorRepository = mockk()
        clearAdvisorDraftUseCase = mockk()
        useCase = DeleteContactUseCase(contactRepository, aiAdvisorRepository, clearAdvisorDraftUseCase)
    }

    @Test
    fun `联系人ID为空返回failure`() = runTest {
        val result = useCase("")
        assertTrue(result.isFailure)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `删除联系人成功后清理会话草稿`() = runTest {
        val contactId = "contact-1"
        val sessions = listOf(
            AiAdvisorSession(
                id = "session-1",
                contactId = contactId,
                title = "s1",
                messageCount = 0,
                createdAt = 1L,
                updatedAt = 1L
            ),
            AiAdvisorSession(
                id = "session-2",
                contactId = contactId,
                title = "s2",
                messageCount = 0,
                createdAt = 2L,
                updatedAt = 2L
            )
        )
        coEvery { aiAdvisorRepository.getSessions(contactId) } returns Result.success(sessions)
        coEvery { contactRepository.deleteProfile(contactId) } returns Result.success(Unit)
        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.success(Unit)

        val result = useCase(contactId)

        assertTrue(result.isSuccess)
        coVerify { clearAdvisorDraftUseCase("session-1") }
        coVerify { clearAdvisorDraftUseCase("session-2") }
    }

    @Test
    fun `删除联系人失败不清理草稿`() = runTest {
        val contactId = "contact-1"
        val error = IllegalStateException("delete failed")
        val sessions = listOf(
            AiAdvisorSession(
                id = "session-1",
                contactId = contactId,
                title = "s1",
                messageCount = 0,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        coEvery { aiAdvisorRepository.getSessions(contactId) } returns Result.success(sessions)
        coEvery { contactRepository.deleteProfile(contactId) } returns Result.failure(error)
        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.success(Unit)

        val result = useCase(contactId)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        coVerify(exactly = 0) { clearAdvisorDraftUseCase(any()) }
    }

    @Test
    fun `获取会话失败时仍删除联系人`() = runTest {
        val contactId = "contact-1"
        coEvery { aiAdvisorRepository.getSessions(contactId) } returns Result.failure(Exception("load failed"))
        coEvery { contactRepository.deleteProfile(contactId) } returns Result.success(Unit)
        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.success(Unit)

        val result = useCase(contactId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { clearAdvisorDraftUseCase(any()) }
    }

    @Test
    fun `清理草稿失败不影响联系人删除结果`() = runTest {
        val contactId = "contact-1"
        val sessions = listOf(
            AiAdvisorSession(
                id = "session-1",
                contactId = contactId,
                title = "s1",
                messageCount = 0,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        coEvery { aiAdvisorRepository.getSessions(contactId) } returns Result.success(sessions)
        coEvery { contactRepository.deleteProfile(contactId) } returns Result.success(Unit)
        coEvery { clearAdvisorDraftUseCase(any()) } returns Result.failure(Exception("draft failed"))

        val result = useCase(contactId)

        assertTrue(result.isSuccess)
        coVerify { clearAdvisorDraftUseCase("session-1") }
    }
}
