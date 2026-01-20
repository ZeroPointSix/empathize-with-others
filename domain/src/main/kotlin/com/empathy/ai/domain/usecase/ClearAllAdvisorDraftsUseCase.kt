package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import javax.inject.Inject

class ClearAllAdvisorDraftsUseCase @Inject constructor(
    private val preferences: AiAdvisorPreferencesRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return runCatching { preferences.clearAllDrafts() }
    }
}
