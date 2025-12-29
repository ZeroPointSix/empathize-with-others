package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.screen.aiconfig.FormModel
import com.empathy.ai.presentation.ui.screen.aiconfig.TestConnectionResult

/**
 * AI 服务商配置表单对话框
 *
 * 功能：
 * - 输入服务商名称、API 端点、API Key
 * - 管理模型列表（添加、删除、设置默认）
 * - 测试连接
 * - 表单验证
 *
 * @param isEditing 是否为编辑模式
 * @param formName 服务商名称
 * @param formBaseUrl API 端点
 * @param formApiKey API Key
 * @param formModels 模型列表
 * @param formDefaultModelId 默认模型 ID
 * @param formNameError 名称错误提示
 * @param formBaseUrlError API 端点错误提示
 * @param formApiKeyError API Key 错误提示
 * @param formModelsError 模型列表错误提示
 * @param isSaving 是否正在保存
 * @param isTestingConnection 是否正在测试连接
 * @param testConnectionResult 连接测试结果
 * @param onNameChange 名称变更回调
 * @param onBaseUrlChange API 端点变更回调
 * @param onApiKeyChange API Key 变更回调
 * @param onAddModel 添加模型回调
 * @param onRemoveModel 删除模型回调
 * @param onSetDefaultModel 设置默认模型回调
 * @param onTestConnection 测试连接回调
 * @param onSave 保存回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ProviderFormDialog(
    isEditing: Boolean,
    formName: String,
    formBaseUrl: String,
    formApiKey: String,
    formModels: List<FormModel>,
    formDefaultModelId: String,
    formNameError: String?,
    formBaseUrlError: String?,
    formApiKeyError: String?,
    formModelsError: String?,
    isSaving: Boolean,
    isTestingConnection: Boolean,
    testConnectionResult: TestConnectionResult?,
    // SR-00001: 模型列表自动获取相关参数
    isFetchingModels: Boolean = false,
    fetchModelsError: String? = null,
    onFetchModels: () -> Unit = {},
    onNameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onAddModel: (String, String) -> Unit,
    onRemoveModel: (String) -> Unit,
    onSetDefaultModel: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKey by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "编辑服务商" else "添加服务商",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                Divider()

                // 表单内容
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 服务商名称
                    item {
                        OutlinedTextField(
                            value = formName,
                            onValueChange = onNameChange,
                            label = { Text("服务商名称 *") },
                            placeholder = { Text("例如：OpenAI") },
                            isError = formNameError != null,
                            supportingText = formNameError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // API 端点
                    item {
                        OutlinedTextField(
                            value = formBaseUrl,
                            onValueChange = onBaseUrlChange,
                            label = { Text("API 端点 *") },
                            placeholder = { Text("https://api.openai.com/v1") },
                            isError = formBaseUrlError != null,
                            supportingText = formBaseUrlError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // API Key
                    item {
                        OutlinedTextField(
                            value = formApiKey,
                            onValueChange = onApiKeyChange,
                            label = { Text("API Key *") },
                            placeholder = { Text("sk-...") },
                            isError = formApiKeyError != null,
                            supportingText = formApiKeyError?.let { { Text(it) } },
                            singleLine = true,
                            visualTransformation = if (showApiKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (showApiKey) "隐藏" else "显示"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 连接测试按钮和结果
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTestConnection,
                                enabled = !isTestingConnection && !isSaving,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isTestingConnection) "测试中..." else "测试连接")
                            }

                            // 测试结果
                            when (testConnectionResult) {
                                is TestConnectionResult.Success -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Text(
                                            text = "✓ 连接成功，延迟 ${testConnectionResult.latencyMs}ms",
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                is TestConnectionResult.Failure -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = "✗ ${testConnectionResult.message}",
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                                null -> {}
                            }
                        }
                    }

                    // 模型列表标题
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "模型列表 *",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = onFetchModels,
                                    enabled = !isFetchingModels && !isSaving
                                ) {
                                    if (isFetchingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(if (isFetchingModels) "获取中..." else "自动获取")
                                }
                                TextButton(
                                    onClick = { showAddModelDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "添加模型",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("手动添加")
                                }
                            }
                        }
                        
                        // 获取模型错误提示
                        if (fetchModelsError != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "✗ $fetchModelsError",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        if (formModelsError != null) {
                            Text(
                                text = formModelsError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 模型列表
                    items(
                        items = formModels,
                        key = { it.id }
                    ) { model ->
                        ModelItem(
                            model = model,
                            isDefault = model.id == formDefaultModelId,
                            onSetDefault = { onSetDefaultModel(model.id) },
                            onRemove = { onRemoveModel(model.id) }
                        )
                    }

                    // 空状态提示
                    if (formModels.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "还没有添加模型，点击上方按钮添加",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Divider()

                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onSave,
                        enabled = !isSaving && !isTestingConnection
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isSaving) "保存中..." else "保存")
                    }
                }
            }
        }
    }

    // 添加模型对话框
    if (showAddModelDialog) {
        AddModelDialog(
            onDismiss = { showAddModelDialog = false },
            onConfirm = { modelId, displayName ->
                onAddModel(modelId, displayName)
                showAddModelDialog = false
            }
        )
    }
}

/**
 * 模型列表项
 */
@Composable
private fun ModelItem(
    model: FormModel,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDefault) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (model.displayName.isNotBlank()) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                if (isDefault) {
                    Text(
                        text = "默认",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isDefault) {
                    TextButton(onClick = onSetDefault) {
                        Text("设为默认")
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * iOS 风格添加模型对话框
 * 
 * BUG-00036 修复：迁移到 iOS 风格
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

// ==================== Previews ====================

@Preview(name = "添加服务商", showBackground = true)
@Composable
private fun ProviderFormDialogAddPreview() {
    EmpathyTheme {
        ProviderFormDialog(
            isEditing = false,
            formName = "",
            formBaseUrl = "",
            formApiKey = "",
            formModels = emptyList(),
            formDefaultModelId = "",
            formNameError = null,
            formBaseUrlError = null,
            formApiKeyError = null,
            formModelsError = null,
            isSaving = false,
            isTestingConnection = false,
            testConnectionResult = null,
            onNameChange = {},
            onBaseUrlChange = {},
            onApiKeyChange = {},
            onAddModel = { _, _ -> },
            onRemoveModel = {},
            onSetDefaultModel = {},
            onTestConnection = {},
            onSave = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "编辑服务商", showBackground = true)
@Composable
private fun ProviderFormDialogEditPreview() {
    EmpathyTheme {
        ProviderFormDialog(
            isEditing = true,
            formName = "OpenAI",
            formBaseUrl = "https://api.openai.com/v1",
            formApiKey = "sk-test123",
            formModels = listOf(
                FormModel(id = "gpt-4", displayName = "GPT-4"),
                FormModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
            ),
            formDefaultModelId = "gpt-4",
            formNameError = null,
            formBaseUrlError = null,
            formApiKeyError = null,
            formModelsError = null,
            isSaving = false,
            isTestingConnection = false,
            testConnectionResult = TestConnectionResult.Success(latencyMs = 150),
            onNameChange = {},
            onBaseUrlChange = {},
            onApiKeyChange = {},
            onAddModel = { _, _ -> },
            onRemoveModel = {},
            onSetDefaultModel = {},
            onTestConnection = {},
            onSave = {},
            onDismiss = {}
        )
    }
}
