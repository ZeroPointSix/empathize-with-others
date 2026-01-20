package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.domain.repository.ContactRecentHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed recent contact history.
 */
@Singleton
class ContactRecentHistoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactRecentHistoryRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getRecentContactIds(): Result<List<String>> {
        return try {
            Result.success(readHistory())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordContactVisit(contactId: String): Result<List<String>> {
        return try {
            val normalized = contactId.trim()
            if (normalized.isBlank()) {
                return Result.success(readHistory())
            }

            val existing = readHistory()
            val updated = buildList {
                add(normalized)
                addAll(existing.filterNot { it == normalized })
            }.take(MAX_HISTORY_SIZE)

            writeHistory(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearRecentContacts(): Result<Unit> {
        return try {
            prefs.edit().remove(KEY_HISTORY).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val array = JSONArray(raw)
        val result = ArrayList<String>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optString(index).trim()
            if (item.isNotBlank()) {
                result.add(item)
            }
        }
        return result
    }

    private fun writeHistory(history: List<String>) {
        val array = JSONArray()
        history.forEach { array.put(it) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private companion object {
        private const val PREFS_NAME = "contact_recent_history_preferences"
        private const val KEY_HISTORY = "contact_recent_history"
        private const val MAX_HISTORY_SIZE = 5
    }
}
