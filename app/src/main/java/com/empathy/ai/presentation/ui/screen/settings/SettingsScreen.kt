package com.empathy.ai.presentation.ui.screen.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.presentation.navigation.PromptEditorRoutes
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.dialog.PermissionRequestDialog
import com.empathy.ai.presentation.viewmodel.SettingsViewModel

/**
 * 设置页面
 *
 * 功能：
 * - API Key 配置
 * - AI 服务商选择
 * - 隐私设置
 * - 关于信息
 *
 * @param onNavigateBack 返回回调
 * @param viewModel 设置 ViewModel
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit = {},
    onNavigateToPromptEditor: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 处理成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // 3秒后自动清除
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(SettingsUiEvent.ClearSuccessMessage)
        }
    }

    // 监听权限变化
    DisposableEffect(Unit) {
        onDispose {
            // 当页面销毁时检查权限状态
            viewModel.checkFloatingWindowPermission()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToAiConfig = onNavigateToAiConfig,
        onNavigateToPromptEditor = onNavigateToPromptEditor,
        onRequestPermission = {
            (context as? Activity)?.let { activity ->
                FloatingWindowManager.requestPermission(activity)
            }
        },
        modifier = modifier
    )
}

/**
 * 设置页面内容（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToPromptEditor: (String) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            // 显示错误或成功消息
            uiState.error?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { onEvent(SettingsUiEvent.ClearError) }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            uiState.successMessage?.let { message ->
                Snackbar {
                    Text(message)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 悬浮窗设置
            FloatingWindowSection(
                uiState = uiState,
                onEvent = onEvent
            )

            Divider()

            // AI 服务商选择（API Key 在服务商配置中统一管理）
            AiProviderSection(
                uiState = uiState,
                onEvent = onEvent,
                onNavigateToAiConfig = onNavigateToAiConfig
            )

            Divider()

            // 隐私设置
            PrivacySection(
                uiState = uiState,
                onEvent = onEvent
            )

            Divider()

            // 提示词设置
            PromptSettingsSection(
                onNavigateToPromptEditor = onNavigateToPromptEditor
            )

            Divider()

            // 关于
            AboutSection(
                uiState = uiState,
                onEvent = onEvent
            )
        }

        // 服务商选择对话框
        if (uiState.showProviderDialog) {
            ProviderSelectionDialog(
                selectedProvider = uiState.selectedProvider,
                availableProviders = uiState.availableProviders,
                onProviderSelected = { onEvent(SettingsUiEvent.SelectProvider(it)) },
                onDismiss = { onEvent(SettingsUiEvent.HideProviderDialog) }
            )
        }

        // 清除数据确认对话框
        if (uiState.showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(SettingsUiEvent.HideClearDataDialog) },
                title = { Text("清除所有设置") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("确定要清除以下数据吗？")
                        
                        Text(
                            text = "将被清除：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("• AI服务商配置", style = MaterialTheme.typography.bodySmall)
                        Text("• 隐私保护设置", style = MaterialTheme.typography.bodySmall)
                        Text("• 悬浮窗设置", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "不会清除：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("• 联系人数据", style = MaterialTheme.typography.bodySmall)
                        Text("• 标签数据", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "此操作不可恢复！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { onEvent(SettingsUiEvent.ClearAllData) }
                    ) {
                        Text("确定清除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(SettingsUiEvent.HideClearDataDialog) }) {
                        Text("取消")
                    }
                }
            )
        }

        // 权限说明对话框
        if (uiState.showPermissionDialog) {
            PermissionRequestDialog(
                onConfirm = {
                    onEvent(SettingsUiEvent.HidePermissionDialog)
                    onRequestPermission()
                },
                onDismiss = { onEvent(SettingsUiEvent.HidePermissionDialog) }
            )
        }
    }
}

/**
 * 悬浮窗设置区域
 */
