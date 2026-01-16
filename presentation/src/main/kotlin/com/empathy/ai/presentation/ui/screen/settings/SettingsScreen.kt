package com.empathy.ai.presentation.ui.screen.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.domain.util.MediaProjectionPermissionConstants
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
import com.empathy.ai.presentation.ui.component.dialog.IOSAlertDialog
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog
import com.empathy.ai.presentation.ui.component.dialog.IOSPermissionRequestDialog
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.ui.component.ios.IOSSwitch
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation
import com.empathy.ai.presentation.ui.screen.settings.component.DeveloperOptionsSection
import com.empathy.ai.presentation.viewmodel.DeveloperModeViewModel
import com.empathy.ai.presentation.viewmodel.SettingsViewModel

private const val SCREENSHOT_PERMISSION_ACTIVITY = "com.empathy.ai.ui.ScreenshotPermissionActivity"

/**
 * 设置页面（iOS风格）
 *
 * ## 业务职责
 * 应用的全局配置中心，提供以下配置项：
 * - AI服务商配置（选择、切换）
 * - 提示词设置（各场景模板）
 * - 悬浮窗开关与权限管理
 * - 隐私保护设置（数据掩码、本地优先模式）
 * - 个人画像管理
 * - 数据清除功能
 *
 * ## 关联文档
 * - PRD-00002: 设置功能需求
 * - TDD-00002: 设置页面技术设计
 * - BUG-00050: 开发者模式导航时意外退出
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │  设置                          34sp │  ← iOS大标题
 * ├─────────────────────────────────────┤
 * │  AI 配置                           │  ← 分组标题
 * │  ┌───────────────────────────────┐ │
 * │  │ [⚙️] AI服务商           DeepSeek>│ │
 * │  │ [ℹ️] 提示词设置              >  │ │
 * │  └───────────────────────────────┘ │
 * ├─────────────────────────────────────┤
 * │  悬浮窗                             │
 * │  ┌───────────────────────────────┐ │
 * │  │ [🌿] 启用悬浮窗    [开关]     │ │
 * │  └───────────────────────────────┘ │
 * ├─────────────────────────────────────┤
 * │  隐私保护                           │
 * │  ┌───────────────────────────────┐ │
 * │  │ [🔒] 数据掩码      [开关]     │ │
 * │  │ [🌿] 本地优先模式  [开关]     │ │
 * │  └───────────────────────────────┘ │
 * ├─────────────────────────────────────┤
 * │  [联系人]  [AI军师]  [设置]        │  ← 底部导航栏
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 核心设计决策
 * 1. **iOS风格设置项**: 使用IOSSettingsSection/IOSSettingsItem组件
 * 2. **分组展示**: 按功能分组（AI配置、悬浮窗、隐私保护等）
 * 3. **Switch切换**: 开关类设置使用IOSSwitch组件
 * 4. **权限处理**: 悬浮窗权限需要特殊处理（跳转到系统设置）
 * 5. **双重导航**: 支持底部Tab导航和内部页面导航
 * 6. **开发者模式**: DeveloperModeViewModel使用Activity作为ViewModelStoreOwner，
 *    通过hiltViewModel()自动获取Activity级别的实例，确保导航时状态不丢失
 *
 * ## 特殊权限处理
 * 悬浮窗权限需要用户手动在系统设置中授权：
 * 1. UI层检测权限状态
 * 2. 用户点击开关 → 无权限 → 显示授权确认对话框
 * 3. 用户确认 → 跳转系统设置页面（ACTION_MANAGE_OVERLAY_PERMISSION）
 * 4. 用户返回 → 检测权限结果 → 更新UI状态
 *
 * @param onNavigateBack 返回回调（用于非底部Tab场景）
 * @param onNavigateToAiConfig 导航到AI配置页
 * @param onNavigateToPromptEditor 导航到提示词编辑器
 * @param onNavigateToUserProfile 导航到个人画像页
 * @param onNavigate 底部导航栏导航回调
 * @param onAddClick 添加按钮点击回调
 * @param currentRoute 当前路由（用于底部导航栏高亮）
 * @param showBottomBar 是否显示底部导航栏
 * @param viewModel 设置ViewModel
 * @param modifier Modifier
 * @see SettingsViewModel 管理设置状态和业务逻辑
 * @see DeveloperModeViewModel 管理开发者模式状态（Activity级别）
 * @see FloatingWindowManager 悬浮窗权限和服务管理
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit = {},
    onNavigateToPromptEditor: (String) -> Unit = {},
    onNavigateToUserProfile: () -> Unit = {},
    onNavigateToSystemPromptList: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    currentRoute: String = NavRoutes.SETTINGS,
    showBottomBar: Boolean = true,
    isVisible: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // 使用hiltViewModel()获取Activity级别的DeveloperModeViewModel实例
    // 这样可以确保在导航时状态不丢失
    val developerModeViewModel: DeveloperModeViewModel = hiltViewModel()
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeveloperMode by developerModeViewModel.isDeveloperMode.collectAsStateWithLifecycle()
    val toastMessage by developerModeViewModel.toastMessage.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 显示Toast消息
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(SettingsUiEvent.ClearSuccessMessage)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshScreenshotPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.checkFloatingWindowPermission() }
    }
    
    // 监听权限请求标志，触发实际的权限请求Intent
    // [BUG-00063修复] 增加可见性门控：不可见时不触发权限请求，避免隐藏Tab跳转系统设置
    LaunchedEffect(uiState.pendingPermissionRequest, isVisible) {
        if (!isVisible) {
            // 清理待处理的权限请求，防止返回可见时意外触发
            if (uiState.pendingPermissionRequest) {
                viewModel.onEvent(SettingsUiEvent.PermissionRequestHandled)
            }
            return@LaunchedEffect
        }

        if (uiState.pendingPermissionRequest) {
            // 标记已处理
            viewModel.onEvent(SettingsUiEvent.PermissionRequestHandled)

            // 触发权限请求
            (context as? Activity)?.let { activity ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivityForResult(
                            intent,
                            FloatingWindowManager.REQUEST_CODE_OVERLAY_PERMISSION
                        )
                        android.util.Log.d("SettingsScreen", "已跳转到悬浮窗权限设置页面")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsScreen", "跳转权限设置失败", e)
                }
            }
        }
    }

    LaunchedEffect(uiState.pendingScreenshotPermissionRequest, isVisible) {
        if (!isVisible) {
            if (uiState.pendingScreenshotPermissionRequest) {
                viewModel.onEvent(SettingsUiEvent.ScreenshotPermissionRequestHandled)
            }
            return@LaunchedEffect
        }

        if (uiState.pendingScreenshotPermissionRequest) {
            viewModel.onEvent(SettingsUiEvent.ScreenshotPermissionRequestHandled)
            try {
                val intent = Intent().apply {
                    setClassName(context.packageName, SCREENSHOT_PERMISSION_ACTIVITY)
                    putExtra(
                        MediaProjectionPermissionConstants.EXTRA_REQUEST_SOURCE,
                        MediaProjectionPermissionConstants.REQUEST_SOURCE_SETTINGS
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d("SettingsScreen", "已发起截图权限请求")
            } catch (e: Exception) {
                Log.e("SettingsScreen", "发起截图权限请求失败", e)
            }
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        isDeveloperMode = isDeveloperMode,
        onEvent = viewModel::onEvent,
        onVersionClick = developerModeViewModel::onVersionClick,
        onNavigateBack = onNavigateBack,
        onNavigateToAiConfig = onNavigateToAiConfig,
        onNavigateToPromptEditor = onNavigateToPromptEditor,
        onNavigateToUserProfile = onNavigateToUserProfile,
        onNavigateToSystemPromptList = onNavigateToSystemPromptList,
        onNavigate = onNavigate,
        onAddClick = onAddClick,
        currentRoute = currentRoute,
        showBottomBar = showBottomBar,
        promptScenes = viewModel.promptScenesOrdered,
        modifier = modifier
    )
}


/**
 * 设置页面内容（iOS风格）
 */
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    isDeveloperMode: Boolean,
    onEvent: (SettingsUiEvent) -> Unit,
    onVersionClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToPromptEditor: (String) -> Unit,
    onNavigateToUserProfile: () -> Unit,
    onNavigateToSystemPromptList: () -> Unit,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    currentRoute: String,
    showBottomBar: Boolean,
    promptScenes: List<PromptScene>,
    modifier: Modifier = Modifier
) {
    val displayId = LocalView.current.display?.displayId

    Scaffold(
        modifier = modifier,
        containerColor = iOSBackground,
        bottomBar = {
            if (showBottomBar) {
                EmpathyBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    onAddClick = onAddClick
                )
            }
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
            // iOS大标题 - 使用响应式字体
            item {
                Text(
                    text = "设置",
                    fontSize = dimensions.fontSizeLargeTitle,
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
                        onClick = {
                            onNavigateToPromptEditor(
                                PromptEditorRoutes.globalSceneWithSource(
                                    PromptScene.ANALYZE,
                                    NavRoutes.SOURCE_SETTINGS
                                )
                            )
                        }
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
                        showDivider = true,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.floatingWindowEnabled,
                                onCheckedChange = {
                                    android.util.Log.d(
                                        "SettingsScreen",
                                        "ToggleFloatingWindow displayId=$displayId"
                                    )
                                    onEvent(SettingsUiEvent.ToggleFloatingWindow(displayId))
                                },
                                enabled = uiState.hasFloatingWindowPermission || !uiState.floatingWindowEnabled
                            )
                        },
                        onClick = if (!uiState.hasFloatingWindowPermission) {
                            { onEvent(SettingsUiEvent.ShowPermissionDialog) }
                        } else null
                    )
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSBlue,
                        title = "截图权限",
                        subtitle = if (uiState.hasScreenshotPermission) null else "需要截图权限",
                        showArrow = false,
                        showDivider = true,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.hasScreenshotPermission,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleScreenshotPermission) }
                            )
                        },
                        onClick = if (!uiState.hasScreenshotPermission) {
                            { onEvent(SettingsUiEvent.ToggleScreenshotPermission) }
                        } else null
                    )
                    IOSSettingsItem(
                        icon = Icons.Default.Info,
                        iconBackgroundColor = iOSBlue,
                        title = "连续截屏",
                        subtitle = "1.5秒内可继续框选",
                        showArrow = false,
                        showDivider = false,
                        trailing = {
                            IOSSwitch(
                                checked = uiState.continuousScreenshotEnabled,
                                onCheckedChange = { onEvent(SettingsUiEvent.ToggleContinuousScreenshot) }
                            )
                        }
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
                        showDivider = false,
                        onClick = onVersionClick
                    )
                }
            }

            // 开发者选项分组（仅在开发者模式解锁后显示）
            if (isDeveloperMode) {
                item {
                    DeveloperOptionsSection(
                        onSystemPromptEditClick = onNavigateToSystemPromptList
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
            IOSAlertDialog(
                title = "清除所有设置",
                message = "确定要清除以下数据吗？\n\n将被清除：\n• AI服务商配置\n• 隐私保护设置\n• 悬浮窗设置\n\n不会清除：\n• 联系人数据\n• 标签数据\n\n此操作不可恢复！",
                confirmText = "确定清除",
                dismissText = "取消",
                onConfirm = { onEvent(SettingsUiEvent.ClearAllData) },
                onDismiss = { onEvent(SettingsUiEvent.HideClearDataDialog) },
                isDestructive = true,
                showDismissButton = true
            )
        }

        if (uiState.showPermissionDialog) {
            IOSPermissionRequestDialog(
                onConfirm = {
                    onEvent(SettingsUiEvent.HidePermissionDialog)
                    // 触发权限请求流程
                    onEvent(SettingsUiEvent.RequestFloatingWindowPermission)
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
    val dimensions = AdaptiveDimensions.current
    
    IOSInputDialog(
        title = "选择 AI 服务商",
        content = {
            Column {
                availableProviders.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensions.spacingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == selectedProvider,
                            onClick = { onProviderSelected(provider) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = iOSBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                        Text(
                            text = provider,
                            fontSize = dimensions.fontSizeBody
                        )
                    }
                }
            }
        },
        confirmText = "关闭",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        showDismissButton = false
    )
}

@Composable
private fun ClearDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // 使用 IOSAlertDialog 替代，已在上方使用
    IOSAlertDialog(
        title = "清除所有设置",
        message = "确定要清除以下数据吗？\n\n将被清除：\n• AI服务商配置\n• 隐私保护设置\n• 悬浮窗设置\n\n不会清除：\n• 联系人数据\n• 标签数据\n\n此操作不可恢复！",
        confirmText = "确定清除",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true
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
            isDeveloperMode = false,
            onEvent = {},
            onVersionClick = {},
            onNavigateBack = {},
            onNavigateToAiConfig = {},
            onNavigateToPromptEditor = {},
            onNavigateToUserProfile = {},
            onNavigateToSystemPromptList = {},
            onNavigate = {},
            onAddClick = {},
            currentRoute = NavRoutes.SETTINGS,
            showBottomBar = true,
            promptScenes = PromptScene.SETTINGS_SCENE_ORDER
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
            isDeveloperMode = false,
            onEvent = {},
            onVersionClick = {},
            onNavigateBack = {},
            onNavigateToAiConfig = {},
            onNavigateToPromptEditor = {},
            onNavigateToUserProfile = {},
            onNavigateToSystemPromptList = {},
            onNavigate = {},
            onAddClick = {},
            currentRoute = NavRoutes.SETTINGS,
            showBottomBar = true,
            promptScenes = PromptScene.SETTINGS_SCENE_ORDER
        )
    }
}

@Preview(name = "设置页面 - 开发者模式", showBackground = true)
@Composable
private fun SettingsScreenDeveloperModePreview() {
    EmpathyTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                selectedProvider = "DeepSeek",
                availableProviders = listOf("OpenAI", "DeepSeek"),
                hasFloatingWindowPermission = true,
                floatingWindowEnabled = true
            ),
            isDeveloperMode = true,
            onEvent = {},
            onVersionClick = {},
            onNavigateBack = {},
            onNavigateToAiConfig = {},
            onNavigateToPromptEditor = {},
            onNavigateToUserProfile = {},
            onNavigateToSystemPromptList = {},
            onNavigate = {},
            onAddClick = {},
            currentRoute = NavRoutes.SETTINGS,
            showBottomBar = true,
            promptScenes = PromptScene.SETTINGS_SCENE_ORDER
        )
    }
}
