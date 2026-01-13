package com.empathy.ai.presentation.ui.screen.aiconfig

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.ui.component.dialog.IOSDeleteConfirmDialog
import com.empathy.ai.presentation.ui.component.dialog.ProviderFormDialog
import com.empathy.ai.presentation.ui.component.ios.IOSLargeTitleBar
import com.empathy.ai.presentation.ui.component.ios.IOSProviderCard
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.state.EmptyType
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.FriendlyErrorCard
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.util.UserFriendlyError
import com.empathy.ai.presentation.viewmodel.AiConfigViewModel
import android.util.Log

/**
 * AI 配置页面 (iOS风格重构)
 *
 * TD-00021 Phase 1: AI配置页面优化
 *
 * 功能：
 * - 显示所有 AI 服务商列表（iOS Inset Grouped风格）
 * - 搜索过滤服务商
 * - 添加、编辑、删除服务商
 * - 设置默认服务商
 * - 测试连接
 * - 通用选项和高级设置
 * - 网络代理配置（TD-00025）
 * - 用量统计导航（TD-00025）
 *
 * @param onNavigateBack 返回回调
 * @param onNavigateToAddProvider 导航到添加服务商页面
 * @param onNavigateToEditProvider 导航到编辑服务商页面
 * @param onNavigateToUsageStats 导航到用量统计页面（TD-00025）
 * @param viewModel AI 配置 ViewModel
 * @param modifier Modifier
 */
@Composable
fun AiConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiConfigViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToAddProvider: (() -> Unit)? = null,
    onNavigateToEditProvider: ((String) -> Unit)? = null,
    onNavigateToUsageStats: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 系统返回手势统一走同一返回逻辑，避免回退到联系人列表
    BackHandler {
        onNavigateBack()
    }

    // 处理导航返回
    // [调试日志] BUG-00063: 记录返回导航触发时机
    LaunchedEffect(uiState.shouldNavigateBack) {
        Log.d("AiConfigScreen", "NavigateBack flag=${uiState.shouldNavigateBack}")
        if (uiState.shouldNavigateBack) {
            onNavigateBack()
        }
    }

    // TD-00025: 处理用量统计导航
    // [调试日志] BUG-00063: 记录用量统计导航触发时机
    LaunchedEffect(uiState.shouldNavigateToUsageStats) {
        Log.d("AiConfigScreen", "NavigateUsageStats flag=${uiState.shouldNavigateToUsageStats}")
        if (uiState.shouldNavigateToUsageStats) {
            viewModel.resetUsageStatsNavigationState()
            onNavigateToUsageStats?.invoke()
        }
    }

    AiConfigScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToAddProvider = onNavigateToAddProvider,
        onNavigateToEditProvider = onNavigateToEditProvider,
        modifier = modifier
    )
}

/**
 * AI 配置页面内容（无状态）
 *
 * 分离为无状态组件便于 Preview 和测试
 */
