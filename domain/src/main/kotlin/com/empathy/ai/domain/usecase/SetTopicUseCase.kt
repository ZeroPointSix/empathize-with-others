package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.domain.repository.TopicRepository
import javax.inject.Inject

/**
 * 设置对话主题用例
 *
 * 负责验证和保存用户设置的对话主题。
 * 设置新主题时会自动停用该联系人之前的活跃主题。
 *
 * 业务背景:
 *   - PRD-00016: 对话主题功能需求
 *   - 场景: 用户为当前对话设置/更新主题
 *
 * 设计决策:
 *   - 单主题模式: 每个联系人同时只能有一个活跃主题
 *   - 自动停用: 设置新主题时，Repository 自动将旧主题设为非活跃
 *   - 长度限制: MAX_CONTENT_LENGTH 防止主题过长
 *
 * @see TopicRepository.setTopic 仓库设置方法
 * @see ConversationTopic 主题领域模型
 */
class SetTopicUseCase @Inject constructor(
    private val topicRepository: TopicRepository
) {
    /**
     * 设置对话主题
     *
     * @param contactId 联系人ID
     * @param content 主题内容
     * @return 操作结果，成功返回创建的主题
     */
    suspend operator fun invoke(
        contactId: String,
        content: String
    ): Result<ConversationTopic> {
        // 1. 验证联系人ID
        if (contactId.isBlank()) {
            return Result.failure(IllegalArgumentException("联系人ID不能为空"))
        }

        // 2. 验证主题内容
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("主题内容不能为空"))
        }

        if (content.length > ConversationTopic.MAX_CONTENT_LENGTH) {
            return Result.failure(
                IllegalArgumentException(
                    "主题内容不能超过${ConversationTopic.MAX_CONTENT_LENGTH}字符"
                )
            )
        }

        // 3. 创建新主题
        val topic = ConversationTopic(
            contactId = contactId,
            content = content.trim(),
            isActive = true
        )

        // 4. 保存主题（Repository会自动停用旧主题）
        return topicRepository.setTopic(topic).map { topic }
    }
}