@Composable
private fun FloatingWindowSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "悬浮窗功能",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "启用悬浮窗",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (uiState.hasFloatingWindowPermission) {
                            "在聊天应用上显示快捷按钮"
                        } else {
                            "需要悬浮窗权限"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.hasFloatingWindowPermission) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Switch(
                    checked = uiState.floatingWindowEnabled,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleFloatingWindow) },
                    enabled = uiState.hasFloatingWindowPermission || !uiState.floatingWindowEnabled
                )
            }
        }

        // 权限状态提示
        if (!uiState.hasFloatingWindowPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "警告",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "需要悬浮窗权限",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "点击查看说明并授权",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(onClick = { onEvent(SettingsUiEvent.ShowPermissionDialog) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "查看",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Text(
            text = "提示：悬浮窗功能用于在聊天应用上显示快捷按钮，方便您快速访问 AI 助手功能",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * AI 服务商选择区域
 *
 * API Key 在服务商配置中统一管理
 */
@Composable
private fun AiProviderSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateToAiConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AI 服务商",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 如果有服务商，显示选择卡片
        if (uiState.availableProviders.isNotEmpty()) {
            Card(
                onClick = { onEvent(SettingsUiEvent.ShowProviderDialog) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "当前服务商",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.selectedProvider,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "选择"
                    )
                }
            }
        } else {
            // 如果没有服务商，显示提示卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "提示",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "尚未配置 AI 服务商",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "请先添加至少一个 AI 服务商才能使用 AI 功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 管理服务商按钮
        OutlinedButton(
            onClick = onNavigateToAiConfig,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("管理 AI 服务商")
        }

        Text(
            text = "提示：您可以添加多个 AI 服务商，每个服务商有独立的 API Key 和模型配置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 隐私设置区域
 */
@Composable
private fun PrivacySection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "隐私设置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 数据掩码开关
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "数据掩码",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "AI 分析前自动掩码敏感信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.dataMaskingEnabled,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleDataMasking) }
                )
            }
        }

        // 本地优先模式
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "本地优先模式",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "优先使用本地规则，减少 AI 调用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.localFirstMode,
                    onCheckedChange = { onEvent(SettingsUiEvent.ToggleLocalFirstMode) }
                )
            }
        }
    }
}

/**
 * 关于区域
 */
@Composable
private fun AboutSection(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "应用版本",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = uiState.appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "共情 AI 助手",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "隐私优先的社交沟通助手",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 数据管理
        Text(
            text = "数据管理",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedButton(
            onClick = { onEvent(SettingsUiEvent.ShowClearDataDialog) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("清除所有设置")
        }
        
        Text(
            text = "注意：此操作将清除所有设置数据，但不会删除联系人和标签",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



/**
 * 提示词设置区域
 */
@Composable
private fun PromptSettingsSection(
    onNavigateToPromptEditor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "提示词设置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // 聊天分析指令
        PromptSettingItem(
            title = "聊天分析指令",
            description = "自定义AI分析聊天内容时的行为",
            onClick = { onNavigateToPromptEditor(PromptEditorRoutes.globalScene(PromptScene.ANALYZE)) }
        )

        // 安全检查指令
        PromptSettingItem(
            title = "安全检查指令",
            description = "自定义AI检查消息安全性时的行为",
            onClick = { onNavigateToPromptEditor(PromptEditorRoutes.globalScene(PromptScene.CHECK)) }
        )

        // 信息提取指令
        PromptSettingItem(
            title = "信息提取指令",
            description = "自定义AI提取关键信息时的行为",
            onClick = { onNavigateToPromptEditor(PromptEditorRoutes.globalScene(PromptScene.EXTRACT)) }
        )

        // 每日总结指令
        PromptSettingItem(
            title = "每日总结指令",
            description = "自定义AI生成每日总结时的行为",
            onClick = { onNavigateToPromptEditor(PromptEditorRoutes.globalScene(PromptScene.SUMMARY)) }
        )

        Text(
            text = "提示：自定义指令可以让AI更好地理解您的需求，提供更精准的分析和建议",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 提示词设置项
 */
@Composable
private fun PromptSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "编辑"
            )
        }
    }
}

/**
 * 服务商选择对话框
 */
@Composable
private fun ProviderSelectionDialog(
    selectedProvider: String,
    availableProviders: List<String>,
    onProviderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择 AI 服务商") },
        text = {
            Column {
                availableProviders.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == selectedProvider,
                            onClick = { onProviderSelected(provider) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(provider)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

// ==================== Previews ====================

@Preview(name = "设置页面 - 无服务商", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    EmpathyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                selectedProvider = "",
                availableProviders = emptyList(),
                hasFloatingWindowPermission = false
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAiConfig = {},
            onNavigateToPromptEditor = {},
            onRequestPermission = {}
        )
    }
}

@Preview(name = "设置页面 - 已配置服务商", showBackground = true)
@Composable
private fun SettingsScreenConfiguredPreview() {
    EmpathyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                selectedProvider = "DeepSeek",
                availableProviders = listOf("OpenAI", "DeepSeek"),
                hasFloatingWindowPermission = true,
                floatingWindowEnabled = true
            ),
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAiConfig = {},
            onNavigateToPromptEditor = {},
            onRequestPermission = {}
        )
    }
}
