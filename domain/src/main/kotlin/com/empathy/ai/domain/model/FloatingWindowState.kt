package com.empathy.ai.domain.model

/**
 * 悬浮窗持久化状态模型
 *
 * 保存悬浮窗的启用状态和位置信息
 * 用于FloatingWindowPreferences持久化
 *
 * @property isEnabled 是否启用悬浮窗
 * @property buttonX 悬浮按钮X坐标
 * @property buttonY 悬浮按钮Y坐标
 *
 * @see FloatingWindowUiState 用于UI状态管理
 */
data class FloatingWindowState(
    val isEnabled: Boolean = false,
    val buttonX: Int = 0,
    val buttonY: Int = 0
)
