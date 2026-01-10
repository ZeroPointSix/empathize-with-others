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
 * ## 关联文档
 * - TDD-00017: Clean Architecture模块化改造技术设计
 * - BUG-00064: AI总结功能未生效修复
 *
 * ## 核心功能
 * - **JSON解析**: 将AI返回的JSON响应解析为领域模型
 * - **容错处理**: 兼容多种AI响应格式，支持字段缺失时的默认值
 * - **Markdown清理**: 移除JSON响应中的markdown代码块标记
 *
 * ## BUG-00064 解析增强
 * - **importance字段增强**: 支持 Int 和 String 两种类型
 *   - AI可能返回数字(如5)或字符串(如"HIGH"/"中")
 * - **event字段增强**: 支持 event 和 description 两种字段名
 *   - 不同AI服务商可能使用不同的字段名
 *
 * ## 设计决策
 * - **多级降级**: 关键字段缺失时使用默认值而非直接失败
 * - **类型安全**: 使用 data class 和 Moshi 适配器保证解析类型安全
 * - **日志记录**: 解析失败时记录错误信息便于排查
 *
 * ## 重要性格式支持
 * | AI响应格式 | 解析结果 |
 * |-----------|---------|
 * | 5 (Int) | 5 |
 * | "5" (String) | 5 |
 * | "HIGH" | 8 |
 * | "高" / "重要" | 8 |
 * | "MEDIUM" / "中" / "一般" | 5 |
 * | "LOW" / "低" / "次要" | 2 |
 *
 * @see AiSummaryResponseParser
 * @see BUG-00064-AI总结功能未生效-修复方案.md
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
        val description: String? = null,  // AI 可能返回 description 而不是 event
        val importance: Any? = null  // 支持 Int 或 String 类型
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
        // 支持 event 或 description 字段
        val eventText = event ?: description ?: return null
        
        // 将 importance 转换为整数
        val importanceValue = when (importance) {
            is Number -> importance.toInt()
            is String -> parseImportanceString(importance)
            else -> 5  // 默认中等重要性
        }
        
        return KeyEventData(
            event = eventText,
            importance = importanceValue
        )
    }

    /**
     * 将字符串类型的重要性转换为整数
     *
     * 支持格式：
     * - "HIGH" / "高" -> 8
     * - "MEDIUM" / "中" -> 5
     * - "LOW" / "低" -> 2
     * - 数字字符串 -> 直接转换
     */
    private fun parseImportanceString(value: String): Int {
        return when (value.uppercase().trim()) {
            "HIGH", "高", "重要" -> 8
            "MEDIUM", "中", "一般" -> 5
            "LOW", "低", "次要" -> 2
            else -> {
                // 尝试直接解析为数字
                value.toIntOrNull() ?: 5
            }
        }
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
