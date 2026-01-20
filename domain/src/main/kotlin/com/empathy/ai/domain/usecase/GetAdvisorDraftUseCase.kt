package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import javax.inject.Inject

/**
 * 获取AI军师会话草稿用例
 *
 * 读取指定会话的草稿内容，用于恢复未发送的输入。
 */
class GetAdvisorDraftUseCase @Inject constructor(
    private val preferences: AiAdvisorPreferencesRepository
) {
    /**
     * @param sessionId 会话ID
     * @return 草稿内容（可能为null）
     */
    suspend operator fun invoke(sessionId: String): Result<String?> {
        return runCatching { preferences.getDraft(sessionId) }
    }
}
