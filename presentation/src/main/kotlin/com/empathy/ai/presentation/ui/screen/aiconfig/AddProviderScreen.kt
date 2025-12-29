package com.empathy.ai.presentation.ui.screen.aiconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.ui.component.ios.IOSFormField
import com.empathy.ai.presentation.ui.component.ios.IOSModelListItem
import com.empathy.ai.presentation.ui.component.ios.IOSNavigationBar
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSTestConnectionButton
import com.empathy.ai.presentation.viewmodel.AiConfigViewModel

/**
 * 添加服务商页面（iOS风格）
 *
 * TD-00021 Phase 2: 添加服务商页面优化
 *
 * 功能：
 * - 基础信息表单（名称、端点、密钥）
 * - 测试连接功能
 * - 模型列表管理（自动获取、手动添加）
 * - 高级选项（Temperature、最大Token数）
 *
 * @param onNavigateBack 返回回调
 * @param viewModel AI配置ViewModel
 * @param modifier Modifier
 * @param providerId 编辑模式时的服务商ID（可选）
 */
@Composable
fun AddProviderScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiConfigViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    providerId: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 监听保存成功状态，自动返回
    androidx.compose.runtime.LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.resetNavigationState()
            onNavigateBack()
        }
    }

    AddProviderScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * 添加服务商页面内容（无状态）
 * 
 * BUG-00037 P4修复：添加底部安全区域处理
 */
@Composable
private fun AddProviderScreenContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS导航栏
        IOSNavigationBar(
            title = if (uiState.isEditing) "编辑服务商" else "添加服务商",
            onCancel = onNavigateBack,
            onDone = { onEvent(AiConfigUiEvent.SaveProvider) },
            isDoneEnabled = uiState.isFormValid && !uiState.isSaving
        )

        // 表单内容 - BUG-00037修复：添加navigationBarsPadding
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = dimensions.spacingXLarge + 48.dp)
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
                    footer = "点击模型设为默认，拖拽调整顺序"
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
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "添加服务商 - 空表单", showBackground = true)
@Composable
private fun AddProviderScreenEmptyPreview() {
    EmpathyTheme {
        AddProviderScreenContent(
            uiState = AiConfigUiState(),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "添加服务商 - 填写中", showBackground = true)
@Composable
private fun AddProviderScreenFillingPreview() {
    EmpathyTheme {
        AddProviderScreenContent(
            uiState = AiConfigUiState(
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

@Preview(name = "添加服务商 - 测试连接成功", showBackground = true)
@Composable
private fun AddProviderScreenTestSuccessPreview() {
    EmpathyTheme {
        AddProviderScreenContent(
            uiState = AiConfigUiState(
                formName = "OpenAI",
                formBaseUrl = "https://api.openai.com/v1",
                formApiKey = "sk-xxxxxxxxxxxxxxxx",
                testConnectionResult = TestConnectionResult.Success(latencyMs = 150)
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "添加服务商 - 编辑模式", showBackground = true)
@Composable
private fun AddProviderScreenEditModePreview() {
    EmpathyTheme {
        AddProviderScreenContent(
            uiState = AiConfigUiState(
                editingProvider = com.empathy.ai.domain.model.AiProvider(
                    id = "1",
                    name = "OpenAI",
                    baseUrl = "https://api.openai.com/v1",
                    apiKey = "sk-xxx",
                    models = emptyList(),
                    defaultModelId = "",
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
