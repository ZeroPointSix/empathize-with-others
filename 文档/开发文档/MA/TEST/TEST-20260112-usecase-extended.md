#### 测试 3：联系人不存在时返回失败
**测试意图**：确保联系人画像缺失时停止执行，避免构建上下文与 AI 调用。

**我的判断**：✅ 建议保留

**判断理由**：这是数据边界的核心约束，稳定且可防止空值传播。

**完整测试代码**：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/AnalyzeChatUseCaseTest.kt
@Test
fun `联系人不存在时返回失败`() = runTest {
    // Given
    coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
    coEvery { contactRepository.getProfile(contactId) } returns Result.success(null)

    // When
    val result = useCase(contactId, listOf("hello"))

    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("未找到联系人画像") == true)
    coVerify(exactly = 0) { aiRepository.analyzeChat(any(), any(), any()) }
}
```

#### 测试 4：关闭脱敏时保留原文并使用去重截断输入
**测试意图**：验证脱敏关闭时不调用 `maskText`，输入去重/截断逻辑有效，保存与 AI 调用使用正确身份前缀。

**我的判断**：✅ 建议保留

**判断理由**：这是核心输入规范与安全开关行为，覆盖了高风险边界组合，防止回归。

**完整测试代码**：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/AnalyzeChatUseCaseTest.kt
@Test
fun `关闭脱敏时保留原文并使用去重截断输入`() = runTest {
    // Given
    val rawContext = listOf("A", "A", "B", "C")
    val analysisResult = AnalysisResult(
        replySuggestion = "reply",
        strategyAnalysis = "analysis",
        riskLevel = RiskLevel.SAFE
    )
    val savedInputSlot = slot<String>()
    val runtimeSlot = slot<String>()
    val promptContextSlot = slot<PromptContext>()
    val promptContextForAiSlot = slot<String>()

    coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
    coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
    every { brainTagRepository.getTagsForContact(contactId) } returns flowOf(listOf(redTag))
    coEvery { settingsRepository.getDataMaskingEnabled() } returns Result.success(false)
    coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)
    coEvery {
        conversationRepository.saveUserInput(contactId, capture(savedInputSlot), any())
    } returns Result.success(99L)
    coEvery { userProfileContextBuilder.buildAnalysisContext(profile, any()) } returns
        Result.success("PROFILE")
    coEvery { topicRepository.getActiveTopic(contactId) } returns null
    coEvery {
        promptBuilder.buildWithTopic(
            scene = PromptScene.ANALYZE,
            contactId = contactId,
            context = capture(promptContextSlot),
            topic = any(),
            runtimeData = capture(runtimeSlot)
        )
    } returns "instruction"
    coEvery {
        aiRepository.analyzeChat(
            provider = testProvider,
            promptContext = capture(promptContextForAiSlot),
            systemInstruction = "instruction"
        )
    } returns Result.success(analysisResult)
    coEvery { conversationRepository.updateAiResponse(any(), any()) } returns Result.success(Unit)
    coEvery { contactRepository.updateLastInteractionDate(any(), any()) } returns Result.success(Unit)

    // When
    val result = useCase(contactId, rawContext)

    // Then
    assertTrue(result.isSuccess)
    assertEquals(
        "${IdentityPrefixHelper.PREFIX_CONTACT}B\nC",
        savedInputSlot.captured
    )
    assertTrue(runtimeSlot.captured.contains("PROFILE"))
    assertTrue(runtimeSlot.captured.contains("【聊天记录】"))
    assertTrue(runtimeSlot.captured.contains("${IdentityPrefixHelper.PREFIX_CONTACT}B"))
    assertTrue(runtimeSlot.captured.contains("${IdentityPrefixHelper.PREFIX_CONTACT}C"))
    assertEquals(runtimeSlot.captured, promptContextForAiSlot.captured)
    coVerify(exactly = 0) { privacyRepository.maskText(any()) }
    coVerify(exactly = 1) { conversationRepository.updateAiResponse(99L, any()) }
    coVerify(exactly = 1) { contactRepository.updateLastInteractionDate(contactId, any()) }
}
```
#### 测试 5：AI 分析失败时返回失败且不更新 AI 回复或互动日期
**测试意图**：验证 AI 调用异常时走失败路径，同时不写回 AI 回复与互动日期。

**我的判断**：✅ 建议保留

**判断理由**：异常路径不会污染数据是稳定契约，回归风险高，必须覆盖。

