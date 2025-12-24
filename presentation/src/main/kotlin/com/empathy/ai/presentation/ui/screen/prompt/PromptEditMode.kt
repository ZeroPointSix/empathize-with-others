package com.empathy.ai.presentation.ui.screen.prompt

import com.empathy.ai.domain.model.PromptScene

/**
 * 提示词编辑模式密封类
 *
 * 定义两种编辑模式：全局场景编辑和联系人专属编辑
 */
sealed class PromptEditMode {
    /**
     * 全局场景提示词编辑
     *
     * @property scene 场景类型
     */
    data class GlobalScene(val scene: PromptScene) : PromptEditMode()

    /**
     * 联系人专属提示词编辑
     *
     * @property contactId 联系人ID
     * @property contactName 联系人名称（用于显示）
     */
    data class ContactCustom(
        val contactId: String,
        val contactName: String
    ) : PromptEditMode()
}
