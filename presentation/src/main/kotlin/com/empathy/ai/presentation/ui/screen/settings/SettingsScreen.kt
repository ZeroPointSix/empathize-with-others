package com.empathy.ai.presentation.ui.screen.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.empathy.ai.presentation.theme.AppSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.navigation.PromptEditorRoutes
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.ui.component.dialog.PermissionRequestDialog
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSSwitch
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation
import com.empathy.ai.presentation.viewmodel.SettingsViewModel

/**
 * 设置页面（iOS风格）
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit = {},
    onNavigateToPromptEditor: (String) -> Unit = {},
    onNavigateToUserProfile: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    currentRoute: String = NavRoutes.SETTINGS,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(SettingsUiEvent.ClearSuccessMessage)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.checkFloatingWindowPermission() }
    }

    SettingsScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToAiConfig = onNavigateToAiConfig,
        onNavigateToPromptEditor = onNavigateToPromptEditor,
        onNavigateToUserProfile = onNavigateToUserProfile,
        onNavigate = onNavigate,
        onAddClick = onAddClick,
        currentRoute = currentRoute,
        promptScenes = viewModel.promptScenesOrdered,
        onRequestPermission = {
            (context as? Activity)?.let { activity ->
                FloatingWindowManager.requestPermission(activity)
            }
        },
        modifier = modifier
    )
}


/**
 * 设置页面内容（iOS风格）
 */
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToPromptEditor: (String) -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    currentRoute: String,
    promptScenes: List<PromptScene>,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = iOSBackground,
        bottomBar = {
            EmpathyBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onAddClick = onAddClick
            )
        },
        snackbarHost = {
            uiState.error?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { onEvent(SettingsUiEvent.ClearError) }) {
                            Text("关闭")
                        }
                    }
                ) { Text(error) }
            }
            uiState.successMessage?.let { message ->
                Snackbar { Text(message) }
            }
        }
    ) { paddingValues ->
        val dimensions = AdaptiveDimensions.current
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(iOSBackground)
        ) {
            // iOS大标题
            item {
                Text(
                    text = "设置",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary,
                    modifier = Modifier.padding(start = dimensions.spacingMedium, top = dimensions.spacingMedium, bottom = dimensions.spacingSmall)
                )
            }

            // AI配置分组
            item {
                IOSSettingsSection(title = "AI 配置") {
                    IOSSettingsItem(
                        icon = Icons.Default.Settings,
                        iconBackgroundColor = iOSBlue,
                        title = "AI 服务商",
                        value = uiState.selectedProvider.ifEmpty { "未配置" },
                        onClick = onNavigateToAiConfig
                    )
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSPurple,
                        title = "提示词设置",
                        showDivider = false,
                        onClick = { onNavigateToPromptEditor(PromptEditorRoutes.globalScene(PromptScene.ANALYZE)) }
                    )
                }
            }

            // 悬浮窗分组
            item {
                IOSSettingsSection(title = "悬浮窗") {
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSGreen,
                        title = "启用悬浮窗",
                        subtitle = if (uiState.hasFloatingWindowPermission) null else "需要悬浮窗权限",
                        showArrow = false,
                        showDivider = false,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.floatingWindowEnabled,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleFloatingWindow) },
                                enabled = uiState.hasFloatingWindowPermission || !uiState.floatingWindowEnabled
                            )
                        },
                        onClick = if (!uiState.hasFloatingWindowPermission) {
                            { onEvent(SettingsUiEvent.ShowPermissionDialog) }
                        } else null
                    )
                }
            }

            // 隐私保护分组
            item {
                IOSSettingsSection(title = "隐私保护") {
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSBlue,
                        title = "数据掩码",
                        subtitle = "AI 分析前自动掩码敏感信息",
                        showArrow = false,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.dataMaskingEnabled,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleDataMasking) }
                            )
                        }
                    )
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSGreen,
                        title = "本地优先模式",
                        subtitle = "优先使用本地规则，减少 AI 调用",
                        showArrow = false,
                        showDivider = false,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.localFirstMode,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleLocalFirstMode) }
                            )
                        }
                    )
                }
            }

            // 个人画像分组
            item {
                IOSSettingsSection(title = "个人画像") {
                    IOSSettingsItem(
                        icon = Icons.Default.Person,
                        iconBackgroundColor = iOSPurple,
                        title = "管理个人画像",
                        subtitle = "设置您的性格特点、价值观等",
                        showDivider = false,
                        onClick = onNavigateToUserProfile
                    )
                }
            }

            // 数据管理分组
            item {
                IOSSettingsSection(title = "数据管理") {
                    IOSSettingsItem(
                        icon = Icons.Default.Delete,
                        iconBackgroundColor = iOSRed,
                        title = "清除所有设置",
                        showDivider = false,
                        onClick = { onEvent(SettingsUiEvent.ShowClearDataDialog) }
                    )
                }
            }

            // 关于分组
            item {
                IOSSettingsSection(title = "关于") {
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = Color.Gray,
                        title = "版本",
                        value = uiState.appVersion,
                        showArrow = false,
                        showDivider = false
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(dimensions.spacingXLarge)) }
        }

        // 对话框
        if (uiState.showProviderDialog) {
            ProviderSelectionDialog(
                selectedProvider = uiState.selectedProvider,
                availableProviders = uiState.availableProviders,
                onProviderSelected = { onEvent(SettingsUiEvent.SelectProvider(it)) },
                onDismiss = { onEvent(SettingsUiEvent.HideProviderDialog) }
            )
        }

        if (uiState.showClearDataDialog) {
            ClearDataDialog(
                onConfirm = { onEvent(SettingsUiEvent.ClearAllData) },
                onDismiss = { onEvent(SettingsUiEvent.HideClearDataDialog) }
            )
        }

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
                            .padding(vertical = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == selectedProvider,
                            onClick = { onProviderSelected(provider) }
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(provider)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun ClearDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清除所有设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text("确定要清除以下数据吗？")
                Text("将被清除：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text("• AI服务商配置", style = MaterialTheme.typography.bodySmall)
                Text("• 隐私保护设置", style = MaterialTheme.typography.bodySmall)
                Text("• 悬浮窗设置", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text("不会清除：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text("• 联系人数据", style = MaterialTheme.typography.bodySmall)
                Text("• 标签数据", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("此操作不可恢复！", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定清除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ==================== Previews ====================

@Preview(name = "设置页面 - iOS风格", showBackground = true)
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
            onNavigateToUserProfile = {},
            onNavigate = {},
            onAddClick = {},
            currentRoute = NavRoutes.SETTINGS,
            promptScenes = PromptScene.SETTINGS_SCENE_ORDER,
            onRequestPermission = {}
        )
    }
}

@Preview(name = "设置页面 - 已配置", showBackground = true)
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
            onNavigateToUserProfile = {},
            onNavigate = {},
            onAddClick = {},
            currentRoute = NavRoutes.SETTINGS,
            promptScenes = PromptScene.SETTINGS_SCENE_ORDER,
            onRequestPermission = {}
        )
    }
}
