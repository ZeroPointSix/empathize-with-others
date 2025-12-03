package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AnalyzeChatUseCase 单元测试
 *
 * 测试场景:
 * 1. 正常流程 - 验证 Prompt 正确拼装
 * 2. 隐私脱敏 - 验证敏感信息被替换
 * 3. 空数据处理 - 验证没有 Facts 或 Tags 时不崩溃
 */
class AnalyzeChatUseCaseTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: AnalyzeChatUseCase

    @Before
    fun setup() {
        contactRepository = mockk()
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        settingsRepository = mockk()
        useCase = AnalyzeChatUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `should build prompt correctly with facts and tags`() = runTest {
        // Given - 准备测试数据
        val profile = ContactProfile(
            id = "contact_1",
            name = "测试联系人",
            targetGoal = "建立长期信任",
            contextDepth = 10,
            facts = mapOf(
                "爱好" to "钓鱼",
                "住址" to "朝阳区"
            )
        )

        val redTags = listOf(
            BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "不要提钱",
                type = TagType.RISK_RED
            )
        )

        val greenTags = listOf(
            BrainTag(
                id = 2,
                contactId = "contact_1",
                content = "多夸他衣品好",
                type = TagType.STRATEGY_GREEN
            )
        )

        val allTags = redTags + greenTags

        // Mock repository 行为
        coEvery { settingsRepository.getApiKey() } returns Result.success("test_api_key")
        coEvery { contactRepository.getProfile("contact_1") } returns Result.success(profile)
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(allTags)
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())

        // Mock AI 返回
        val expectedResult = AnalysisResult(
            replySuggestion = "测试回复",
            strategyAnalysis = "测试分析",
            riskLevel = RiskLevel.SAFE
        )
        coEvery { aiRepository.analyzeChat(any(), any()) } returns Result.success(expectedResult)

        val rawContext = listOf("你好", "最近怎么样")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then
        assertTrue(result.isSuccess)

        // 捕获传递给 AI 的 prompt
        val promptSlot = slot<String>()
        coVerify {
            aiRepository.analyzeChat(
                promptContext = capture(promptSlot),
                systemInstruction = any()
            )
        }

        val prompt = promptSlot.captured
        // 验证 Prompt 包含关键信息
        assertTrue("Prompt 应包含目标", prompt.contains("建立长期信任"))
        assertTrue("Prompt 应包含事实", prompt.contains("爱好: 钓鱼"))
        assertTrue("Prompt 应包含事实", prompt.contains("住址: 朝阳区"))
        assertTrue("Prompt 应包含雷区", prompt.contains("不要提钱"))
        assertTrue("Prompt 应包含策略", prompt.contains("多夸他衣品好"))
        assertTrue("Prompt 应包含聊天记录", prompt.contains("你好"))
        assertTrue("Prompt 应包含聊天记录", prompt.contains("最近怎么样"))
    }

    @Test
    fun `should mask sensitive information before sending to AI`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_1",
            name = "张三",
            targetGoal = "测试",
            facts = emptyMap()
        )

        val privacyMapping = mapOf(
            "张三" to "[NAME_01]",
            "13800138000" to "[PHONE_01]"
        )

        coEvery { settingsRepository.getApiKey() } returns Result.success("test_api_key")
        coEvery { contactRepository.getProfile("contact_1") } returns Result.success(profile)
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(emptyList())
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(privacyMapping)

        val expectedResult = AnalysisResult(
            replySuggestion = "测试",
            strategyAnalysis = "测试",
            riskLevel = RiskLevel.SAFE
        )
        coEvery { aiRepository.analyzeChat(any(), any()) } returns Result.success(expectedResult)

        val rawContext = listOf("我是张三", "我的电话是13800138000")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then
        assertTrue(result.isSuccess)

        // 验证传递给 AI 的内容已脱敏
        val promptSlot = slot<String>()
        coVerify {
            aiRepository.analyzeChat(
                promptContext = capture(promptSlot),
                systemInstruction = any()
            )
        }

        val prompt = promptSlot.captured
        assertTrue("应包含脱敏后的名称", prompt.contains("[NAME_01]"))
        assertTrue("应包含脱敏后的电话", prompt.contains("[PHONE_01]"))
        assertTrue("不应包含真实姓名", !prompt.contains("张三"))
        assertTrue("不应包含真实电话", !prompt.contains("13800138000"))
    }

    @Test
    fun `should handle empty facts and tags gracefully`() = runTest {
        // Given - 没有 Facts 和 Tags
        val profile = ContactProfile(
            id = "contact_1",
            name = "测试",
            targetGoal = "测试目标",
            facts = emptyMap()
        )

        coEvery { settingsRepository.getApiKey() } returns Result.success("test_api_key")
        coEvery { contactRepository.getProfile("contact_1") } returns Result.success(profile)
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(emptyList())
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())

        val expectedResult = AnalysisResult(
            replySuggestion = "测试",
            strategyAnalysis = "测试",
            riskLevel = RiskLevel.SAFE
        )
        coEvery { aiRepository.analyzeChat(any(), any()) } returns Result.success(expectedResult)

        val rawContext = listOf("你好")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then - 应该成功，不崩溃
        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
    }

    @Test
    fun `should return failure when API key is missing`() = runTest {
        // Given - 没有 API Key
        coEvery { settingsRepository.getApiKey() } returns Result.success(null)

        val rawContext = listOf("你好")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception!!.message!!.contains("未配置 API Key"))
    }

    @Test
    fun `should return failure when contact profile not found`() = runTest {
        // Given - 联系人不存在
        coEvery { settingsRepository.getApiKey() } returns Result.success("test_api_key")
        coEvery { contactRepository.getProfile("contact_1") } returns Result.success(null)

        val rawContext = listOf("你好")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception!!.message!!.contains("未找到联系人画像"))
    }

    @Test
    fun `should respect context depth limit`() = runTest {
        // Given
        val profile = ContactProfile(
            id = "contact_1",
            name = "测试",
            targetGoal = "测试",
            contextDepth = 3, // 限制只读取最近 3 条
            facts = emptyMap()
        )

        coEvery { settingsRepository.getApiKey() } returns Result.success("test_api_key")
        coEvery { contactRepository.getProfile("contact_1") } returns Result.success(profile)
        coEvery { brainTagRepository.getTagsForContact("contact_1") } returns flowOf(emptyList())
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())

        val expectedResult = AnalysisResult(
            replySuggestion = "测试",
            strategyAnalysis = "测试",
            riskLevel = RiskLevel.SAFE
        )
        coEvery { aiRepository.analyzeChat(any(), any()) } returns Result.success(expectedResult)

        // 提供 5 条消息，但只应该取最近 3 条
        val rawContext = listOf("msg1", "msg2", "msg3", "msg4", "msg5")

        // When
        val result = useCase(contactId = "contact_1", rawScreenContext = rawContext)

        // Then
        assertTrue(result.isSuccess)

        val promptSlot = slot<String>()
        coVerify {
            aiRepository.analyzeChat(
                promptContext = capture(promptSlot),
                systemInstruction = any()
            )
        }

        val prompt = promptSlot.captured
        // 应该只包含最后 3 条消息
        assertTrue("Should contain msg3", prompt.contains("msg3"))
        assertTrue("Should contain msg4", prompt.contains("msg4"))
        assertTrue("Should contain msg5", prompt.contains("msg5"))
        // 不应该包含前面的消息
        assertFalse("Should not contain msg1", prompt.contains("msg1"))
    }
}
