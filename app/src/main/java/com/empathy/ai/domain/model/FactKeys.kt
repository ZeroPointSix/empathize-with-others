package com.empathy.ai.domain.model

/**
 * 预定义的Fact字段常量
 *
 * 提供标准化的字段名，便于AI识别和处理
 */
object FactKeys {
    const val PERSONALITY = "性格特点"
    const val INTERESTS = "兴趣爱好"
    const val TABOOS = "沟通雷区"
    const val PREFERENCES = "喜好偏好"
    const val FAMILY = "家庭情况"
    const val WORK = "工作情况"
    const val HEALTH = "健康状况"
    const val HABITS = "生活习惯"

    /**
     * 所有预定义字段列表
     */
    val ALL = listOf(
        PERSONALITY,
        INTERESTS,
        TABOOS,
        PREFERENCES,
        FAMILY,
        WORK,
        HEALTH,
        HABITS
    )
}
