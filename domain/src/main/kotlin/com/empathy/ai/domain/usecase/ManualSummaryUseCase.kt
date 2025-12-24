package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiSummaryResponse
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
import com.empathy.ai.domain.util.AiSummaryResponseParser
import com.empathy.ai.domain.util.ContextBuilder
import com.empathy.ai.domain.util.CoroutineDispatchers
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.SummaryConflictChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
    private val aiSummaryResponseParser: AiSummaryResponseParser,
    private val dispatchers: CoroutineDispatchers,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "ManualSummaryUseCase"
    }

    data class SummaryResult(
        val summary: DailySummary,
        val conversationCount: Int,
        val keyEventCount: Int,
        val factCount: Int,
        val relationshipChange: Int
    )

    fun interface ProgressCallback {
        fun onProgress(progress: Float, step: String)
    }

    suspend operator fun invoke(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution? = null,
        progressCallback: ProgressCallback? = null
    ): Result<SummaryResult> = withContext(dispatchers.io) {
        try {
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
                else -> { }
            }

            progressCallback?.onProgress(0.1f, "获取联系人信息...")
            val profile = contactRepository.getProfile(contactId).getOrNull()
                ?: return@withContext Result.failure(
                    SummaryException(SummaryError.Unknown("联系人不存在"))
                )

            if (conflictResolution == ConflictResolution.OVERWRITE) {
                progressCallback?.onProgress(0.15f, "清理已有总结...")
                dailySummaryRepository.deleteSummariesInRange(
                    contactId, dateRange.startDate, dateRange.endDate
                )
            }

            progressCallback?.onProgress(0.2f, "获取对话记录...")
            val conversations = loadConversations(contactId, dateRange, conflictResolution)

            if (conversations.isEmpty()) {
                return@withContext Result.failure(SummaryException(SummaryError.NoConversations))
            }

            progressCallback?.onProgress(0.4f, "AI正在分析对话内容...")
            val aiResult = generateAiSummary(profile, conversations, dateRange)

            val summary = if (aiResult.isSuccess) {
                aiResult.getOrThrow()
            } else {
                logger.w(TAG, "AI总结失败，使用降级方案")
                progressCallback?.onProgress(0.7f, "使用本地分析...")
                generateLocalSummary(profile, conversations, dateRange)
            }

            progressCallback?.onProgress(0.85f, "生成总结...")
            val finalSummary = summary.copy(
                summaryType = SummaryType.CUSTOM_RANGE,
                generationSource = GenerationSource.MANUAL,
                startDate = dateRange.startDate,
                endDate = dateRange.endDate,
                conversationCount = conversations.size,
                generatedAt = System.currentTimeMillis()
            )

            progressCallback?.onProgress(0.90f, "保存结果...")
            val savedId = dailySummaryRepository.saveSummary(finalSummary).getOrThrow()

            progressCallback?.onProgress(0.93f, "同步数据...")
            syncSummaryDataToContact(profile, finalSummary)

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
            logger.e(TAG, "手动总结失败", e)
            Result.failure(SummaryException(SummaryError.Unknown(e.message ?: "未知错误")))
        }
    }

    private suspend fun loadConversations(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution?
    ): List<ConversationLog> {
        val allDates = dateRange.getAllDates()
        val targetDates = if (conflictResolution == ConflictResolution.FILL_GAPS) {
            val summarizedDates = dailySummaryRepository
                .getSummarizedDatesInRange(contactId, dateRange.startDate, dateRange.endDate)
                .getOrNull() ?: emptyList()
            allDates.filter { it !in summarizedDates }
        } else {
            allDates
        }
        return targetDates.flatMap { date ->
            conversationRepository.getLogsByContactAndDate(contactId, date)
                .getOrNull() ?: emptyList()
        }
    }

    private suspend fun generateAiSummary(
        profile: com.empathy.ai.domain.model.ContactProfile,
        conversations: List<ConversationLog>,
        dateRange: DateRange
    ): Result<DailySummary> {
        return try {
            val provider = aiProviderRepository.getDefaultProvider().getOrNull()
                ?: return Result.failure(SummaryException(SummaryError.ApiError))

            val conversationTexts = conversations.map { conv ->
                buildString {
                    append("[${DateUtils.formatTimestamp(conv.timestamp)}] ")
                    append("用户: ${conv.userInput}")
                    conv.aiResponse?.let { append("\nAI: $it") }
                }
            }
            val prompt = contextBuilder.buildSummaryPrompt(profile, conversationTexts)

            val promptContext = PromptContext.fromContact(profile)
            val systemInstruction = promptBuilder.buildSimpleInstruction(
                scene = PromptScene.SUMMARY,
                contactId = profile.id,
                context = promptContext
            )

            val aiResponse = aiRepository.generateText(
                provider = provider,
                prompt = prompt,
                systemInstruction = systemInstruction
            ).getOrThrow()

            val summaryResponse = aiSummaryResponseParser.parse(aiResponse)
                ?: return Result.failure(SummaryException(SummaryError.ApiError))

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
                    summaryDate = dateRange.startDate,
                    content = summaryResponse.summary,
                    keyEvents = keyEvents,
                    newFacts = newFacts,
                    updatedTags = updatedTags,
                    relationshipScoreChange = summaryResponse.relationshipScoreChange,
                    relationshipTrend = trend
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "AI总结生成失败", e)
            Result.failure(e)
        }
    }

    private fun generateLocalSummary(
        profile: com.empathy.ai.domain.model.ContactProfile,
        conversations: List<ConversationLog>,
        dateRange: DateRange
    ): DailySummary {
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
            relationshipTrend = if (scoreChange > 0) RelationshipTrend.IMPROVING else RelationshipTrend.STABLE
        )
    }

    private suspend fun syncSummaryDataToContact(
        profile: com.empathy.ai.domain.model.ContactProfile,
        summary: DailySummary
    ) {
        try {
            if (summary.newFacts.isNotEmpty()) {
                syncFactsToContact(profile, summary.newFacts)
            }
            if (summary.updatedTags.isNotEmpty()) {
                syncTagsToContact(profile.id, summary.updatedTags)
            }
            if (summary.relationshipScoreChange != 0) {
                val newScore = (profile.relationshipScore + summary.relationshipScoreChange).coerceIn(0, 100)
                contactRepository.updateRelationshipScore(profile.id, newScore)
                logger.d(TAG, "关系分数更新: ${profile.relationshipScore} -> $newScore")
            }
            logger.d(TAG, "数据同步完成: facts=${summary.newFacts.size}, tags=${summary.updatedTags.size}")
        } catch (e: Exception) {
            logger.w(TAG, "数据同步部分失败，但总结已保存")
        }
    }

    private suspend fun syncFactsToContact(
        profile: com.empathy.ai.domain.model.ContactProfile,
        newFacts: List<Fact>
    ) {
        try {
            val existingKeys = profile.facts.map { it.key }.toSet()
            val factsToAdd = newFacts.filter { it.key !in existingKeys }
            if (factsToAdd.isNotEmpty()) {
                val mergedFacts = profile.facts + factsToAdd
                contactRepository.updateFacts(profile.id, mergedFacts)
                logger.d(TAG, "事实同步: 新增${factsToAdd.size}条")
            }
        } catch (e: Exception) {
            logger.e(TAG, "事实同步失败", e)
        }
    }

    private suspend fun syncTagsToContact(contactId: String, tagUpdates: List<TagUpdate>) {
        for (update in tagUpdates) {
            try {
                when (update.action.uppercase()) {
                    "ADD" -> {
                        val tagType = try {
                            TagType.valueOf(update.type)
                        } catch (e: IllegalArgumentException) {
                            logger.w(TAG, "未知标签类型: ${update.type}，跳过")
                            continue
                        }
                        val brainTag = BrainTag(
                            id = 0,
                            contactId = contactId,
                            content = update.content,
                            type = tagType,
                            source = "AI_INFERRED",
                            isConfirmed = false
                        )
                        brainTagRepository.saveTag(brainTag)
                        logger.d(TAG, "标签同步: 添加 ${update.type} - ${update.content}")
                    }
                    "REMOVE" -> {
                        logger.d(TAG, "标签同步: 跳过REMOVE操作 - ${update.content}")
                    }
                    else -> {
                        logger.w(TAG, "未知标签操作: ${update.action}")
                    }
                }
            } catch (e: Exception) {
                logger.e(TAG, "标签同步失败: ${update.content}", e)
            }
        }
    }
}

class SummaryException(val error: SummaryError) : Exception(error.userMessage)
