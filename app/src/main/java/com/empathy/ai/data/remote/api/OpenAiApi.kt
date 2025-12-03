package com.empathy.ai.data.remote.api

import com.empathy.ai.data.remote.model.ChatRequestDto
import com.empathy.ai.data.remote.model.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * OpenAI 兼容 API 接口
 *
 * 支持多服务商(OpenAI、DeepSeek等)的动态路由。
 * 使用 @Url 注解实现运行时动态 URL，解决用户切换服务商的需求。
 *
 * 核心特性:
 * - 动态 URL: 通过 @Url 参数在运行时指定完整 URL
 * - 动态 Header: 通过 @HeaderMap 注入鉴权信息
 * - 挂起函数: 使用 Kotlin 协程实现异步调用
 *
 * 使用示例:
 * ```kotlin
 * // OpenAI
 * val openAiUrl = "https://api.openai.com/v1/chat/completions"
 * val openAiHeaders = mapOf("Authorization" to "Bearer $openAiKey")
 * val response = api.chatCompletion(openAiUrl, openAiHeaders, request)
 *
 * // DeepSeek
 * val deepSeekUrl = "https://api.deepseek.com/chat/completions"
 * val deepSeekHeaders = mapOf("Authorization" to "Bearer $deepSeekKey")
 * val response = api.chatCompletion(deepSeekUrl, deepSeekHeaders, request)
 * ```
 *
 * @see ChatRequestDto
 * @see ChatResponseDto
 */
interface OpenAiApi {

    /**
     * 聊天补全 API
     *
     * 发送消息给 AI 模型并获取回复。
     *
     * @param fullUrl 完整的 API URL (例如: "https://api.openai.com/v1/chat/completions")
     *                使用 @Url 注解覆盖 Retrofit 的 baseUrl
     * @param headers 请求头映射,包含鉴权信息 (例如: {"Authorization": "Bearer xxx"})
     * @param request 聊天请求体,包含模型、消息列表等参数
     * @return AI 回复响应
     */
    @POST
    suspend fun chatCompletion(
        @Url fullUrl: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ChatRequestDto
    ): ChatResponseDto
}
