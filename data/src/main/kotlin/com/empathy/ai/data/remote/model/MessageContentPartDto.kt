package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 多模态消息内容片段
 */
@JsonClass(generateAdapter = true)
data class MessageContentPartDto(
    @Json(name = "type")
    val type: String,
    @Json(name = "text")
    val text: String? = null,
    @Json(name = "image_url")
    val imageUrl: ImageUrlDto? = null
)

@JsonClass(generateAdapter = true)
data class ImageUrlDto(
    @Json(name = "url")
    val url: String
)
