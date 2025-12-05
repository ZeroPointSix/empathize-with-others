package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
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
 * SaveBrainTagUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功保存标签
 * 2. 仓库异常处理
 * 3. 空标签内容验证
 * 4. 空联系人ID验证
 */
class SaveBrainTagUseCaseTest {

    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var useCase: SaveBrainTagUseCase

    @Before
    fun setup() {
        brainTagRepository = mockk()
        useCase = SaveBrainTagUseCase(brainTagRepository)
    }

    @Test
    fun `should return tag id when repository save succeeds`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L, // 由数据库生成
            contactId = "contact_1",
            content = "雷区：金钱",
            type = TagType.RISK_RED,
            source = "MANUAL"
        )
        val expectedTagId = 123L

        coEvery {
            brainTagRepository.saveTag(brainTag)
        } returns Result.success(expectedTagId)

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedTagId, result.getOrNull())
    }

    @Test
    fun `should return failure when repository save fails`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "contact_1",
            content = "标签内容",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )
        val expectedError = Exception("保存失败")

        coEvery {
            brainTagRepository.saveTag(brainTag)
        } returns Result.failure(expectedError)

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
    }

    @Test
    fun `should return failure when tag content is blank`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "contact_1",
            content = "",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("标签内容不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when tag content is whitespace`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "contact_1",
            content = "   ",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("标签内容不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when contactId is blank`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "",
            content = "标签内容",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should return failure when contactId is whitespace`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "   ",
            content = "标签内容",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("联系人ID不能为空", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should handle repository exception during save`() = runTest {
        // Given
        val brainTag = BrainTag(
            id = 0L,
            contactId = "contact_1",
            content = "标签内容",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )
        val expectedException = RuntimeException("数据库连接失败")

        coEvery {
            brainTagRepository.saveTag(brainTag)
        } throws expectedException

        // When
        val result = useCase(brainTag)

        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }
}