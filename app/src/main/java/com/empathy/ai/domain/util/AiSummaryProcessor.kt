package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.data.remote.model.AiSummaryResponse
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.KeyEvent
import com.empathy.ai.domain.model.PromptContext
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.model.TagUpdate
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI总结处理器
 *
 * 负责处理AI相关的总结逻辑：
 * - 调用AI生成总结
 * - 解析AI响应
 * - 更新联系人数据
 * - 保存总结记录
 *
 * 提示词系统集成:
 * - 使用PromptBuilder构建SUMMARY场景的系统指令
 * - 支持用户自定义提示词
 */
@Singleton
class AiSummaryProcessor @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val contactRepository: ContactRepository,
    private val brainTagRepository: BrainTagRepository,
    private val dailySummaryRepository: DailySummaryRepository,
    private val contextBuilder: ContextBuilder,
    private val promptBuilder: PromptBuilder,
    private val moshi: Moshi
) {
    companion object {
        private const val TAG = "AiSummaryProcessor"
    }

    /**
     * 执行AI总结
     *
     * @param profile 联系人画像
     * @param conversations 对话记录列表
     * @param date 总结日期
     * @return 成功返回Unit，失败返回异常
     */
    suspend fun process(
        profile: ContactProfile,
        conversations: List<ConversationLog>,
        date: String
    ): Result<Unit> {
        return try {
            // 获取默认AI服务商
            val provider = aiProviderRepository.getDefaultProvider().getOrNull()
                ?: return Result.failure(IllegalStateException("未配置默认AI服务商"))

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
            val summaryResponse = parseResponse(aiResponse)
                ?: return Result.failure(IllegalStateException("AI响应解析失败"))

            // 更新联系人数据
            updateContactData(profile, summaryResponse)

            // 保存总结记录
            saveSummary(profile.id, date, summaryResponse)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "AI总结处理失败", e)
            Result.failure(e)
        }
    }

    /**
     * 解析AI响应
     */
    private fun parseResponse(jsonResponse: String): AiSummaryResponse? {
        return try {
            val jsonStart = jsonResponse.indexOf('{')
            val jsonEnd = jsonResponse.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1) return null

            val jsonStr = jsonResponse.substring(jsonStart, jsonEnd + 1)
            val adapter = moshi.adapter(AiSummaryResponse::class.java)
            adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "解析AI响应失败", e)
            null
        }
    }

    /**
     * 更新联系人数据
     */
    private suspend fun updateContactData(
        profile: ContactProfile,
        response: AiSummaryResponse
    ) {
        val now = System.currentTimeMillis()
        val currentFacts = profile.facts.toMutableList()

        // 删除指定的Facts
        response.deletedFactKeys?.forEach { key ->
            currentFacts.removeAll { it.key == key }
        }

        // 更新已有Facts
        response.updatedFacts?.forEach { dto ->
            val index = currentFacts.indexOfFirst { it.key == dto.key }
            if (index >= 0) {
                currentFacts[index] = Fact(
                    key = dto.key,
                    value = dto.value,
                    timestamp = now,
                    source = FactSource.AI_INFERRED
                )
            }
        }

        // 添加新Facts
        response.newFacts.forEach { dto ->
            if (currentFacts.none { it.key == dto.key }) {
                currentFacts.add(
                    Fact(
                        key = dto.key,
                        value = dto.value,
                        timestamp = now,
                        source = FactSource.AI_INFERRED
                    )
                )
            }
        }

        // 计算新的关系分数
        val scoreChange = response.relationshipScoreChange.coerceIn(
            -MemoryConstants.MAX_DAILY_SCORE_CHANGE,
            MemoryConstants.MAX_DAILY_SCORE_CHANGE
        )
        val newScore = (profile.relationshipScore + scoreChange).coerceIn(
            MemoryConstants.MIN_RELATIONSHIP_SCORE,
            MemoryConstants.MAX_RELATIONSHIP_SCORE
        )

        // 批量更新联系人数据
        contactRepository.updateContactData(
            contactId = profile.id,
            facts = currentFacts,
            relationshipScore = newScore,
            lastInteractionDate = DateUtils.getCurrentDateString()
        )

        // 处理新标签
        response.newTags?.forEach { tagDto ->
            val tagType = when (tagDto.type.uppercase()) {
                "RISK_RED" -> TagType.RISK_RED
                "STRATEGY_GREEN" -> TagType.STRATEGY_GREEN
                else -> null
            }
            tagType?.let {
                val newTag = BrainTag(
                    id = 0, // 数据库自增ID
                    contactId = profile.id,
                    type = it,
                    content = tagDto.content,
                    source = "AI_INFERRED"
                )
                brainTagRepository.saveTag(newTag)
            }
        }
    }

    /**
     * 保存总结记录
     */
    private suspend fun saveSummary(
        contactId: String,
        date: String,
        response: AiSummaryResponse
    ) {
        val keyEvents = response.keyEvents.map { dto ->
            KeyEvent(event = dto.event, importance = dto.importance)
        }

        val newFacts = response.newFacts.map { dto ->
            Fact(
                key = dto.key,
                value = dto.value,
                timestamp = System.currentTimeMillis(),
                source = FactSource.AI_INFERRED
            )
        }

        val updatedTags = response.newTags?.map { dto ->
            TagUpdate(action = dto.action, type = dto.type, content = dto.content)
        } ?: emptyList()

        val trend = when (response.relationshipTrend.uppercase()) {
            "IMPROVING" -> RelationshipTrend.IMPROVING
            "DECLINING" -> RelationshipTrend.DECLINING
            else -> RelationshipTrend.STABLE
        }

        val summary = DailySummary(
            id = 0,
            contactId = contactId,
            summaryDate = date,
            content = response.summary,
            keyEvents = keyEvents,
            newFacts = newFacts,
            updatedTags = updatedTags,
            relationshipScoreChange = response.relationshipScoreChange,
            relationshipTrend = trend
        )

        dailySummaryRepository.saveSummary(summary)
    }
}
