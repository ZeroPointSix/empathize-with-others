package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.empathy.ai.R
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.util.ContentValidator

/**
 * AI总结编辑对话框
 *
 * 显示日期范围、来源标识和对话数量
 * 支持编辑总结内容
 */
@Composable
fun EditSummaryDialog(
    summary: DailySummary,
    onSave: (newContent: String) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(summary.content) }
    var showOriginal by remember { mutableStateOf(false) }

    val contentValidator = remember { ContentValidator() }
    val validation = contentValidator.validateSummary(content)
    val isValid = validation.isValid()
    val hasChanges = content != summary.content

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.edit_summary_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 日期范围信息
                Text(
                    text = stringResource(R.string.summary_date_range, summary.getDisplayDateRange()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 来源标识
                Text(
                    text = stringResource(
                        R.string.summary_source,
                        if (summary.isManualGenerated()) {
                            stringResource(R.string.source_manual)
                        } else {
                            stringResource(R.string.source_auto)
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 对话数量
                Text(
                    text = stringResource(R.string.summary_conversation_count, summary.conversationCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 内容输入
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.summary_content_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    isError = !validation.isValid(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = validation.getErrorMessage() ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${content.length}/${ContentValidator.MAX_SUMMARY_LENGTH}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // 查看原始内容（已编辑时显示）
                if (summary.isUserModified && summary.originalContent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOriginal = !showOriginal },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showOriginal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.view_original_content),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(visible = showOriginal) {
                        Text(
                            text = summary.originalContent ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(content.trim()) },
                enabled = isValid && hasChanges
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
