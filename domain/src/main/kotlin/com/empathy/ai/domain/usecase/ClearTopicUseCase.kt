package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.TopicRepository
import javax.inject.Inject

/**
 * 清除对话主题用例
 *
 * 负责清除联系人当前的对话主题（设置为非活跃状态）。
 * 主题数据不会被删除，会保留在历史记录中。
 *
 * 业务背景:
 *   - PRD-00016: 对话主题功能需求
 *   - 场景: 用户完成当前话题讨论后，清除活跃主题
 *
 * 设计决策:
 *   - 软清除: 设置 isActive = false，不删除主题数据
 *   - 保留历史: 便于用户回顾历史主题记录
 *
 * @see TopicRepository.clearTopic 仓库清除方法
 */
class ClearTopicUseCase @Inject constructor(
    private val topicRepository: TopicRepository
) {
    /**
     * 清除联系人当前主题
     *
     * @param contactId 联系人ID
     * @return 操作结果
     */
    suspend operator fun invoke(contactId: String): Result<Unit> {
        if (contactId.isBlank()) {
            return Result.failure(IllegalArgumentException("联系人ID不能为空"))
        }
        return topicRepository.clearTopic(contactId)
    }
}
