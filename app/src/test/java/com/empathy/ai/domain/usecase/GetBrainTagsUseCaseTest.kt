package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.BrainTagRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * GetBrainTagsUseCase 单元测试
 *
 * 测试覆盖：
 * 1. 成功获取标签列表
 * 2. 空标签列表的情况
 * 3. 空联系人ID返回空Flow
 * 4. Flow响应式数据流
 */
class GetBrainTagsUseCaseTest {

    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var useCase: GetBrainTagsUseCase

    @Before
    fun setup() {
        brainTagRepository = mockk()
        useCase = GetBrainTagsUseCase(brainTagRepository)
    }

    @Test
    fun `should return tag list flow when repository provides data`() = runTest {
        // Given
        val contactId = "contact_1"
        val expectedTags = listOf(
            BrainTag(
                id = 1L,
                contactId = contactId,
                content = "雷区：金钱",
                type = TagType.RISK_RED,
                source = "MANUAL"
            ),
            BrainTag(
                id = 2L,
                contactId = contactId,
                content = "策略：夸她独立",
                type = TagType.STRATEGY_GREEN,
                source = "AI_AUTO"
            )
        )

        every {
            brainTagRepository.getTagsForContact(contactId)
        } returns flowOf(expectedTags)

        // When
        val result = useCase(contactId).toList()

        // Then
        assertEquals(1, result.size) // Flow只发射一次
        assertEquals(expectedTags, result.first())
        assertEquals(2, result.first().size)
    }

    @Test
    fun `should return empty list flow when repository provides empty data`() = runTest {
        // Given
        val contactId = "contact_1"
        val emptyTags = emptyList<BrainTag>()

        every {
            brainTagRepository.getTagsForContact(contactId)
        } returns flowOf(emptyTags)

        // When
        val result = useCase(contactId).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result.first().isEmpty())
    }

    @Test
    fun `should return empty list flow when contactId is blank`() = runTest {
        // Given
        val blankContactId = ""

        // When
        val result = useCase(blankContactId).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result.first().isEmpty())
    }

    @Test
    fun `should return empty list flow when contactId is whitespace`() = runTest {
        // Given
        val whitespaceContactId = "   "

        // When
        val result = useCase(whitespaceContactId).toList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result.first().isEmpty())
    }

    @Test
    fun `should return tag list with single tag`() = runTest {
        // Given
        val contactId = "contact_1"
        val singleTag = BrainTag(
            id = 1L,
            contactId = contactId,
            content = "单个标签",
            type = TagType.STRATEGY_GREEN,
            source = "MANUAL"
        )

        every {
            brainTagRepository.getTagsForContact(contactId)
        } returns flowOf(listOf(singleTag))

        // When
        val result = useCase(contactId).toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(1, result.first().size)
        assertEquals(singleTag, result.first().first())
    }
}