package com.empathy.ai.domain.usecase

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import javax.inject.Inject

/**
 * Sort contacts by selected option.
 */
class SortContactsUseCase @Inject constructor() {
    operator fun invoke(
        contacts: List<ContactProfile>,
        option: ContactSortOption
    ): List<ContactProfile> {
        return when (option) {
            ContactSortOption.NAME -> contacts.sortedWith(compareBy { it.name })
            ContactSortOption.LAST_INTERACTION -> contacts.sortedWith(
                compareByDescending<ContactProfile> { it.lastInteractionDate ?: "" }
                    .thenBy { it.name }
            )
            ContactSortOption.RELATIONSHIP_SCORE -> contacts.sortedWith(
                compareByDescending<ContactProfile> { it.relationshipScore }
                    .thenBy { it.name }
            )
        }
    }
}
