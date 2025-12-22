package com.empathy.ai.presentation.ui.component.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationTopic
import com.empathy.ai.presentation.viewmodel.TopicUiEvent
import com.empathy.ai.presentation.viewmodel.TopicUiState

/**
 * 主题设置对话框
 *
 * 提供主题的创建、编辑和清除功能
 *
 * @param uiState 当前UI状态
 * @param onEvent 事件处理回调
 * @param modifier Modifier
 */
@Composable
fun TopicSettingDialog(
    uiState: TopicUiState,
    onEvent: (TopicUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.showSettingDialog) return

    AlertDialog(
        onDismissRequest = { onEvent(TopicUiEvent.HideSettingDialog) },
        modifier = modifier,
        title = {
            Text(
                text = "设置对话主题",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 说明文字
                Text(
                    text = "设置对话主题可以帮助AI更好地理解对话背景，提供更精准的回复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 输入框
                TopicInputField(
                    value = uiState.inputContent,
                    onValueChange = { onEvent(TopicUiEvent.UpdateInput(it)) },
                    charCount = uiState.inputCharCount,
                    maxLength = ConversationTopic.MAX_CONTENT_LENGTH,
                    isError = uiState.isOverLimit,
                    modifier = Modifier.fillMaxWidth()
                )

                // 错误提示
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 历史记录
                if (uiState.topicHistory.isNotEmpty()) {
                    TopicHistorySection(
                        history = uiState.topicHistory,
                        onSelect = { onEvent(TopicUiEvent.SelectFromHistory(it)) }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 清除按钮（仅当有活跃主题时显示）
                if (uiState.hasActiveTopic) {
                    TextButton(
                        onClick = { onEvent(TopicUiEvent.ClearTopic) },
                        enabled = !uiState.isLoading
                    ) {
                        Text("清除主题")
                    }
                }

                // 保存按钮
                Button(
                    onClick = { onEvent(TopicUiEvent.SaveTopic) },
                    enabled = uiState.canSave
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TopicUiEvent.HideSettingDialog) },
                enabled = !uiState.isLoading
            ) {
                Text("取消")
            }
        }
    )
}
