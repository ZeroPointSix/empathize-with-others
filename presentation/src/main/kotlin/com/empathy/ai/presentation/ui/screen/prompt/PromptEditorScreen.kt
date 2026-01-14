package com.empathy.ai.presentation.ui.screen.prompt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.screen.prompt.component.CharacterCounter
import com.empathy.ai.presentation.ui.screen.prompt.component.DiscardConfirmDialog
import com.empathy.ai.presentation.ui.screen.prompt.component.InlineErrorBanner
import com.empathy.ai.presentation.ui.screen.prompt.component.PromptInputField
import com.empathy.ai.presentation.viewmodel.PromptEditorViewModel

/**
 * 提示词编辑器主界面（iOS风格）
 *
 * @param onNavigateBack 返回导航回调
 * @param viewModel ViewModel实例
 */
@Composable
fun PromptEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PromptEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 系统返回手势按“取消编辑”逻辑处理（包含未保存确认）
    BackHandler {
        viewModel.onEvent(PromptEditorUiEvent.CancelEdit)
    }

    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            when (result) {
                is PromptEditorResult.Saved,
                is PromptEditorResult.Cancelled -> onNavigateBack()
            }
        }
    }

    PromptEditorContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

/**
 * 提示词编辑器内容（无状态组件，便于预览和测试）
 * 
 * BUG-00061 修复：
 * - 只在 isInitialLoading 时显示全屏加载
 * - Tab 区域始终可见，不受加载状态影响
 * - 内容区域支持局部加载状态（isSceneSwitching）
 */
