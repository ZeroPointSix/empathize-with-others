package com.empathy.ai.data.repository

import com.empathy.ai.data.remote.api.OpenAiApi
import com.empathy.ai.data.remote.model.ChatResponseDto
import com.empathy.ai.data.remote.model.ChoiceDto
import com.empathy.ai.data.remote.model.MessageDto
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.repository.AiProviderRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SafetyCheckResult 解析增强测试
 * 
 * 测试目标：
 * - 验证标准格式解析
 * - 验证中文字段名解析
 * - 验证布尔类型转换（"true"/"false"、1/0、"是"/"否"）
 * - 验证默认值填充
 * - 验证降级策略
 */
class SafetyCheckResultEnhancementTest {

    private lateinit var api: OpenAiApi
    private lateinit var providerRepository: AiProviderRepository
    private lateinit var repository: AiRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        providerRepository = mockk()
        repository = AiRepositoryImpl(api, providerRepository)

        // Mock 默认服务商配置
        val mockProvider = AiProvider(
            id = "test-provider",
            name = "Test Provider",
            baseUrl = "https://api.test.com",
            apiKey = "test-key",
            models = listOf(
                com.empathy.ai.domain.model.AiModel(
                    id = "test-model",
                    displayName = "Test Model"
                )
            ),
            defaultModelId = "test-model",
            isDefault = true
        )
        coEvery { providerRepository.getDefaultProvider() } returns Result.success(mockProvider)
    }

    /**
     * 测试标准格式解析
     */
    @Test
    fun `test standard format parsing`() = runTest {
        // 标准 JSON 格式
        val standardJson = """
            {
              "isSafe": false,
              "triggeredRisks": ["不要提钱", "避免谈论前任"],
              "suggestion": "建议删除敏感内容"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = standardJson),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf("不要提钱"))

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
        assertEquals(2, safetyCheck.triggeredRisks.size)
        assertTrue(safetyCheck.triggeredRisks.contains("不要提钱"))
        assertEquals("建议删除敏感内容", safetyCheck.suggestion)
    }

    /**
     * 测试中文字段名解析
     */
    @Test
    fun `test chinese field names parsing`() = runTest {
        // 中文字段名 JSON
        val chineseJson = """
            {
              "是否安全": false,
              "触发的风险": ["不要提钱", "避免谈论前任"],
              "建议": "建议删除敏感内容"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = chineseJson),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf("不要提钱"))

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
        assertEquals(2, safetyCheck.triggeredRisks.size)
        assertEquals("建议删除敏感内容", safetyCheck.suggestion)
    }

    /**
     * 测试布尔类型转换 - 字符串 "true"/"false"
     */
    @Test
    fun `test boolean conversion - string true false`() = runTest {
        val jsonWithStringBoolean = """
            {
              "isSafe": "false",
              "triggeredRisks": ["风险1"],
              "suggestion": "建议修改"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = jsonWithStringBoolean),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
    }

    /**
     * 测试布尔类型转换 - 数字 1/0
     */
    @Test
    fun `test boolean conversion - number 1 0`() = runTest {
        val jsonWithNumberBoolean = """
            {
              "isSafe": 0,
              "triggeredRisks": ["风险1"],
              "suggestion": "建议修改"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = jsonWithNumberBoolean),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
    }

    /**
     * 测试布尔类型转换 - 中文 "是"/"否"
     */
    @Test
    fun `test boolean conversion - chinese yes no`() = runTest {
        val jsonWithChineseBoolean = """
            {
              "是否安全": "否",
              "触发的风险": ["风险1"],
              "建议": "建议修改"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = jsonWithChineseBoolean),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
    }

    /**
     * 测试默认值填充 - triggeredRisks 缺失
     */
    @Test
    fun `test default value filling - missing triggeredRisks`() = runTest {
        val jsonWithoutTriggeredRisks = """
            {
              "isSafe": true,
              "suggestion": "检查通过"
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = jsonWithoutTriggeredRisks),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertTrue(safetyCheck!!.isSafe)
        assertTrue(safetyCheck.triggeredRisks.isEmpty())
        assertEquals("检查通过", safetyCheck.suggestion)
    }

    /**
     * 测试默认值填充 - suggestion 缺失
     */
    @Test
    fun `test default value filling - missing suggestion`() = runTest {
        val jsonWithoutSuggestion = """
            {
              "isSafe": true,
              "triggeredRisks": []
            }
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = jsonWithoutSuggestion),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        safetyCheck!!
        assertTrue(safetyCheck.isSafe)
        assertNotNull(safetyCheck.suggestion)
        assertTrue(safetyCheck.suggestion!!.isNotBlank())
    }

    /**
     * 测试降级策略 - 完全无法解析时使用默认值
     */
    @Test
    fun `test fallback strategy - use default values on parse failure`() = runTest {
        // 完全无效的 JSON
        val invalidJson = "这不是一个有效的JSON"

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = invalidJson),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        // 默认值应该是安全的（保守策略）
        assertTrue(safetyCheck!!.isSafe)
        assertTrue(safetyCheck.triggeredRisks.isEmpty())
        assertNotNull(safetyCheck.suggestion)
    }

    /**
     * 测试 Markdown 包裹的 JSON
     */
    @Test
    fun `test markdown wrapped json`() = runTest {
        val markdownJson = """
            ```json
            {
              "isSafe": false,
              "triggeredRisks": ["风险1"],
              "suggestion": "建议修改"
            }
            ```
        """.trimIndent()

        val mockResponse = ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = MessageDto(role = "assistant", content = markdownJson),
                    finishReason = "stop"
                )
            )
        )

        coEvery { 
            api.chatCompletion(any(), any(), any()) 
        } returns mockResponse

        val result = repository.checkDraftSafety("测试草稿", listOf())

        assertTrue(result.isSuccess)
        val safetyCheck = result.getOrNull()
        assertNotNull(safetyCheck)
        assertFalse(safetyCheck!!.isSafe)
    }
}
