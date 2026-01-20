package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import javax.inject.Inject

/**
 * Clear recent contact history.
 */
class ClearContactRecentHistoryUseCase @Inject constructor(
    private val repository: ContactRecentHistoryRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.clearRecentContacts()
}