@Composable
private fun PromptEditorContent(
    uiState: PromptEditorUiState,
    onEvent: (PromptEditorUiEvent) -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            IOSNavigationBar(
                canSave = uiState.canSave,
                isSaving = uiState.isSaving,
                onCancelClick = { onEvent(PromptEditorUiEvent.CancelEdit) },
                onSaveClick = { onEvent(PromptEditorUiEvent.SavePrompt) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // 只在首次加载时显示全屏加载
            if (uiState.isInitialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = iOSBlue)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab区域 - 始终显示，不受加载状态影响
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = iOSCardBackground
                    ) {
                        PromptSceneTab(
                            selectedScene = uiState.currentScene,
                            onSceneSelected = { onEvent(PromptEditorUiEvent.SwitchScene(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    HorizontalDivider(color = iOSSeparator, thickness = 0.5.dp)

                    InlineErrorBanner(
                        errorMessage = uiState.errorMessage,
                        onDismiss = { onEvent(PromptEditorUiEvent.ClearError) }
                    )

                    // 内容区域 - 支持局部加载状态
                    PromptEditorContentSection(
                        uiState = uiState,
                        onEvent = onEvent,
                        isLoading = uiState.isSceneSwitching
                    )

                    BottomButtons(
                        canSave = uiState.canSave,
                        isSaving = uiState.isSaving,
                        onResetClick = { onEvent(PromptEditorUiEvent.ResetToDefault) },
                        onSaveClick = { onEvent(PromptEditorUiEvent.SavePrompt) }
                    )
                }
            }

            if (uiState.showDiscardDialog) {
                DiscardConfirmDialog(
                    onConfirm = { onEvent(PromptEditorUiEvent.ConfirmDiscard) },
                    onDismiss = { onEvent(PromptEditorUiEvent.DismissDiscardDialog) }
                )
            }
        }
    }
}

/**
 * 内容区域组件
 * 
 * BUG-00061 修复：支持局部加载状态，不影响 Tab 区域
 */
@Composable
private fun ColumnScope.PromptEditorContentSection(
    uiState: PromptEditorUiState,
    onEvent: (PromptEditorUiEvent) -> Unit,
    isLoading: Boolean
) {
    val dimensions = AdaptiveDimensions.current
    
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 编辑区域卡片（带边框）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = iOSCardBackground,
                border = BorderStroke(1.dp, iOSSeparator)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 输入区域
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PromptInputField(
                            value = uiState.currentPrompt,
                            onValueChange = { if (!isLoading) onEvent(PromptEditorUiEvent.UpdatePrompt(it)) },
                            placeholder = uiState.placeholderText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 分隔线
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp),
                        color = iOSSeparator,
                        thickness = 0.5.dp
                    )

                    // 工具栏：AI优化按钮 + 字数统计
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI优化按钮
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !isLoading
                                ) {
                                    // TODO: AI优化功能
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI优化",
                                tint = if (isLoading) iOSTextSecondary else iOSBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AI优化",
                                fontSize = dimensions.fontSizeBody,
                                fontWeight = FontWeight.Medium,
                                color = if (isLoading) iOSTextSecondary else iOSBlue
                            )
                        }

                        // 字数统计
                        CharacterCounter(
                            charCount = uiState.charCount,
                            maxLength = PromptEditorUiState.MAX_PROMPT_LENGTH,
                            isOverLimit = uiState.isOverLimit,
                            isNearLimit = uiState.isNearLimit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "提示词将帮助 AI 更好地理解你的需求，请根据场景自定义你的提示词。",
                fontSize = dimensions.fontSizeCaption,
                color = iOSTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        // 加载指示器覆盖层（场景切换时显示）
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(iOSCardBackground.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = iOSBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}


/**
 * iOS风格导航栏
 */
@Composable
private fun IOSNavigationBar(
    canSave: Boolean,
    isSaving: Boolean,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancelClick) {
                Text(text = "取消", color = iOSBlue, fontSize = dimensions.fontSizeTitle)
            }

            Text(
                text = "编辑提示词",
                fontSize = dimensions.fontSizeTitle,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            TextButton(
                onClick = onSaveClick,
                enabled = canSave && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = iOSBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "完成",
                        color = if (canSave) iOSBlue else iOSTextSecondary,
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * 底部按钮区域
 * 
 * BUG-00037 P3修复：缩短按钮文字，防止换行
 */
@Composable
private fun BottomButtons(
    canSave: Boolean,
    isSaving: Boolean,
    onResetClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(iOSBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, iOSSeparator)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = iOSTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "重置",
                color = iOSTextSecondary,
                fontSize = dimensions.fontSizeSubtitle,
                maxLines = 1,
                softWrap = false
            )
        }

        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = iOSBlue,
                disabledContainerColor = iOSBlue.copy(alpha = 0.5f)
            ),
            enabled = canSave && !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "保存",
                    fontSize = dimensions.fontSizeSubtitle,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}


// ========== 预览 ==========

@Preview(showBackground = true, name = "默认状态")
@Composable
private fun PromptEditorScreenPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                editMode = PromptEditMode.GlobalScene(PromptScene.ANALYZE),
                currentScene = PromptScene.ANALYZE,
                placeholderText = "例如：请帮我分析她这句话的潜台词...",
                isInitialLoading = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "首次加载状态")
@Composable
private fun PromptEditorScreenLoadingPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(isInitialLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "场景切换加载状态")
@Composable
private fun PromptEditorScreenSceneSwitchingPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                currentPrompt = "分析时请特别注意对方的情绪变化",
                currentScene = PromptScene.ANALYZE,
                isInitialLoading = false,
                isSceneSwitching = true
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "保存状态")
@Composable
private fun PromptEditorScreenSavingPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                currentPrompt = "分析时请特别注意对方的情绪变化",
                currentScene = PromptScene.ANALYZE,
                isInitialLoading = false,
                isSaving = true
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "润色场景")
@Composable
private fun PromptEditorScreenPolishPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                editMode = PromptEditMode.GlobalScene(PromptScene.POLISH),
                currentScene = PromptScene.POLISH,
                currentPrompt = "润色时保持原意，让表达更加委婉得体",
                placeholderText = "例如：润色时请保持原意...",
                isInitialLoading = false
            ),
            onEvent = {}
        )
    }
}
