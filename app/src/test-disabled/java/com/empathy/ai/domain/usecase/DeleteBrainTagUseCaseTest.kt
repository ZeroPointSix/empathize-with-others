package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.BrainTagRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * DeleteBrainTagUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功删除标签
 * 2. 仓库异常处理
 * 3. 无效标签ID验证
 */
class DeleteBrainTagUseCaseTest {

    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var useCase: DeleteBrainTagUseCase

    @Before
    fun setup() {
        brainTagRepository = mockk()
        useCase = DeleteBrainTagUseCase(brainTagRepository)
    }

    @Test
    fun `should return success when repository deletion succeeds`() = runTest {
        // Given
        val tagId = 123L

        coEvery {
            brainTagRepository.deleteTag(tagId)
        } returns Result.success(Unit)

        // When
        val result = useCase(tagId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `should return failure when repository deletion fails`() = runTest {
        // Given
        val tagId = 123L
        val expectedError = Exception("删除失败")

        coEvery {
            brainTagRepository.deleteTag(tagId)
        } returns Result.failure(expectedError)

        // When
        val result = useCase(tagId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
    }

    @Test
    fun `should return failure when tagId is zero`() = runTest {
        // Given
        val invalidTagId = 0L

        // When
        val result = useCase(invalidTagId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("标签ID无效", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when tagId is negative`() = runTest {
        // Given
        val invalidTagId = -1L

        // When
        val result = useCase(invalidTagId)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("标签ID无效", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should handle repository exception during deletion`() = runTest {
        // Given
        val tagId = 123L
        val expectedException = RuntimeException("数据库连接失败")

        coEvery {
            brainTagRepository.deleteTag(tagId)
        } throws expectedException

        // When
        val result = useCase(tagId)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `should work with large tagId values`() = runTest {
        // Given
        val largeTagId = Long.MAX_VALUE

        coEvery {
            brainTagRepository.deleteTag(largeTagId)
        } returns Result.success(Unit)

        // When
        val result = useCase(largeTagId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }
}