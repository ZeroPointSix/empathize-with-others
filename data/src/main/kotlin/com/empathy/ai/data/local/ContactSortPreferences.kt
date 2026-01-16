package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.repository.ContactSortPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed contact list sort preference.
 */
@Singleton
class ContactSortPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : ContactSortPreferencesRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getSortOption(): Result<ContactSortOption> {
        return try {
            val key = prefs.getString(KEY_SORT_OPTION, null)
            Result.success(ContactSortOption.fromStorageKey(key))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setSortOption(option: ContactSortOption): Result<Unit> {
        return try {
            prefs.edit()
                .putString(KEY_SORT_OPTION, option.storageKey)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        private const val PREFS_NAME = "contact_list_preferences"
        private const val KEY_SORT_OPTION = "contact_sort_option"
    }
}
