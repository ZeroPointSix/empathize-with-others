package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.repository.ContactSortPreferencesRepository
import javax.inject.Inject

/**
 * Persist contact list sort preference.
 */
class SaveContactSortOptionUseCase @Inject constructor(
    private val repository: ContactSortPreferencesRepository
) {
    suspend operator fun invoke(option: ContactSortOption): Result<Unit> {
        return repository.setSortOption(option)
    }
}
