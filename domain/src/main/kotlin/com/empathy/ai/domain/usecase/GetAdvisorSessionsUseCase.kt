package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import javax.inject.Inject

/**
 * 获取AI军师会话列表用例
 *
 * 获取指定联系人的所有AI军师对话会话。
 */
class GetAdvisorSessionsUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository
) {
    /**
     * 获取联系人的会话列表
     *
     * @param contactId 联系人ID
     * @return 会话列表，按更新时间降序排列
     */
    suspend operator fun invoke(contactId: String): Result<List<AiAdvisorSession>> {
        return aiAdvisorRepository.getSessions(contactId)
    }
}
