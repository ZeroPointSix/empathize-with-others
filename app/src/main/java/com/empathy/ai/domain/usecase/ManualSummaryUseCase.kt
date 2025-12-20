package com.empathy.ai.domain.usecase

import android.util.Log
import com.empathy.ai.di.IoDispatcher
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.GenerationSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.SummaryType
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TagUpdate
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.util.ContextBuilder
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.SummaryConflictChecker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 手动触发AI总结用例
 *
 * 负责编排手动总结的完整流程：
 * 1. 验证日期范围
 * 2. 检测冲突
 * 3. 获取对话数据
 * 4. 调用AI生成总结
 * 5. 保存结果
 */
@Singleton
class ManualSummaryUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val dateRangeValidator: DateRangeValidator,
    private val conflictChecker: SummaryConflictChecker,
    private val contextBuilder: ContextBuilder,
    private val promptBuilder: PromptBuilder,
    private val moshi: Moshi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ManualSummaryUseCase"
    }

    /**
     * 总结结果
     */
    data class SummaryResult(
        val summary: DailySummary,
        val conversationCount: Int,
        val keyEventCount: Int,
        val factCount: Int,
        val relationshipChange: Int
    )

    /**
     * 进度回调
     */
    fun interface ProgressCallback {
        fun onProgress(progress: Float, step: String)
    }

    /**
     * 执行手动总结
     *
     * @param contactId 联系人ID
     * @param dateRange 日期范围
     * @param conflictResolution 冲突处理方式（如果有冲突）
     * @param progressCallback 进度回调
     * @return 总结结果
     */
    suspend operator fun invoke(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution? = null,
        progressCallback: ProgressCallback? = null
    ): Result<SummaryResult> = withContext(ioDispatcher) {
        try {
            // 1. 验证日期范围
            progressCallback?.onProgress(0.05f, "验证日期范围...")
            val validationResult = dateRangeValidator.validate(dateRange, contactId)
            if (validationResult.isFailure) {
                return@withContext Result.failure(validationResult.exceptionOrNull()!!)
            }
            when (val validation = validationResult.getOrNull()) {
                is DateRangeValidator.ValidationResult.Invalid -> {
                    return@withContext Result.failure(
                        SummaryException(SummaryError.Unknown(validation.message))
                    )
                }
                else -> { /* Valid or Warning, continue */ }
            }

            // 2. 获取联系人信息
            progressCallback?.onProgress(0.1f, "获取联系人信息...")
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.failure(
                    SummaryException(SummaryError.Unknown("联系人不存在"))
                )

            // 3. 处理冲突（如果需要覆盖）
            if (conflictResolution == ConflictResolution.OVERWRITE) {
                progressCallback?.onProgress(0.15f, "清理已有总结...")
                dailySummaryRepository.deleteSummariesInRange(
                    contactId,
                    dateRange.startDate,
                    dateRange.endDate
                )
            }

            // 4. 获取对话数据
            progressCallback?.onProgress(0.2f, "获取对话记录...")
            val conversations = loadConversations(
                contactId,
                dateRange,
                conflictResolution
            )

            if (conversations.isEmpty()) {
                return@withContext Result.failure(
                    SummaryException(SummaryError.NoConversations)
                )
            }

            // 5. AI分析
            progressCallback?.onProgress(0.4f, "AI正在分析对话内容...")
            val aiResult = generateAiSummary(profile, conversations, dateRange)

            val summary = if (aiResult.isSuccess) {
                aiResult.getOrThrow()
            } else {
                // AI失败，使用降级方案
                Log.w(TAG, "AI总结失败，使用降级方案", aiResult.exceptionOrNull())
                progressCallback?.onProgress(0.7f, "使用本地分析...")
                generateLocalSummary(profile, conversations, dateRange)
            }

            // 6. 构建最终总结对象
            progressCallback?.onProgress(0.85f, "生成总结...")
            val finalSummary = summary.copy(
                summaryType = SummaryType.CUSTOM_RANGE,
                generationSource = GenerationSource.MANUAL,
                startDate = dateRange.startDate,
                endDate = dateRange.endDate,
                conversationCount = conversations.size,
                generatedAt = System.currentTimeMillis()
            )

            // 7. 保存结果
            progressCallback?.onProgress(0.90f, "保存结果...")
            val savedId = dailySummaryRepository.saveSummary(finalSummary).getOrThrow()

            // 8. 同步数据到联系人画像和标签系统
            progressCallback?.onProgress(0.93f, "同步数据...")
            syncSummaryDataToContact(profile, finalSummary)

            // 9. 标记对话为已总结
            val logIds = conversations.map { it.id }
            conversationRepository.markAsSummarized(logIds)

            progressCallback?.onProgress(1f, "完成")

            Result.success(
                SummaryResult(
                    summary = finalSummary.copy(id = savedId),
                    conversationCount = conversations.size,
                    keyEventCount = finalSummary.keyEvents.size,
                    factCount = finalSummary.newFacts.size,
                    relationshipChange = finalSummary.relationshipScoreChange
                )
            )
        } catch (e: CancellationException) {
            Result.failure(SummaryException(SummaryError.Cancelled))
        } catch (e: SummaryException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "手动总结失败", e)
            Result.failure(SummaryException(SummaryError.Unknown(e.message ?: "未知错误")))
        }
    }


    /**
     * 加载对话数据
     */
    private suspend fun loadConversations(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution?
    ): List<ConversationLog> {
        val allDates = dateRange.getAllDates()

        // 如果是仅补充模式，过滤掉已有总结的日期
        val targetDates = if (conflictResolution == ConflictResolution.FILL_GAPS) {
            val summarizedDates = dailySummaryRepository
                .getSummarizedDatesInRange(contactId, dateRange.startDate, dateRange.endDate)
                .getOrNull() ?: emptyList()
            allDates.filter { it !in summarizedDates }
        } else {
            allDates
        }

        // 加载对话
        return targetDates.flatMap { date ->
            conversationRepository.getLogsByContactAndDate(contactId, date)
                .getOrNull() ?: emptyList()
        }
    }

    /**
     * 生成AI总结
     */
    private suspend fun generateAiSummary(
        profile: com.empathy.ai.domain.model.ContactProfile,
        conversations: List<ConversationLog>,
        dateRange: DateRange
    ): Result<DailySummary> {
        return try {
            // 获取默认AI服务商
            val provider = aiProviderRepository.getDefaultProvider().getOrNull()
                ?: return Result.failure(SummaryException(SummaryError.ApiError))

            // 构建总结Prompt
            val conversationTexts = conversations.map { conv ->
                buildString {
                    append("[${DateUtils.formatTimestamp(conv.timestamp)}] ")
                    append("用户: ${conv.userInput}")
                    conv.aiResponse?.let { append("\nAI: $it") }
                }
            }
            val prompt = contextBuilder.buildSummaryPrompt(profile, conversationTexts)

            // 使用PromptBuilder构建系统指令
            val promptContext = PromptContext.fromContact(profile)
            val systemInstruction = promptBuilder.buildSimpleInstruction(
                scene = PromptScene.SUMMARY,
                contactId = profile.id,
                context = promptContext
            )

            // 调用AI
            val aiResponse = aiRepository.generateText(
                provider = provider,
                prompt = prompt,
                systemInstruction = systemInstruction
            ).getOrThrow()

            // 解析AI响应
            val summaryResponse = parseAiResponse(aiResponse)
                ?: return Result.failure(SummaryException(SummaryError.ApiError))

            // 构建DailySummary
            val keyEvents = summaryResponse.keyEvents?.map { dto ->
                KeyEvent(event = dto.event, importance = dto.importance)
            } ?: emptyList()

            val newFacts = summaryResponse.newFacts.map { dto ->
                Fact(
                    key = dto.key,
                    value = dto.value,
                    timestamp = System.currentTimeMillis(),
                    source = FactSource.AI_INFERRED
                )
            }

            val updatedTags = summaryResponse.newTags?.map { dto ->
                TagUpdate(action = dto.action, type = dto.type, content = dto.content)
            } ?: emptyList()

            val trend = when (summaryResponse.relationshipTrend.uppercase()) {
                "IMPROVING" -> RelationshipTrend.IMPROVING
                "DECLINING" -> RelationshipTrend.DECLINING
                else -> RelationshipTrend.STABLE
            }

            Result.success(
                DailySummary(
                    id = 0,
                    contactId = profile.id,
                    summaryDate = dateRange.startDate, // 使用开始日期作为主日期
                    content = summaryResponse.summary,
                    keyEvents = keyEvents,
                    newFacts = newFacts,
                    updatedTags = updatedTags,
                    relationshipScoreChange = summaryResponse.relationshipScoreChange,
                    relationshipTrend = trend
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "AI总结生成失败", e)
            Result.failure(e)
        }
    }

    /**
     * 解析AI响应
     *
     * 采用容错解析策略，仿照 AiRepositoryImpl.parseAnalysisResult 的设计：
     * 1. 先尝试直接解析（严格模式）
     * 2. 失败后使用字段映射（宽松模式），为缺失字段提供默认值
     *
     * 解决问题：AI 返回的 newTags 缺少 action 字段导致 Moshi 解析失败
     */
    private fun parseAiResponse(jsonResponse: String): com.empathy.ai.data.remote.model.AiSummaryResponse? {
        return try {
            // 1. 提取 JSON 部分（去除 Markdown 代码块等）
            val jsonStr = extractJsonFromResponse(jsonResponse) ?: return null

            // 2. 先尝试直接解析（严格模式）
            try {
                val adapter = moshi.adapter(com.empathy.ai.data.remote.model.AiSummaryResponse::class.java)
                val result = adapter.fromJson(jsonStr)
                if (result != null) {
                    Log.d(TAG, "AI响应直接解析成功")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "直接解析失败，尝试容错解析: ${e.message}")
            }

            // 3. 直接解析失败，使用容错解析（手动映射字段）
            parseAiResponseWithFallback(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "解析AI响应失败", e)
            null
        }
    }

    /**
     * 从 AI 响应中提取 JSON 字符串
     *
     * 处理 AI 可能返回的各种格式：
     * - 纯 JSON
     * - Markdown 代码块包裹的 JSON（```json ... ```）
     * - 带前后文字说明的 JSON
     */
    private fun extractJsonFromResponse(response: String): String? {
        // 尝试提取 Markdown 代码块中的 JSON
        val codeBlockPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val codeBlockMatch = codeBlockPattern.find(response)
        if (codeBlockMatch != null) {
            val jsonContent = codeBlockMatch.groupValues[1].trim()
            if (jsonContent.startsWith("{")) {
                return jsonContent
            }
        }

        // 直接查找 JSON 对象
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) return null

        return response.substring(jsonStart, jsonEnd + 1)
    }

    /**
     * 容错解析 AI 响应
     *
     * 手动解析 JSON Map，为缺失字段提供默认值
     * 特别处理 newTags 中缺失的 action 字段（默认为 "ADD"）
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseAiResponseWithFallback(jsonStr: String): com.empathy.ai.data.remote.model.AiSummaryResponse? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                com.squareup.moshi.Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val jsonMap = mapAdapter.fromJson(jsonStr) ?: return null

            // 提取各字段，为缺失字段提供默认值
            val summary = (jsonMap["summary"] as? String) ?: "AI 总结完成"

            val keyEvents = parseKeyEvents(jsonMap["keyEvents"])
            val newFacts = parseFacts(jsonMap["newFacts"])
            val updatedFacts = parseFacts(jsonMap["updatedFacts"])
            val deletedFactKeys = (jsonMap["deletedFactKeys"] as? List<String>) ?: emptyList()
            val newTags = parseTagUpdates(jsonMap["newTags"])
            val updatedTags = parseTagUpdates(jsonMap["updatedTags"])
            val relationshipScoreChange = parseIntValue(jsonMap["relationshipScoreChange"], 0)
            val relationshipTrend = (jsonMap["relationshipTrend"] as? String) ?: "STABLE"

            Log.d(TAG, "容错解析成功: newTags=${newTags.size}, newFacts=${newFacts.size}")

            com.empathy.ai.data.remote.model.AiSummaryResponse(
                summary = summary,
                keyEvents = keyEvents,
                newFacts = newFacts,
                updatedFacts = updatedFacts,
                deletedFactKeys = deletedFactKeys,
                newTags = newTags,
                updatedTags = updatedTags,
                relationshipScoreChange = relationshipScoreChange,
                relationshipTrend = relationshipTrend
            )
        } catch (e: Exception) {
            Log.e(TAG, "容错解析失败", e)
            null
        }
    }

    /**
     * 解析 keyEvents 字段
     *
     * 支持两种格式：
     * - {"event": "...", "importance": 3}（数字）
     * - {"description": "...", "importance": "HIGH"}（字符串）
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseKeyEvents(raw: Any?): List<com.empathy.ai.data.remote.model.KeyEventDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val event = (item["event"] as? String)
                ?: (item["description"] as? String)
                ?: return@mapNotNull null
            val importance = parseImportanceValue(item["importance"])
            com.empathy.ai.data.remote.model.KeyEventDto(event = event, importance = importance)
        }
    }

    /**
     * 解析 importance 值
     *
     * 支持数字和字符串两种格式
     */
    private fun parseImportanceValue(raw: Any?): Int {
        return when (raw) {
            is Number -> raw.toInt().coerceIn(1, 5)
            is String -> when (raw.uppercase()) {
                "HIGH" -> 5
                "MEDIUM" -> 3
                "LOW" -> 1
                else -> raw.toIntOrNull()?.coerceIn(1, 5) ?: 3
            }
            else -> 3
        }
    }

    /**
     * 解析 facts 字段
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseFacts(raw: Any?): List<com.empathy.ai.data.remote.model.FactDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val key = item["key"] as? String ?: return@mapNotNull null
            val value = item["value"] as? String ?: return@mapNotNull null
            val source = (item["source"] as? String) ?: "AI_INFERRED"
            com.empathy.ai.data.remote.model.FactDto(key = key, value = value, source = source)
        }
    }

    /**
     * 解析 tags 字段（核心容错逻辑）
     *
     * 为缺失的 action 字段提供默认值 "ADD"
     * 这是解决 BUG-00025 的关键：AI 返回的 newTags 只有 content 和 type，缺少 action
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseTagUpdates(raw: Any?): List<com.empathy.ai.data.remote.model.TagUpdateDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val content = item["content"] as? String ?: return@mapNotNull null
            val type = item["type"] as? String ?: return@mapNotNull null
            // 关键：为缺失的 action 字段提供默认值 "ADD"
            val action = (item["action"] as? String) ?: "ADD"
            com.empathy.ai.data.remote.model.TagUpdateDto(
                action = action,
                type = type,
                content = content
            )
        }
    }

    /**
     * 解析整数值
     */
    private fun parseIntValue(raw: Any?, default: Int): Int {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }

    /**
     * 生成本地降级总结
     */
    private fun generateLocalSummary(
        profile: com.empathy.ai.domain.model.ContactProfile,
        conversations: List<ConversationLog>,
        dateRange: DateRange
    ): DailySummary {
        // 简单统计对话次数作为关系分数增量
        val scoreChange = minOf(conversations.size, 5)

        return DailySummary(
            id = 0,
            contactId = profile.id,
            summaryDate = dateRange.startDate,
            content = "${dateRange.startDate}至${dateRange.endDate}期间共${conversations.size}次互动（本地统计）",
            keyEvents = emptyList(),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = scoreChange,
            relationshipTrend = if (scoreChange > 0) {
                RelationshipTrend.IMPROVING
            } else {
                RelationshipTrend.STABLE
            }
        )
    }

    /**
     * 同步总结数据到联系人画像和标签系统
     *
     * 将AI总结中提取的数据同步到相应的数据表：
     * 1. newFacts -> ContactProfile.facts（追加到现有事实列表）
     * 2. newTags -> BrainTag表（作为AI推断的标签）
     * 3. relationshipScoreChange -> ContactProfile.relationshipScore（累加）
     *
     * @param profile 联系人画像
     * @param summary 生成的总结
     */
    private suspend fun syncSummaryDataToContact(
        profile: com.empathy.ai.domain.model.ContactProfile,
        summary: DailySummary
    ) {
        try {
            // 1. 同步新事实到联系人画像
            if (summary.newFacts.isNotEmpty()) {
                syncFactsToContact(profile, summary.newFacts)
            }

            // 2. 同步新标签到BrainTag表
            if (summary.updatedTags.isNotEmpty()) {
                syncTagsToContact(profile.id, summary.updatedTags)
            }

            // 3. 更新关系分数
            if (summary.relationshipScoreChange != 0) {
                val newScore = (profile.relationshipScore + summary.relationshipScoreChange)
                    .coerceIn(0, 100)
                contactRepository.updateRelationshipScore(profile.id, newScore)
                Log.d(TAG, "关系分数更新: ${profile.relationshipScore} -> $newScore")
            }

            Log.d(TAG, "数据同步完成: facts=${summary.newFacts.size}, tags=${summary.updatedTags.size}")
        } catch (e: Exception) {
            // 数据同步失败不应阻止总结保存，只记录警告
            Log.w(TAG, "数据同步部分失败，但总结已保存", e)
        }
    }

    /**
     * 同步事实到联系人画像
     *
     * 将新事实追加到联系人现有的事实列表中，避免重复
     */
    private suspend fun syncFactsToContact(
        profile: com.empathy.ai.domain.model.ContactProfile,
        newFacts: List<Fact>
    ) {
        try {
            // 获取现有事实的key集合，用于去重
            val existingKeys = profile.facts.map { it.key }.toSet()

            // 过滤掉已存在的事实（基于key去重）
            val factsToAdd = newFacts.filter { it.key !in existingKeys }

            if (factsToAdd.isNotEmpty()) {
                // 合并现有事实和新事实
                val mergedFacts = profile.facts + factsToAdd
                contactRepository.updateFacts(profile.id, mergedFacts)
                Log.d(TAG, "事实同步: 新增${factsToAdd.size}条，跳过${newFacts.size - factsToAdd.size}条重复")
            }
        } catch (e: Exception) {
            Log.e(TAG, "事实同步失败", e)
        }
    }

    /**
     * 同步标签到BrainTag表
     *
     * 将AI总结中的标签更新同步到BrainTag表：
     * - ADD操作：创建新的AI推断标签
     * - REMOVE操作：暂不处理（需要查找匹配的标签ID）
     */
    private suspend fun syncTagsToContact(
        contactId: String,
        tagUpdates: List<TagUpdate>
    ) {
        for (update in tagUpdates) {
            try {
                when (update.action.uppercase()) {
                    "ADD" -> {
                        val tagType = try {
                            TagType.valueOf(update.type)
                        } catch (e: IllegalArgumentException) {
                            Log.w(TAG, "未知标签类型: ${update.type}，跳过")
                            continue
                        }

                        val brainTag = BrainTag(
                            id = 0, // 新标签，ID由数据库生成
                            contactId = contactId,
                            content = update.content,
                            type = tagType,
                            source = "AI_INFERRED",
                            isConfirmed = false // AI推断的标签默认未确认
                        )

                        brainTagRepository.saveTag(brainTag)
                        Log.d(TAG, "标签同步: 添加 ${update.type} - ${update.content}")
                    }
                    "REMOVE" -> {
                        // REMOVE操作需要先查找匹配的标签，暂时只记录日志
                        Log.d(TAG, "标签同步: 跳过REMOVE操作 - ${update.content}")
                    }
                    else -> {
                        Log.w(TAG, "未知标签操作: ${update.action}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "标签同步失败: ${update.content}", e)
            }
        }
    }
}

/**
 * 总结异常
 */
class SummaryException(val error: SummaryError) : Exception(error.userMessage)
