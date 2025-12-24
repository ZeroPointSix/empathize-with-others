package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.empathy.ai.domain.model.ConversationContextConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话上下文配置存储
 *
 * 使用 SharedPreferences 存储对话上下文相关的配置信息
 *
 * 功能:
 * - 历史对话条数配置（0/5/10）
 * - 提供线程安全的读写操作
 */
@Singleton
class ConversationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "conversation_settings"
        private const val KEY_HISTORY_CONVERSATION_COUNT = "history_conversation_count"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 获取历史对话条数
     *
     * @return 历史条数（0/5/10），默认5
     */
    fun getHistoryConversationCount(): Int {
        return prefs.getInt(
            KEY_HISTORY_CONVERSATION_COUNT,
            ConversationContextConfig.DEFAULT_HISTORY_COUNT
        )
    }

    /**
     * 设置历史对话条数
     *
     * @param count 条数，必须是 0/5/10 之一
     */
    fun setHistoryConversationCount(count: Int) {
        require(count in ConversationContextConfig.HISTORY_COUNT_OPTIONS) {
            "历史条数必须是 ${ConversationContextConfig.HISTORY_COUNT_OPTIONS.joinToString()} 之一"
        }
        prefs.edit {
            putInt(KEY_HISTORY_CONVERSATION_COUNT, count)
        }
    }
}
