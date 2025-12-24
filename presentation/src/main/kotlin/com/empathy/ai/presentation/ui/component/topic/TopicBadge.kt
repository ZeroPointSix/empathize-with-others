package com.empathy.ai.presentation.ui.component.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationTopic

/**
 * 主题状态徽章
 *
 * 显示当前是否有活跃主题，点击可展开设置。
 * - 有主题时：显示主题预览和主题图标
 * - 无主题时：显示"设置主题"和添加图标
 *
 * @param topic 当前主题（可为null）
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun TopicBadge(
    topic: ConversationTopic?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveTopic = topic != null

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (hasActiveTopic) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (hasActiveTopic) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasActiveTopic) {
                    Icons.Default.Topic
                } else {
                    Icons.Outlined.AddCircle
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (hasActiveTopic) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = if (hasActiveTopic) {
                    topic!!.getPreview()
                } else {
                    "设置主题"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (hasActiveTopic) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
