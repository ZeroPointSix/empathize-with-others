package com.empathy.ai.presentation.ui.screen.aiconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.ui.component.ios.IOSFormField
import com.empathy.ai.presentation.ui.component.ios.IOSModelListItem
import com.empathy.ai.presentation.ui.component.ios.IOSNavigationBar
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSTestConnectionButton
import com.empathy.ai.presentation.viewmodel.AiConfigViewModel

/**
 * 编辑服务商页面（iOS风格）
 *
 * TD-00021: 编辑服务商页面
 *
 * 功能：
 * - 加载现有服务商数据
 * - 编辑基础信息（名称、端点、密钥）
 * - 测试连接功能
 * - 模型列表管理（切换默认模型）
 * - 设为默认服务商
 * - 删除服务商
 *
 * @param providerId 服务商ID
 * @param onNavigateBack 返回回调
 * @param viewModel AI配置ViewModel
 * @param modifier Modifier
 */
@Composable
fun EditProviderScreen(
    providerId: String,
    onNavigateBack: () -> Unit,
    viewModel: AiConfigViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 加载服务商数据
    LaunchedEffect(providerId) {
        android.util.Log.d("EditProviderScreen", "LaunchedEffect triggered with providerId: $providerId")
        viewModel.onEvent(AiConfigUiEvent.LoadProviderForEdit(providerId))
    }
    
    // 调试日志：监控 uiState 变化
    LaunchedEffect(uiState.formName, uiState.editingProvider) {
        android.util.Log.d("EditProviderScreen", "uiState changed - formName: ${uiState.formName}, editingProvider: ${uiState.editingProvider?.name}")
    }

    // 监听保存成功状态，自动返回
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.resetNavigationState()
            onNavigateBack()
        }
    }

    // 监听删除确认后返回
    val wasDeleting = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.showDeleteConfirmDialog, uiState.deletingProviderId) {
        if (wasDeleting.value && !uiState.showDeleteConfirmDialog && uiState.deletingProviderId == null) {
            // 删除完成，返回上一页
            onNavigateBack()
        }
        wasDeleting.value = uiState.showDeleteConfirmDialog
    }

    EditProviderScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )

    // 删除确认对话框
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AiConfigUiEvent.DismissDeleteConfirmDialog) },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个服务商吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(AiConfigUiEvent.ConfirmDeleteProvider) }
                ) {
                    Text("删除", color = iOSRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(AiConfigUiEvent.DismissDeleteConfirmDialog) }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 编辑服务商页面内容（无状态）
 */
@Composable
private fun EditProviderScreenContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val provider = uiState.editingProvider

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS导航栏
        IOSNavigationBar(
            title = "编辑服务商",
            onCancel = onNavigateBack,
            onDone = { onEvent(AiConfigUiEvent.SaveProvider) },
            isDoneEnabled = uiState.isFormValid && !uiState.isSaving
        )

        // 表单内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 基础信息分组
            item {
                IOSSettingsSection(
                    title = "基础信息",
                    footer = "API密钥将使用加密存储，请放心填写"
                ) {
                    IOSFormField(
                        label = "名称",
                        value = uiState.formName,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormName(it)) },
                        placeholder = "如：OpenAI",
                        showDivider = true
                    )
                    IOSFormField(
                        label = "端点",
                        value = uiState.formBaseUrl,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormBaseUrl(it)) },
                        placeholder = "https://api.openai.com/v1",
                        showDivider = true
                    )
                    IOSFormField(
                        label = "密钥",
                        value = uiState.formApiKey,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormApiKey(it)) },
                        placeholder = "sk-xxxxxxxx",
                        isPassword = true,
                        showDivider = false
                    )
                }
            }

            // 测试连接分组
            item {
                IOSSettingsSection {
                    IOSTestConnectionButton(
                        isLoading = uiState.isTestingConnection,
                        isSuccess = uiState.testConnectionResult?.let {
                            it is TestConnectionResult.Success
                        },
                        onClick = { onEvent(AiConfigUiEvent.TestConnection) }
                    )
                }
            }

            // 模型列表分组
            item {
                IOSSettingsSection(
                    title = "模型列表",
                    footer = "点击模型设为默认推理模型"
                ) {
                    // 自动获取按钮
                    IOSSettingsItem(
                        icon = Icons.Default.CloudDownload,
                        iconBackgroundColor = iOSGreen,
                        title = "自动获取模型",
                        showArrow = false,
                        showDivider = true,
                        onClick = { onEvent(AiConfigUiEvent.FetchModels) }
                    )
                    // 手动添加按钮
                    IOSSettingsItem(
                        icon = Icons.Default.Add,
                        iconBackgroundColor = iOSBlue,
                        title = "手动添加模型",
                        showArrow = false,
                        showDivider = uiState.formModels.isNotEmpty(),
                        onClick = { /* TODO: 显示添加模型对话框 */ }
                    )
                    // 模型列表
                    uiState.formModels.forEachIndexed { index, model ->
                        IOSModelListItem(
                            modelId = model.id,
                            displayName = model.displayName,
                            isDefault = model.id == uiState.formDefaultModelId,
                            onClick = { onEvent(AiConfigUiEvent.SetFormDefaultModel(model.id)) },
                            showDivider = index < uiState.formModels.lastIndex
                        )
                    }
                }
            }

            // 设为默认服务商
            item {
                IOSSettingsSection(
                    title = "默认设置"
                ) {
                    val isCurrentDefault = provider?.isDefault == true
                    IOSSettingsItem(
                        icon = Icons.Default.Check,
                        iconBackgroundColor = if (isCurrentDefault) iOSGreen else iOSBlue,
                        title = if (isCurrentDefault) "当前为默认服务商" else "设为默认服务商",
                        showArrow = false,
                        showDivider = false,
                        onClick = {
                            provider?.let {
                                onEvent(AiConfigUiEvent.SetDefaultProvider(it.id))
                            }
                        }
                    )
                }
            }

            // 高级选项分组
            item {
                IOSSettingsSection(title = "高级选项") {
                    IOSSettingsItem(
                        icon = Icons.Default.Add,
                        iconBackgroundColor = iOSPurple,
                        title = "Temperature",
                        value = "0.7",
                        showDivider = true,
                        onClick = { /* TODO: 实现Temperature设置 */ }
                    )
                    IOSSettingsItem(
                        icon = Icons.Default.Add,
                        iconBackgroundColor = iOSBlue,
                        title = "最大Token数",
                        value = "4096",
                        showDivider = false,
                        onClick = { /* TODO: 实现最大Token数设置 */ }
                    )
                }
            }

            // 删除服务商按钮
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        provider?.let {
                            onEvent(AiConfigUiEvent.ShowDeleteConfirmDialog(it.id))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = iOSRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "删除服务商",
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "编辑服务商 - 已填写", showBackground = true)
@Composable
private fun EditProviderScreenPreview() {
    EmpathyTheme {
        EditProviderScreenContent(
            uiState = AiConfigUiState(
                editingProvider = AiProvider(
                    id = "1",
                    name = "OpenAI",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "sk-xxx",
                    models = listOf(
                        AiModel(id = "gpt-4", displayName = "GPT-4"),
                        AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
                    ),
                    defaultModelId = "gpt-4",
                    isDefault = true
                ),
                formName = "OpenAI",
                formBaseUrl = "https://api.openai.com/v1",
                formApiKey = "sk-xxxxxxxxxxxxxxxx",
                formModels = listOf(
                    FormModel("gpt-4", "GPT-4"),
                    FormModel("gpt-3.5-turbo", "GPT-3.5 Turbo")
                ),
                formDefaultModelId = "gpt-4"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "编辑服务商 - 非默认", showBackground = true)
@Composable
private fun EditProviderScreenNonDefaultPreview() {
    EmpathyTheme {
        EditProviderScreenContent(
            uiState = AiConfigUiState(
                editingProvider = AiProvider(
                    id = "2",
                    name = "DeepSeek",
                    baseUrl = "https://api.deepseek.com/v1",
                    apiKey = "sk-xxx",
                    models = listOf(
                        AiModel(id = "deepseek-chat", displayName = "DeepSeek Chat")
                    ),
                    defaultModelId = "deepseek-chat",
                    isDefault = false
                ),
                formName = "DeepSeek",
                formBaseUrl = "https://api.deepseek.com/v1",
                formApiKey = "sk-xxxxxxxxxxxxxxxx",
                formModels = listOf(
                    FormModel("deepseek-chat", "DeepSeek Chat")
                ),
                formDefaultModelId = "deepseek-chat"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
