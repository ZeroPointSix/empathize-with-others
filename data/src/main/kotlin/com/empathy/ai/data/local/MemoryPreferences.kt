package com.empathy.ai.data.local

import android.content.Context
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.MemoryConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 记忆系统配置存储
 *
 * 使用SharedPreferences存储记忆系统相关的配置
 */
@Singleton
class MemoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(
        MemoryConstants.PREFS_MEMORY_SETTINGS,
        Context.MODE_PRIVATE
    )

    /**
     * 获取最后总结日期
     */
    fun getLastSummaryDate(): String {
        return prefs.getString(MemoryConstants.KEY_LAST_SUMMARY_DATE, "") ?: ""
    }

    /**
     * 设置最后总结日期
     */
    fun setLastSummaryDate(date: String) {
        prefs.edit().putString(MemoryConstants.KEY_LAST_SUMMARY_DATE, date).apply()
    }

    /**
     * 获取最后清理日期
     */
    fun getLastCleanupDate(): String {
        return prefs.getString(MemoryConstants.KEY_LAST_CLEANUP_DATE, "") ?: ""
    }

    /**
     * 设置最后清理日期
     */
    fun setLastCleanupDate(date: String) {
        prefs.edit().putString(MemoryConstants.KEY_LAST_CLEANUP_DATE, date).apply()
    }

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     */
    fun getCurrentDateString(): String = DateUtils.getCurrentDateString()
}
