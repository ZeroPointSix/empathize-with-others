package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.MemoryConstants

/**
 * 事实领域模型
 *
 * 纯Kotlin类，无Android依赖
 * 表示联系人的一条事实信息
 *
 * @property key 字段名，如"性格特点"、"兴趣爱好"
 * @property value 字段值
 * @property timestamp 创建/更新时间（毫秒）
 * @property source 来源：MANUAL 或 AI_INFERRED
 */
data class Fact(
    val key: String,
    val value: String,
    val timestamp: Long,
    val source: FactSource
) {
    init {
        require(key.isNotBlank()) { "Fact的key不能为空" }
        require(value.isNotBlank()) { "Fact的value不能为空" }
        require(timestamp > 0) { "Fact的timestamp必须大于0" }
    }

    /**
     * 判断是否过期（超过90天）
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        val expiryThreshold = now - MemoryConstants.EXPIRY_DAYS * MemoryConstants.ONE_DAY_MILLIS
        return timestamp < expiryThreshold
    }

    /**
     * 判断是否为最近的（7天内）
     */
    fun isRecent(now: Long = System.currentTimeMillis()): Boolean {
        val recentThreshold = now - MemoryConstants.RECENT_DAYS * MemoryConstants.ONE_DAY_MILLIS
        return timestamp >= recentThreshold
    }

    /**
     * 格式化时间戳为日期字符串
     */
    fun formatDate(): String = DateUtils.formatDate(timestamp)
}
