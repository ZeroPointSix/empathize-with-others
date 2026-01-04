package com.empathy.ai.domain.util

/**
 * 记忆系统常量
 *
 * 集中管理记忆系统相关的常量，避免硬编码。
 *
 * 业务背景 (PRD-00003):
 * - 联系人画像记忆系统采用三层记忆架构（短期/中期/长期）
 * - Facts按时间分层筛选，确保AI分析时使用最相关的信息
 *
 * 设计决策 (TDD-00003):
 * - 最近7天的Facts优先保留（新鲜度优先）
 * - 30天以上的Facts只保留手动的（AI推断的可能过时）
 * - 每日分数变化限制在±10分内（防止单日波动过大）
 *
 * 阈值来源:
 * - 7天/30天/90天: 基于用户活跃周期和数据有效性研究
 * - 关系分数: 参考社交心理学的关系阶段划分
 */
object MemoryConstants {

    // ==================== 时间常量 ====================

    /**
     * 一天的毫秒数
     *
     * 用于时间计算，如判断Facts的新鲜度
     */
    const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * 最近Facts的天数阈值（7天）
     *
     * 业务规则 (PRD-00003/US-006):
     * - 最近7天的Facts全部保留，包括AI推断的
     * - 理由：新鲜信息最相关，有助于AI理解当前关系状态
     */
    const val RECENT_DAYS = 7

    /**
     * 中期Facts的天数阈值（30天）
     *
     * 业务规则 (PRD-00003/US-006):
     * - 7-30天的Facts只保留手动的
     * - 理由：AI推断的Facts可能随时间推移而过时
     */
    const val MEDIUM_DAYS = 30

    /**
     * Facts过期天数阈值（90天）
     *
     * 业务规则 (PRD-00003/US-006):
     * - 超过90天的Facts考虑清理（除非非常重要）
     * - 理由：长期未更新的信息价值降低
     */
    const val EXPIRY_DAYS = 90

    /**
     * 最大重试天数（7天）
     *
     * 设计权衡:
     * - 失败任务最多保留7天重试
     * - 理由：超过7天的对话上下文已不具时效性
     */
    const val MAX_RETRY_DAYS = 7

    /**
     * 数据清理间隔天数（7天）
     *
     * 设计权衡:
     * - 每周执行一次数据清理
     * - 理由：平衡存储空间和性能开销
     */
    const val CLEANUP_INTERVAL_DAYS = 7

    /**
     * 数据保留天数（90天）
     *
     * 业务规则:
     * - 对话记录和总结保留90天
     * - 理由：平衡数据价值和存储成本
     */
    const val DATA_RETENTION_DAYS = 90

    // ==================== 数量限制 ====================

    /**
     * 上下文构建时最大Facts数量
     *
     * 设计权衡 (TDD-00003):
     * - 限制最多20条Facts发送给AI
     * - 理由：避免Token消耗过多，同时保证信息量
     * - 超出时按优先级截断（手动 > AI推断 > 时间倒序）
     */
    const val MAX_FACTS_COUNT = 20

    /**
     * AI调用最大重试次数
     *
     * 设计权衡:
     * - 网络不稳定时最多重试3次
     * - 理由：平衡成功率和响应时间
     */
    const val MAX_AI_RETRIES = 3

    /**
     * 失败任务最大重试次数
     *
     * 设计权衡:
     * - 每日总结失败最多重试3次
     * - 超过后放弃，避免无限循环
     */
    const val MAX_TASK_RETRIES = 3

    // ==================== 关系分数 ====================

    /**
     * 默认关系分数
     *
     * 业务规则 (PRD-00003/US-005):
     * - 新联系人默认50分（中性）
     * - 分数范围：0-100
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
     *
     * 业务规则 (PRD-00003/AC-010):
     * - 单日分数变化限制在±10分
     * - 理由：防止单次互动导致分数剧烈波动
     * - AI评估分数变化时必须在此范围内
     */
    const val MAX_DAILY_SCORE_CHANGE = 10

    // ==================== 关系等级阈值 ====================

    /**
     * 陌生/冷淡等级上限 (0-30)
     *
     * 社交心理学依据:
     * - 0-30: 关系建立初期，了解有限
     * - 需要更多互动来了解对方
     */
    const val STRANGER_THRESHOLD = 30

    /**
     * 普通等级上限 (31-60)
     *
     * 社交心理学依据:
     * - 31-60: 关系发展期，有一定了解
     * - 可以适度开玩笑，但仍需谨慎
     */
    const val ACQUAINTANCE_THRESHOLD = 60

    /**
     * 熟悉等级上限 (61-80)
     *
     * 社交心理学依据:
     * - 61-80: 关系深入期，了解较充分
     * - 可以更随意地交流
     */
    const val FAMILIAR_THRESHOLD = 80

    /**
     * 亲密等级 (81-100)
     *
     * 社交心理学依据:
     * - 81-100: 关系成熟期，非常了解
     * - 可以非常自然地交流
     */

    // ==================== 数据库相关 ====================

    /**
     * 数据库名称
     */
    const val DATABASE_NAME = "empathy_database"

    /**
     * 当前数据库版本
     *
     * 版本历史:
     * - v1: 初始版本
     * - v3: 添加对话记录表
     * - v4: 添加每日总结表
     * - v5: 添加AI军师相关表 (TD-00026)
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
