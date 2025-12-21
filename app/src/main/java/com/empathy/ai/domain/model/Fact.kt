package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.MemoryConstants
import java.util.UUID

/**
 * 事实领域模型（扩展版）
 *
 * 纯Kotlin类，无Android依赖
 * 表示联系人的一条事实信息
 * 支持编辑追踪功能
 *
 * @property id 唯一标识符，用于LazyColumn的key，保证全局唯一
 * @property key 字段名，如"性格特点"、"兴趣爱好"
 * @property value 字段值
 * @property timestamp 创建/更新时间（毫秒）
 * @property source 来源：MANUAL 或 AI_INFERRED
 * @property isUserModified 是否被用户修改过
 * @property lastModifiedTime 最后修改时间
 * @property originalKey 原始键（修改前）
 * @property originalValue 原始值（修改前）
 */
data class Fact(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long,
    val source: FactSource,
    // ==================== 编辑追踪字段 ====================
    val isUserModified: Boolean = false,
    val lastModifiedTime: Long = timestamp,
    val originalKey: String? = null,
    val originalValue: String? = null
) {
    init {
        require(id.isNotBlank()) { "Fact的id不能为空" }
        require(key.isNotBlank()) { "Fact的key不能为空" }
        require(value.isNotBlank()) { "Fact的value不能为空" }
        require(timestamp > 0) { "Fact的timestamp必须大于0" }
    }

    /**
     * 创建编辑后的副本
     *
     * @param newKey 新的键
     * @param newValue 新的值
     * @return 编辑后的Fact副本
     */
    fun copyWithEdit(newKey: String, newValue: String): Fact {
        return copy(
            key = newKey,
            value = newValue,
            isUserModified = true,
            lastModifiedTime = System.currentTimeMillis(),
            // 仅首次编辑时保存原始值
            originalKey = if (originalKey == null) key else originalKey,
            originalValue = if (originalValue == null) value else originalValue
        )
    }

    /**
     * 判断内容是否有变化
     *
     * @param newKey 新的键
     * @param newValue 新的值
     * @return 是否有变化
     */
    fun hasChanges(newKey: String, newValue: String): Boolean {
        return key != newKey || value != newValue
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

    /**
     * 格式化最后修改时间
     */
    fun formatLastModifiedTime(): String = DateUtils.formatRelativeTime(lastModifiedTime)
}
