package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 获取对话主题用例
 *
 * 负责获取联系人当前活跃的对话主题，支持单次获取和响应式观察。
 */
class GetTopicUseCase @Inject constructor(
    private val topicRepository: TopicRepository
) {
    /**
     * 获取联系人当前活跃主题
     *
     * @param contactId 联系人ID
     * @return 当前活跃主题，如果没有则返回null
     */
    suspend operator fun invoke(contactId: String): ConversationTopic? {
        if (contactId.isBlank()) {
            return null
        }
        return topicRepository.getActiveTopic(contactId)
    }

    /**
     * 观察联系人主题变化（响应式）
     *
     * @param contactId 联系人ID
     * @return 主题Flow，实时更新
     */
    fun observe(contactId: String): Flow<ConversationTopic?> {
        if (contactId.isBlank()) {
            return flowOf(null)
        }
        return topicRepository.observeActiveTopic(contactId)
    }
}
