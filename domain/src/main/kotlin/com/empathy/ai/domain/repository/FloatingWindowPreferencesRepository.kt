package com.empathy.ai.domain.repository

import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.domain.model.FloatingWindowState
import com.empathy.ai.domain.model.FloatingWindowUiState
import com.empathy.ai.domain.model.MinimizedRequestInfo

/**
 * 悬浮窗偏好设置仓储接口
 *
 * 业务背景 (PRD-00009):
 * - 悬浮窗是应用的核心交互入口，通过无障碍服务与宿主应用交互
 * - 悬浮窗状态需要持久化，包括位置、展开状态、UI状态等
 * - 支持两种显示模式：BUBBLE（悬浮球）和DIALOG（对话框）
 *
 * 设计决策:
 * - 轻量级存储：使用SharedPreferences，非关键状态不存数据库
 * - 状态恢复：应用重启后恢复上次UI状态，提供无缝体验
 * - 最小化请求：用户切换应用时保存请求信息，恢复后继续处理
 *
 * 遵循Clean Architecture原则：接口定义在domain层，由data层实现
 */
interface FloatingWindowPreferencesRepository {

    // ==================== 基础状态管理 ====================

    /**
     * 保存悬浮窗状态
     */
    fun saveState(state: FloatingWindowState)

    /**
     * 加载悬浮窗状态
     *
     * @return 上次保存的状态或默认状态
     */
    fun loadState(): FloatingWindowState

    /**
     * 保存悬浮窗启用状态
     */
    fun saveEnabled(isEnabled: Boolean)

    /**
     * 检查悬浮窗是否启用
     */
    fun isEnabled(): Boolean

    /**
     * 清除所有偏好设置
     */
    fun clear()

    // ==================== 位置管理 ====================

    /**
     * 保存悬浮球位置
     */
    fun saveButtonPosition(x: Int, y: Int)

    /**
     * 获取悬浮球X坐标
     */
    fun getButtonX(): Int

    /**
     * 获取悬浮球Y坐标
     */
    fun getButtonY(): Int

    /**
     * 获取悬浮球位置
     *
     * @return Pair(x, y)
     */
    fun getButtonPosition(): Pair<Int, Int>

    /**
     * 保存指示器位置
     */
    fun saveIndicatorPosition(x: Int, y: Int)

    /**
     * 获取指示器位置
     */
    fun getIndicatorPosition(): Pair<Int, Int>

    // ==================== 最小化请求管理 ====================

    /**
     * 保存最小化时的请求信息
     *
     * 业务规则:
     * - 用户切换到其他应用时，保存当前分析请求
     * - 返回true表示保存成功
     *
     * @param requestInfo 请求信息
     * @return 是否保存成功
     */
    fun saveRequestInfo(requestInfo: MinimizedRequestInfo): Boolean

    /**
     * 获取最小化的请求信息
     *
     * @return 请求信息，不存在或已过期则返回null
     */
    fun getRequestInfo(): MinimizedRequestInfo?

    /**
     * 清除请求信息
     */
    fun clearRequestInfo()

    // ==================== Tab和联系人管理 ====================

    /**
     * 保存当前选中的Tab（字符串形式）
     */
    fun saveSelectedTab(tabName: String)

    /**
     * 保存当前选中的Tab（ActionType形式）
     */
    fun saveSelectedTab(tab: ActionType)

    /**
     * 获取当前选中的Tab名称
     */
    fun getSelectedTab(): String

    /**
     * 获取当前选中的Tab（ActionType形式）
     */
    fun getSelectedTabAsActionType(): ActionType

    /**
     * 保存最近使用的联系人ID
     */
    fun saveLastContactId(contactId: String)

    /**
     * 获取最近使用的联系人ID
     */
    fun getLastContactId(): String?

    // ==================== 输入文本管理 ====================

    /**
     * 保存输入文本
     *
     * 用途: 应用重启后恢复未发送的输入内容
     */
    fun saveInputText(text: String)

    /**
     * 获取输入文本
     */
    fun getInputText(): String

    // ==================== UI状态管理 ====================

    /**
     * 保存完整UI状态
     *
     * @param tabName Tab名称
     * @param contactId 联系人ID（可选）
     * @param inputText 输入文本
     */
    fun saveUiState(tabName: String, contactId: String?, inputText: String)

    /**
     * 保存UI状态（对象形式）
     */
    fun saveUiState(state: FloatingWindowUiState)

    /**
     * 恢复UI状态（三元组形式）
     *
     * @return Triple(tabName, contactId?, inputText)
     */
    fun restoreUiState(): Triple<String, String?, String>

    /**
     * 恢复UI状态（对象形式）
     *
     * @return FloatingWindowUiState，不存在则返回null
     */
    fun restoreUiStateAsObject(): FloatingWindowUiState?

    /**
     * 清除保存的UI状态
     */
    fun clearSavedUiState()

    /**
     * 检查是否有保存的UI状态
     */
    fun hasSavedUiState(): Boolean

    // ==================== 悬浮球管理 ====================

    /**
     * 保存悬浮球位置
     */
    fun saveBubblePosition(x: Int, y: Int)

    /**
     * 获取悬浮球位置
     *
     * @param defaultX 默认X坐标
     * @param defaultY 默认Y坐标
     * @return Pair(x, y)
     */
    fun getBubblePosition(defaultX: Int, defaultY: Int): Pair<Int, Int>

    /**
     * 保存悬浮球状态
     */
    fun saveBubbleState(state: FloatingBubbleState)

    /**
     * 获取悬浮球状态
     */
    fun getBubbleState(): FloatingBubbleState

    // ==================== 最小化状态管理 ====================

    /**
     * 保存最小化状态
     */
    fun saveMinimizeState(requestInfo: String)

    /**
     * 获取有效的最小化状态
     *
     * @return 最小化请求信息，不存在或已过期则返回null
     */
    fun getMinimizeStateIfValid(): String?

    /**
     * 清除最小化状态
     */
    fun clearMinimizeState()

    /**
     * 检查是否有有效的最小化状态
     */
    fun hasValidMinimizeState(): Boolean

    // ==================== 显示模式管理 ====================

    /**
     * 保存显示模式
     *
     * @param mode DISPLAY_MODE_BUBBLE 或 DISPLAY_MODE_DIALOG
     */
    fun saveDisplayMode(mode: String)

    /**
     * 获取显示模式
     *
     * @return 显示模式
     */
    fun getDisplayMode(): String

    /**
     * 检查是否应该以悬浮球模式启动
     */
    fun shouldStartAsBubble(): Boolean

    // ==================== 显示屏管理 ====================

    /**
     * 保存悬浮窗所在显示屏ID（多显示屏场景）
     */
    fun saveDisplayId(displayId: Int)

    /**
     * 获取悬浮窗所在显示屏ID
     *
     * @return 显示屏ID，不存在则返回null
     */
    fun getDisplayId(): Int?

    // ==================== 截图设置 ====================

    /**
     * 保存连续截屏开关
     */
    fun saveContinuousScreenshotEnabled(enabled: Boolean)

    /**
     * 获取连续截屏开关状态
     */
    fun isContinuousScreenshotEnabled(): Boolean

    /**
     * 是否已缓存截图权限（MediaProjection 授权结果）
     */
    fun hasScreenshotPermission(): Boolean

    /**
     * 清除缓存的截图权限
     */
    fun clearScreenshotPermission()

    companion object {
        const val DISPLAY_MODE_BUBBLE = "BUBBLE"
        const val DISPLAY_MODE_DIALOG = "DIALOG"
    }
}
