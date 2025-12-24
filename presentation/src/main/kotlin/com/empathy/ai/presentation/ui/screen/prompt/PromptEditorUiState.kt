package com.empathy.ai.presentation.ui.screen.prompt

import androidx.annotation.StringRes
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.PromptScene

/**
 * 提示词编辑器UI状态
 *
 * @property editMode 编辑模式（全局场景/联系人专属）
 * @property originalPrompt 原始提示词（用于检测修改）
 * @property currentPrompt 当前编辑的提示词
 * @property placeholderText 动态占位符文案
 * @property isLoading 初始加载状态
 * @property isSaving 保存状态
 * @property showDiscardDialog 显示放弃修改对话框
 * @property errorMessage 错误信息
 */
data class PromptEditorUiState(
    val editMode: PromptEditMode = PromptEditMode.GlobalScene(PromptScene.ANALYZE),
    val originalPrompt: String = "",
    val currentPrompt: String = "",
    val placeholderText: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        /** 提示词最大长度 */
        const val MAX_PROMPT_LENGTH = 1000

        /** 警告长度阈值 */
        const val WARN_PROMPT_LENGTH = 800
    }

    /** 当前字符数 */
    val charCount: Int get() = currentPrompt.length

    /** 是否超出字数限制 */
    val isOverLimit: Boolean get() = charCount > MAX_PROMPT_LENGTH

    /** 是否接近字数限制 */
    val isNearLimit: Boolean get() = charCount > WARN_PROMPT_LENGTH

    /** 是否有未保存的修改 */
    val hasUnsavedChanges: Boolean get() = currentPrompt != originalPrompt

    /** 是否可以保存 */
    val canSave: Boolean get() = !isOverLimit && !isSaving && !isLoading

    /**
     * 获取标题资源ID
     *
     * 全局模式返回固定字符串资源，联系人模式返回参数化字符串资源
     *
     * @return 字符串资源ID
     */
    @StringRes
    fun getTitleResId(): Int = when (editMode) {
        is PromptEditMode.GlobalScene -> R.string.prompt_editor_title_global
        is PromptEditMode.ContactCustom -> R.string.prompt_editor_title_contact
    }

    /**
     * 获取联系人名称（仅联系人模式有效）
     *
     * @return 联系人名称，全局模式返回null
     */
    fun getContactName(): String? = when (editMode) {
        is PromptEditMode.GlobalScene -> null
        is PromptEditMode.ContactCustom -> editMode.contactName
    }
}
