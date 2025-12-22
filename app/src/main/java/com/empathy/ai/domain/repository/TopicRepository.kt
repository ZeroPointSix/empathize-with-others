package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ConversationTopic
import kotlinx.coroutines.flow.Flow

/**
 * 对话主题仓库接口
 *
 * 定义主题数据的访问和管理操作，遵循Clean Architecture原则，
 * 在Domain层定义接口，由Data层实现。
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
     * @param contactId 联系人ID
     * @return 主题Flow，实时更新
     */
    fun observeActiveTopic(contactId: String): Flow<ConversationTopic?>

    /**
     * 设置联系人的对话主题
     *
     * 会自动停用该联系人之前的活跃主题
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
     * @param topicId 主题ID
     * @return 操作结果
     */
    suspend fun deleteTopic(topicId: String): Result<Unit>
}
