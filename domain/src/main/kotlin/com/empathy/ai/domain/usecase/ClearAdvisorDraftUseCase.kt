package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import javax.inject.Inject

/**
 * 清除AI军师会话草稿用例
 *
 * 发送消息或主动清空输入时移除对应会话草稿。
 */
class ClearAdvisorDraftUseCase @Inject constructor(
    private val preferences: AiAdvisorPreferencesRepository
) {
    /**
     * @param sessionId 会话ID
     */
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        return runCatching { preferences.clearDraft(sessionId) }
    }
}