**完整测试代码**：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/AnalyzeChatUseCaseTest.kt
@Test
fun `AI分析失败时返回失败且不更新AI回复或互动日期`() = runTest {
    // Given
    coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
    coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
    every { brainTagRepository.getTagsForContact(contactId) } returns flowOf(emptyList())
    coEvery { settingsRepository.getDataMaskingEnabled() } returns Result.success(false)
    coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)
    coEvery { conversationRepository.saveUserInput(contactId, any(), any()) } returns Result.success(11L)
    coEvery { userProfileContextBuilder.buildAnalysisContext(profile, any()) } returns
        Result.success("")
    coEvery { topicRepository.getActiveTopic(contactId) } returns null
    coEvery { promptBuilder.buildWithTopic(any(), any(), any(), any(), any()) } returns "instruction"
    coEvery {
        aiRepository.analyzeChat(any(), any(), any())
    } returns Result.failure(IllegalStateException("boom"))

    // When
    val result = useCase(contactId, listOf("hello"))

    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("boom") == true)
    coVerify(exactly = 0) { conversationRepository.updateAiResponse(any(), any()) }
    coVerify(exactly = 0) { contactRepository.updateLastInteractionDate(any(), any()) }
}
```

#### 测试 6：脱敏开启时应调用 maskText 并使用脱敏内容
**测试意图**：验证脱敏开关开启时会调用 `maskText`，并将脱敏内容用于 runtimeData。

**我的判断**：✅ 建议保留

**判断理由**：隐私保护是核心原则，该测试确保“先脱敏后发送”不被破坏。

**完整测试代码**：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/AnalyzeChatUseCaseTest.kt
@Test
fun `脱敏开启时应调用maskText并使用脱敏内容`() = runTest {
    // Given
    val rawContext = listOf("Alpha", "Beta")
    val runtimeSlot = slot<String>()
    val analysisResult = AnalysisResult(
        replySuggestion = "reply",
        strategyAnalysis = "analysis",
        riskLevel = RiskLevel.SAFE
    )

    coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
    coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
    every { brainTagRepository.getTagsForContact(contactId) } returns flowOf(emptyList())
    coEvery { settingsRepository.getDataMaskingEnabled() } returns Result.success(true)
    coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)
    coEvery { privacyRepository.maskText("Alpha") } returns "MASK-A"
    coEvery { privacyRepository.maskText("Beta") } returns "MASK-B"
    coEvery { conversationRepository.saveUserInput(contactId, any(), any()) } returns Result.success(1L)
    coEvery { userProfileContextBuilder.buildAnalysisContext(profile, any()) } returns
        Result.success("")
    coEvery { topicRepository.getActiveTopic(contactId) } returns null
    coEvery {
        promptBuilder.buildWithTopic(any(), any(), any(), any(), capture(runtimeSlot))
    } returns "instruction"
    coEvery {
        aiRepository.analyzeChat(any(), any(), any())
    } returns Result.success(analysisResult)
    coEvery { conversationRepository.updateAiResponse(any(), any()) } returns Result.success(Unit)
    coEvery { contactRepository.updateLastInteractionDate(any(), any()) } returns Result.success(Unit)

    // When
    val result = useCase(contactId, rawContext)

    // Then
    assertTrue(result.isSuccess)
    assertTrue(runtimeSlot.captured.contains("${IdentityPrefixHelper.PREFIX_CONTACT}MASK-A"))
    assertTrue(runtimeSlot.captured.contains("${IdentityPrefixHelper.PREFIX_CONTACT}MASK-B"))
    coVerify(exactly = 2) { privacyRepository.maskText(any()) }
}
```

#### 测试 7：保存用户输入失败时不写 AI 回复但仍更新互动日期
**测试意图**：验证 `saveUserInput` 失败时不写回 AI 回复，但仍更新互动日期。

**我的判断**：✅ 建议保留

**判断理由**：该路径确保“输入保存失败”不影响成功分析结果的互动日期更新，契约稳定。

**完整测试代码**：
```kotlin
// file: domain/src/test/kotlin/com/empathy/ai/domain/usecase/AnalyzeChatUseCaseTest.kt
@Test
fun `保存用户输入失败时不写AI回复但仍更新互动日期`() = runTest {
    // Given
    val analysisResult = AnalysisResult(
        replySuggestion = "reply",
        strategyAnalysis = "analysis",
        riskLevel = RiskLevel.SAFE
    )
    coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(testProvider)
    coEvery { contactRepository.getProfile(contactId) } returns Result.success(profile)
    every { brainTagRepository.getTagsForContact(contactId) } returns flowOf(emptyList())
    coEvery { settingsRepository.getDataMaskingEnabled() } returns Result.success(false)
    coEvery { settingsRepository.getHistoryConversationCount() } returns Result.success(0)
    coEvery { conversationRepository.saveUserInput(contactId, any(), any()) } returns
        Result.failure(IllegalStateException("db"))
    coEvery { userProfileContextBuilder.buildAnalysisContext(profile, any()) } returns
        Result.success("")
    coEvery { topicRepository.getActiveTopic(contactId) } returns null
    coEvery { promptBuilder.buildWithTopic(any(), any(), any(), any(), any()) } returns "instruction"
    coEvery { aiRepository.analyzeChat(any(), any(), any()) } returns Result.success(analysisResult)
    coEvery { contactRepository.updateLastInteractionDate(any(), any()) } returns Result.success(Unit)

    // When
    val result = useCase(contactId, listOf("hello"))

    // Then
    assertTrue(result.isSuccess)
    coVerify(exactly = 0) { conversationRepository.updateAiResponse(any(), any()) }
    coVerify(exactly = 1) { contactRepository.updateLastInteractionDate(contactId, any()) }
}
```
