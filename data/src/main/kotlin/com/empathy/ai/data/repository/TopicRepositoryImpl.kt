package com.empathy.ai.data.repository

import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.data.local.dao.ConversationTopicDao
import com.empathy.ai.data.local.entity.ConversationTopicEntity
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TopicRepositoryImpl 实现了对话主题的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)联系人画像记忆系统
 *   - 对话主题：当前与联系人交流的主要话题
 *   - 活跃主题：同一时间只有一个主题处于活跃状态
 *   - 主题历史：记录历史交流话题，分析交流趋势
 *
 * 【设计决策】(TDD-00003)
 *   - 使用@IoDispatcher指定IO调度器
 *   - Flow响应式查询：observeActiveTopic监听主题变化
 *   - 互斥策略：设置新主题时自动停用所有旧主题
 *
 * 【关键逻辑】
 *   - setTopic：设置新主题前先停用所有旧主题
 *   - observeActiveTopic：返回Flow，主题变更时自动通知UI
 *   - getActiveTopic：获取当前活跃主题（一次性查询）
 *
 * 【任务追踪】
 *   - FD-00003/Task-002: 对话主题管理功能
 */
@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val topicDao: ConversationTopicDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TopicRepository {

    override suspend fun getActiveTopic(contactId: String): ConversationTopic? =
        withContext(ioDispatcher) {
            topicDao.getActiveTopic(contactId)?.toDomain()
        }

    override fun observeActiveTopic(contactId: String): Flow<ConversationTopic?> =
        topicDao.observeActiveTopic(contactId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun setTopic(topic: ConversationTopic): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                topicDao.deactivateAllTopics(topic.contactId)
                topicDao.insert(ConversationTopicEntity.fromDomain(topic))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateTopicContent(topicId: String, content: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                topicDao.updateContent(topicId, content)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun clearTopic(contactId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                topicDao.deactivateAllTopics(contactId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTopicHistory(contactId: String, limit: Int): List<ConversationTopic> =
        withContext(ioDispatcher) {
            topicDao.getTopicHistory(contactId, limit).map { it.toDomain() }
        }

    override suspend fun deleteTopic(topicId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                topicDao.deleteById(topicId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
