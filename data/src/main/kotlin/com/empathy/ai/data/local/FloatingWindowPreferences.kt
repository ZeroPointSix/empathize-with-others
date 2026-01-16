package com.empathy.ai.data.local

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.FloatingWindowUiState
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.empathy.ai.domain.repository.FloatingWindowPreferencesRepository
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 悬浮窗状态持久化类
 * 
 * 实现FloatingWindowPreferencesRepository接口，遵循Clean Architecture原则
 */
@Singleton
class FloatingWindowPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) : FloatingWindowPreferencesRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun saveState(state: FloatingWindowState) {
        prefs.edit {
            putBoolean(KEY_IS_ENABLED, state.isEnabled)
            putInt(KEY_BUTTON_X, state.buttonX)
            putInt(KEY_BUTTON_Y, state.buttonY)
        }
    }
    
    override fun loadState(): FloatingWindowState {
        return FloatingWindowState(
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            buttonX = prefs.getInt(KEY_BUTTON_X, 0),
            buttonY = prefs.getInt(KEY_BUTTON_Y, 0)
        )
    }
    
    override fun saveEnabled(isEnabled: Boolean) {
        prefs.edit { putBoolean(KEY_IS_ENABLED, isEnabled) }
    }
    
    override fun isEnabled(): Boolean = prefs.getBoolean(KEY_IS_ENABLED, false)

    override fun saveButtonPosition(x: Int, y: Int) {
        require(x >= 0 && y >= 0) { "Position coordinates must be non-negative" }
        prefs.edit {
            putInt(KEY_BUTTON_X, x)
            putInt(KEY_BUTTON_Y, y)
        }
    }
    
    override fun getButtonX(): Int = prefs.getInt(KEY_BUTTON_X, 0)
    override fun getButtonY(): Int = prefs.getInt(KEY_BUTTON_Y, 0)
    
    override fun clear() { prefs.edit { clear() } }
    
    private val requestInfoAdapter by lazy {
        moshi.adapter(MinimizedRequestInfo::class.java)
    }

    override fun saveRequestInfo(requestInfo: MinimizedRequestInfo): Boolean {
        return try {
            val json = requestInfoAdapter.toJson(requestInfo)
            prefs.edit { putString(KEY_MINIMIZED_REQUEST, json) }
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save request info", e)
            false
        }
    }
    
    override fun getRequestInfo(): MinimizedRequestInfo? {
        val json = prefs.getString(KEY_MINIMIZED_REQUEST, null)
        if (json.isNullOrBlank()) return null
        return try {
            requestInfoAdapter.fromJson(json)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse request info", e)
            clearRequestInfo()
            null
        }
    }
    
    override fun clearRequestInfo() { prefs.edit { remove(KEY_MINIMIZED_REQUEST) } }
    
    override fun saveIndicatorPosition(x: Int, y: Int) {
        require(x >= 0 && y >= 0) { "Position coordinates must be non-negative" }
        prefs.edit {
            putInt(KEY_INDICATOR_X, x)
            putInt(KEY_INDICATOR_Y, y)
        }
    }
    
    override fun getIndicatorPosition(): Pair<Int, Int> {
        val x = prefs.getInt(KEY_INDICATOR_X, INVALID_POSITION)
        val y = prefs.getInt(KEY_INDICATOR_Y, INVALID_POSITION)
        return if (x == INVALID_POSITION || y == INVALID_POSITION) {
            getButtonPosition()
        } else {
            Pair(x, y)
        }
    }
    
    override fun getButtonPosition(): Pair<Int, Int> = Pair(getButtonX(), getButtonY())

    override fun saveSelectedTab(tabName: String) {
        prefs.edit { putString(KEY_SELECTED_TAB, tabName) }
    }
    
    override fun saveSelectedTab(tab: ActionType) { saveSelectedTab(tab.name) }
    
    override fun getSelectedTab(): String {
        return prefs.getString(KEY_SELECTED_TAB, DEFAULT_TAB_NAME) ?: DEFAULT_TAB_NAME
    }
    
    override fun getSelectedTabAsActionType(): ActionType {
        val tabName = getSelectedTab()
        return try {
            ActionType.valueOf(tabName)
        } catch (e: IllegalArgumentException) {
            ActionType.ANALYZE
        }
    }

    override fun saveLastContactId(contactId: String) {
        prefs.edit { putString(KEY_LAST_CONTACT_ID, contactId) }
    }
    
    override fun getLastContactId(): String? = prefs.getString(KEY_LAST_CONTACT_ID, null)

    override fun saveInputText(text: String) {
        prefs.edit { putString(KEY_SAVED_INPUT_TEXT, text) }
    }
    
    override fun getInputText(): String {
        return prefs.getString(KEY_SAVED_INPUT_TEXT, DEFAULT_INPUT_TEXT) ?: DEFAULT_INPUT_TEXT
    }

    override fun saveUiState(tabName: String, contactId: String?, inputText: String) {
        prefs.edit {
            putString(KEY_SELECTED_TAB, tabName)
            if (contactId != null) putString(KEY_LAST_CONTACT_ID, contactId)
            putString(KEY_SAVED_INPUT_TEXT, inputText)
            putBoolean(KEY_HAS_SAVED_STATE, true)
        }
    }

    override fun saveUiState(state: FloatingWindowUiState) {
        saveUiState(state.selectedTab.name, state.selectedContactId, state.inputText)
    }

    override fun restoreUiState(): Triple<String, String?, String> {
        return Triple(getSelectedTab(), getLastContactId(), getInputText())
    }

    override fun restoreUiStateAsObject(): FloatingWindowUiState? {
        if (!hasSavedUiState()) return null
        val (tabName, contactId, inputText) = restoreUiState()
        return FloatingWindowUiState.fromPersisted(tabName, contactId, inputText)
    }

    override fun clearSavedUiState() {
        prefs.edit {
            remove(KEY_SAVED_INPUT_TEXT)
            putBoolean(KEY_HAS_SAVED_STATE, false)
        }
    }

    override fun hasSavedUiState(): Boolean = prefs.getBoolean(KEY_HAS_SAVED_STATE, false)

    override fun saveBubblePosition(x: Int, y: Int) {
        prefs.edit {
            putInt(KEY_BUBBLE_X, x)
            putInt(KEY_BUBBLE_Y, y)
        }
    }

    override fun getBubblePosition(defaultX: Int, defaultY: Int): Pair<Int, Int> {
        val x = prefs.getInt(KEY_BUBBLE_X, INVALID_POSITION)
        val y = prefs.getInt(KEY_BUBBLE_Y, INVALID_POSITION)
        return if (x == INVALID_POSITION || y == INVALID_POSITION) {
            Pair(defaultX, defaultY)
        } else {
            Pair(x, y)
        }
    }

    override fun saveBubbleState(state: FloatingBubbleState) {
        prefs.edit { putString(KEY_BUBBLE_STATE, state.name) }
    }

    override fun getBubbleState(): FloatingBubbleState {
        val stateName = prefs.getString(KEY_BUBBLE_STATE, null)
        return if (stateName.isNullOrBlank()) {
            FloatingBubbleState.IDLE
        } else {
            try {
                FloatingBubbleState.valueOf(stateName)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e(TAG, "Invalid bubble state: $stateName", e)
                FloatingBubbleState.IDLE
            }
        }
    }

    override fun saveMinimizeState(requestInfo: String) {
        prefs.edit {
            putLong(KEY_MINIMIZE_TIMESTAMP, System.currentTimeMillis())
            putString(KEY_MINIMIZE_REQUEST_INFO, requestInfo)
        }
    }

    override fun getMinimizeStateIfValid(): String? {
        val timestamp = prefs.getLong(KEY_MINIMIZE_TIMESTAMP, 0)
        if (System.currentTimeMillis() - timestamp > MINIMIZE_VALIDITY_PERIOD) {
            clearMinimizeState()
            return null
        }
        return prefs.getString(KEY_MINIMIZE_REQUEST_INFO, null)
    }

    override fun clearMinimizeState() {
        prefs.edit {
            remove(KEY_MINIMIZE_TIMESTAMP)
            remove(KEY_MINIMIZE_REQUEST_INFO)
        }
    }

    override fun hasValidMinimizeState(): Boolean {
        val timestamp = prefs.getLong(KEY_MINIMIZE_TIMESTAMP, 0)
        return System.currentTimeMillis() - timestamp <= MINIMIZE_VALIDITY_PERIOD &&
               prefs.getString(KEY_MINIMIZE_REQUEST_INFO, null) != null
    }

    override fun saveDisplayMode(mode: String) {
        prefs.edit { putString(KEY_DISPLAY_MODE, mode) }
        android.util.Log.d(TAG, "保存显示模式: $mode")
    }

    override fun getDisplayMode(): String {
        return prefs.getString(KEY_DISPLAY_MODE, DISPLAY_MODE_BUBBLE) ?: DISPLAY_MODE_BUBBLE
    }

    override fun shouldStartAsBubble(): Boolean = getDisplayMode() == DISPLAY_MODE_BUBBLE

    override fun saveDisplayId(displayId: Int) {
        prefs.edit { putInt(KEY_DISPLAY_ID, displayId) }
    }

    override fun getDisplayId(): Int? {
        val savedId = prefs.getInt(KEY_DISPLAY_ID, INVALID_POSITION)
        return if (savedId == INVALID_POSITION) null else savedId
    }

    override fun saveContinuousScreenshotEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CONTINUOUS_SCREENSHOT_ENABLED, enabled) }
    }

    override fun isContinuousScreenshotEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONTINUOUS_SCREENSHOT_ENABLED, false)
    }

    override fun hasScreenshotPermission(): Boolean {
        return getMediaProjectionPermissionInternal() != null
    }

    override fun clearScreenshotPermission() {
        clearMediaProjectionPermissionInternal()
    }

    fun saveMediaProjectionPermission(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            clearMediaProjectionPermissionInternal()
            return
        }
        mediaProjectionPermissionCache = MediaProjectionPermission(resultCode, data)
        clearMediaProjectionPermissionPersisted()
        android.util.Log.d(TAG, "MediaProjection 授权已缓存（仅内存）")
    }

    fun getMediaProjectionPermission(): MediaProjectionPermission? {
        return getMediaProjectionPermissionInternal()
    }

    private fun getMediaProjectionPermissionInternal(): MediaProjectionPermission? {
        return mediaProjectionPermissionCache
    }

    private fun clearMediaProjectionPermissionInternal() {
        mediaProjectionPermissionCache = null
        clearMediaProjectionPermissionPersisted()
    }

    private fun clearMediaProjectionPermissionPersisted() {
        prefs.edit {
            remove(KEY_MEDIA_PROJECTION_RESULT_CODE)
            remove(KEY_MEDIA_PROJECTION_RESULT_DATA)
            remove(KEY_MEDIA_PROJECTION_DATA_VERSION)
        }
    }

    companion object {
        private const val TAG = "FloatingWindowPrefs"
        private const val PREFS_NAME = "floating_window_prefs"
        private const val KEY_IS_ENABLED = "is_enabled"
        private const val KEY_BUTTON_X = "button_x"
        private const val KEY_BUTTON_Y = "button_y"
        private const val KEY_MINIMIZED_REQUEST = "minimized_request"
        private const val KEY_INDICATOR_X = "indicator_x"
        private const val KEY_INDICATOR_Y = "indicator_y"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_LAST_CONTACT_ID = "last_contact_id"
        private const val KEY_SAVED_INPUT_TEXT = "saved_input_text"
        private const val KEY_HAS_SAVED_STATE = "has_saved_state"
        private const val KEY_BUBBLE_X = "bubble_position_x"
        private const val KEY_BUBBLE_Y = "bubble_position_y"
        private const val KEY_BUBBLE_STATE = "bubble_state"
        private const val KEY_MINIMIZE_TIMESTAMP = "minimize_timestamp"
        private const val KEY_MINIMIZE_REQUEST_INFO = "minimize_request_info"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_DISPLAY_ID = "display_id"
        private const val KEY_CONTINUOUS_SCREENSHOT_ENABLED = "continuous_screenshot_enabled"
        private const val KEY_MEDIA_PROJECTION_RESULT_CODE = "media_projection_result_code"
        private const val KEY_MEDIA_PROJECTION_RESULT_DATA = "media_projection_result_data"
        private const val KEY_MEDIA_PROJECTION_DATA_VERSION = "media_projection_data_version"
        private const val MINIMIZE_VALIDITY_PERIOD = 10 * 60 * 1000L
        const val DISPLAY_MODE_BUBBLE = "BUBBLE"
        const val DISPLAY_MODE_DIALOG = "DIALOG"
        const val DEFAULT_TAB_NAME = "ANALYZE"
        const val DEFAULT_INPUT_TEXT = ""
        const val DEFAULT_POSITION = 0
        const val INVALID_POSITION = -1
        @Volatile
        private var mediaProjectionPermissionCache: MediaProjectionPermission? = null
    }
}

data class MediaProjectionPermission(
    val resultCode: Int,
    val data: Intent
)
