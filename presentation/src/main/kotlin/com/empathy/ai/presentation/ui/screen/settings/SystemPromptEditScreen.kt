package com.empathy.ai.presentation.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.SystemPromptScene
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.viewmodel.SystemPromptEditUiState
import com.empathy.ai.presentation.viewmodel.SystemPromptEditViewModel

/**
 * 系统提示词编辑页面
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 编辑单个场景的Header和Footer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: SystemPromptEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 监听成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    SystemPromptEditContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onHeaderChange = viewModel::updateHeader,
        onSave = viewModel::save,
        onReset = viewModel::resetToDefault,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemPromptEditContent(
    uiState: SystemPromptEditUiState,
    onNavigateBack: () -> Unit,
    onHeaderChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onClearError: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    var showResetDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }

    // 复制默认提示词到编辑框
    val onCopyDefaultToEditor: () -> Unit = {
        onHeaderChange(uiState.defaultHeader)
    }

    // 复制到剪贴板
    val onCopyToClipboard: () -> Unit = {
        clipboardManager.setText(AnnotatedString(uiState.defaultHeader))
        showCopiedToast = true
    }

    // 自动隐藏复制成功提示
    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            kotlinx.coroutines.delay(2000)
            showCopiedToast = false
        }
    }

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            TopAppBar(
                title = { Text(uiState.sceneName.ifEmpty { "编辑提示词" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 重置按钮
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = uiState.hasCustomConfig
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置",
                            tint = if (uiState.hasCustomConfig) iOSBlue else iOSTextSecondary
                        )
                    }
                    // 保存按钮
                    TextButton(
                        onClick = onSave,
                        enabled = uiState.hasUnsavedChanges && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = iOSBlue
                            )
                        } else {
                            Text(
                                "保存",
                                color = if (uiState.hasUnsavedChanges) iOSBlue else iOSTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iOSBackground
                )
            )
        },
        snackbarHost = {
            uiState.error?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = onClearError) {
                            Text("关闭")
                        }
                    }
                ) { Text(error) }
            }
            uiState.successMessage?.let { message ->
                Snackbar { Text(message) }
            }
            if (showCopiedToast) {
                Snackbar { Text("已复制到剪贴板") }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iOSBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(iOSBackground)
                    .verticalScroll(rememberScrollState())
            ) {
                // 场景描述
                if (uiState.sceneDescription.isNotEmpty()) {
                    Text(
                        text = uiState.sceneDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = iOSTextSecondary,
                        modifier = Modifier.padding(
                            horizontal = dimensions.spacingMedium,
                            vertical = dimensions.spacingSmall
                        )
                    )
                }

                // 系统提示词编辑区（仅Header，直接替换模式）
                IOSSettingsSection(title = "系统提示词（角色定义）") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(iOSCardBackground)
                            .padding(dimensions.spacingMedium)
                    ) {
                        Text(
                            text = "自定义AI的角色定义和行为风格。留空则使用默认值。",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSTextSecondary,
                            modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                        )
                        
                        // 复制默认值按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = dimensions.spacingSmall),
                            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                        ) {
                            OutlinedButton(
                                onClick = onCopyDefaultToEditor,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = iOSBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制默认值到编辑框", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        OutlinedTextField(
                            value = uiState.header,
                            onValueChange = onHeaderChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp),
                            placeholder = { Text("留空使用默认值...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = iOSBlue,
                                unfocusedBorderColor = iOSTextSecondary.copy(alpha = 0.3f)
                            )
                        )
                        // 显示默认值预览
                        if (uiState.defaultHeader.isNotEmpty()) {
                            ExpandableDefaultPreview(
                                title = "查看默认系统提示词",
                                content = uiState.defaultHeader,
                                onCopyToClipboard = onCopyToClipboard
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensions.spacingMedium))

                // 输出格式说明（只读）
                IOSSettingsSection(title = "输出格式（不可编辑）") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(iOSCardBackground)
                            .padding(dimensions.spacingMedium)
                    ) {
                        Text(
                            text = "输出格式与数据解析逻辑绑定，不支持自定义修改。",
                            style = MaterialTheme.typography.bodySmall,
                            color = iOSTextSecondary,
                            modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                        )
                        // 显示输出格式预览（只读）
                        if (uiState.defaultFooter.isNotEmpty()) {
                            ExpandableDefaultPreview(
                                title = "查看输出格式定义",
                                content = uiState.defaultFooter
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimensions.spacingXLarge))
            }
        }

        // 重置确认对话框
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("重置为默认值") },
                text = { Text("确定要清除自定义配置，恢复为默认值吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetDialog = false
                            onReset()
                        }
                    ) {
                        Text("确定", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 可展开的默认值预览组件
 */
@Composable
private fun ExpandableDefaultPreview(
    title: String,
    content: String,
    onCopyToClipboard: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val dimensions = AdaptiveDimensions.current

    Column(modifier = Modifier.padding(top = dimensions.spacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (expanded) "收起$title" else title,
                    style = MaterialTheme.typography.bodySmall,
                    color = iOSBlue
                )
            }
            
            // 复制到剪贴板按钮
            if (onCopyToClipboard != null) {
                TextButton(
                    onClick = onCopyToClipboard,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(14.dp),
                        tint = iOSBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "复制",
                        style = MaterialTheme.typography.bodySmall,
                        color = iOSBlue
                    )
                }
            }
        }
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions.spacingSmall),
                color = iOSBackground,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = iOSTextSecondary,
                    modifier = Modifier.padding(dimensions.spacingSmall)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SystemPromptEditScreenPreview() {
    EmpathyTheme {
        SystemPromptEditContent(
            uiState = SystemPromptEditUiState(
                scene = SystemPromptScene.ANALYZE,
                sceneName = "聊天分析",
                sceneDescription = "分析对方意图和情绪，提供沟通建议",
                header = "",
                footer = "",
                defaultHeader = "你是用户的\"恋爱军师\"...",
                defaultFooter = "请以JSON格式返回..."
            ),
            onNavigateBack = {},
            onHeaderChange = {},
            onSave = {},
            onReset = {},
            onClearError = {}
        )
    }
}
