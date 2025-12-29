package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * iOS 风格添加标签对话框
 *
 * 功能：
 * - 输入标签内容（必填）
 * - 选择标签类型（雷区/策略）
 * - 实时验证和错误显示
 * - iOS 风格视觉设计
 *
 * BUG-00036 修复：迁移到 iOS 风格对话框
 *
 * @param tagContent 标签内容
 * @param selectedType 选中的标签类型
 * @param contentError 内容验证错误信息
 * @param onContentChange 内容变化回调
 * @param onTypeChange 类型变化回调
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认添加回调
 */
@Composable
fun AddTagDialog(
    tagContent: String,
    selectedType: TagType,
    contentError: String? = null,
    onContentChange: (String) -> Unit,
    onTypeChange: (TagType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val dimensions = AdaptiveDimensions.current
    
    IOSInputDialog(
        title = "添加标签",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
            ) {
                // iOS 风格输入框
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingXSmall)
                ) {
                    Text(
                        text = "标签内容",
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .border(
                                width = if (contentError != null) 1.dp else 0.dp,
                                color = if (contentError != null) iOSRed else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (tagContent.isEmpty()) {
                            Text(
                                text = "例如：不要提他前妻",
                                fontSize = dimensions.fontSizeBody,
                                color = Color.Black.copy(alpha = 0.3f)
                            )
                        }
                        BasicTextField(
                            value = tagContent,
                            onValueChange = onContentChange,
                            textStyle = TextStyle(
                                fontSize = dimensions.fontSizeBody,
                                color = Color.Black
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (tagContent.isNotBlank()) {
                                        onConfirm()
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // 错误提示
                    if (contentError != null) {
                        Text(
                            text = contentError,
                            fontSize = dimensions.fontSizeXSmall,
                            color = iOSRed
                        )
                    }
                }
                
                // 标签类型选择
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    Text(
                        text = "标签类型",
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    
                    // 雷区选项
                    IOSTagTypeOption(
                        title = "雷区",
                        subtitle = "绝对不能踩的点",
                        isSelected = selectedType == TagType.RISK_RED,
                        color = iOSRed,
                        onClick = { onTypeChange(TagType.RISK_RED) }
                    )
                    
                    // 策略选项
                    IOSTagTypeOption(
                        title = "策略",
                        subtitle = "建议切入的点",
                        isSelected = selectedType == TagType.STRATEGY_GREEN,
                        color = iOSBlue,
                        onClick = { onTypeChange(TagType.STRATEGY_GREEN) }
                    )
                }
            }
        },
        confirmText = "添加",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = tagContent.isNotBlank()
    )
}

/**
 * iOS 风格标签类型选项
 */
@Composable
private fun IOSTagTypeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.1f) 
                else Color.Black.copy(alpha = 0.03f)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = dimensions.fontSizeBody,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) color else Color.Black
            )
            Text(
                text = subtitle,
                fontSize = dimensions.fontSizeXSmall,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== Previews ====================

@Preview(name = "添加标签对话框 - 默认状态", showBackground = true)
@Composable
private fun AddTagDialogPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "",
            selectedType = TagType.STRATEGY_GREEN,
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "添加标签对话框 - 有内容", showBackground = true)
@Composable
private fun AddTagDialogWithContentPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "不要提他前妻",
            selectedType = TagType.RISK_RED,
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "添加标签对话框 - 有错误", showBackground = true)
@Composable
private fun AddTagDialogWithErrorPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "",
            selectedType = TagType.STRATEGY_GREEN,
            contentError = "标签内容不能为空",
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}
