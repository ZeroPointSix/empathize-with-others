// Package message 实现了对话气泡组件
//
// 业务背景 (PRD-00008):
//   - 对方说的话和我的回复需要区分展示
//   - UI层隐藏身份前缀，以自然对话流形式展示
//
// 设计决策 (PRD-00008/6.2):
//   - CONTACT(对方): 气泡靠左，浅灰背景
//   - USER(我): 气泡靠右，主题色背景
//   - LEGACY(旧数据): 居中显示，中性背景
//
// 任务追踪:
//   - FEATURE-20260114: 身份前缀历史功能实现
package com.empathy.ai.presentation.ui.component.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationLog
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.EmpathyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话气泡组件
 *
 * 根据身份前缀自动渲染左右布局的对话气泡。
 *
 * 布局规则（PRD-00008）：
 * - CONTACT（对方说）：靠左对齐，浅灰背景
 * - USER（我正在回复）：靠右对齐，主题色背景
 * - LEGACY（旧数据）：居中对齐，中性背景
 *
 * UI 隐藏前缀：用户不会看到【对方说】：等前缀文本，只显示纯内容。
 *
 * @param log 对话记录
 * @param modifier Modifier
 * @param onClick 点击回调（用于编辑）
 */
@Composable
fun ConversationBubble(
    log: ConversationLog,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showHeader: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // 解析身份前缀，使用 remember 缓存结果避免重复计算
    // [Performance] 重组时直接使用缓存结果，避免重复字符串解析
    val parseResult = remember(log.userInput) {
        IdentityPrefixHelper.parse(log.userInput)
    }

    // 根据身份确定对齐方式 (PRD-00008/6.2)
    // CONTACT → 左对齐, USER → 右对齐, LEGACY → 居中
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色 (PRD-00008/6.2)
    // CONTACT → 浅灰, USER → 主题色, LEGACY → 中性
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状 (PRD-00008/6.2)
    // 气泡靠近屏幕边缘的一角圆角较小，模拟真实聊天软件效果
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)  // 左上角小
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)  // 右上角小
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)  // 全圆角
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (showHeader) {
            // 标签和时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = parseResult.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ${formatTime(log.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,  // 显示纯文本，不含前缀 (PRD-00008/6.2)
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}

/**
 * 简化版对话气泡（直接传入内容和角色）
 *
 * @param content 对话内容（可能带前缀）
 * @param timestamp 时间戳
 * @param modifier Modifier
 * @param onClick 点击回调
 */
@Composable
fun ConversationBubble(
    content: String,
    timestamp: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showHeader: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // 解析身份前缀
    val parseResult = remember(content) {
        IdentityPrefixHelper.parse(content)
    }

    // 根据身份确定对齐方式
    val alignment = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT -> Alignment.Start
        IdentityPrefixHelper.IdentityRole.USER -> Alignment.End
        IdentityPrefixHelper.IdentityRole.LEGACY -> Alignment.CenterHorizontally
    }

    // 根据身份确定背景色
    val backgroundColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.surfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.primaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.surface
    }

    // 根据身份确定气泡形状
    val bubbleShape = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.USER ->
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            RoundedCornerShape(16.dp)
    }

    // 根据身份确定文字颜色
    val textColor = when (parseResult.role) {
        IdentityPrefixHelper.IdentityRole.CONTACT ->
            MaterialTheme.colorScheme.onSurfaceVariant
        IdentityPrefixHelper.IdentityRole.USER ->
            MaterialTheme.colorScheme.onPrimaryContainer
        IdentityPrefixHelper.IdentityRole.LEGACY ->
            MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (showHeader) {
            // 标签和时间
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = parseResult.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " · ${formatTime(timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 气泡
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                text = parseResult.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                maxLines = maxLines,
                overflow = overflow
            )
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ==================== Previews ====================

@Preview(name = "对方说的气泡", showBackground = true)
@Composable
private fun PreviewContactBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 1,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "我正在回复的气泡", showBackground = true)
@Composable
private fun PreviewUserBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 2,
                contactId = "contact_1",
                userInput = "${IdentityPrefixHelper.PREFIX_USER}刚才在开会，抱歉让你久等了",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "旧数据气泡（无前缀）", showBackground = true)
@Composable
private fun PreviewLegacyBubble() {
    EmpathyTheme {
        ConversationBubble(
            log = ConversationLog(
                id = 3,
                contactId = "contact_1",
                userInput = "这是一条旧数据，没有身份前缀",
                aiResponse = null,
                timestamp = System.currentTimeMillis(),
                isSummarized = false
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "对话气泡组合", showBackground = true)
@Composable
private fun PreviewConversationFlow() {
    EmpathyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}早安",
                timestamp = System.currentTimeMillis() - 3600000
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_USER}早呀，今天天气真好",
                timestamp = System.currentTimeMillis() - 3500000,
                modifier = Modifier.padding(top = 8.dp)
            )
            ConversationBubble(
                content = "${IdentityPrefixHelper.PREFIX_CONTACT}是啊，要不要出去走走？",
                timestamp = System.currentTimeMillis(),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
