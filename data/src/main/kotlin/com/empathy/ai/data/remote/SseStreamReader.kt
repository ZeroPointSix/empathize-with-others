package com.empathy.ai.data.remote

import com.empathy.ai.domain.model.AiStreamChunk
import com.empathy.ai.domain.model.TokenUsage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * SSE流式读取器
 *
 * 使用OkHttp EventSource实现Server-Sent Events流式读取。
 * 参考Cherry Studio的AiSdkToChunkAdapter设计。
 *
 * 业务背景 (FD-00028):
 * - 流式响应是现代AI应用的标配，提升用户体验
 * - 支持DeepSeek R1等模型的思考过程展示
 * - 实现智能降级策略，连续失败时切换到非流式模式
 *
 * 设计决策:
 * - 使用OkHttp SSE库处理Server-Sent Events
 * - 解析OpenAI兼容格式的流式响应
 * - 支持reasoning_content字段（DeepSeek R1思考过程）
 *
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see RESEARCH-00004 Cherry项目AI对话实现深度分析报告
 */
@Singleton
class SseStreamReader @Inject constructor(
    @Named("sse") private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "SseStreamReader"
        private const val MAX_RETRY_COUNT = 3
        private const val FALLBACK_THRESHOLD = 3
        private const val DONE_SIGNAL = "[DONE]"
    }

    private var consecutiveFailures = 0

    /**
     * 发起SSE流式请求
     *
     * @param url API端点URL
     * @param requestBodyJson 请求体JSON字符串
     * @param headers 请求头
     * @return 流式响应Flow
     */
    fun stream(
        url: String,
        requestBodyJson: String,
        headers: Map<String, String>
    ): Flow<AiStreamChunk> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
                addHeader("Accept", "text/event-stream")
                addHeader("Cache-Control", "no-cache")
            }
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val eventSourceFactory = EventSources.createFactory(okHttpClient)
        val eventSource = eventSourceFactory.newEventSource(
            request,
            object : EventSourceListener() {

                override fun onOpen(eventSource: EventSource, response: Response) {
                    consecutiveFailures = 0
                    trySend(AiStreamChunk.Started)
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data == DONE_SIGNAL) {
                        return
                    }
                    try {
                        val chunk = parseChunk(data)
                        chunk?.let { trySend(it) }
                    } catch (e: Exception) {
                        trySend(AiStreamChunk.Error(e))
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    channel.close()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    consecutiveFailures++
                    // BUG-044-P1-004: 改进错误提示，提供友好的错误信息
                    val errorMessage = parseErrorMessage(response, t)
                    val error = IOException(errorMessage)

                    if (consecutiveFailures >= FALLBACK_THRESHOLD) {
                        trySend(
                            AiStreamChunk.Error(
                                SseFallbackException(
                                    "连续${consecutiveFailures}次SSE失败，建议降级到非流式模式",
                                    error
                                )
                            )
                        )
                    } else {
                        trySend(AiStreamChunk.Error(error))
                    }
                    channel.close()
                }
            }
        )

        awaitClose { eventSource.cancel() }
    }

    /**
     * 解析错误信息，提供友好的错误提示
     *
     * BUG-044-P1-004: 根据HTTP状态码和异常类型提供用户友好的错误信息
     *
     * @param response HTTP响应
     * @param throwable 异常
     * @return 友好的错误信息
     */
    private fun parseErrorMessage(response: Response?, throwable: Throwable?): String {
        return when {
            response?.code == 401 -> "API密钥无效或已过期，请在设置中检查API密钥配置"
            response?.code == 403 -> "API访问被拒绝，请检查API密钥权限"
            response?.code == 429 -> "请求过于频繁，请稍后再试"
            response?.code == 500 -> "AI服务器错误，请稍后再试"
            response?.code == 502 || response?.code == 503 -> "AI服务暂时不可用，请稍后再试"
            response?.code == 504 -> "AI服务响应超时，请稍后再试"
            throwable is java.net.UnknownHostException -> "网络连接失败，请检查网络设置"
            throwable is java.net.SocketTimeoutException -> "请求超时，请检查网络连接"
            throwable is java.net.ConnectException -> "无法连接到AI服务器，请检查网络设置"
            response != null -> "连接失败 (错误码: ${response.code})，请稍后再试"
            throwable != null -> "连接失败: ${throwable.message ?: "未知错误"}"
            else -> "连接失败，请稍后再试"
        }
    }


    /**
     * 解析SSE数据块
     *
     * 支持的字段：
     * - delta.content: 普通文本内容
     * - delta.reasoning_content: DeepSeek R1思考过程
     * - finish_reason: 完成标记
     * - usage: Token使用统计
     *
     * @param data SSE事件数据
     * @return 解析后的AiStreamChunk，无法解析时返回null
     */
    private fun parseChunk(data: String): AiStreamChunk? {
        val json = JSONObject(data)

        // 检查错误响应
        if (json.has("error")) {
            val errorObj = json.getJSONObject("error")
            val message = errorObj.optString("message", "Unknown error")
            return AiStreamChunk.Error(IOException(message))
        }

        val choices = json.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null

        val choice = choices.getJSONObject(0)
        val delta = choice.optJSONObject("delta")

        // 检查完成状态
        val finishReason = choice.optString("finish_reason", "")
        if (finishReason == "stop" || finishReason == "length") {
            val usage = parseUsage(json)
            return AiStreamChunk.Complete("", usage)
        }

        if (delta == null) return null

        // DeepSeek R1思考过程
        val reasoning = delta.optString("reasoning_content", "")
        if (reasoning.isNotEmpty()) {
            return AiStreamChunk.ThinkingDelta(reasoning)
        }

        // 普通文本内容
        val content = delta.optString("content", "")
        if (content.isNotEmpty()) {
            return AiStreamChunk.TextDelta(content)
        }

        return null
    }

    /**
     * 解析Token使用统计
     *
     * @param json 响应JSON对象
     * @return TokenUsage对象，解析失败时返回null
     */
    private fun parseUsage(json: JSONObject): TokenUsage? {
        val usage = json.optJSONObject("usage") ?: return null
        return TokenUsage(
            promptTokens = usage.optInt("prompt_tokens", 0),
            completionTokens = usage.optInt("completion_tokens", 0),
            totalTokens = usage.optInt("total_tokens", 0)
        )
    }

    /**
     * 检查是否应该降级到非流式模式
     *
     * @return true表示应该降级
     */
    fun shouldFallbackToNonStreaming(): Boolean = consecutiveFailures >= FALLBACK_THRESHOLD

    /**
     * 重置失败计数
     *
     * 在成功请求后调用，重置连续失败计数器。
     */
    fun resetFailureCount() {
        consecutiveFailures = 0
    }
}
