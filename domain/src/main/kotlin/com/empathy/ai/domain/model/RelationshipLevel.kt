package com.empathy.ai.domain.model

/**
 * 关系等级枚举
 *
 * 根据关系分数划分的关系等级
 *
 * @property displayName 显示名称
 */
enum class RelationshipLevel(val displayName: String) {
    /**
     * 陌生/冷淡 (0-30)
     */
    STRANGER("陌生"),

    /**
     * 普通 (31-60)
     */
    ACQUAINTANCE("普通"),

    /**
     * 熟悉 (61-80)
     */
    FAMILIAR("熟悉"),

    /**
     * 亲密 (81-100)
     */
    CLOSE("亲密")
}
