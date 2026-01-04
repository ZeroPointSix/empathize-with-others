package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorRepository
import javax.inject.Inject

/**
 * 删除AI军师对话记录用例
 *
 * 删除单条对话记录。
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
