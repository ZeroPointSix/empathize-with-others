package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.KnowledgeQueryRequest
import com.empathy.ai.domain.model.KnowledgeQueryResponse
import com.empathy.ai.domain.model.Recommendation
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.PromptBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QueryKnowledgeUseCase 单元测试
 *
 * 测试知识查询用例的核心逻辑：
 * - 查询内容验证
 * - AI服务商配置检查
 * - 知识查询调用
 * - 错误处理
 *
 * @see PRD-00031 悬浮窗快速知识回答功能需求
 * @see TDD-00031 悬浮窗快速知识回答功能技术设计
 */
class QueryKnowledgeUseCaseTest {

    private lateinit var aiRepository: AiRepository
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var promptBuilder: PromptBuilder
    private lateinit var logger: Logger
    private lateinit var useCase: QueryKnowledgeUseCase

    private val testProvider = AiProvider(
        id = "test-provider",
        name = "Test Provider",
        baseUrl = "https://api.test.com",
        apiKey = "test-key",
        models = listOf(
            AiModel(
                id = "test-model",
                displayName = "Test Model"
            )
        ),
        defaultModelId = "test-model",
        temperature = 0.7f,
        maxTokens = 1000
    )

    private val testResponse = KnowledgeQueryResponse(
        title = "测试标题",
        content = "这是测试内容",
        source = "AI知识库",
        sourceTime = null,
        isFromNetwork = false,
        recommendations = listOf(
            Recommendation.fromTitle("相关话题1"),
            Recommendation.fromTitle("相关话题2")
        )
    )

    @Before
    fun setup() {
        aiRepository = mockk()
        aiProviderRepository = mockk()
        promptBuilder = mockk()
        logger = mockk(relaxed = true)

        every { promptBuilder.buildKnowledgePrompt() } returns "系统提示词"

        useCase = QueryKnowledgeUseCase(
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            logger = logger
        )
    }

    @Test
    fun `查询成功时返回知识响应`() = runTest {
        // Given
        val request = KnowledgeQueryRequest(content = "什么是Kotlin?")
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { 
            aiRepository.queryKnowledge(any(), any(), any()) 
        } returns Result.success(testResponse)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("测试标题", result.getOrNull()?.title)
        assertEquals("这是测试内容", result.getOrNull()?.content)
        assertEquals(2, result.getOrNull()?.recommendations?.size)
    }

    @Test
    fun `查询内容为空时返回验证错误`() = runTest {
        // Given
        val request = KnowledgeQueryRequest(content = "")

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("不能为空") == true)
    }

    @Test
    fun `查询内容超出长度限制时返回验证错误`() = runTest {
        // Given
        val longContent = "a".repeat(600) // 超过500字符限制
        val request = KnowledgeQueryRequest(content = longContent)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("超出") == true)
    }

    @Test
    fun `未配置AI服务商时返回配置错误`() = runTest {
        // Given
        val request = KnowledgeQueryRequest(content = "什么是Kotlin?")
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("配置") == true)
    }

    @Test
    fun `AI调用失败时返回错误`() = runTest {
        // Given
        val request = KnowledgeQueryRequest(content = "什么是Kotlin?")
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { 
            aiRepository.queryKnowledge(any(), any(), any()) 
        } returns Result.failure(Exception("网络错误"))

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertEquals("网络错误", result.exceptionOrNull()?.message)
    }

    @Test
    fun `简化调用方式正常工作`() = runTest {
        // Given
        val content = "什么是Kotlin?"
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { 
            aiRepository.queryKnowledge(any(), any(), any()) 
        } returns Result.success(testResponse)

        // When
        val result = useCase(content)

        // Then
        assertTrue(result.isSuccess)
        coVerify { aiRepository.queryKnowledge(testProvider, content, "系统提示词") }
    }

    @Test
    fun `查询内容会被清理和截断`() = runTest {
        // Given
        val contentWithSpaces = "  什么是Kotlin?  "
        val request = KnowledgeQueryRequest(content = contentWithSpaces)
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
        coEvery { 
            aiRepository.queryKnowledge(any(), any(), any()) 
        } returns Result.success(testResponse)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        coVerify { aiRepository.queryKnowledge(testProvider, "什么是Kotlin?", any()) }
    }
}
