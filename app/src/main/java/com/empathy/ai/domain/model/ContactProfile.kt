package com.empathy.ai.domain.model

/**
 * 联系人画像 - 核心"角色卡"
 *
 * 存储目标联系人的基本信息、攻略目标和所有事实性信息
 *
 * @property id 唯一标识 (UUID 或加密 ID)
 * @property name 显示名称 (例如: "王总", "李铁柱")
 * @property targetGoal 核心攻略目标 (例如: "拿下合同", "修复父子关系")
 * @property contextDepth 上下文读取深度 (每次分析读取最近多少条记录，默认为 10)
 * @property facts 核心事实槽 - 存储所有事实类信息
 *                结构示例: {"电话": "138...", "住址": "朝阳区", "性格": "吃软不吃硬", "爱好": "钓鱼"}
 */
data class ContactProfile(
    val id: String,
    val name: String,
    val targetGoal: String,
    val contextDepth: Int = 10,
    val facts: Map<String, String> = emptyMap()
)
