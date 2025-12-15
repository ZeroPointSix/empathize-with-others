package com.empathy.ai.presentation.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.EmotionType
import com.empathy.ai.domain.model.TimelineItem
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 里程碑卡片组件
 *
 * 标记重大事件，如"第一次旅行"、"相识100天"
 * 宽度贯穿屏幕，醒目的视觉效果
 *
 * @param item 里程碑数据
 * @param onClick 点击回调
 * @param modifier Modifier
 */
@Composable
fun MilestoneCard(
    item: TimelineItem.Milestone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        onClick = { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Text(
                text = item.icon,
                fontSize = 32.sp
            )
            
            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ========== 预览 ==========

@Preview(name = "里程碑卡片 - 相识100天", showBackground = true)
@Composable
private fun PreviewMilestoneCard100Days() {
    EmpathyTheme {
        MilestoneCard(
            item = TimelineItem.Milestone(
                id = "1",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.SWEET,
                title = "相识100天",
                description = "从陌生到熟悉，感谢每一天的陪伴",
                icon = "🏆"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "里程碑卡片 - 第一次旅行", showBackground = true)
@Composable
private fun PreviewMilestoneCardFirstTrip() {
    EmpathyTheme {
        MilestoneCard(
            item = TimelineItem.Milestone(
                id = "2",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.DATE,
                title = "第一次旅行",
                description = "一起去了杭州，留下美好回忆",
                icon = "✈️"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "里程碑卡片 - 生日", showBackground = true)
@Composable
private fun PreviewMilestoneCardBirthday() {
    EmpathyTheme {
        MilestoneCard(
            item = TimelineItem.Milestone(
                id = "3",
                timestamp = System.currentTimeMillis(),
                emotionType = EmotionType.GIFT,
                title = "TA的生日",
                description = "送了一份特别的礼物",
                icon = "🎂"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
