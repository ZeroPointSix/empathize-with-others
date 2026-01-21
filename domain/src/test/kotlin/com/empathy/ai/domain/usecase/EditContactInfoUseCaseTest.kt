package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.EditResult
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.CoroutineDispatchers
import com.empathy.ai.domain.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * EditContactInfoUseCase unit tests.
 *
 * Covers contact info and avatar updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditContactInfoUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var dispatchers: CoroutineDispatchers
    private lateinit var logger: Logger
    private lateinit var useCase: EditContactInfoUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        logger = mockk(relaxed = true)
        val testDispatcher = UnconfinedTestDispatcher()
        dispatchers = object : CoroutineDispatchers {
            override val io = testDispatcher
            override val default = testDispatcher
            override val main = testDispatcher
        }
        useCase = EditContactInfoUseCase(
            contactRepository = contactRepository,
            contentValidator = ContentValidator(),
            dispatchers = dispatchers,
            logger = logger
        )
    }

    @Test
    fun `returns NoChanges when contact info is unchanged`() = runTest {
        // Given
        val contactId = "contact-1"
        val profile = ContactProfile(
            id = contactId,
            name = "Alice",
            targetGoal = "Goal",
            contactInfo = "123"
        )
        coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)

        // When
        val result = useCase.editContactInfo(contactId, "123")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NoChanges)
        coVerify(exactly = 0) { contactRepository.updateContactInfo(any(), any()) }
    }

    @Test
    fun `trims and saves updated contact info`() = runTest {
        // Given
        val contactId = "contact-2"
        val profile = ContactProfile(
            id = contactId,
            name = "Bob",
            targetGoal = "Goal",
            contactInfo = null
        )
        coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
        coEvery { contactRepository.updateContactInfo(contactId, "123") } returns 1

        // When
        val result = useCase.editContactInfo(contactId, " 123 ")

        // Then
        assertEquals(EditResult.Success, result.getOrNull())
        coVerify { contactRepository.updateContactInfo(contactId, "123") }
    }

    @Test
    fun `returns NoChanges when avatar is unchanged`() = runTest {
        // Given
        val contactId = "contact-3"
        val profile = ContactProfile(
            id = contactId,
            name = "Carol",
            targetGoal = "Goal",
            avatarUrl = "file:///avatar.jpg",
            avatarColorSeed = 2
        )
        coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)

        // When
        val result = useCase.editAvatar(contactId, "file:///avatar.jpg", 2)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() is EditResult.NoChanges)
        coVerify(exactly = 0) { contactRepository.updateAvatar(any(), any(), any()) }
    }

    @Test
    fun `returns Success when avatar update succeeds`() = runTest {
        // Given
        val contactId = "contact-4"
        val profile = ContactProfile(
            id = contactId,
            name = "Diana",
            targetGoal = "Goal",
            avatarUrl = "file:///old.jpg",
            avatarColorSeed = 1
        )
        coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
        coEvery { contactRepository.updateAvatar(contactId, "file:///new.jpg", 4) } returns 1

        // When
        val result = useCase.editAvatar(contactId, "file:///new.jpg", 4)

        // Then
        assertEquals(EditResult.Success, result.getOrNull())
        coVerify { contactRepository.updateAvatar(contactId, "file:///new.jpg", 4) }
    }
}
