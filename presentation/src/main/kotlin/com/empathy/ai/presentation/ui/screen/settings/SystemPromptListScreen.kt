package com.empathy.ai.presentation.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSRed
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsItem
import com.empathy.ai.presentation.ui.component.ios.IOSSettingsSection
import com.empathy.ai.presentation.viewmodel.SceneItem
import com.empathy.ai.presentation.viewmodel.SystemPromptListUiState
import com.empathy.ai.presentation.viewmodel.SystemPromptListViewModel

/**
 * 系统提示词列表页面
 *
 * PRD-00033: 开发者模式 - 系统提示词编辑
 * 显示所有可编辑的AI场景列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: SystemPromptListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SystemPromptListContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToEdit = onNavigateToEdit,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemPromptListContent(
    uiState: SystemPromptListUiState,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onClearError: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            TopAppBar(
                title = { Text("系统提示词") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(iOSBackground)
            ) {
                // 说明文字
                item {
                    Text(
                        text = "自定义各AI场景的系统提示词Header和Footer。留空则使用默认值。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iOSTextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(
                            horizontal = dimensions.spacingMedium,
                            vertical = dimensions.spacingSmall
                        )
                    )
                }

                // 场景列表
                item {
                    IOSSettingsSection(title = "AI场景") {
                        uiState.scenes.forEachIndexed { index, sceneItem ->
                            IOSSettingsItem(
                                icon = Icons.Default.Code,
                                iconBackgroundColor = getSceneColor(sceneItem.scene),
                                title = sceneItem.displayName,
                                subtitle = sceneItem.description,
                                showDivider = index < uiState.scenes.size - 1,
                                trailing = if (sceneItem.hasCustomConfig) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "已自定义",
                                            tint = iOSGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else null,
                                onClick = { onNavigateToEdit(sceneItem.scene.name) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(dimensions.spacingXLarge))
                }
            }
        }
    }
}

/**
 * 获取场景对应的颜色
 */
private fun getSceneColor(scene: SystemPromptScene) = when (scene) {
    SystemPromptScene.ANALYZE -> iOSBlue
    SystemPromptScene.POLISH -> iOSGreen
    SystemPromptScene.REPLY -> iOSPurple
    SystemPromptScene.SUMMARY -> iOSOrange
    SystemPromptScene.AI_ADVISOR -> iOSRed
}

@Preview(showBackground = true)
@Composable
private fun SystemPromptListScreenPreview() {
    EmpathyTheme {
        SystemPromptListContent(
            uiState = SystemPromptListUiState(
                scenes = listOf(
                    SceneItem(
                        scene = SystemPromptScene.ANALYZE,
                        displayName = "聊天分析",
                        description = "分析对方意图和情绪",
                        hasCustomConfig = true
                    ),
                    SceneItem(
                        scene = SystemPromptScene.POLISH,
                        displayName = "文本润色",
                        description = "优化表达方式",
                        hasCustomConfig = false
                    ),
                    SceneItem(
                        scene = SystemPromptScene.REPLY,
                        displayName = "智能回复",
                        description = "生成回复建议",
                        hasCustomConfig = false
                    )
                )
            ),
            onNavigateBack = {},
            onNavigateToEdit = {},
            onClearError = {}
        )
    }
}
