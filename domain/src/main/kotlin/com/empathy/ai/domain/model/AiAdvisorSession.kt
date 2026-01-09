package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI军师会话
 *
 * 每个联系人可以有多个会话，每个会话包含多条对话记录。
 * 会话用于组织和管理用户与AI军师的对话历史。
 *
 * 业务背景 (PRD-00026/3.1.3):
 * - 会话隔离：每个联系人的对话完全独立
 * - 自动保存：每次对话后自动保存到本地
 * - 会话恢复：切换联系人时自动恢复该联系人的最新会话
 * - 新建会话：支持为同一联系人创建多个会话主题
 *
 * 设计决策 (TDD-00026):
 * - 使用isActive标记支持多会话管理，区分活跃/归档会话
 * - messageCount字段用于快速预览，无需实时查询计数
 * - updatedAt用于排序，优先展示最近活跃的会话
 * - 会话与联系人是1:N关系，支持"赛前分析"和"赛后复盘"分开讨论
 *
 * BUG-00060增强 (2026-01-09):
 * - 添加isPinned字段支持会话置顶功能
 * - 置顶会话在列表中优先显示
 *
 * 任务追踪 (FD-00026/T003): 数据层实现 - 会话模型
 *
 * @property id 会话ID（UUID）
 * @property contactId 联系人ID，关联ContactProfile
 * @property title 会话标题，用户可自定义，如"关于她的态度变化"
 * @property createdAt 创建时间戳，用于会话排序
 * @property updatedAt 最后更新时间戳，消息发送时自动更新
 * @property messageCount 消息数量，用于UI预览显示
 * @property isActive 是否为活跃会话，支持会话归档
 * @property isPinned 是否置顶，置顶会话优先显示（BUG-00060新增）
 */
data class AiAdvisorSession(
    val id: String,
    val contactId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val isActive: Boolean = true,
    val isPinned: Boolean = false
) {
    companion object {
        /**
         * 创建新会话
         *
         * 业务规则 (PRD-00026/3.1.3):
         * - 新会话默认isActive=true，作为活跃会话
         * - 默认标题"新对话"，用户后续可修改
         * - 新会话messageCount=0，对话开始后递增
         *
         * 设计权衡 (TDD-00026):
         * - 使用System.currentTimeMillis()保证时间戳单调递增
         * - UUID确保全局唯一性，支持跨设备同步
         * - 默认标题简化创建流程，支持后续自定义
         *
         * @param contactId 联系人ID
         * @param title 会话标题，默认为"新对话"
         * @return 新创建的会话实例
         */
        fun create(contactId: String, title: String = "新对话"): AiAdvisorSession {
            val now = System.currentTimeMillis()
            return AiAdvisorSession(
                id = UUID.randomUUID().toString(),
                contactId = contactId,
                title = title,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
