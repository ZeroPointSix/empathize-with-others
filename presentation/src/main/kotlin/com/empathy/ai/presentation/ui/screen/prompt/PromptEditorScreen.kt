package com.empathy.ai.presentation.ui.screen.prompt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.EmotionalBackground
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import com.empathy.ai.presentation.ui.screen.prompt.component.CharacterCounter
import com.empathy.ai.presentation.ui.screen.prompt.component.DiscardConfirmDialog
import com.empathy.ai.presentation.ui.screen.prompt.component.InlineErrorBanner
import com.empathy.ai.presentation.ui.screen.prompt.component.PromptEditorTopBar
import com.empathy.ai.presentation.ui.screen.prompt.component.PromptInputField
import com.empathy.ai.presentation.viewmodel.PromptEditorViewModel

/**
 * 提示词编辑器主界面
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

    // 监听编辑结果
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
 */
@Composable
private fun PromptEditorContent(
    uiState: PromptEditorUiState,
    onEvent: (PromptEditorUiEvent) -> Unit
) {
    // 获取标题文本
    val title = when (val mode = uiState.editMode) {
        is PromptEditMode.GlobalScene -> stringResource(R.string.prompt_editor_title_global)
        is PromptEditMode.ContactCustom -> stringResource(
            R.string.prompt_editor_title_contact,
            mode.contactName
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // 情感化背景（使用中性分数50）
        EmotionalBackground(relationshipScore = 50)

        // 主内容
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                // 加载状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 顶部导航栏
                    PromptEditorTopBar(
                        title = title,
                        isSaving = uiState.isSaving,
                        canSave = uiState.canSave,
                        onCancelClick = { onEvent(PromptEditorUiEvent.CancelEdit) },
                        onSaveClick = { onEvent(PromptEditorUiEvent.SavePrompt) }
                    )

                    // 错误提示条
                    InlineErrorBanner(
                        errorMessage = uiState.errorMessage,
                        onDismiss = { onEvent(PromptEditorUiEvent.ClearError) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 输入区域（可滚动）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PromptInputField(
                            value = uiState.currentPrompt,
                            onValueChange = { onEvent(PromptEditorUiEvent.UpdatePrompt(it)) },
                            placeholder = uiState.placeholderText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 字符计数器
                    CharacterCounter(
                        charCount = uiState.charCount,
                        maxLength = PromptEditorUiState.MAX_PROMPT_LENGTH,
                        isOverLimit = uiState.isOverLimit,
                        isNearLimit = uiState.isNearLimit,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // 放弃修改对话框
        if (uiState.showDiscardDialog) {
            DiscardConfirmDialog(
                onConfirm = { onEvent(PromptEditorUiEvent.ConfirmDiscard) },
                onDismiss = { onEvent(PromptEditorUiEvent.DismissDiscardDialog) }
            )
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
                placeholderText = "例如：请帮我分析她这句话的潜台词..."
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "加载状态")
@Composable
private fun PromptEditorScreenLoadingPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                isLoading = true
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
                isSaving = true
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "错误状态")
@Composable
private fun PromptEditorScreenErrorPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                currentPrompt = "测试内容",
                errorMessage = "保存失败：网络连接超时"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "接近字数限制")
@Composable
private fun PromptEditorScreenNearLimitPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                currentPrompt = "a".repeat(850),
                originalPrompt = ""
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "超出字数限制")
@Composable
private fun PromptEditorScreenOverLimitPreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                currentPrompt = "a".repeat(1050),
                originalPrompt = ""
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "联系人模式")
@Composable
private fun PromptEditorScreenContactModePreview() {
    EmpathyTheme {
        PromptEditorContent(
            uiState = PromptEditorUiState(
                editMode = PromptEditMode.ContactCustom(
                    contactId = "1",
                    contactName = "林婉儿"
                ),
                placeholderText = "例如：和她聊天时要注意避开前任话题..."
            ),
            onEvent = {}
        )
    }
}
