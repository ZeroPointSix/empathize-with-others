package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ConversationTopic
import kotlinx.coroutines.flow.Flow

/**
 * 对话主题仓储接口
 *
 * 业务背景:
 * - 主题(Topic)是联系人对话的上下文锚点
 * - 每个联系人同一时间只有一个活跃主题
 * - 主题用于标识当前对话的关注点，支持上下文切换
 *
 * 设计决策:
 * - 活跃主题优先：设置新主题时自动停用旧的
   - 响应式监听：observeActiveTopic返回Flow，支持UI实时更新
   - 历史记录：保留最近的主题历史，便于回顾和切换
   - 轻量级结构：不与对话记录强关联，可独立管理
 *
 * 遵循Clean Architecture原则：接口定义在domain层，由data层实现
 */
interface TopicRepository {

    /**
     * 获取联系人当前活跃的主题
     *
     * @param contactId 联系人ID
     * @return 当前活跃主题，如果没有则返回null
     */
    suspend fun getActiveTopic(contactId: String): ConversationTopic?

    /**
     * 获取联系人当前活跃主题的Flow（响应式）
     *
     * 用途: UI实时监听主题变化
     *
     * @param contactId 联系人ID
     * @return 主题Flow，实时更新
     */
    fun observeActiveTopic(contactId: String): Flow<ConversationTopic?>

    /**
     * 设置联系人的对话主题
     *
     * 业务规则:
     * - 自动停用该联系人之前的活跃主题
     * - 新主题自动设为活跃状态
     * - 如果已存在相同内容的主题，则更新而非新建
     *
     * @param topic 要设置的主题
     * @return 操作结果
     */
    suspend fun setTopic(topic: ConversationTopic): Result<Unit>

    /**
     * 更新现有主题内容
     *
     * @param topicId 主题ID
     * @param content 新的主题内容
     * @return 操作结果
     */
    suspend fun updateTopicContent(topicId: String, content: String): Result<Unit>

    /**
     * 清除联系人的当前主题（设置为非活跃）
     *
     * 业务规则:
     * - 仅将主题标记为非活跃，不删除
     * - 保留历史记录供后续查询
     *
     * @param contactId 联系人ID
     * @return 操作结果
     */
    suspend fun clearTopic(contactId: String): Result<Unit>

    /**
     * 获取联系人的主题历史记录
     *
     * @param contactId 联系人ID
     * @param limit 返回数量限制，默认10条
     * @return 主题历史列表，按更新时间倒序
     */
    suspend fun getTopicHistory(contactId: String, limit: Int = 10): List<ConversationTopic>

    /**
     * 删除指定主题
     *
     * 业务规则:
     * - 物理删除，非活跃主题才能删除
     * - 活跃主题需先调用clearTopic
     *
     * @param topicId 主题ID
     * @return 操作结果
     */
    suspend fun deleteTopic(topicId: String): Result<Unit>
}
