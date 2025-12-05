package com.empathy.ai.domain.usecase

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
 * DeleteContactUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功删除联系人
 * 2. 仓库异常处理
 * 3. 空联系人ID验证
 */
class DeleteContactUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var useCase: DeleteContactUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        useCase = DeleteContactUseCase(contactRepository)
    }

    @Test
    fun `should return success when repository deletion succeeds`() = runTest {
        // Given
        val contactId = "contact_1"

        coEvery {
            contactRepository.deleteProfile(contactId)
        } returns Result.success(Unit)

        // When
        val result = useCase(contactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `should return failure when repository deletion fails`() = runTest {
        // Given
        val contactId = "contact_1"
        val expectedError = Exception("删除失败")

        coEvery {
            contactRepository.deleteProfile(contactId)
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

    @Test
    fun `should handle repository exception during deletion`() = runTest {
        // Given
        val contactId = "contact_1"
        val expectedException = RuntimeException("数据库连接失败")

        coEvery {
            contactRepository.deleteProfile(contactId)
        } throws expectedException

        // When
        val result = useCase(contactId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }
}