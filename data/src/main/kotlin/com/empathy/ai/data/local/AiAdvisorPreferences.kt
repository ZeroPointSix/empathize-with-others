package com.empathy.ai.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.empathy.ai.domain.repository.AiAdvisorPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
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
 * | draftsBySession | String | 会话草稿JSON映射 |
 * | draftSessionIndex | String | 草稿会话索引JSON数组 |
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
        private const val KEY_DRAFTS = "drafts_by_session"
        private const val KEY_DRAFT_INDEX = "draft_session_index"
        private const val MAX_DRAFT_COUNT = 12
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
     * 获取指定会话的草稿内容
     *
     * 业务规则 (FREE-20260118):
     * - 返回null表示无草稿或草稿已清除
     * - 草稿为空字符串时应视为无草稿
     * - 会话切换时优先恢复草稿，提供无缝输入体验
     *
     * 设计权衡:
     * - 使用JSONObject存储多会话草稿，减少SharedPreferences条目数量
     * - 避免频繁解析完整JSON，采用lazy读取策略
     *
     * @param sessionId 会话ID
     * @return 草稿内容，或null
     */
    override fun getDraft(sessionId: String): String? {
        if (sessionId.isBlank()) return null
        val drafts = readDrafts()
        val draft = drafts.optString(sessionId, "")
        return draft.takeIf { it.isNotBlank() }
    }

    /**
     * 保存指定会话的草稿内容
     *
     * 业务规则 (FREE-20260118):
     * - 草稿为空时自动清除该会话的草稿
     * - 最多保留12个会话的草稿（防止存储无限增长）
     * - 最近使用的会话草稿排在索引前面
     *
     * 设计权衡 (TDD-FREE-20260118):
     * - 采用LRU策略淘汰旧草稿：超过MAX_DRAFT_COUNT时删除最旧草稿
     * - 权衡：牺牲最旧草稿的持久化，换取存储空间可控
     * - 草稿索引与草稿数据分离存储，便于快速查询最近使用顺序
     *
     * @param sessionId 会话ID
     * @param draft 草稿内容
     */
    override fun setDraft(sessionId: String, draft: String) {
        if (sessionId.isBlank()) return
        if (draft.isBlank()) {
            clearDraft(sessionId)
            return
        }

        val drafts = readDrafts()
        drafts.put(sessionId, draft)

        val updatedIndex = buildList {
            add(sessionId)
            addAll(readDraftIndex().filterNot { it == sessionId })
        }
        val prunedIndex = updatedIndex.take(MAX_DRAFT_COUNT)
        val removedIds = updatedIndex.drop(MAX_DRAFT_COUNT)
        removedIds.forEach { drafts.remove(it) }

        writeDrafts(drafts)
        writeDraftIndex(prunedIndex)
    }

    /**
     * 清除指定会话的草稿内容
     *
     * 调用场景:
     * - 用户发送消息后自动清除草稿
     * - 用户主动清空输入框
     * - 会话被删除时同步清理草稿
     *
     * @param sessionId 会话ID
     */
    override fun clearDraft(sessionId: String) {
        if (sessionId.isBlank()) return
        val drafts = readDrafts()
        drafts.remove(sessionId)
        writeDrafts(drafts)
        writeDraftIndex(readDraftIndex().filterNot { it == sessionId })
    }

    /**
     * 清除所有会话草稿
     *
     * 使用场景:
     * - 设置页"清除AI军师草稿"功能
     * - 用户主动请求清除所有草稿
     *
     * 设计权衡:
     * - 一次性移除所有草稿，避免多次磁盘写入
     */
    override fun clearAllDrafts() {
        prefs.edit()
            .remove(KEY_DRAFTS)
            .remove(KEY_DRAFT_INDEX)
            .apply()
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

    /**
     * 读取草稿数据JSON
     *
     * 容错处理:
     * - JSON解析失败时返回空JSONObject，避免数据损坏导致崩溃
     * - 使用takeIf过滤空白值，确保getDraft返回null而非空字符串
     */
    private fun readDrafts(): JSONObject {
        val raw = prefs.getString(KEY_DRAFTS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    /**
     * 写入草稿数据JSON
     *
     * 性能优化:
     * - 直接将JSONObject转为String存储，减少解析开销
     */
    private fun writeDrafts(drafts: JSONObject) {
        prefs.edit().putString(KEY_DRAFTS, drafts.toString()).apply()
    }

    /**
     * 读取草稿会话索引
     *
     * 用途:
     * - 追踪最近使用过的会话顺序
     * - 支持LRU淘汰策略
     *
     * 容错:
     * - 过滤空白和无效的会话ID
     * - JSONArray解析失败时返回空列表
     */
    private fun readDraftIndex(): List<String> {
        val raw = prefs.getString(KEY_DRAFT_INDEX, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optString(index).trim()
                    if (item.isNotBlank()) {
                        add(item)
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 写入草稿会话索引
     *
     * 设计权衡:
     * - 使用JSONArray而非分隔字符串，便于扩展和容错
     */
    private fun writeDraftIndex(index: List<String>) {
        val array = JSONArray()
        index.forEach { array.put(it) }
        prefs.edit().putString(KEY_DRAFT_INDEX, array.toString()).apply()
    }
}
