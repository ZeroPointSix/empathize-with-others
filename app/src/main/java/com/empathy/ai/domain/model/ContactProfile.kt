package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.MemoryConstants

/**
 * 联系人画像 - 核心"角色卡"（扩展版）
 *
 * 存储目标联系人的基本信息、攻略目标和所有事实性信息
 *
 * @property id 唯一标识 (UUID 或加密 ID)
 * @property name 显示名称 (例如: "王总", "李铁柱")
 * @property targetGoal 核心攻略目标 (例如: "拿下合同", "修复父子关系")
 * @property contextDepth 上下文读取深度 (每次分析读取最近多少条记录，默认为 10)
 * @property facts 核心事实槽 - 存储所有事实类信息（修改为List<Fact>）
 * @property relationshipScore 关系分数 (0-100，默认50)
 * @property lastInteractionDate 最后互动日期 (格式: "yyyy-MM-dd")
 */
data class ContactProfile(
    val id: String,
    val name: String,
    val targetGoal: String,
    val contextDepth: Int = 10,
    val facts: List<Fact> = emptyList(),
    val relationshipScore: Int = MemoryConstants.DEFAULT_RELATIONSHIP_SCORE,
    val lastInteractionDate: String? = null
) {
    init {
        require(id.isNotBlank()) { "id不能为空" }
        require(name.isNotBlank()) { "name不能为空" }
        require(contextDepth > 0) { "contextDepth必须大于0" }
        require(
            relationshipScore in MemoryConstants.MIN_RELATIONSHIP_SCORE..MemoryConstants.MAX_RELATIONSHIP_SCORE
        ) { "relationshipScore必须在0到100之间" }
    }

    /**
     * 获取最近的Facts（7天内）
     */
    fun getRecentFacts(): List<Fact> {
        val recentThreshold = System.currentTimeMillis() -
            MemoryConstants.RECENT_DAYS * MemoryConstants.ONE_DAY_MILLIS
        return facts.filter { it.timestamp >= recentThreshold }
    }

    /**
     * 获取手动添加的Facts
     */
    fun getManualFacts(): List<Fact> {
        return facts.filter { it.source == FactSource.MANUAL }
    }

    /**
     * 判断关系等级
     */
    fun getRelationshipLevel(): RelationshipLevel {
        return when (relationshipScore) {
            in 0..MemoryConstants.STRANGER_THRESHOLD -> RelationshipLevel.STRANGER
            in (MemoryConstants.STRANGER_THRESHOLD + 1)..MemoryConstants.ACQUAINTANCE_THRESHOLD ->
                RelationshipLevel.ACQUAINTANCE
            in (MemoryConstants.ACQUAINTANCE_THRESHOLD + 1)..MemoryConstants.FAMILIAR_THRESHOLD ->
                RelationshipLevel.FAMILIAR
            else -> RelationshipLevel.CLOSE
        }
    }
}
