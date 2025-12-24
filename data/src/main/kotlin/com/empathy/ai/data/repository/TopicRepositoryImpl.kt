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
 * 对话主题仓库实现
 *
 * 实现TopicRepository接口，负责：
 * - 主题数据的持久化存储
 * - Entity与Domain模型的转换
 * - 异步操作的线程调度
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
