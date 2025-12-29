package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.LocalSemanticColors
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSRed

/**
 * iOS 风格标签确认对话框
 *
 * 用于确认或驳回AI推测的标签
 *
 * BUG-00036 修复：迁移到 iOS 风格对话框
 *
 * 功能：
 * - 显示标签内容和类型
 * - 提供确认和驳回两个操作
 * - iOS 风格视觉设计
 *
 * @param tag 待确认的标签
 * @param onConfirm 确认回调
 * @param onReject 驳回回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun TagConfirmationDialog(
    tag: BrainTag,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    val semanticColors = LocalSemanticColors.current
    val tagColor = when (tag.type) {
        TagType.RISK_RED -> semanticColors.riskRed
        TagType.STRATEGY_GREEN -> semanticColors.strategyGreen
    }
    
    val tagTypeText = when (tag.type) {
        TagType.RISK_RED -> "雷区标签"
        TagType.STRATEGY_GREEN -> "策略标签"
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .padding(dimensions.spacingMedium),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 内容区域
                Column(
                    modifier = Modifier.padding(
                        top = dimensions.spacingLarge,
                        start = dimensions.spacingMedium,
                        end = dimensions.spacingMedium,
                        bottom = dimensions.spacingMedium
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // AI 图标
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = tagColor,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    
                    // 标题
                    Text(
                        text = "AI 推测标签",
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    
                    // 标签类型
                    Text(
                        text = tagTypeText,
                        fontSize = dimensions.fontSizeCaption,
                        color = tagColor
                    )
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingXSmall))
                    
                    // 标签内容
                    Text(
                        text = "「${tag.content}」",
                        fontSize = dimensions.fontSizeBody,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(dimensions.spacingMedium))
                    
                    // 提示文字
                    Text(
                        text = "这个标签准确吗？确认后将帮助AI更好地理解这位联系人。",
                        fontSize = dimensions.fontSizeCaption,
                        color = Color.Black.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
                
                // 分隔线
                Divider(
                    color = Color.Black.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
                
                // 按钮区域
                Row(modifier = Modifier.fillMaxWidth()) {
                    // 驳回按钮
                    TextButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = iOSRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "不准确",
                            fontSize = dimensions.fontSizeBody,
                            color = iOSRed
                        )
                    }
                    
                    // 垂直分隔线
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                    
                    // 确认按钮
                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RectangleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "确认",
                            fontSize = dimensions.fontSizeBody,
                            fontWeight = FontWeight.SemiBold,
                            color = tagColor
                        )
                    }
                }
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "雷区标签确认", showBackground = true)
@Composable
private fun PreviewTagConfirmationDialogRisk() {
    EmpathyTheme {
        TagConfirmationDialog(
            tag = BrainTag(
                id = 1,
                contactId = "contact_1",
                content = "不喜欢被催促",
                type = TagType.RISK_RED,
                isConfirmed = false,
                source = "ai"
            ),
            onConfirm = {},
            onReject = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "策略标签确认", showBackground = true)
@Composable
private fun PreviewTagConfirmationDialogStrategy() {
    EmpathyTheme {
        TagConfirmationDialog(
            tag = BrainTag(
                id = 2,
                contactId = "contact_1",
                content = "喜欢收到早安问候",
                type = TagType.STRATEGY_GREEN,
                isConfirmed = false,
                source = "ai"
            ),
            onConfirm = {},
            onReject = {},
            onDismiss = {}
        )
    }
}
