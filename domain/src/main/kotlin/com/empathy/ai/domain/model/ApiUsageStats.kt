package com.empathy.ai.domain.model

/**
 * API 用量统计
 *
 * 聚合统计结果，用于展示用量概览和成本分析。
 *
 * 业务背景:
 * - 追踪AI API调用次数和Token消耗
 * - 支持按服务商、模型维度统计分析
 * - 便于用户了解API使用情况和成本控制
 *
 * 设计决策:
 * - 使用嵌套数据类支持多维度统计
 * - 提供格式化方法便于UI展示
 * - 冗余存储providerName和modelName，避免查询时的关联
 *
 * @property totalRequests 总请求数
 * @property successRequests 成功请求数
 * @property failedRequests 失败请求数
 * @property totalTokens 总 Token 数
 * @property totalPromptTokens 总输入 Token 数
 * @property totalCompletionTokens 总输出 Token 数
 * @property averageRequestTimeMs 平均请求耗时（毫秒）
 * @property providerStats 按服务商统计
 * @property modelStats 按模型统计
 * @property startTime 统计开始时间
 * @property endTime 统计结束时间
 */
data class ApiUsageStats(
    val totalRequests: Int = 0,
    val successRequests: Int = 0,
    val failedRequests: Int = 0,
    val totalTokens: Long = 0,
    val totalPromptTokens: Long = 0,
    val totalCompletionTokens: Long = 0,
    val averageRequestTimeMs: Long = 0,
    val providerStats: List<ProviderUsageStats> = emptyList(),
    val modelStats: List<ModelUsageStats> = emptyList(),
    val startTime: Long = 0,
    val endTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取成功率（百分比）
     */
    fun getSuccessRate(): Float {
        return if (totalRequests > 0) {
            successRequests.toFloat() / totalRequests * 100
        } else {
            0f
        }
    }

    /**
     * 获取格式化的成功率
     */
    fun formatSuccessRate(): String {
        return String.format("%.1f%%", getSuccessRate())
    }

    /**
     * 获取格式化的总 Token 数
     */
    fun formatTotalTokens(): String {
        return when {
            totalTokens < 1000 -> "$totalTokens"
            totalTokens < 1000000 -> String.format("%.1fK", totalTokens / 1000.0)
            else -> String.format("%.2fM", totalTokens / 1000000.0)
        }
    }

    /**
     * 获取格式化的平均请求耗时
     */
    fun formatAverageRequestTime(): String {
        return when {
            averageRequestTimeMs < 1000 -> "${averageRequestTimeMs}ms"
            else -> String.format("%.1fs", averageRequestTimeMs / 1000.0)
        }
    }

    /**
     * 是否有数据
     */
    fun hasData(): Boolean = totalRequests > 0
}

/**
 * 按服务商统计
 *
 * @property providerId 服务商 ID
 * @property providerName 服务商名称
 * @property requestCount 请求数
 * @property successCount 成功数
 * @property totalTokens 总 Token 数
 * @property averageRequestTimeMs 平均请求耗时
 */
data class ProviderUsageStats(
    val providerId: String,
    val providerName: String,
    val requestCount: Int,
    val successCount: Int,
    val totalTokens: Long,
    val averageRequestTimeMs: Long
) {
    /**
     * 获取成功率
     */
    fun getSuccessRate(): Float {
        return if (requestCount > 0) {
            successCount.toFloat() / requestCount * 100
        } else {
            0f
        }
    }

    /**
     * 获取格式化的 Token 数
     */
    fun formatTotalTokens(): String {
        return when {
            totalTokens < 1000 -> "$totalTokens"
            totalTokens < 1000000 -> String.format("%.1fK", totalTokens / 1000.0)
            else -> String.format("%.2fM", totalTokens / 1000000.0)
        }
    }
}

/**
 * 按模型统计
 *
 * @property modelId 模型 ID
 * @property modelName 模型名称
 * @property providerId 服务商 ID
 * @property providerName 服务商名称
 * @property requestCount 请求数
 * @property successCount 成功数
 * @property totalTokens 总 Token 数
 * @property averageRequestTimeMs 平均请求耗时
 */
data class ModelUsageStats(
    val modelId: String,
    val modelName: String,
    val providerId: String,
    val providerName: String,
    val requestCount: Int,
    val successCount: Int,
    val totalTokens: Long,
    val averageRequestTimeMs: Long
) {
    /**
     * 获取成功率
     */
    fun getSuccessRate(): Float {
        return if (requestCount > 0) {
            successCount.toFloat() / requestCount * 100
        } else {
            0f
        }
    }

    /**
     * 获取格式化的 Token 数
     */
    fun formatTotalTokens(): String {
        return when {
            totalTokens < 1000 -> "$totalTokens"
            totalTokens < 1000000 -> String.format("%.1fK", totalTokens / 1000.0)
            else -> String.format("%.2fM", totalTokens / 1000000.0)
        }
    }
}

/**
 * 统计时间范围
 */
enum class UsageStatsPeriod {
    /** 今日 */
    TODAY,
    /** 本周 */
    THIS_WEEK,
    /** 本月 */
    THIS_MONTH,
    /** 全部 */
    ALL
}
