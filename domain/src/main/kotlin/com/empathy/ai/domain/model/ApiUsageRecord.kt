package com.empathy.ai.domain.model

/**
 * API 用量记录
 *
 * 记录单次 API 调用的详细信息，用于统计分析和问题排查。
 *
 * 业务背景:
 * - 每一次AI API调用都生成一条记录
 * - 记录Token消耗和请求耗时
 * - 支持成功/失败状态的追踪
 *
 * 设计决策:
 * - 使用String类型id支持分布式环境
 * - 冗余存储providerName和modelName，便于查询和展示
 * - 提供格式化方法便于UI展示
 *
 * @property id 唯一标识
 * @property providerId 服务商 ID
 * @property providerName 服务商名称（冗余存储，便于显示）
 * @property modelId 模型 ID
 * @property modelName 模型名称（冗余存储，便于显示）
 * @property promptTokens 输入 Token 数
 * @property completionTokens 输出 Token 数
 * @property totalTokens 总 Token 数
 * @property requestTimeMs 请求耗时（毫秒）
 * @property isSuccess 是否成功
 * @property errorMessage 错误信息（失败时）
 * @property createdAt 创建时间戳
 */
data class ApiUsageRecord(
    val id: String,
    val providerId: String,
    val providerName: String,
    val modelId: String,
    val modelName: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val requestTimeMs: Long = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取格式化的请求耗时
     */
    fun getFormattedRequestTime(): String {
        return when {
            requestTimeMs < 1000 -> "${requestTimeMs}ms"
            requestTimeMs < 60000 -> String.format("%.1fs", requestTimeMs / 1000.0)
            else -> String.format("%.1fmin", requestTimeMs / 60000.0)
        }
    }

    /**
     * 获取格式化的 Token 数
     */
    fun getFormattedTokens(): String {
        return when {
            totalTokens < 1000 -> "$totalTokens"
            totalTokens < 1000000 -> String.format("%.1fK", totalTokens / 1000.0)
            else -> String.format("%.1fM", totalTokens / 1000000.0)
        }
    }
}
