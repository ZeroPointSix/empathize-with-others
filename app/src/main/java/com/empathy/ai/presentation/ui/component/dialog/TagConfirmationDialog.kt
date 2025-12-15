package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.LocalSemanticColors

/**
 * 标签确认对话框
 *
 * 用于确认或驳回AI推测的标签
 *
 * 功能：
 * - 显示标签内容和类型
 * - 提供确认和驳回两个操作
 * - 显示AI推测的置信度（如果有）
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
    val semanticColors = LocalSemanticColors.current
    val tagColor = when (tag.type) {
        TagType.RISK_RED -> semanticColors.riskRed
        TagType.STRATEGY_GREEN -> semanticColors.strategyGreen
    }
    
    val tagTypeText = when (tag.type) {
        TagType.RISK_RED -> "雷区标签"
        TagType.STRATEGY_GREEN -> "策略标签"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        icon = {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = tagColor,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "AI 推测标签",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标签类型
                Text(
                    text = tagTypeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = tagColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 标签内容
                Text(
                    text = "「${tag.content}」",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 提示文字
                Text(
                    text = "这个标签准确吗？确认后将帮助AI更好地理解这位联系人。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 驳回按钮
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("不准确")
                }
                
                // 确认按钮
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tagColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("确认")
                }
            }
        }
    )
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
