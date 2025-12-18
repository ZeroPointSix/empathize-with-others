package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.MinimizedRequestInfo
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 悬浮窗状态持久化类
 * 
 * 职责：
 * - 保存悬浮窗的启用状态和位置信息
 * - 加载悬浮窗的配置
 * - 保存和恢复最小化请求信息
 * - 保存和恢复最小化指示器位置
 * - 提供线程安全的读写操作
 * 
 * 使用 SharedPreferences 存储配置信息
 */
@Singleton
class FloatingWindowPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * 保存悬浮窗状态
     * 
     * @param state 悬浮窗状态
     */
    fun saveState(state: FloatingWindowState) {
        prefs.edit {
            putBoolean(KEY_IS_ENABLED, state.isEnabled)
            putInt(KEY_BUTTON_X, state.buttonX)
            putInt(KEY_BUTTON_Y, state.buttonY)
        }
    }
    
    /**
     * 加载悬浮窗状态
     * 
     * @return 悬浮窗状态，如果没有保存过则返回默认值
     */
    fun loadState(): FloatingWindowState {
        return FloatingWindowState(
            isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false),
            buttonX = prefs.getInt(KEY_BUTTON_X, 0),
            buttonY = prefs.getInt(KEY_BUTTON_Y, 0)
        )
    }
    
    /**
     * 保存悬浮窗启用状态
     * 
     * @param isEnabled 是否启用
     */
    fun saveEnabled(isEnabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_IS_ENABLED, isEnabled)
        }
    }
    
    /**
     * 获取悬浮窗启用状态
     * 
     * @return 是否启用
     */
    fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_IS_ENABLED, false)
    }
    
    /**
     * 保存悬浮按钮位置
     * 
     * @param x X 坐标
     * @param y Y 坐标
     * @throws IllegalArgumentException 如果坐标为负数
     */
    fun saveButtonPosition(x: Int, y: Int) {
        require(x >= 0 && y >= 0) { "Position coordinates must be non-negative: x=$x, y=$y" }
        prefs.edit {
            putInt(KEY_BUTTON_X, x)
            putInt(KEY_BUTTON_Y, y)
        }
    }
    
    /**
     * 获取悬浮按钮 X 坐标
     * 
     * @return X 坐标
     */
    fun getButtonX(): Int {
        return prefs.getInt(KEY_BUTTON_X, 0)
    }
    
    /**
     * 获取悬浮按钮 Y 坐标
     * 
     * @return Y 坐标
     */
    fun getButtonY(): Int {
        return prefs.getInt(KEY_BUTTON_Y, 0)
    }
    
    /**
     * 清除所有保存的状态
     */
    fun clear() {
        prefs.edit {
            clear()
        }
    }
    
    // 缓存JSON适配器，避免重复创建（CR-00014 CR-010优化）
    private val requestInfoAdapter by lazy {
        moshi.adapter(MinimizedRequestInfo::class.java)
    }

    /**
     * 保存最小化请求信息
     * 
     * 使用 Moshi 将请求信息序列化为 JSON 并保存到 SharedPreferences
     * 
     * @param requestInfo 请求信息
     * @return true 如果保存成功，false 如果序列化失败
     */
    fun saveRequestInfo(requestInfo: MinimizedRequestInfo): Boolean {
        return try {
            val json = requestInfoAdapter.toJson(requestInfo)
            prefs.edit {
                putString(KEY_MINIMIZED_REQUEST, json)
            }
            true
        } catch (e: Exception) {
            // 序列化失败，记录日志但不抛出异常
            android.util.Log.e(TAG, "Failed to save request info: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取最小化请求信息
     * 
     * 从 SharedPreferences 读取 JSON 并使用 Moshi 反序列化
     * 
     * @return 请求信息，如果不存在或解析失败则返回 null
     */
    fun getRequestInfo(): MinimizedRequestInfo? {
        val json = prefs.getString(KEY_MINIMIZED_REQUEST, null)
        if (json.isNullOrBlank()) {
            return null
        }
        return try {
            requestInfoAdapter.fromJson(json)
        } catch (e: Exception) {
            // 反序列化失败，记录日志并返回 null
            android.util.Log.e(TAG, "Failed to parse request info: ${e.message}", e)
            // 清除损坏的数据
            clearRequestInfo()
            null
        }
    }
    
    /**
     * 清除最小化请求信息
     */
    fun clearRequestInfo() {
        prefs.edit {
            remove(KEY_MINIMIZED_REQUEST)
        }
    }
    
    /**
     * 保存最小化指示器位置
     * 
     * @param x X 坐标
     * @param y Y 坐标
     * @throws IllegalArgumentException 如果坐标为负数
     */
    fun saveIndicatorPosition(x: Int, y: Int) {
        require(x >= 0 && y >= 0) { "Position coordinates must be non-negative: x=$x, y=$y" }
        prefs.edit {
            putInt(KEY_INDICATOR_X, x)
            putInt(KEY_INDICATOR_Y, y)
        }
    }
    
    /**
     * 获取最小化指示器位置
     * 
     * 如果没有保存过指示器位置，则返回悬浮按钮的位置作为默认值
     * 
     * @return 指示器位置 (x, y)
     */
    fun getIndicatorPosition(): Pair<Int, Int> {
        val x = prefs.getInt(KEY_INDICATOR_X, INVALID_POSITION)
        val y = prefs.getInt(KEY_INDICATOR_Y, INVALID_POSITION)
        
        // 如果没有保存过指示器位置，使用悬浮按钮位置
        return if (x == INVALID_POSITION || y == INVALID_POSITION) {
            getButtonPosition()
        } else {
            Pair(x, y)
        }
    }
    
    /**
     * 获取悬浮按钮位置
     * 
     * @return 按钮位置 (x, y)
     */
    fun getButtonPosition(): Pair<Int, Int> {
        return Pair(getButtonX(), getButtonY())
    }
    
    // ==================== TD-00009 阶段2: 状态管理扩展 ====================

    /**
     * 保存选中的Tab
     *
     * @param tabName Tab名称（ActionType.name）
     */
    fun saveSelectedTab(tabName: String) {
        prefs.edit {
            putString(KEY_SELECTED_TAB, tabName)
        }
    }

    /**
     * 保存选中的Tab（使用ActionType）
     *
     * @param tab ActionType枚举值
     */
    fun saveSelectedTab(tab: com.empathy.ai.domain.model.ActionType) {
        saveSelectedTab(tab.name)
    }

    /**
     * 获取上次选中的Tab
     *
     * @return Tab名称，默认返回DEFAULT_TAB_NAME
     */
    fun getSelectedTab(): String {
        return prefs.getString(KEY_SELECTED_TAB, DEFAULT_TAB_NAME) ?: DEFAULT_TAB_NAME
    }

    /**
     * 获取上次选中的Tab（返回ActionType）
     *
     * @return ActionType枚举值，默认返回ANALYZE
     */
    fun getSelectedTabAsActionType(): com.empathy.ai.domain.model.ActionType {
        val tabName = getSelectedTab()
        return try {
            com.empathy.ai.domain.model.ActionType.valueOf(tabName)
        } catch (e: IllegalArgumentException) {
            com.empathy.ai.domain.model.ActionType.ANALYZE
        }
    }

    /**
     * 保存上次对话的联系人ID
     *
     * @param contactId 联系人ID
     */
    fun saveLastContactId(contactId: String) {
        prefs.edit {
            putString(KEY_LAST_CONTACT_ID, contactId)
        }
    }

    /**
     * 获取上次对话的联系人ID
     *
     * @return 联系人ID，如果没有则返回null
     */
    fun getLastContactId(): String? {
        return prefs.getString(KEY_LAST_CONTACT_ID, null)
    }

    /**
     * 保存输入框内容（用于最小化时保持状态）
     *
     * @param text 输入框内容
     */
    fun saveInputText(text: String) {
        prefs.edit {
            putString(KEY_SAVED_INPUT_TEXT, text)
        }
    }

    /**
     * 获取保存的输入框内容
     *
     * @return 输入框内容，如果没有则返回空字符串
     */
    fun getInputText(): String {
        return prefs.getString(KEY_SAVED_INPUT_TEXT, DEFAULT_INPUT_TEXT) ?: DEFAULT_INPUT_TEXT
    }

    /**
     * 保存完整的UI状态（用于最小化时保持状态）
     *
     * @param tabName 当前Tab名称
     * @param contactId 当前联系人ID
     * @param inputText 输入框内容
     */
    fun saveUiState(tabName: String, contactId: String?, inputText: String) {
        prefs.edit {
            putString(KEY_SELECTED_TAB, tabName)
            if (contactId != null) {
                putString(KEY_LAST_CONTACT_ID, contactId)
            }
            putString(KEY_SAVED_INPUT_TEXT, inputText)
            putBoolean(KEY_HAS_SAVED_STATE, true)
        }
    }

    /**
     * 保存完整的UI状态（使用FloatingWindowUiState对象）
     *
     * @param state UI状态对象
     */
    fun saveUiState(state: com.empathy.ai.domain.model.FloatingWindowUiState) {
        saveUiState(
            tabName = state.selectedTab.name,
            contactId = state.selectedContactId,
            inputText = state.inputText
        )
    }

    /**
     * 恢复UI状态
     *
     * @return Triple(tabName, contactId, inputText)，如果没有保存的状态则返回默认值
     */
    fun restoreUiState(): Triple<String, String?, String> {
        return Triple(
            getSelectedTab(),
            getLastContactId(),
            getInputText()
        )
    }

    /**
     * 恢复UI状态为FloatingWindowUiState对象
     *
     * @return FloatingWindowUiState对象，如果没有保存的状态则返回null
     */
    fun restoreUiStateAsObject(): com.empathy.ai.domain.model.FloatingWindowUiState? {
        if (!hasSavedUiState()) {
            return null
        }
        val (tabName, contactId, inputText) = restoreUiState()
        return com.empathy.ai.domain.model.FloatingWindowUiState.fromPersisted(
            tabName = tabName,
            contactId = contactId,
            inputText = inputText
        )
    }

    /**
     * 清除保存的UI状态（关闭悬浮窗时调用）
     *
     * 注意：只清除输入内容，保留Tab和联系人记忆
     */
    fun clearSavedUiState() {
        prefs.edit {
            remove(KEY_SAVED_INPUT_TEXT)
            putBoolean(KEY_HAS_SAVED_STATE, false)
        }
    }

    /**
     * 检查是否有保存的UI状态
     *
     * @return true 如果有保存的状态（最小化时保存的）
     */
    fun hasSavedUiState(): Boolean {
        return prefs.getBoolean(KEY_HAS_SAVED_STATE, false)
    }

    // ==================== TD-00010: 悬浮球状态指示与拖动 ====================

    /**
     * 保存悬浮球位置
     *
     * @param x X坐标
     * @param y Y坐标
     */
    fun saveBubblePosition(x: Int, y: Int) {
        prefs.edit {
            putInt(KEY_BUBBLE_X, x)
            putInt(KEY_BUBBLE_Y, y)
        }
    }

    /**
     * 获取悬浮球位置
     *
     * @param defaultX 默认X坐标
     * @param defaultY 默认Y坐标
     * @return 悬浮球位置 (x, y)
     */
    fun getBubblePosition(defaultX: Int, defaultY: Int): Pair<Int, Int> {
        val x = prefs.getInt(KEY_BUBBLE_X, INVALID_POSITION)
        val y = prefs.getInt(KEY_BUBBLE_Y, INVALID_POSITION)
        
        return if (x == INVALID_POSITION || y == INVALID_POSITION) {
            Pair(defaultX, defaultY)
        } else {
            Pair(x, y)
        }
    }

    /**
     * 保存悬浮球状态
     *
     * @param state 悬浮球状态
     */
    fun saveBubbleState(state: com.empathy.ai.domain.model.FloatingBubbleState) {
        prefs.edit {
            putString(KEY_BUBBLE_STATE, state.name)
        }
    }

    /**
     * 获取悬浮球状态
     *
     * @return 悬浮球状态，默认返回IDLE
     */
    fun getBubbleState(): com.empathy.ai.domain.model.FloatingBubbleState {
        val stateName = prefs.getString(KEY_BUBBLE_STATE, null)
        return if (stateName.isNullOrBlank()) {
            com.empathy.ai.domain.model.FloatingBubbleState.IDLE
        } else {
            try {
                com.empathy.ai.domain.model.FloatingBubbleState.valueOf(stateName)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e(TAG, "Invalid bubble state: $stateName", e)
                com.empathy.ai.domain.model.FloatingBubbleState.IDLE
            }
        }
    }

    /**
     * 保存最小化状态（用于应用重启恢复）
     *
     * @param requestInfo 请求信息的JSON字符串
     */
    fun saveMinimizeState(requestInfo: String) {
        prefs.edit {
            putLong(KEY_MINIMIZE_TIMESTAMP, System.currentTimeMillis())
            putString(KEY_MINIMIZE_REQUEST_INFO, requestInfo)
        }
    }

    /**
     * 获取最小化状态（如果有效）
     *
     * 检查最小化状态是否在有效期内（10分钟）
     *
     * @return 请求信息字符串，如果已过期或不存在则返回null
     */
    fun getMinimizeStateIfValid(): String? {
        val timestamp = prefs.getLong(KEY_MINIMIZE_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - timestamp > MINIMIZE_VALIDITY_PERIOD) {
            // 已过期，清除状态
            clearMinimizeState()
            return null
        }
        
        return prefs.getString(KEY_MINIMIZE_REQUEST_INFO, null)
    }

    /**
     * 清除最小化状态
     */
    fun clearMinimizeState() {
        prefs.edit {
            remove(KEY_MINIMIZE_TIMESTAMP)
            remove(KEY_MINIMIZE_REQUEST_INFO)
        }
    }

    /**
     * 检查是否有有效的最小化状态
     *
     * @return true 如果有未过期的最小化状态
     */
    fun hasValidMinimizeState(): Boolean {
        val timestamp = prefs.getLong(KEY_MINIMIZE_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return currentTime - timestamp <= MINIMIZE_VALIDITY_PERIOD &&
               prefs.getString(KEY_MINIMIZE_REQUEST_INFO, null) != null
    }

    // ==================== TD-00010: 显示模式管理 ====================

    /**
     * 保存显示模式
     *
     * @param mode 显示模式（BUBBLE 或 DIALOG）
     */
    fun saveDisplayMode(mode: String) {
        prefs.edit {
            putString(KEY_DISPLAY_MODE, mode)
        }
        android.util.Log.d(TAG, "保存显示模式: $mode")
    }

    /**
     * 获取显示模式
     *
     * @return 显示模式，默认返回 DIALOG
     */
    fun getDisplayMode(): String {
        return prefs.getString(KEY_DISPLAY_MODE, DISPLAY_MODE_DIALOG) ?: DISPLAY_MODE_DIALOG
    }

    /**
     * 检查是否应该以悬浮球模式启动
     *
     * @return true 如果上次退出时是悬浮球模式
     */
    fun shouldStartAsBubble(): Boolean {
        return getDisplayMode() == DISPLAY_MODE_BUBBLE
    }

    companion object {
        /**
         * 日志标签
         */
        private const val TAG = "FloatingWindowPrefs"

        /**
         * SharedPreferences 文件名
         */
        private const val PREFS_NAME = "floating_window_prefs"

        // ==================== 键名常量 ====================

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
        
        // TD-00010: 悬浮球状态指示与拖动
        private const val KEY_BUBBLE_X = "bubble_position_x"
        private const val KEY_BUBBLE_Y = "bubble_position_y"
        private const val KEY_BUBBLE_STATE = "bubble_state"
        private const val KEY_MINIMIZE_TIMESTAMP = "minimize_timestamp"
        private const val KEY_MINIMIZE_REQUEST_INFO = "minimize_request_info"
        private const val KEY_DISPLAY_MODE = "display_mode"
        
        /**
         * 最小化状态有效期：10分钟
         */
        private const val MINIMIZE_VALIDITY_PERIOD = 10 * 60 * 1000L
        
        /**
         * 显示模式：悬浮球
         */
        const val DISPLAY_MODE_BUBBLE = "BUBBLE"
        
        /**
         * 显示模式：对话框
         */
        const val DISPLAY_MODE_DIALOG = "DIALOG"

        // ==================== 默认值常量 ====================

        /**
         * 默认Tab名称
         */
        const val DEFAULT_TAB_NAME = "ANALYZE"

        /**
         * 默认输入文本
         */
        const val DEFAULT_INPUT_TEXT = ""

        /**
         * 默认坐标值
         */
        const val DEFAULT_POSITION = 0

        /**
         * 无效坐标标记
         */
        const val INVALID_POSITION = -1
    }
}
