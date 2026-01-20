package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import javax.inject.Inject

/**
 * Record a contact visit into recent history.
 */
class RecordContactVisitUseCase @Inject constructor(
    private val repository: ContactRecentHistoryRepository
) {
    suspend operator fun invoke(contactId: String): Result<List<String>> =
        repository.recordContactVisit(contactId)
}
