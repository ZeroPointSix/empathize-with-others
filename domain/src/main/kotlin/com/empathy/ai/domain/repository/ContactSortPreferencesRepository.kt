package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ContactSortOption

/**
 * Contact list sort preference repository.
 */
interface ContactSortPreferencesRepository {
    suspend fun getSortOption(): Result<ContactSortOption>

    suspend fun setSortOption(option: ContactSortOption): Result<Unit>
}
