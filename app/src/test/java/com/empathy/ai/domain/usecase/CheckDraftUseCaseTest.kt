package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CheckDraftUseCase 单元测试
 *
 * 测试场景:
 * 1. 命中雷区 -> 返回 DANGER
 * 2. 安全通过 -> 返回 SAFE
 * 3. 忽略大小写
 */
class CheckDraftUseCaseTest {

    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var useCase: CheckDraftUseCase

    @Before
    fun setup() {
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        useCase = CheckDraftUseCase(
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository
        )
    }

    @Test
    fun `should return DANGER when draft contains red flag`() = runTest {
        // Given - 配置雷区标签
        val redTags = listOf(
            BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "借钱",
                type = TagType.RISK_RED
            )
        )
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(redTags)

        val draft = "能不能借钱给我"

        // When
        val result = useCase(
            contactId = "contact_1",
            draftSnapshot = draft,
            enableDeepCheck = false
        )

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrNull()!!
        assertFalse(checkResult.isSafe)
        assertEquals(1, checkResult.triggeredRisks.size)
        assertTrue(checkResult.triggeredRisks.contains("借钱"))
    }

    @Test
    fun `should return SAFE when draft is safe`() = runTest {
        // Given
        val redTags = listOf(
            BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "借钱",
                type = TagType.RISK_RED
            )
        )
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(redTags)

        val draft = "吃饭了吗"

        // When
        val result = useCase(
            contactId = "contact_1",
            draftSnapshot = draft,
            enableDeepCheck = false
        )

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrNull()!!
        assertTrue(checkResult.isSafe)
        assertEquals(0, checkResult.triggeredRisks.size)
    }

    @Test
    fun `should be case insensitive`() = runTest {
        // Given
        val redTags = listOf(
            BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "Money",
                type = TagType.RISK_RED
            )
        )
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(redTags)

        val draft = "I need money"

        // When
        val result = useCase(
            contactId = "contact_1",
            draftSnapshot = draft,
            enableDeepCheck = false
        )

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrNull()!!
        assertFalse(checkResult.isSafe)
        assertTrue(checkResult.triggeredRisks.contains("Money"))
    }

    @Test
    fun `should return SAFE when no red tags exist`() = runTest {
        // Given - 没有雷区标签
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(emptyList())

        val draft = "任意内容"

        // When
        val result = useCase(
            contactId = "contact_1",
            draftSnapshot = draft,
            enableDeepCheck = false
        )

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrNull()!!
        assertTrue(checkResult.isSafe)
    }

    @Test
    fun `should detect multiple red flags`() = runTest {
        // Given
        val redTags = listOf(
            BrainTag(id = 1, contactId = "contact_1", content = "借钱", type = TagType.RISK_RED),
            BrainTag(id = 2, contactId = "contact_1", content = "前任", type = TagType.RISK_RED)
        )
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(redTags)

        val draft = "我想借钱，顺便问问你前任"

        // When
        val result = useCase(
            contactId = "contact_1",
            draftSnapshot = draft,
            enableDeepCheck = false
        )

        // Then
        assertTrue(result.isSuccess)
        val checkResult = result.getOrNull()!!
        assertFalse(checkResult.isSafe)
        assertEquals(2, checkResult.triggeredRisks.size)
        assertTrue(checkResult.triggeredRisks.contains("借钱"))
        assertTrue(checkResult.triggeredRisks.contains("前任"))
    }
}
