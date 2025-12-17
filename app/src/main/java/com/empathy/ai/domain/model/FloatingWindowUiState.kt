package com.empathy.ai.domain.model

/**
 * 悬浮窗UI状态模型
 *
 * 管理悬浮窗的完整UI状态，支持状态保持和恢复
 *
 * @property selectedTab 当前选中的Tab
 * @property selectedContactId 选中的联系人ID
 * @property inputText 输入框内容
 * @property lastResult AI返回的结果
 * @property isLoading 是否正在加载
 * @property errorMessage 错误信息
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
data class FloatingWindowUiState(
    /**
     * 当前选中的Tab
     */
    val selectedTab: ActionType = ActionType.ANALYZE,

    /**
     * 选中的联系人ID
     */
    val selectedContactId: String? = null,

    /**
     * 输入框内容
     */
    val inputText: String = "",

    /**
     * AI返回的结果
     */
    val lastResult: AiResult? = null,

    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,

    /**
     * 错误信息
     */
    val errorMessage: String? = null
) {
    /**
     * 是否有结果可显示
     *
     * @return true 如果有AI结果
     */
    fun hasResult(): Boolean = lastResult != null

    /**
     * 是否可以发送请求
     *
     * 需要满足：不在加载中、输入不为空、已选择联系人
     *
     * @return true 如果可以发送
     */
    fun canSubmit(): Boolean =
        !isLoading &&
            inputText.isNotBlank() &&
            selectedContactId != null

    /**
     * 清空结果和错误
     *
     * @return 清空后的新状态
     */
    fun clearResult(): FloatingWindowUiState = copy(
        lastResult = null,
        errorMessage = null
    )

    /**
     * 是否有错误
     *
     * @return true 如果有错误信息
     */
    fun hasError(): Boolean = !errorMessage.isNullOrBlank()

    /**
     * 重置为初始状态（保留Tab和联系人记忆）
     *
     * @return 重置后的新状态
     */
    fun resetKeepingMemory(): FloatingWindowUiState = copy(
        inputText = "",
        lastResult = null,
        isLoading = false,
        errorMessage = null
    )

    /**
     * 完全重置为初始状态
     *
     * @return 初始状态
     */
    fun resetAll(): FloatingWindowUiState = FloatingWindowUiState()

    /**
     * 从持久化数据恢复状态
     *
     * @param tabName Tab名称
     * @param contactId 联系人ID
     * @param inputText 输入文本
     * @return 恢复后的状态
     */
    fun restoreFrom(tabName: String, contactId: String?, inputText: String): FloatingWindowUiState {
        val tab = try {
            ActionType.valueOf(tabName)
        } catch (e: IllegalArgumentException) {
            ActionType.ANALYZE
        }
        return copy(
            selectedTab = tab,
            selectedContactId = contactId,
            inputText = inputText
        )
    }

    companion object {
        /**
         * 默认Tab名称
         */
        const val DEFAULT_TAB_NAME = "ANALYZE"

        /**
         * 默认输入文本
         */
        const val DEFAULT_INPUT_TEXT = ""

        /**
         * 最大输入长度
         */
        const val MAX_INPUT_LENGTH = 5000

        /**
         * 从持久化数据创建状态
         *
         * @param tabName Tab名称
         * @param contactId 联系人ID
         * @param inputText 输入文本
         * @return 新的状态实例
         */
        fun fromPersisted(tabName: String, contactId: String?, inputText: String): FloatingWindowUiState {
            val tab = try {
                ActionType.valueOf(tabName)
            } catch (e: IllegalArgumentException) {
                ActionType.ANALYZE
            }
            return FloatingWindowUiState(
                selectedTab = tab,
                selectedContactId = contactId,
                inputText = inputText
            )
        }
    }
}
