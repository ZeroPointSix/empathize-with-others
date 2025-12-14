package com.empathy.ai.domain.util

/**
 * 记忆系统常量
 *
 * 集中管理记忆系统相关的常量，避免硬编码
 */
object MemoryConstants {

    // ==================== 时间常量 ====================

    /**
     * 一天的毫秒数
     */
    const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * 最近Facts的天数阈值（7天）
     */
    const val RECENT_DAYS = 7

    /**
     * 中期Facts的天数阈值（30天）
     */
    const val MEDIUM_DAYS = 30

    /**
     * Facts过期天数阈值（90天）
     */
    const val EXPIRY_DAYS = 90

    /**
     * 最大重试天数（7天）
     */
    const val MAX_RETRY_DAYS = 7

    /**
     * 数据清理间隔天数（7天）
     */
    const val CLEANUP_INTERVAL_DAYS = 7

    /**
     * 数据保留天数（90天）
     */
    const val DATA_RETENTION_DAYS = 90

    // ==================== 数量限制 ====================

    /**
     * 上下文构建时最大Facts数量
     */
    const val MAX_FACTS_COUNT = 20

    /**
     * AI调用最大重试次数
     */
    const val MAX_AI_RETRIES = 3

    /**
     * 失败任务最大重试次数
     */
    const val MAX_TASK_RETRIES = 3

    // ==================== 关系分数 ====================

    /**
     * 默认关系分数
     */
    const val DEFAULT_RELATIONSHIP_SCORE = 50

    /**
     * 最小关系分数
     */
    const val MIN_RELATIONSHIP_SCORE = 0

    /**
     * 最大关系分数
     */
    const val MAX_RELATIONSHIP_SCORE = 100

    /**
     * 每日最大分数变化
     */
    const val MAX_DAILY_SCORE_CHANGE = 10

    // ==================== 关系等级阈值 ====================

    /**
     * 陌生/冷淡等级上限
     */
    const val STRANGER_THRESHOLD = 30

    /**
     * 普通等级上限
     */
    const val ACQUAINTANCE_THRESHOLD = 60

    /**
     * 熟悉等级上限
     */
    const val FAMILIAR_THRESHOLD = 80

    // ==================== 数据库相关 ====================

    /**
     * 数据库名称
     */
    const val DATABASE_NAME = "empathy_database"

    /**
     * 当前数据库版本
     */
    const val DATABASE_VERSION = 5

    // ==================== SharedPreferences ====================

    /**
     * 记忆系统配置文件名
     */
    const val PREFS_MEMORY_SETTINGS = "memory_settings"

    /**
     * 最后总结日期Key
     */
    const val KEY_LAST_SUMMARY_DATE = "last_summary_date"

    /**
     * 最后清理日期Key
     */
    const val KEY_LAST_CLEANUP_DATE = "last_cleanup_date"
}
