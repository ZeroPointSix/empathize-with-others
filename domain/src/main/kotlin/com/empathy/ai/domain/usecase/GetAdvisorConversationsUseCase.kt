package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorConversation
import com.empathy.ai.domain.repository.AiAdvisorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取AI军师对话记录用例
 *
 * 获取指定会话的对话记录流（响应式）。
 */
class GetAdvisorConversationsUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository
) {
    /**
     * 获取会话的对话记录流
     *
     * @param sessionId 会话ID
     * @return 对话记录流，按时间戳升序排列
     */
    operator fun invoke(sessionId: String): Flow<List<AiAdvisorConversation>> {
        return aiAdvisorRepository.getConversationsFlow(sessionId)
    }
}
