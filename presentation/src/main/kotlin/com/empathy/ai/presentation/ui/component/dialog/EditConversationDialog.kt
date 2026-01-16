package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.presentation.R
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.util.ContentValidator
import com.empathy.ai.domain.util.DateUtils
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * 编辑对话记录对话框 - iOS风格
 *
 * BUG-00036 修复：迁移到iOS风格对话框
 * 
 * 功能：
 * - 编辑用户输入的对话内容
 * - 删除对话记录
 * - 显示编辑追踪信息（v10新增）
 * - 查看原始内容（v10新增）
 *
 * 【PRD-00008】身份前缀处理：
 * - 加载时：解析前缀，只显示纯文本，同时记住原始身份
 * - 保存时：根据记住的身份，重新拼接前缀
 * - 用户无感知前缀存在
 *
 * @param initialContent 初始内容（可能带身份前缀）
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认编辑回调（返回带前缀的完整内容）
 * @param onDelete 删除回调
 * @param timestamp 对话时间戳（可选，用于显示时间信息）
 * @param isUserModified 是否已被用户修改过（可选）
 * @param originalUserInput 原始用户输入（可选，用于查看原始内容）
 */
@Composable
fun EditConversationDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (newContent: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    timestamp: Long? = null,
    isUserModified: Boolean = false,
    originalUserInput: String? = null
) {
    val dimensions = AdaptiveDimensions.current
    
    // 【PRD-00008】解析身份前缀，记住原始身份
    val parseResult = remember(initialContent) {
        IdentityPrefixHelper.parse(initialContent)
    }
    
    // 编辑框只显示纯文本内容（不含前缀）
    var content by remember { mutableStateOf(parseResult.content) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }

    val contentValidator = remember { ContentValidator() }
    val validation = contentValidator.validateConversation(content)
    val isValid = validation.isValid()
    val hasChanges = content.trim() != parseResult.content

    if (showDeleteConfirm) {
        DeleteConversationConfirmDialog(
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = modifier
                    .widthIn(max = 320.dp)
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
                    // 标题栏 - 【PRD-00008】显示身份标签
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = dimensions.spacingLarge,
                                start = dimensions.spacingMedium,
                                end = dimensions.spacingSmall
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.edit_conversation_title) + " (${parseResult.role.displayName})",
                            fontSize = dimensions.fontSizeTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = iOSRed
                            )
                        }
                    }
                    
                    // 内容区域
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions.spacingMedium)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                    ) {
                        // 时间信息
                        timestamp?.let {
                            Text(
                                text = stringResource(R.string.conversation_time_info, DateUtils.formatRelativeTime(it)),
                                fontSize = dimensions.fontSizeCaption,
                                color = Color.Black.copy(alpha = 0.5f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                        
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text(stringResource(R.string.conversation_content_label)) },
                            placeholder = { Text("请输入对话内容") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = "${content.length}/${ContentValidator.MAX_CONVERSATION_LENGTH}",
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
                        if (isUserModified && originalUserInput != null) {
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
                                    text = originalUserInput,
                                    fontSize = dimensions.fontSizeCaption,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = dimensions.spacingSmall)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    }
                    
                    // 分隔线
                    HorizontalDivider(
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
                            onClick = { 
                                // 【PRD-00008】保存时重新拼接前缀，保留原始身份
                                val finalContent = IdentityPrefixHelper.rebuildWithPrefix(
                                    role = parseResult.role,
                                    newContent = content.trim()
                                )
                                onConfirm(finalContent) 
                            },
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
}

/**
 * 使用ConversationLog模型的编辑对话框
 *
 * 便捷方法，自动提取编辑追踪信息
 */
@Composable
fun EditConversationDialog(
    conversationLog: ConversationLog,
    onDismiss: () -> Unit,
    onConfirm: (newContent: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditConversationDialog(
        initialContent = conversationLog.userInput,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onDelete = onDelete,
        modifier = modifier,
        timestamp = conversationLog.timestamp,
        isUserModified = conversationLog.isUserModified,
        originalUserInput = conversationLog.originalUserInput
    )
}

// ==================== Previews ====================

@Preview(name = "编辑对话对话框 - 对方说", showBackground = true)
@Composable
private fun EditConversationDialogContactPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}

@Preview(name = "编辑对话对话框 - 我正在回复", showBackground = true)
@Composable
private fun EditConversationDialogUserPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "${IdentityPrefixHelper.PREFIX_USER}刚才在开会",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}

@Preview(name = "编辑对话对话框 - 旧数据", showBackground = true)
@Composable
private fun EditConversationDialogLegacyPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "今天想约她出去吃饭，但不知道怎么开口比较好",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}
