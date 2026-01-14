package com.empathy.ai.presentation.ui.screen.settings

import com.empathy.ai.domain.model.AiProvider

/**
 * 设置页面的 UI 状态
 *
 * ## 业务职责
 * 封装设置页面的所有配置项和交互状态，采用不可变数据模式。
 *
 * ## 状态分组
 * ```
 * SettingsUiState
 * ├── 通用状态          ← isLoading, error, successMessage
 * ├── AI服务商          ← selectedProvider, availableProviders, providersList
 * ├── 隐私设置          ← dataMaskingEnabled, localFirstMode
 * ├── AI分析设置        ← historyConversationCount, historyCountOptions
 * ├── 应用信息          ← appVersion
 * ├── 悬浮窗设置        ← floatingWindowEnabled, hasFloatingWindowPermission
 * ├── 交互状态          ← showProviderDialog, showClearDataDialog...
 * └── 权限请求          ← pendingPermissionRequest
 * ```
 *
 * ## 关键设计决策
 * 1. **API Key 集中管理**: API Key 配置移至服务商配置中，避免状态分散
 * 2. **历史条数预设**: 提供预设选项（0/5/10条），简化用户选择
 * 3. **权限状态分离**: 悬浮窗启用状态与权限状态分开，便于权限检查
 * 4. **延迟权限请求**: pendingPermissionRequest 标志实现 ViewModel → UI 的权限请求流程
 *
 * @property isLoading 是否正在加载
 * @property error 错误信息（显示Snackbar）
 * @property successMessage 成功消息（显示Snackbar，3秒后自动消失）
 * @property selectedProvider 当前选中的服务商名称
 * @property availableProviders 可选服务商名称列表（用于下拉选择）
 * @property providersList 完整服务商列表（用于查找完整信息）
 * @property dataMaskingEnabled 是否开启数据掩码（AI分析前自动脱敏）
 * @property localFirstMode 是否开启本地优先模式（减少API调用）
 * @property historyConversationCount 历史对话条数
 * @property historyCountOptions 历史条数选项列表（预设值）
 * @property appVersion 应用版本号
 * @property floatingWindowEnabled 悬浮窗是否启用
 * @property hasFloatingWindowPermission 悬浮窗权限是否已获取
 * @property continuousScreenshotEnabled 连续截屏是否启用
 * @property showProviderDialog 是否显示服务商选择对话框
 * @property showClearDataDialog 是否显示清除数据确认对话框
 * @property showPermissionDialog 是否显示悬浮窗权限请求对话框
 * @property pendingPermissionRequest 是否需要触发权限请求（ViewModel → UI）
 */
data class SettingsUiState(
    // 通用状态
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // AI 服务商
    val selectedProvider: String = "",
    val availableProviders: List<String> = emptyList(),
    val providersList: List<AiProvider> = emptyList(),

    // 隐私设置
    val dataMaskingEnabled: Boolean = true,
    val localFirstMode: Boolean = true,

    // AI 分析设置
    val historyConversationCount: Int = 5,
    val historyCountOptions: List<HistoryCountOption> = listOf(
        HistoryCountOption(0, "不发送历史", "每次分析独立，不包含历史对话"),
        HistoryCountOption(5, "最近5条", "推荐，平衡上下文和Token消耗"),
        HistoryCountOption(10, "最近10条", "更完整的上下文，Token消耗较高")
    ),

    // 应用信息
    val appVersion: String = "1.0.0",

    // 悬浮窗设置
    val floatingWindowEnabled: Boolean = false,
    val hasFloatingWindowPermission: Boolean = false,
    val continuousScreenshotEnabled: Boolean = false,

    // 交互状态
    val showProviderDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showPermissionDialog: Boolean = false,

    // 权限请求
    val pendingPermissionRequest: Boolean = false
) {
    /** 是否已配置至少一个服务商 */
    val hasProvider: Boolean get() = providersList.isNotEmpty()
}

/**
 * 历史对话条数选项
 *
 * 用于AI分析时携带的历史消息数量配置。
 * 较多的历史消息提供更好的上下文，但会增加Token消耗。
 *
 * @property count 历史条数值
 * @property label 显示标签（UI展示）
 * @property description 选项描述（帮助用户理解）
 */
data class HistoryCountOption(
    val count: Int,
    val label: String,
    val description: String
)
