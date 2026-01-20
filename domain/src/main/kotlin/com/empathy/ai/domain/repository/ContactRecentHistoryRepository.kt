package com.empathy.ai.domain.repository

/**
 * Repository for tracking recently visited contacts.
 */
interface ContactRecentHistoryRepository {
    /**
     * Get recent contact ids (most recent first).
     */
    suspend fun getRecentContactIds(): Result<List<String>>

    /**
     * Record a contact visit and return updated history.
     */
    suspend fun recordContactVisit(contactId: String): Result<List<String>>

    /**
     * Clear recent contact history.
     */
    suspend fun clearRecentContacts(): Result<Unit>
}
