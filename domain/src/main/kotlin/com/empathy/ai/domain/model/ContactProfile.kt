package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.MemoryConstants

/**
 * 联系人画像 - 核心"角色卡"（扩展版）
 *
 * 存储目标联系人的基本信息、攻略目标和所有事实性信息。
 * 这是AI军师功能的核心数据源，用于个性化分析和策略建议。
 *
 * 业务背景 (PRD-00003/PRD-00026):
 * - 联系人画像是AI分析的基础背景信息
 * - facts包含用户的生日、爱好、工作等关键信息
 * - targetGoal用于对齐分析方向与用户目标
 * - AI军师分析时会结合facts提供针对性建议（PRD-00026/3.2.2）
 *
 * 设计决策 (TDD-00003):
 * - 使用List<Fact>替代Map，支持事实的增删改查和版本追踪
 * - relationshipScore量化关系发展程度（0-100）
 * - 支持编辑追踪（originalName/originalGoal），用于数据审计
 * - contextDepth控制每次分析读取的对话记录数量
 *
 * 任务追踪: FD-00003/基础数据模型设计
 *
 * @property id 唯一标识 (UUID 或加密 ID)
 * @property name 显示名称 (例如: "王总", "李铁柱")
 * @property targetGoal 核心攻略目标 (例如: "拿下合同", "修复父子关系")
 * @property contextDepth 上下文读取深度 (每次分析读取最近多少条记录，默认为 10)
 * @property facts 核心事实槽 - 存储所有事实类信息
 * @property relationshipScore 关系分数 (0-100，默认50)
 * @property lastInteractionDate 最后互动日期 (格式: "yyyy-MM-dd")
 * @property avatarUrl 头像URL
 * @property isNameUserModified 姓名是否被用户修改过
 * @property isGoalUserModified 目标是否被用户修改过
 * @property nameLastModifiedTime 姓名最后修改时间
 * @property goalLastModifiedTime 目标最后修改时间
 * @property originalName 原始姓名（修改前）
 * @property originalGoal 原始目标（修改前）
 */
data class ContactProfile(
    val id: String,
    val name: String,
    val targetGoal: String,
    val contextDepth: Int = 10,
    val facts: List<Fact> = emptyList(),
    val relationshipScore: Int = MemoryConstants.DEFAULT_RELATIONSHIP_SCORE,
    val lastInteractionDate: String? = null,
    val avatarUrl: String? = null,
    // ==================== 编辑追踪字段 ====================
    val isNameUserModified: Boolean = false,
    val isGoalUserModified: Boolean = false,
    val nameLastModifiedTime: Long = 0L,
    val goalLastModifiedTime: Long = 0L,
    val originalName: String? = null,
    val originalGoal: String? = null
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
     * 创建姓名编辑后的副本
     *
     * @param newName 新的姓名
     * @return 编辑后的ContactProfile副本
     */
    fun copyWithNameEdit(newName: String): ContactProfile {
        return copy(
            name = newName,
            isNameUserModified = true,
            nameLastModifiedTime = System.currentTimeMillis(),
            // 仅首次编辑时保存原始值
            originalName = if (originalName == null) name else originalName
        )
    }

    /**
     * 创建目标编辑后的副本
     *
     * @param newGoal 新的目标
     * @return 编辑后的ContactProfile副本
     */
    fun copyWithGoalEdit(newGoal: String): ContactProfile {
        return copy(
            targetGoal = newGoal,
            isGoalUserModified = true,
            goalLastModifiedTime = System.currentTimeMillis(),
            // 仅首次编辑时保存原始值
            originalGoal = if (originalGoal == null) targetGoal else originalGoal
        )
    }

    /**
     * 判断姓名是否有变化
     *
     * @param newName 新的姓名
     * @return 是否有变化
     */
    fun hasNameChanges(newName: String): Boolean = name != newName

    /**
     * 判断目标是否有变化
     *
     * @param newGoal 新的目标
     * @return 是否有变化
     */
    fun hasGoalChanges(newGoal: String): Boolean = targetGoal != newGoal

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