@Composable
private fun AiConfigScreenContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAddProvider: (() -> Unit)?,
    onNavigateToEditProvider: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS大标题导航栏
        IOSLargeTitleBar(
            title = "AI 配置",
            onBackClick = onNavigateBack,
            onAddClick = {
                if (onNavigateToAddProvider != null) {
                    onNavigateToAddProvider()
                } else {
                    onEvent(AiConfigUiEvent.ShowAddDialog)
                }
            },
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { onEvent(AiConfigUiEvent.UpdateSearchQuery(it)) },
            searchPlaceholder = "搜索服务商"
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingIndicatorFullScreen(message = "加载服务商...")
            }
            uiState.error != null -> {
                FriendlyErrorCard(
                    error = UserFriendlyError(
                        title = "出错了",
                        message = uiState.error ?: "未知错误",
                        icon = Icons.Default.Warning
                    ),
                    onAction = { onEvent(AiConfigUiEvent.LoadProviders) }
                )
            }
            !uiState.hasProviders -> {
                EmptyView(
                    message = "还没有配置 AI 服务商\n点击右上角按钮添加",
                    actionText = null,
                    onAction = null
                )
            }
            uiState.searchQuery.isNotBlank() && uiState.filteredProviders.isEmpty() -> {
                EmptyView(
                    message = "未找到匹配的服务商",
                    actionText = null,
                    onAction = null,
                    emptyType = EmptyType.NoResults
                )
            }
            else -> {
                ProviderListContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    onNavigateToEditProvider = onNavigateToEditProvider
                )
            }
        }
    }

    // 表单对话框（兼容旧版）
    if (uiState.showFormDialog) {
        ProviderFormDialog(
            isEditing = uiState.isEditing,
            formName = uiState.formName,
            formBaseUrl = uiState.formBaseUrl,
            formApiKey = uiState.formApiKey,
            formModels = uiState.formModels,
            formDefaultModelId = uiState.formDefaultModelId,
            formNameError = uiState.formNameError,
            formBaseUrlError = uiState.formBaseUrlError,
            formApiKeyError = uiState.formApiKeyError,
            formModelsError = uiState.formModelsError,
            isSaving = uiState.isSaving,
            isTestingConnection = uiState.isTestingConnection,
            testConnectionResult = uiState.testConnectionResult,
            isFetchingModels = uiState.isFetchingModels,
            fetchModelsError = uiState.fetchModelsError,
            onFetchModels = { onEvent(AiConfigUiEvent.FetchModels) },
            onNameChange = { onEvent(AiConfigUiEvent.UpdateFormName(it)) },
            onBaseUrlChange = { onEvent(AiConfigUiEvent.UpdateFormBaseUrl(it)) },
            onApiKeyChange = { onEvent(AiConfigUiEvent.UpdateFormApiKey(it)) },
            onAddModel = { modelId, displayName ->
                onEvent(AiConfigUiEvent.AddFormModel(modelId, displayName))
            },
            onRemoveModel = { modelId ->
                onEvent(AiConfigUiEvent.RemoveFormModel(modelId))
            },
            onSetDefaultModel = { modelId ->
                onEvent(AiConfigUiEvent.SetFormDefaultModel(modelId))
            },
            onTestConnection = { onEvent(AiConfigUiEvent.TestConnection) },
            onSave = { onEvent(AiConfigUiEvent.SaveProvider) },
            onDismiss = { onEvent(AiConfigUiEvent.DismissFormDialog) }
        )
    }

    // 删除确认对话框 - 使用 iOS 风格
    if (uiState.showDeleteConfirmDialog) {
        IOSDeleteConfirmDialog(
            title = "确认删除",
            message = "确定要删除这个服务商吗？此操作无法撤销。",
            onConfirm = { onEvent(AiConfigUiEvent.ConfirmDeleteProvider) },
            onDismiss = { onEvent(AiConfigUiEvent.DismissDeleteConfirmDialog) }
        )
    }

    // TD-00025: 代理设置对话框
    if (uiState.showProxyDialog) {
        ProxySettingsDialog(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = { onEvent(AiConfigUiEvent.DismissProxyDialog) }
        )
    }
}


/**
 * 服务商列表内容（iOS Inset Grouped风格）
 */
