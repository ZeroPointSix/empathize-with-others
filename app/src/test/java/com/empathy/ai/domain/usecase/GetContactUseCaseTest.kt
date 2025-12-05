package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * GetContactUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功获取联系人
 * 2. 联系人不存在的情况
 * 3. 仓库异常处理
 * 4. 空联系人ID验证
 */
class GetContactUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: GetContactUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = GetContactUseCase(contactRepository)
    }

    @Test
    fun `should return contact when repository succeeds`() = runTest {
        // Given
        val contactId = "contact_1"
        val expectedContact = ContactProfile(
            id = contactId,
            name = "测试联系人",
            targetGoal = "建立长期信任",
            contextDepth = 10,
            facts = emptyMap()
        )

        coEvery {
            contactRepository.getProfile(contactId)
        } returns Result.success(expectedContact)

        // When
        val result = useCase(contactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedContact, result.getOrNull())
    }

    @Test
    fun `should return null when contact not found`() = runTest {
        // Given
        val contactId = "nonexistent_contact"

        coEvery {
            contactRepository.getProfile(contactId)
        } returns Result.success(null)

        // When
        val result = useCase(contactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `should return failure when repository fails`() = runTest {
        // Given
        val contactId = "contact_1"
        val expectedError = Exception("网络错误")

        coEvery {
            contactRepository.getProfile(contactId)
        } returns Result.failure(expectedError)

        // When
        val result = useCase(contactId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
    }

    @Test
    fun `should return failure when contactId is blank`() = runTest {
        // Given
        val blankContactId = ""

        // When
        val result = useCase(blankContactId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when contactId is whitespace`() = runTest {
        // Given
        val whitespaceContactId = "   "

        // When
        val result = useCase(whitespaceContactId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }
}