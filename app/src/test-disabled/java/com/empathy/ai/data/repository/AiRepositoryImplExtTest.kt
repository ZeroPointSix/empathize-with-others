package com.empathy.ai.data.repository

/**
 * AiRepositoryImpl扩展方法单元测试
 *
 * 测试覆盖 (TDD-00026/4.5.1):
 * - polishDraft() - 润色草稿功能，验证AI调用和JSON解析
 * - generateReply() - 生成回复功能，验证策略说明解析
 * - refineAnalysis() - 精炼分析功能，验证提示词构建
 * - refinePolish() - 精炼润色功能，验证提示词构建
 * - refineReply() - 精炼回复功能，验证提示词构建
 * - Fallback机制 - JSON解析失败时的降级处理
 *
 * 业务规则 (PRD-00026/3.3.1):
 * - AI军师需支持多种分析场景的提示词定制
 * - JSON解析失败时不应崩溃，应有Fallback机制
 *
 * 任务追踪:
 * - TD-00009 T040 - 测试新增的polishDraft、generateReply和refine方法
 */
class AiRepositoryImplExtTest {

    private lateinit var openAiApi: OpenAiApi
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: AiRepositoryImpl

    private val testProvider = AiProvider(
        id = "test-provider",
        name = "TestProvider",
        baseUrl = "https://api.test.com",
        apiKey = "test-api-key",
        models = listOf(AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")),
        defaultModelId = "gpt-3.5-turbo",
        isDefault = true
    )

    @Before
    fun setup() {
        openAiApi = mockk()
        settingsRepository = mockk()
        repository = AiRepositoryImpl(openAiApi, settingsRepository)
    }

    // ==================== polishDraft 测试 ====================

    @Test
    fun `polishDraft 成功返回润色结果`() = runTest {
        // Given
        val draft = "你好"
        val systemInstruction = "请润色以下内容"
        val polishJson = """{"polishedText":"您好","hasRisk":false,"riskWarning":null}"""
        val response = createMockResponse(polishJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.polishDraft(testProvider, draft, systemInstruction)

        // Then
        assertTrue(result.isSuccess)
        val polishResult = result.getOrNull()
        assertNotNull(polishResult)
        assertEquals("您好", polishResult?.polishedText)
        assertEquals(false, polishResult?.hasRisk)
    }

    @Test
    fun `polishDraft 检测到风险时返回风险提示`() = runTest {
        // Given
        val draft = "敏感内容"
        val systemInstruction = "请润色以下内容"
        val polishJson = """{"polishedText":"修改后的内容","hasRisk":true,"riskWarning":"检测到敏感词汇"}"""
        val response = createMockResponse(polishJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.polishDraft(testProvider, draft, systemInstruction)

        // Then
        assertTrue(result.isSuccess)
        val polishResult = result.getOrNull()
        assertNotNull(polishResult)
        assertEquals(true, polishResult?.hasRisk)
        assertEquals("检测到敏感词汇", polishResult?.riskWarning)
    }

    @Test
    fun `polishDraft JSON解析失败时使用Fallback`() = runTest {
        // Given
        val draft = "你好"
        val systemInstruction = "请润色以下内容"
        val invalidJson = "这不是有效的JSON，但是一个有效的回复"
        val response = createMockResponse(invalidJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.polishDraft(testProvider, draft, systemInstruction)

        // Then
        // Fallback机制会将原始内容作为润色结果
        assertTrue(result.isSuccess)
        val polishResult = result.getOrNull()
        assertNotNull(polishResult)
        // Fallback会提取文本内容
        assertTrue(polishResult?.polishedText?.isNotBlank() == true)
    }

    @Test
    fun `polishDraft 网络异常时返回失败`() = runTest {
        // Given
        val draft = "你好"
        val systemInstruction = "请润色以下内容"
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } throws Exception("网络错误")

        // When
        val result = repository.polishDraft(testProvider, draft, systemInstruction)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== generateReply 测试 ====================

    @Test
    fun `generateReply 成功返回回复结果`() = runTest {
        // Given
        val message = "对方说你好"
        val systemInstruction = "请生成回复"
        val replyJson = """{"suggestedReply":"你好呀！","strategyNote":"保持友好"}"""
        val response = createMockResponse(replyJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.generateReply(testProvider, message, systemInstruction)

        // Then
        assertTrue(result.isSuccess)
        val replyResult = result.getOrNull()
        assertNotNull(replyResult)
        assertEquals("你好呀！", replyResult?.suggestedReply)
        assertEquals("保持友好", replyResult?.strategyNote)
    }

    @Test
    fun `generateReply 无策略说明时正常返回`() = runTest {
        // Given
        val message = "对方说你好"
        val systemInstruction = "请生成回复"
        val replyJson = """{"suggestedReply":"你好！","strategyNote":null}"""
        val response = createMockResponse(replyJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.generateReply(testProvider, message, systemInstruction)

        // Then
        assertTrue(result.isSuccess)
        val replyResult = result.getOrNull()
        assertNotNull(replyResult)
        assertEquals("你好！", replyResult?.suggestedReply)
        assertEquals(null, replyResult?.strategyNote)
    }

    @Test
    fun `generateReply JSON解析失败时使用Fallback`() = runTest {
        // Given
        val message = "对方说你好"
        val systemInstruction = "请生成回复"
        val invalidJson = "建议回复：你好啊！"
        val response = createMockResponse(invalidJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.generateReply(testProvider, message, systemInstruction)

        // Then
        // Fallback机制会将原始内容作为回复建议
        assertTrue(result.isSuccess)
        val replyResult = result.getOrNull()
        assertNotNull(replyResult)
        assertTrue(replyResult?.suggestedReply?.isNotBlank() == true)
    }

    @Test
    fun `generateReply 网络异常时返回失败`() = runTest {
        // Given
        val message = "对方说你好"
        val systemInstruction = "请生成回复"
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } throws Exception("网络错误")

        // When
        val result = repository.generateReply(testProvider, message, systemInstruction)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== refineAnalysis 测试 ====================

    @Test
    fun `refineAnalysis 正确调用AI`() = runTest {
        // Given
        val refinementPrompt = "请更详细分析：原始内容"
        val analysisJson = """{"riskLevel":"SAFE","strategyAnalysis":"详细分析","replySuggestion":"建议回复"}"""
        val response = createMockResponse(analysisJson)
        val requestSlot = slot<ChatRequestDto>()
        coEvery { openAiApi.chatCompletion(any(), any(), capture(requestSlot)) } returns response

        // When
        val result = repository.refineAnalysis(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        // 验证请求中包含微调提示词
        val capturedRequest = requestSlot.captured
        val userMessage = capturedRequest.messages.find { it.role == "user" }
        assertNotNull(userMessage)
        assertTrue(userMessage?.content?.contains("请更详细分析") == true)
    }

    @Test
    fun `refineAnalysis 返回正确的分析结果`() = runTest {
        // Given
        val refinementPrompt = "请更详细分析"
        val analysisJson = """{"riskLevel":"WARNING","strategyAnalysis":"需要注意","replySuggestion":"谨慎回复"}"""
        val response = createMockResponse(analysisJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.refineAnalysis(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        val analysisResult = result.getOrNull()
        assertNotNull(analysisResult)
        assertEquals("谨慎回复", analysisResult?.replySuggestion)
    }

    // ==================== refinePolish 测试 ====================

    @Test
    fun `refinePolish 正确调用AI`() = runTest {
        // Given
        val refinementPrompt = "请更正式一些：原始内容"
        val polishJson = """{"polishedText":"正式的内容","hasRisk":false,"riskWarning":null}"""
        val response = createMockResponse(polishJson)
        val requestSlot = slot<ChatRequestDto>()
        coEvery { openAiApi.chatCompletion(any(), any(), capture(requestSlot)) } returns response

        // When
        val result = repository.refinePolish(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        val capturedRequest = requestSlot.captured
        val userMessage = capturedRequest.messages.find { it.role == "user" }
        assertNotNull(userMessage)
        assertTrue(userMessage?.content?.contains("请更正式一些") == true)
    }

    @Test
    fun `refinePolish 返回正确的润色结果`() = runTest {
        // Given
        val refinementPrompt = "请更正式一些"
        val polishJson = """{"polishedText":"尊敬的先生/女士，您好","hasRisk":false,"riskWarning":null}"""
        val response = createMockResponse(polishJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.refinePolish(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        val polishResult = result.getOrNull()
        assertNotNull(polishResult)
        assertEquals("尊敬的先生/女士，您好", polishResult?.polishedText)
    }

    // ==================== refineReply 测试 ====================

    @Test
    fun `refineReply 正确调用AI`() = runTest {
        // Given
        val refinementPrompt = "请更幽默一些：原始回复"
        val replyJson = """{"suggestedReply":"幽默的回复","strategyNote":"使用幽默策略"}"""
        val response = createMockResponse(replyJson)
        val requestSlot = slot<ChatRequestDto>()
        coEvery { openAiApi.chatCompletion(any(), any(), capture(requestSlot)) } returns response

        // When
        val result = repository.refineReply(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        val capturedRequest = requestSlot.captured
        val userMessage = capturedRequest.messages.find { it.role == "user" }
        assertNotNull(userMessage)
        assertTrue(userMessage?.content?.contains("请更幽默一些") == true)
    }

    @Test
    fun `refineReply 返回正确的回复结果`() = runTest {
        // Given
        val refinementPrompt = "请更幽默一些"
        val replyJson = """{"suggestedReply":"哈哈，你好呀！","strategyNote":"轻松幽默"}"""
        val response = createMockResponse(replyJson)
        coEvery { openAiApi.chatCompletion(any(), any(), any()) } returns response

        // When
        val result = repository.refineReply(testProvider, refinementPrompt)

        // Then
        assertTrue(result.isSuccess)
        val replyResult = result.getOrNull()
        assertNotNull(replyResult)
        assertEquals("哈哈，你好呀！", replyResult?.suggestedReply)
        assertEquals("轻松幽默", replyResult?.strategyNote)
    }

    // ==================== 辅助方法 ====================

    private fun createMockResponse(content: String): ChatResponseDto {
        return ChatResponseDto(
            id = "test-id",
            choices = listOf(
                ChoiceDto(
                    index = 0,
                    message = ResponseMessageDto(
                        role = "assistant",
                        content = content,
                        toolCalls = null
                    ),
                    finishReason = "stop"
                )
            ),
            usage = null
        )
    }
}
