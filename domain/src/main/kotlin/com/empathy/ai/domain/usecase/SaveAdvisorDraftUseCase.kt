package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import javax.inject.Inject

/**
 * 保存AI军师会话草稿用例
 *
 * 将用户当前输入保存为会话草稿，便于后续恢复。
 */
class SaveAdvisorDraftUseCase @Inject constructor(
    private val preferences: AiAdvisorPreferencesRepository
) {
    /**
     * @param sessionId 会话ID
     * @param draft 草稿内容
     */
    suspend operator fun invoke(sessionId: String, draft: String): Result<Unit> {
        return runCatching { preferences.setDraft(sessionId, draft) }
    }
}
