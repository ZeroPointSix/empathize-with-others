package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.util.CleanupPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CleanupPreferences的Android实现
 * 
 * 使用SharedPreferences存储清理相关的偏好设置
 */
@Singleton
class CleanupPreferencesImpl @Inject constructor(
    @ApplicationContext context: Context
) : CleanupPreferences {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getLastCleanupDate(): String {
        return prefs.getString(KEY_LAST_CLEANUP_DATE, "") ?: ""
    }

    override fun setLastCleanupDate(date: String) {
        prefs.edit().putString(KEY_LAST_CLEANUP_DATE, date).apply()
    }

    override fun getCurrentDateString(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    companion object {
        private const val PREFS_NAME = "cleanup_preferences"
        private const val KEY_LAST_CLEANUP_DATE = "last_cleanup_date"
    }
}
