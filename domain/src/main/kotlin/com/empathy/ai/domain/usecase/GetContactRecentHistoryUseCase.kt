package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import javax.inject.Inject

/**
 * Load recent contact ids for quick access.
 */
class GetContactRecentHistoryUseCase @Inject constructor(
    private val repository: ContactRecentHistoryRepository
) {
    suspend operator fun invoke(): Result<List<String>> = repository.getRecentContactIds()
}
