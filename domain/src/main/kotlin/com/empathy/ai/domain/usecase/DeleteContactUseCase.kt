package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 删除联系人用例
 *
 * 职责:
 *   1. 根据联系人ID删除联系人
 *   2. 验证输入参数
 *   3. 统一错误处理
 *
 * 业务背景:
 *   - PRD-00003: 联系人画像记忆系统需求
 *   - 场景: 用户删除不再需要的联系人
 *
 * 级联删除:
 *   - 联系人的 facts、tags、对话记录等通过外键约束自动删除
 *   - AI军师会话和对话通过数据库外键 CASCADE 策略删除
 *   - 草稿存储在偏好中，需在删除成功后手动清理
 *
 * @see ContactRepository.deleteProfile 仓库删除方法
 */
class DeleteContactUseCase @Inject constructor(
    private val contactRepository: ContactRepository,
    private val aiAdvisorRepository: AiAdvisorRepository,
    private val clearAdvisorDraftUseCase: ClearAdvisorDraftUseCase
) {
    /**
     * 执行用例
     *
     * @param contactId 联系人ID
     * @return Result<Unit> 成功时返回Unit，失败时返回异常
     */
    suspend operator fun invoke(contactId: String): Result<Unit> {
        return try {
            if (contactId.isBlank()) {
                return Result.failure(IllegalArgumentException("联系人ID不能为空"))
            }

            val sessions = aiAdvisorRepository.getSessions(contactId).getOrDefault(emptyList())
            val deleteResult = contactRepository.deleteProfile(contactId)
            if (deleteResult.isSuccess) {
                // Best-effort draft cleanup to keep storage aligned with session lifecycle.
                // 设计权衡 (FREE-20260118): 草稿清理失败不应阻断联系人删除主流程
                sessions.forEach { session -> clearAdvisorDraftUseCase(session.id) }
            }
            deleteResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
