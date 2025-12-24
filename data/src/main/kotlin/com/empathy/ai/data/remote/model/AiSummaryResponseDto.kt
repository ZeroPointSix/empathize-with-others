package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * AI总结响应DTO
 *
 * 用于解析AI返回的每日总结JSON。
 * 这是Data层的DTO，带有Moshi序列化注解。
 * 与Domain层的AiSummaryResponse领域模型对应。
 */
@JsonClass(generateAdapter = true)
data class AiSummaryResponseDto(
    @Json(name = "summary")
    val summary: String,

    @Json(name = "keyEvents")
    val keyEvents: List<KeyEventDto>,

    @Json(name = "newFacts")
    val newFacts: List<FactDto>,

    @Json(name = "updatedFacts")
    val updatedFacts: List<FactDto>? = null,

    @Json(name = "deletedFactKeys")
    val deletedFactKeys: List<String>? = null,

    @Json(name = "newTags")
    val newTags: List<TagUpdateDto>? = null,

    @Json(name = "updatedTags")
    val updatedTags: List<TagUpdateDto>,

    @Json(name = "relationshipScoreChange")
    val relationshipScoreChange: Int,

    @Json(name = "relationshipTrend")
    val relationshipTrend: String
)

@JsonClass(generateAdapter = true)
data class KeyEventDto(
    @Json(name = "event")
    val event: String,

    @Json(name = "importance")
    val importance: Int
)

@JsonClass(generateAdapter = true)
data class FactDto(
    @Json(name = "key")
    val key: String,

    @Json(name = "value")
    val value: String,

    @Json(name = "source")
    val source: String? = "AI_INFERRED"
)

@JsonClass(generateAdapter = true)
data class TagUpdateDto(
    @Json(name = "action")
    val action: String,

    @Json(name = "type")
    val type: String,

    @Json(name = "content")
    val content: String
)
