package com.empathy.ai.presentation.ui.screen.aiconfig

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.card.ProviderCard
import com.empathy.ai.presentation.ui.component.dialog.ProviderFormDialog
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.ErrorView
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.viewmodel.AiConfigViewModel

/**
 * AI 配置页面
 *
 * 功能：
 * - 显示所有 AI 服务商列表
 * - 添加、编辑、删除服务商
 * - 设置默认服务商
 * - 测试连接
 *
 * @param onNavigateBack 返回回调
 * @param viewModel AI 配置 ViewModel
 * @param modifier Modifier
 */
@Composable
fun AiConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiConfigViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 处理导航返回
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onNavigateBack()
        }
    }

    AiConfigScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * AI 配置页面内容（无状态）
 *
 * 分离为无状态组件便于 Preview 和测试
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiConfigScreenContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AI 配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(AiConfigUiEvent.ShowAddDialog) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加服务商"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载服务商..."
                    )
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error,
                        onRetry = { onEvent(AiConfigUiEvent.LoadProviders) }
                    )
                }
                !uiState.hasProviders -> {
                    EmptyView(
                        message = "还没有配置 AI 服务商\n点击右下角按钮添加",
                        actionText = null,
                        onAction = null
                    )
                }
                else -> {
                    ProviderList(
                        providers = uiState.providers,
                        onProviderClick = { provider ->
                            onEvent(AiConfigUiEvent.ShowEditDialog(provider))
                        },
                        onDeleteClick = { providerId ->
                            onEvent(AiConfigUiEvent.ShowDeleteConfirmDialog(providerId))
                        },
                        onSetDefaultClick = { providerId ->
                            onEvent(AiConfigUiEvent.SetDefaultProvider(providerId))
                        }
                    )
                }
            }

            // 显示错误 Snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { onEvent(AiConfigUiEvent.ClearError) }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(uiState.error)
                }
            }
        }

        // 表单对话框
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

        // 删除确认对话框
        if (uiState.showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(AiConfigUiEvent.DismissDeleteConfirmDialog) },
                title = { Text("确认删除") },
                text = { Text("确定要删除这个服务商吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = { onEvent(AiConfigUiEvent.ConfirmDeleteProvider) }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(AiConfigUiEvent.DismissDeleteConfirmDialog) }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 服务商列表
 */
@Composable
private fun ProviderList(
    providers: List<AiProvider>,
    onProviderClick: (AiProvider) -> Unit,
    onDeleteClick: (String) -> Unit,
    onSetDefaultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = providers,
            key = { it.id }
        ) { provider ->
            ProviderCard(
                provider = provider,
                onEdit = { onProviderClick(provider) },
                onDelete = { onDeleteClick(provider.id) },
                onSetDefault = { onSetDefaultClick(provider.id) }
            )
        }
    }
}

// ==================== Previews ====================

@Preview(name = "AI 配置 - 默认", showBackground = true)
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
            onNavigateBack = {}
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
            onNavigateBack = {}
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
            onNavigateBack = {}
        )
    }
}

@Preview(name = "AI 配置 - 错误", showBackground = true)
@Composable
private fun AiConfigScreenErrorPreview() {
    EmpathyTheme {
        AiConfigScreenContent(
            uiState = AiConfigUiState(error = "加载服务商列表失败"),
            onEvent = {},
            onNavigateBack = {}
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
            onNavigateBack = {}
        )
    }
}
