package com.empathy.ai.presentation.ui.screen.aiconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog
import com.empathy.ai.presentation.ui.component.ios.DraggableModelItem
import com.empathy.ai.presentation.ui.component.ios.DraggableModelList
import com.empathy.ai.presentation.ui.component.ios.IOSFormField
import com.empathy.ai.presentation.ui.component.ios.IOSModelListItem
import com.empathy.ai.presentation.ui.component.ios.IOSNavigationBar
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSTestConnectionButton
import com.empathy.ai.presentation.ui.component.ios.TemperatureSlider
import com.empathy.ai.presentation.ui.component.ios.TimeoutInput
import com.empathy.ai.presentation.ui.component.ios.TokenLimitInput
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

    // BUG-00040修复：编辑模式时加载服务商数据
    androidx.compose.runtime.LaunchedEffect(providerId) {
        if (!providerId.isNullOrEmpty()) {
            android.util.Log.d("AddProviderScreen", "Loading provider for edit: $providerId")
            viewModel.onEvent(AiConfigUiEvent.LoadProviderForEdit(providerId))
        }
    }

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
        modifier = modifier,
        isEditMode = !providerId.isNullOrEmpty()
    )
}

/**
 * 添加服务商页面内容（无状态）
 * 
 * BUG-00037 P4修复：添加底部安全区域处理
 * BUG-00040修复：支持编辑模式
 * BUG-00054修复：添加手动添加模型对话框
 */
@Composable
private fun AddProviderScreenContent(
    uiState: AiConfigUiState,
    onEvent: (AiConfigUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false
) {
    val dimensions = AdaptiveDimensions.current
    
    // BUG-00054: 添加模型对话框状态
    var showAddModelDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS导航栏 - BUG-00040修复：使用isEditMode参数
        IOSNavigationBar(
            title = if (isEditMode) "编辑服务商" else "添加服务商",
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
                        isUrl = true,
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
                    footer = "长按模型拖拽调整顺序，点击✓设为默认"
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
                    // 手动添加按钮 - BUG-00054修复：添加点击事件
                    IOSSettingsItem(
                        icon = Icons.Default.Add,
                        iconBackgroundColor = iOSBlue,
                        title = "手动添加模型",
                        showArrow = false,
                        showDivider = uiState.formModels.isNotEmpty(),
                        onClick = { showAddModelDialog = true }
                    )
                    // TD-00025 T4-03: 使用DraggableModelList替换原有模型列表
                    if (uiState.formModels.isNotEmpty()) {
                        DraggableModelList(
                            models = uiState.formModels.map { model ->
                                DraggableModelItem(
                                    id = model.id,
                                    displayName = model.displayName,
                                    isDefault = model.id == uiState.formDefaultModelId
                                )
                            },
                            onReorder = { fromIndex, toIndex ->
                                onEvent(AiConfigUiEvent.ReorderFormModels(fromIndex, toIndex))
                            },
                            onSetDefault = { modelId ->
                                onEvent(AiConfigUiEvent.SetFormDefaultModel(modelId))
                            },
                            onDelete = { modelId ->
                                onEvent(AiConfigUiEvent.RemoveFormModel(modelId))
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // 高级选项分组 - TD-00025 T3-04: 集成TemperatureSlider和TokenLimitInput
            // BUG-00054: 添加TimeoutInput
            item {
                IOSSettingsSection(
                    title = "高级选项",
                    footer = "这些设置会影响AI的响应行为"
                ) {
                    // Temperature滑块
                    TemperatureSlider(
                        value = uiState.formTemperature,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormTemperature(it)) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 分隔线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(iOSSeparator)
                    )
                    
                    // Token限制输入
                    TokenLimitInput(
                        value = uiState.formMaxTokens,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormMaxTokens(it)) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 分隔线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(iOSSeparator)
                    )
                    
                    // BUG-00054: 超时设置输入
                    TimeoutInput(
                        valueMs = uiState.formTimeoutMs,
                        onValueChange = { onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(it)) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
        
        // BUG-00054: 添加模型对话框
        if (showAddModelDialog) {
            AddModelDialog(
                onDismiss = { showAddModelDialog = false },
                onConfirm = { modelId, displayName ->
                    onEvent(AiConfigUiEvent.AddFormModel(modelId, displayName))
                    showAddModelDialog = false
                }
            )
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

// ============================================================
// 添加模型对话框
// ============================================================

/**
 * 添加模型对话框
 * 
 * BUG-00054修复：从 ProviderFormDialog 复制并适配
 */
@Composable
private fun AddModelDialog(
    onDismiss: () -> Unit,
    onConfirm: (modelId: String, displayName: String) -> Unit
) {
    var modelId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var modelIdError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    IOSInputDialog(
        title = "添加模型",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = modelId,
                    onValueChange = {
                        modelId = it
                        modelIdError = null
                    },
                    label = { Text("模型 ID *") },
                    placeholder = { Text("例如：gpt-4") },
                    isError = modelIdError != null,
                    supportingText = modelIdError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("显示名称") },
                    placeholder = { Text("例如：GPT-4") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (modelId.isBlank()) {
                                modelIdError = "模型 ID 不能为空"
                            } else {
                                onConfirm(modelId.trim(), displayName.trim())
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmText = "添加",
        dismissText = "取消",
        onConfirm = {
            if (modelId.isBlank()) {
                modelIdError = "模型 ID 不能为空"
                return@IOSInputDialog
            }
            onConfirm(modelId.trim(), displayName.trim())
        },
        onDismiss = onDismiss,
        confirmEnabled = modelId.isNotBlank()
    )
}
