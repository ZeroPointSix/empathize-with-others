package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FeedTextUseCase 单元测试
 *
 * 测试场景:
 * 1. AI 成功提取信息
 * 2. AI 调用失败，降级到 Mock 数据
 * 3. 隐私脱敏正常工作
 * 4. 空文本和异常输入处理
 */
class FeedTextUseCaseTest {

    private lateinit var aiRepository: AiRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var useCase: FeedTextUseCase

    @Before
    fun setup() {
        aiRepository = mockk()
        privacyRepository = mockk()
        useCase = FeedTextUseCase(
            aiRepository = aiRepository,
            privacyRepository = privacyRepository
        )
    }

    @Test
    fun `should successfully extract data from text`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "我生日是12月21日，喜欢阅读，是个工程师。不要催我，要多夸我衣品好。"
        val expectedExtractedData = ExtractedData(
            facts = mapOf(
                "生日" to "12月21日",
                "爱好" to "阅读",
                "职业" to "工程师"
            ),
            redTags = listOf("不要催"),
            greenTags = listOf("多夸衣品好")
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(3, extractedData.facts.size)
        assertEquals("12月21日", extractedData.facts["生日"])
        assertEquals("阅读", extractedData.facts["爱好"])
        assertEquals("工程师", extractedData.facts["职业"])
        assertEquals(1, extractedData.redTags.size)
        assertEquals("不要催", extractedData.redTags[0])
        assertEquals(1, extractedData.greenTags.size)
        assertEquals("多夸衣品好", extractedData.greenTags[0])
    }

    @Test
    fun `should fallback to mock data when AI call fails`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "一些测试文本"

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.failure(Exception("网络错误"))

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(3, extractedData.facts.size) // Mock 数据的默认 facts
        assertEquals("阅读", extractedData.facts["爱好"])
        assertEquals("工程师", extractedData.facts["职业"])
        assertEquals("12.21", extractedData.facts["生日"])
        assertEquals(2, extractedData.redTags.size) // Mock 数据的默认 redTags
        assertEquals("不要提前任", extractedData.redTags[0])
        assertEquals("不要催促付款", extractedData.redTags[1])
        assertEquals(2, extractedData.greenTags.size) // Mock 数据的默认 greenTags
        assertEquals("多夸衣品好", extractedData.greenTags[0])
        assertEquals("耐心倾听", extractedData.greenTags[1])
    }

    @Test
    fun `should handle empty input text`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = ""

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(
            ExtractedData(
                facts = emptyMap(),
                redTags = emptyList(),
                greenTags = emptyList()
            )
        )

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertTrue(extractedData.facts.isEmpty())
        assertTrue(extractedData.redTags.isEmpty())
        assertTrue(extractedData.greenTags.isEmpty())
    }

    @Test
    fun `should apply privacy masking before AI extraction`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "我的电话是13812345678，生日是12月21日"
        val maskedText = "我的电话是[PHONE]，生日是12月21日"
        val expectedExtractedData = ExtractedData(
            facts = mapOf("生日" to "12月21日"),
            redTags = emptyList(),
            greenTags = emptyList()
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(
            mapOf("13812345678" to "[PHONE]")
        )
        coEvery { aiRepository.extractTextInfo(maskedText) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(1, extractedData.facts.size)
        assertEquals("12月21日", extractedData.facts["生日"])

        // 验证 AI 收到的是脱敏后的文本
        coEvery { aiRepository.extractTextInfo(maskedText) } returns Result.success(expectedExtractedData)
    }

    @Test
    fun `should handle privacy mapping failure gracefully`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "我的电话是13812345678，生日是12月21日"
        val expectedExtractedData = ExtractedData(
            facts = mapOf("生日" to "12月21日"),
            redTags = emptyList(),
            greenTags = emptyList()
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.failure(Exception("隐私配置读取失败"))
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(1, extractedData.facts.size)
        assertEquals("12月21日", extractedData.facts["生日"])
    }

    @Test
    fun `should handle complex extraction results`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "我叫张三，是个软件工程师，平时喜欢爬山和看书。不喜欢别人催我，希望对方有耐心。生日在冬天。"
        val expectedExtractedData = ExtractedData(
            facts = mapOf(
                "姓名" to "张三",
                "职业" to "软件工程师",
                "爱好" to "爬山、看书",
                "生日季节" to "冬天"
            ),
            redTags = listOf("不要催促", "避免急躁"),
            greenTags = listOf("保持耐心", "讨论户外活动")
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(4, extractedData.facts.size)
        assertEquals("张三", extractedData.facts["姓名"])
        assertEquals("软件工程师", extractedData.facts["职业"])
        assertEquals("爬山、看书", extractedData.facts["爱好"])
        assertEquals("冬天", extractedData.facts["生日季节"])
        assertEquals(2, extractedData.redTags.size)
        assertTrue(extractedData.redTags.contains("不要催促"))
        assertTrue(extractedData.redTags.contains("避免急躁"))
        assertEquals(2, extractedData.greenTags.size)
        assertTrue(extractedData.greenTags.contains("保持耐心"))
        assertTrue(extractedData.greenTags.contains("讨论户外活动"))
    }

    @Test
    fun `should handle partial extraction results`() = runTest {
        // Given
        val contactId = "contact_1"
        val inputText = "今天天气不错"
        val expectedExtractedData = ExtractedData(
            facts = emptyMap(),
            redTags = emptyList(),
            greenTags = listOf("可以谈论天气")
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, inputText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertTrue(extractedData.facts.isEmpty())
        assertTrue(extractedData.redTags.isEmpty())
        assertEquals(1, extractedData.greenTags.size)
        assertEquals("可以谈论天气", extractedData.greenTags[0])
    }

    @Test
    fun `should handle very long input text`() = runTest {
        // Given
        val contactId = "contact_1"
        val longText = "这是一个很长的文本。" + "重复的内容。".repeat(1000)
        val expectedExtractedData = ExtractedData(
            facts = mapOf("文本长度" to "超长"),
            redTags = emptyList(),
            greenTags = emptyList()
        )

        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
        coEvery { aiRepository.extractTextInfo(any()) } returns Result.success(expectedExtractedData)

        // When
        val result = useCase(contactId, longText)

        // Then
        assertTrue(result.isSuccess)
        val extractedData = result.getOrNull()!!
        assertEquals(1, extractedData.facts.size)
        assertEquals("超长", extractedData.facts["文本长度"])
    }
}