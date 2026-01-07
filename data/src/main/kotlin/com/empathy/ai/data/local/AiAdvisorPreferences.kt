package com.empathy.ai.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI军师偏好设置存储实现
 *
 * 使用EncryptedSharedPreferences加密存储用户偏好设置，
 * 包括上次使用的联系人ID和会话ID，用于实现自动恢复功能。
 *
 * ## 业务背景 (PRD-00029)
 * - 自动恢复上次联系人：进入AI军师时自动加载上次使用的联系人
 * - 首次使用引导：无历史联系人时进入联系人选择页面
 * - 会话连续性：支持恢复上次会话（可选）
 *
 * ## 设计决策 (TDD-00029)
 * - 使用EncryptedSharedPreferences确保数据安全
 * - 采用AES256_GCM加密方案
 * - 单例模式确保全局唯一实例
 * - 实现AiAdvisorPreferencesRepository接口，遵循Clean Architecture
 *
 * ## 存储字段
 * | 字段 | 类型 | 说明 |
 * |------|------|------|
 * | lastContactId | String | 上次使用的联系人ID |
 * | lastSessionId | String? | 上次使用的会话ID（可选） |
 *
 * @see com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
 * @see com.empathy.ai.presentation.viewmodel.AiAdvisorChatViewModel
 */
@Singleton
class AiAdvisorPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : AiAdvisorPreferencesRepository {
    companion object {
        private const val PREFS_NAME = "ai_advisor_preferences"
        private const val KEY_LAST_CONTACT_ID = "last_contact_id"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 获取上次使用的联系人ID
     *
     * 业务规则 (PRD-00029/US-003):
     * - 返回null表示首次使用，应导航到联系人选择页面
     * - 返回非空值表示有历史记录，应自动恢复该联系人
     *
     * @return 联系人ID，如果不存在返回null
     */
    override fun getLastContactId(): String? {
        return prefs.getString(KEY_LAST_CONTACT_ID, null)
    }

    /**
     * 保存上次使用的联系人ID
     *
     * 调用时机 (TDD-00029):
     * - 用户选择联系人时
     * - 进入对话界面时
     * - 切换联系人时
     *
     * @param contactId 联系人ID
     */
    override fun setLastContactId(contactId: String) {
        prefs.edit().putString(KEY_LAST_CONTACT_ID, contactId).apply()
    }

    /**
     * 获取上次使用的会话ID
     *
     * 业务规则 (PRD-00029):
     * - 当前版本不加载历史会话内容，仅用于记录
     * - 后续版本可用于恢复上次会话
     *
     * @return 会话ID，如果不存在返回null
     */
    override fun getLastSessionId(): String? {
        return prefs.getString(KEY_LAST_SESSION_ID, null)
    }

    /**
     * 保存上次使用的会话ID
     *
     * @param sessionId 会话ID，传null时清除记录
     */
    override fun setLastSessionId(sessionId: String?) {
        if (sessionId != null) {
            prefs.edit().putString(KEY_LAST_SESSION_ID, sessionId).apply()
        } else {
            prefs.edit().remove(KEY_LAST_SESSION_ID).apply()
        }
    }

    /**
     * 清除所有偏好设置
     *
     * 使用场景:
     * - 用户登出
     * - 清除应用数据
     * - 重置AI军师状态
     */
    override fun clear() {
        prefs.edit().clear().apply()
    }
}
