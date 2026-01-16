package com.empathy.ai.domain.model

/**
 * Contact list sort options.
 *
 * Stored as stable keys for persistence.
 */
enum class ContactSortOption(val storageKey: String) {
    NAME("name"),
    LAST_INTERACTION("last_interaction"),
    RELATIONSHIP_SCORE("relationship_score");

    companion object {
        fun fromStorageKey(key: String?): ContactSortOption {
            return values().firstOrNull { it.storageKey == key } ?: NAME
        }
    }
}
