package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorRepository
import javax.inject.Inject

/**
 * 删除AI军师对话记录用例
 *
 * 删除单条对话记录。
 *
 * 业务背景:
 *   - PRD-00026/3.1.1: 支持消息删除功能（仅删除当前会话）
 *   - 场景: 用户删除不想要的对话记录
 *
 * 注意事项:
 *   - 仅删除单条记录，不是清空整个会话
 *   - 会话内其他对话记录不受影响
 *
 * @see AiAdvisorRepository.deleteConversation 仓库删除方法
 */
class DeleteAdvisorConversationUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository
) {
    /**
     * 删除对话记录
     *
     * @param conversationId 对话记录ID
     * @return 删除结果
     */
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return aiAdvisorRepository.deleteConversation(conversationId)
    }
}
