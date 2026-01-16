package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.repository.ContactSortPreferencesRepository
import javax.inject.Inject

/**
 * Load contact list sort preference.
 */
class GetContactSortOptionUseCase @Inject constructor(
    private val repository: ContactSortPreferencesRepository
) {
    suspend operator fun invoke(): Result<ContactSortOption> {
        return repository.getSortOption()
    }
}