@Composable
private fun ProviderListContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateToEditProvider: ((String) -> Unit)?
) {
    val dimensions = AdaptiveDimensions.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensions.spacingXLarge)
    ) {
        // 推理引擎分组
        item {
            IOSSettingsSection(
                title = "推理引擎",
                footer = "向左滑动可编辑或删除服务商，点击切换默认引擎"
            ) {
                val providers = uiState.filteredProviders
                providers.forEachIndexed { index, provider ->
                    IOSProviderCard(
                        provider = provider,
                        isDefault = provider.isDefault,
                        onClick = { 
                            // 点击直接设为默认服务商
                            onEvent(AiConfigUiEvent.SetDefaultProvider(provider.id))
                        },
                        onEdit = {
                            // 滑动编辑 - 导航到编辑页面
                            if (onNavigateToEditProvider != null) {
                                onNavigateToEditProvider(provider.id)
                            } else {
                                onEvent(AiConfigUiEvent.ShowEditDialog(provider))
                            }
                        },
                        onDelete = {
                            // 滑动删除
                            onEvent(AiConfigUiEvent.ShowDeleteConfirmDialog(provider.id))
                        },
                        showDivider = index < providers.lastIndex,
                        highlightQuery = uiState.searchQuery
                    )
                }
            }
        }

        // 通用选项分组
        item {
            IOSSettingsSection(title = "通用选项") {
                IOSSettingsItem(
                    icon = Icons.Default.Language,
                    iconBackgroundColor = iOSBlue,
                    title = "网络代理",
                    value = if (uiState.proxyConfig?.enabled == true) {
                        "${uiState.proxyConfig?.type?.name ?: "HTTP"} ${uiState.proxyConfig?.host ?: ""}:${uiState.proxyConfig?.port ?: ""}"
                    } else {
                        "未设置"
                    },
                    showDivider = true,
                    onClick = { onEvent(AiConfigUiEvent.ShowProxyDialog) }
                )
                IOSSettingsItem(
                    icon = Icons.Default.QueryStats,
                    iconBackgroundColor = iOSPurple,
                    title = "用量统计",
                    showDivider = false,
                    onClick = { onEvent(AiConfigUiEvent.NavigateToUsageStats) }
                )
            }
        }
    }
}

// ==================== Previews ====================

@Preview(name = "AI 配置 - iOS风格", showBackground = true)
@Composable
private fun AiConfigScreenPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(
                providers = listOf(
                    AiProvider(
                        id = "1",
                        name = "OpenAI",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "sk-test123",
                        models = listOf(
                            AiModel(id = "gpt-4", displayName = "GPT-4"),
                            AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
                        ),
                        defaultModelId = "gpt-4",
                        isDefault = true
                    ),
                    AiProvider(
                        id = "2",
                        name = "DeepSeek",
                        baseUrl = "https://api.deepseek.com/v1",
                        apiKey = "sk-test456",
                        models = listOf(
                            AiModel(id = "deepseek-chat", displayName = "DeepSeek Chat")
                        ),
                        defaultModelId = "deepseek-chat",
                        isDefault = false
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddProvider = {},
            onNavigateToEditProvider = {}
        )
    }
}

@Preview(name = "AI 配置 - 加载中", showBackground = true)
@Composable
private fun AiConfigScreenLoadingPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(isLoading = true),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddProvider = {},
            onNavigateToEditProvider = {}
        )
    }
}

@Preview(name = "AI 配置 - 空状态", showBackground = true)
@Composable
private fun AiConfigScreenEmptyPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddProvider = {},
            onNavigateToEditProvider = {}
        )
    }
}

@Preview(name = "AI 配置 - 搜索", showBackground = true)
@Composable
private fun AiConfigScreenSearchPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(
                searchQuery = "Open",
                providers = listOf(
                    AiProvider(
                        id = "1",
                        name = "OpenAI",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "sk-test123",
                        models = listOf(
                            AiModel(id = "gpt-4", displayName = "GPT-4")
                        ),
                        defaultModelId = "gpt-4",
                        isDefault = true
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddProvider = {},
            onNavigateToEditProvider = {}
        )
    }
}

@Preview(
    name = "AI 配置 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AiConfigScreenDarkPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(
                providers = listOf(
                    AiProvider(
                        id = "1",
                        name = "OpenAI",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "sk-test123",
                        models = listOf(
                            AiModel(id = "gpt-4", displayName = "GPT-4")
                        ),
                        defaultModelId = "gpt-4",
                        isDefault = true
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddProvider = {},
            onNavigateToEditProvider = {}
        )
    }
}
