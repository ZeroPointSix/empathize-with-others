package com.empathy.ai.presentation.ui.component.message

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 消息气泡组件
 *
 * 用于展示聊天消息，支持发送和接收两种样式
 *
 * @param text 消息文本内容
 * @param timestamp 时间戳文本
 * @param isFromUser 是否为用户发送的消息（true=发送，false=接收）
 * @param modifier 修饰符
 */
@Composable
fun MessageBubble(
    text: String,
    timestamp: String,
    isFromUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            horizontalAlignment = if (isFromUser) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            // 消息气泡
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        color = if (isFromUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .then(Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 时间戳
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "用户发送消息", showBackground = true)
@Composable
private fun MessageBubbleUserPreview() {
    EmpathyTheme {
        MessageBubble(
            text = "你好，最近怎么样？",
            timestamp = "14:30",
            isFromUser = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "接收消息", showBackground = true)
@Composable
private fun MessageBubbleReceivedPreview() {
    EmpathyTheme {
        MessageBubble(
            text = "挺好的，谢谢关心！",
            timestamp = "14:32",
            isFromUser = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "长文本消息", showBackground = true)
@Composable
private fun MessageBubbleLongTextPreview() {
    EmpathyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MessageBubble(
                text = "这是一条很长的消息，用来测试消息气泡在处理长文本时的表现。消息应该自动换行，并且保持合适的宽度，不会占满整个屏幕。同时，圆角和背景色应该正确显示。",
                timestamp = "14:35",
                isFromUser = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MessageBubble(
                text = "收到！我也想测试一下长文本在接收消息气泡中的显示效果。看起来应该和发送消息一样，能够正确地自动换行和保持合适的宽度。",
                timestamp = "14:36",
                isFromUser = false
            )
        }
    }
}

@Preview(name = "对话场景", showBackground = true)
@Composable
private fun MessageBubbleConversationPreview() {
    EmpathyTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessageBubble(
                text = "周末有空吗？",
                timestamp = "10:15",
                isFromUser = false
            )
            
            MessageBubble(
                text = "有啊，什么事？",
                timestamp = "10:16",
                isFromUser = true
            )
            
            MessageBubble(
                text = "一起去看电影吧",
                timestamp = "10:17",
                isFromUser = false
            )
            
            MessageBubble(
                text = "好啊！看什么电影？",
                timestamp = "10:18",
                isFromUser = true
            )
        }
    }
}

@Preview(name = "深色模式 - 用户消息", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBubbleUserDarkPreview() {
    EmpathyTheme {
        MessageBubble(
            text = "深色模式下的发送消息",
            timestamp = "20:30",
            isFromUser = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "深色模式 - 对话", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MessageBubbleConversationDarkPreview() {
    EmpathyTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessageBubble(
                text = "深色模式看起来怎么样？",
                timestamp = "20:15",
                isFromUser = false
            )
            
            MessageBubble(
                text = "很不错！颜色对比度很好，看起来很舒服。",
                timestamp = "20:16",
                isFromUser = true
            )
            
            MessageBubble(
                text = "那就好，我们继续测试其他功能吧。",
                timestamp = "20:17",
                isFromUser = false
            )
        }
    }
}
