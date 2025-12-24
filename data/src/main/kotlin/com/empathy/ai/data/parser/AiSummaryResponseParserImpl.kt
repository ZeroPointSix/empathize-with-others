package com.empathy.ai.data.parser

import com.empathy.ai.domain.model.AiSummaryResponse
import com.empathy.ai.domain.model.FactData
import com.empathy.ai.domain.model.KeyEventData
import com.empathy.ai.domain.model.TagUpdateData
import com.empathy.ai.domain.util.AiSummaryResponseParser
import com.empathy.ai.domain.util.Logger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI总结响应解析器实现
 *
 * 使用Moshi解析AI返回的JSON响应为AiSummaryResponse领域模型。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Singleton
class AiSummaryResponseParserImpl @Inject constructor(
    private val moshi: Moshi,
    private val logger: Logger
) : AiSummaryResponseParser {

    companion object {
        private const val TAG = "AiSummaryResponseParser"
    }

    // 内部DTO用于JSON解析
    private data class SummaryResponseDto(
        val summary: String? = null,
        val keyEvents: List<KeyEventDto>? = null,
        val newFacts: List<FactDto>? = null,
        val updatedFacts: List<FactDto>? = null,
        val deletedFactKeys: List<String>? = null,
        val newTags: List<TagUpdateDto>? = null,
        val updatedTags: List<TagUpdateDto>? = null,
        val relationshipScoreChange: Int? = null,
        val relationshipTrend: String? = null
    )

    private data class KeyEventDto(
        val event: String? = null,
        val importance: Int? = null
    )

    private data class FactDto(
        val key: String? = null,
        val value: String? = null,
        val source: String? = null
    )

    private data class TagUpdateDto(
        val action: String? = null,
        val type: String? = null,
        val content: String? = null
    )

    private val adapter: JsonAdapter<SummaryResponseDto> by lazy {
        moshi.adapter(SummaryResponseDto::class.java)
    }

    override fun parse(jsonResponse: String): AiSummaryResponse? {
        return try {
            // 清理JSON响应
            val cleanedJson = cleanJsonResponse(jsonResponse)
            
            // 解析DTO
            val dto = adapter.fromJson(cleanedJson)
            if (dto == null) {
                logger.w(TAG, "JSON解析返回null")
                return null
            }

            // 转换为领域模型
            AiSummaryResponse(
                summary = dto.summary ?: "",
                keyEvents = dto.keyEvents?.mapNotNull { it.toDomain() },
                newFacts = dto.newFacts?.mapNotNull { it.toDomain() } ?: emptyList(),
                updatedFacts = dto.updatedFacts?.mapNotNull { it.toDomain() },
                deletedFactKeys = dto.deletedFactKeys,
                newTags = dto.newTags?.mapNotNull { it.toDomain() },
                updatedTags = dto.updatedTags?.mapNotNull { it.toDomain() },
                relationshipScoreChange = dto.relationshipScoreChange ?: 0,
                relationshipTrend = dto.relationshipTrend ?: "STABLE"
            )
        } catch (e: Exception) {
            logger.e(TAG, "解析AI总结响应失败: ${e.message}", e)
            null
        }
    }

    /**
     * 清理JSON响应
     *
     * 移除可能的markdown代码块标记等
     */
    private fun cleanJsonResponse(response: String): String {
        var cleaned = response.trim()
        
        // 移除markdown代码块标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        
        return cleaned.trim()
    }

    private fun KeyEventDto.toDomain(): KeyEventData? {
        val eventText = event ?: return null
        return KeyEventData(
            event = eventText,
            importance = importance ?: 3
        )
    }

    private fun FactDto.toDomain(): FactData? {
        val factKey = key ?: return null
        val factValue = value ?: return null
        return FactData(
            key = factKey,
            value = factValue,
            source = source ?: "AI_INFERRED"
        )
    }

    private fun TagUpdateDto.toDomain(): TagUpdateData? {
        val tagType = type ?: return null
        val tagContent = content ?: return null
        return TagUpdateData(
            action = action ?: "ADD",
            type = tagType,
            content = tagContent
        )
    }
}
