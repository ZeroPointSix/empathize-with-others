package com.empathy.ai.domain.model

/**
 * 悬浮窗状态数据模型
 * 
 * 用于持久化悬浮窗的配置和位置信息
 * 
 * @property isEnabled 悬浮窗服务是否启用
 * @property buttonX 悬浮按钮的 X 坐标
 * @property buttonY 悬浮按钮的 Y 坐标
 */
data class FloatingWindowState(
    val isEnabled: Boolean = false,
    val buttonX: Int = 0,
    val buttonY: Int = 0
)
