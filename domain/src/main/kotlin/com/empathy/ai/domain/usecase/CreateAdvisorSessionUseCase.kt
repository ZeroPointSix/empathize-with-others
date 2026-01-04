package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import javax.inject.Inject

/**
 * 创建AI军师会话用例
 *
 * 为指定联系人创建新的AI军师对话会话。
 */
class CreateAdvisorSessionUseCase @Inject constructor(
    private val aiAdvisorRepository: AiAdvisorRepository
) {
    /**
     * 创建新会话
     *
     * @param contactId 联系人ID
     * @param title 会话标题，默认为"新对话"
     * @return 创建的会话对象
     */
    suspend operator fun invoke(
        contactId: String,
        title: String = "新对话"
    ): Result<AiAdvisorSession> {
        val session = AiAdvisorSession.create(
            contactId = contactId,
            title = title
        )
        return aiAdvisorRepository.createSession(session).map { session }
    }
}
