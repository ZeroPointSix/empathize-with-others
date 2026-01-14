package com.empathy.ai.presentation.ui.screen.settings

/**
 * 设置页面的用户事件
 *
 * 注意：API Key 配置已移至服务商配置中统一管理
 */
sealed interface SettingsUiEvent {
    // === AI 服务商事件 ===
    
    /**
     * 选择 AI 服务商
     */
    data class SelectProvider(val provider: String) : SettingsUiEvent
    
    /**
     * 显示服务商选择对话框
     */
    data object ShowProviderDialog : SettingsUiEvent
    
    /**
     * 隐藏服务商选择对话框
     */
    data object HideProviderDialog : SettingsUiEvent

    // === 隐私设置事件 ===
    
    /**
     * 切换数据掩码
     */
    data object ToggleDataMasking : SettingsUiEvent
    
    /**
     * 切换本地优先模式
     */
    data object ToggleLocalFirstMode : SettingsUiEvent

    // === AI 分析设置事件 ===

    /**
     * 更改历史对话条数
     *
     * @param count 条数，必须是 0/5/10 之一
     */
    data class ChangeHistoryConversationCount(val count: Int) : SettingsUiEvent

    // === 数据管理事件 ===
    
    /**
     * 显示清除数据对话框
     */
    data object ShowClearDataDialog : SettingsUiEvent
    
    /**
     * 隐藏清除数据对话框
     */
    data object HideClearDataDialog : SettingsUiEvent
    
    /**
     * 清除所有数据
     */
    data object ClearAllData : SettingsUiEvent

    // === 悬浮窗事件 ===
    
    /**
     * 切换悬浮窗功能
     */
    data class ToggleFloatingWindow(val displayId: Int?) : SettingsUiEvent

    /**
     * 切换连续截屏
     */
    data object ToggleContinuousScreenshot : SettingsUiEvent
    
    /**
     * 请求悬浮窗权限（触发权限请求流程）
     */
    data object RequestFloatingWindowPermission : SettingsUiEvent
    
    /**
     * 权限请求已处理（UI层调用，清除pending标志）
     */
    data object PermissionRequestHandled : SettingsUiEvent
    
    /**
     * 显示权限说明对话框
     */
    data object ShowPermissionDialog : SettingsUiEvent
    
    /**
     * 隐藏权限说明对话框
     */
    data object HidePermissionDialog : SettingsUiEvent
    
    /**
     * 检查悬浮窗权限
     */
    data object CheckFloatingWindowPermission : SettingsUiEvent

    // === 通用事件 ===
    
    /**
     * 清除错误
     */
    data object ClearError : SettingsUiEvent
    
    /**
     * 清除成功消息
     */
    data object ClearSuccessMessage : SettingsUiEvent
    
    /**
     * 导航返回
     */
    data object NavigateBack : SettingsUiEvent
}
