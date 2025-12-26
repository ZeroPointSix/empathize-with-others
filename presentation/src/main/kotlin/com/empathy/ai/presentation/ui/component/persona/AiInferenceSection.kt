package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * AI推测标签数据类
 * 
 * @param id 标签ID
 * @param name 标签名称
 * @param source 推测来源（如：聊天记录分析）
 * @param confidence 置信度（0f-1f）
 */
data class InferredTag(
    val id: String,
    val name: String,
    val source: String,
    val confidence: Float = 0.8f
) {
    /**
     * 是否高置信度（>= 0.8）
     */
    val isHighConfidence: Boolean
        get() = confidence >= 0.8f
}

/**
 * AI推测区域组件
 * 
 * 技术要点:
 * - 头部显示🧠图标+"AI 自动推测"+全部采纳按钮
 * - 列表显示推测标签，每项有确认/拒绝按钮
 * - 确认按钮使用iOSGreen背景
 * - 拒绝按钮使用灰色背景
 * 
 * @param inferredTags 推测标签列表
 * @param onAccept 确认单个标签回调
 * @param onReject 拒绝单个标签回调
 * @param onAcceptAll 全部采纳回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 5.4 AiInferenceSection AI推测区域
 */
@Composable
fun AiInferenceSection(
    inferredTags: List<InferredTag>,
    onAccept: (InferredTag) -> Unit,
    onReject: (InferredTag) -> Unit,
    onAcceptAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (inferredTags.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = iOSPurple.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧠",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI 自动推测",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSPurple
                    )
                }
                
                TextButton(onClick = onAcceptAll) {
                    Text(
                        text = "全部采纳",
                        fontSize = 14.sp,
                        color = iOSBlue
                    )
                }
            }
            
            // 推测标签列表
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                inferredTags.forEach { tag ->
                    InferredTagItem(
                        tag = tag,
                        onAccept = { onAccept(tag) },
                        onReject = { onReject(tag) }
                    )
                }
            }
        }
    }
}

/**
 * 单个推测标签项
 */
@Composable
private fun InferredTagItem(
    tag: InferredTag,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = iOSCardBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            Text(
                text = "来源：${tag.source}",
                fontSize = 12.sp,
                color = iOSTextSecondary
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 拒绝按钮
            IconButton(
                onClick = onReject,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFE5E5EA),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "拒绝",
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // 确认按钮
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = iOSGreen,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "确认",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "AI推测区域", showBackground = true)
@Composable
private fun AiInferenceSectionPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInferenceSection(
                inferredTags = listOf(
                    InferredTag("1", "喜欢户外运动", "聊天记录分析"),
                    InferredTag("2", "对科技产品感兴趣", "话题偏好分析"),
                    InferredTag("3", "注重健康饮食", "日常对话推测")
                ),
                onAccept = {},
                onReject = {},
                onAcceptAll = {}
            )
        }
    }
}

@Preview(name = "AI推测区域-空状态", showBackground = true)
@Composable
private fun AiInferenceSectionEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInferenceSection(
                inferredTags = emptyList(),
                onAccept = {},
                onReject = {},
                onAcceptAll = {}
            )
        }
    }
}
