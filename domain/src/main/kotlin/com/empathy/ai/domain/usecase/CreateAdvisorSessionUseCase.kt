package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.AiAdvisorSession
import com.empathy.ai.domain.repository.AiAdvisorRepository
import javax.inject.Inject

/**
 * 创建AI军师会话用例
 *
 * 为指定联系人创建新的AI军师对话会话。
 *
 * 业务背景:
 *   - PRD-00026/3.1.3: 对话会话管理
 *   - 场景: 用户首次进入AI军师对话或主动创建新会话
 *
 * 设计决策:
 *   - 每个联系人可有多个会话（独立对话主题）
 *   - 默认标题"新对话"，支持用户自定义
 *   - 会话创建后立即返回，供调用方使用
 *
 * @see AiAdvisorSession 会话领域模型
 * @see AiAdvisorRepository.createSession 仓库创建方法
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
