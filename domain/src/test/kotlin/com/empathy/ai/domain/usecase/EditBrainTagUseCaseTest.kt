package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * EditBrainTagUseCase 单元测试
 *
 * ## 测试范围 (BUG-00066)
 * - 正常编辑标签
 * - 空内容验证
 * - 类型切换
 * - 错误处理
 *
 * @see EditBrainTagUseCase
 */
class EditBrainTagUseCaseTest {

    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var editBrainTagUseCase: EditBrainTagUseCase

    @Before
    fun setup() {
        brainTagRepository = mockk()
        editBrainTagUseCase = EditBrainTagUseCase(brainTagRepository)
    }

    @Test
    fun `编辑标签成功 - 修改内容`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "新的标签内容"
        val newType = TagType.RISK_RED

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            brainTagRepository.updateTag(match {
                it.id == tagId && it.content == newContent && it.type == newType
            })
        }
    }

    @Test
    fun `编辑标签成功 - 切换类型从雷区到策略`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "标签内容"
        val newType = TagType.STRATEGY_GREEN

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            brainTagRepository.updateTag(match { it.type == TagType.STRATEGY_GREEN })
        }
    }

    @Test
    fun `编辑标签成功 - 切换类型从策略到雷区`() = runTest {
        // Given
        val tagId = 2L
        val newContent = "标签内容"
        val newType = TagType.RISK_RED

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            brainTagRepository.updateTag(match { it.type == TagType.RISK_RED })
        }
    }

    @Test
    fun `编辑标签失败 - 内容为空`() = runTest {
        // Given
        val tagId = 1L
        val newContent = ""
        val newType = TagType.RISK_RED

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isFailure)
        assertEquals("标签内容不能为空", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { brainTagRepository.updateTag(any()) }
    }

    @Test
    fun `编辑标签失败 - 内容只有空格`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "   "
        val newType = TagType.RISK_RED

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isFailure)
        assertEquals("标签内容不能为空", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { brainTagRepository.updateTag(any()) }
    }

    @Test
    fun `编辑标签失败 - Repository返回错误`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "标签内容"
        val newType = TagType.RISK_RED
        val errorMessage = "数据库更新失败"

        coEvery { brainTagRepository.updateTag(any()) } returns Result.failure(Exception(errorMessage))

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isFailure)
        assertEquals(errorMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun `编辑标签成功 - 内容前后有空格会被trim`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "  标签内容  "
        val newType = TagType.RISK_RED

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            brainTagRepository.updateTag(match { it.content == "标签内容" })
        }
    }

    @Test
    fun `编辑标签成功 - 超长内容`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "这是一个非常长的标签内容".repeat(10)
        val newType = TagType.STRATEGY_GREEN

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `编辑标签成功 - 特殊字符内容`() = runTest {
        // Given
        val tagId = 1L
        val newContent = "标签@#\$%^&*()内容"
        val newType = TagType.RISK_RED

        coEvery { brainTagRepository.updateTag(any()) } returns Result.success(Unit)

        // When
        val result = editBrainTagUseCase(tagId, newContent, newType)

        // Then
        assertTrue(result.isSuccess)
    }
}
