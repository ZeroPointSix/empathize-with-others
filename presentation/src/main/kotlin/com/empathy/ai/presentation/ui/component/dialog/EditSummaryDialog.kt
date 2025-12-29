package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * AI总结编辑对话框 - iOS风格
 *
 * BUG-00036 修复：迁移到iOS风格对话框
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
    val dimensions = AdaptiveDimensions.current
    
    var content by remember { mutableStateOf(summary.content) }
    var showOriginal by remember { mutableStateOf(false) }

    val contentValidator = remember { ContentValidator() }
    val validation = contentValidator.validateSummary(content)
    val isValid = validation.isValid()
    val hasChanges = content != summary.content

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.edit_summary_title),
                    fontSize = dimensions.fontSizeTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium
                    )
                )
                
                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.spacingMedium)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                    
                    // 日期范围信息
                    Text(
                        text = stringResource(R.string.summary_date_range, summary.getDisplayDateRange()),
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.5f)
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
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 对话数量
                    Text(
                        text = stringResource(R.string.summary_conversation_count, summary.conversationCount),
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))

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
                                    color = iOSRed,
                                    fontSize = dimensions.fontSizeCaption
                                )
                                Text(
                                    text = "${content.length}/${ContentValidator.MAX_SUMMARY_LENGTH}",
                                    fontSize = dimensions.fontSizeCaption
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = iOSBlue,
                            cursorColor = iOSBlue
                        )
                    )

                    // 查看原始内容（已编辑时显示）
                    if (summary.isUserModified && summary.originalContent != null) {
                        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOriginal = !showOriginal },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showOriginal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = iOSBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.view_original_content),
                                fontSize = dimensions.fontSizeCaption,
                                color = iOSBlue
                            )
                        }
                        AnimatedVisibility(visible = showOriginal) {
                            Text(
                                text = summary.originalContent ?: "",
                                fontSize = dimensions.fontSizeCaption,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = dimensions.spacingSmall)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                }
                
                // 分隔线
                Divider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 按钮区域 - iOS风格水平排列
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = dimensions.fontSizeTitle,
                            color = iOSBlue
                        )
                    }
                    
                    // 垂直分隔线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    
                    // 保存按钮
                    TextButton(
                        onClick = { onSave(content.trim()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape,
                        enabled = isValid && hasChanges
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = dimensions.fontSizeTitle,
                            color = if (isValid && hasChanges) iOSBlue else iOSBlue.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
