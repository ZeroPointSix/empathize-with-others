package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import javax.inject.Inject

/**
 * 清除AI军师偏好用例
 *
 * 清除联系人/会话偏好与草稿等本地状态。
 */
class ClearAdvisorPreferencesUseCase @Inject constructor(
    private val preferences: AiAdvisorPreferencesRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return runCatching { preferences.clear() }
    }
}
