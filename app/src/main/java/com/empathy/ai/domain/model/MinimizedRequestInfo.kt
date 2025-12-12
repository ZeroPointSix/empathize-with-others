package com.empathy.ai.domain.model

/**
 * 最小化请求信息
 * 
 * 保存最小化对话框时的请求状态，用于应用重启后恢复
 * 
 * @property id 请求唯一标识（UUID）
 * @property type 操作类型（ANALYZE 或 CHECK）
 * @property contactId 联系人ID（用于恢复时选中正确的联系人）
 * @property inputText 用户输入的文本内容（用于恢复时填充输入框）
 * @property selectedContactIndex 选中的联系人索引（用于恢复时选中正确的联系人）
 * @property timestamp 请求时间戳（毫秒）
 */
data class MinimizedRequestInfo(
    val id: String,
    val type: ActionType,
    val contactId: String = "",
    val inputText: String = "",
    val selectedContactIndex: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
