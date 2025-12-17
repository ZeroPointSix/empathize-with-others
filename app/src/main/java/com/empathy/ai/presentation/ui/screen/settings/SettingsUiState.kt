package com.empathy.ai.presentation.ui.screen.settings

import com.empathy.ai.domain.model.AiProvider

/**
 * 设置页面的 UI 状态
 *
 * 包含所有设置项的当前值和 UI 交互状态
 *
 * 注意：API Key 配置已移至服务商配置中统一管理
 */
data class SettingsUiState(
    // 通用状态
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // AI 服务商选择
    val selectedProvider: String = "",
    val availableProviders: List<String> = emptyList(), // 服务商名称列表（用于 UI 显示）
    val providersList: List<AiProvider> = emptyList(), // 完整服务商列表（用于查找）

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

    // UI 交互状态
    val showProviderDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showPermissionDialog: Boolean = false
) {
    /**
     * 是否已配置服务商
     */
    val hasProvider: Boolean get() = providersList.isNotEmpty()
}

/**
 * 历史条数选项
 *
 * @property count 条数值
 * @property label 显示标签
 * @property description 描述说明
 */
data class HistoryCountOption(
    val count: Int,
    val label: String,
    val description: String
)
