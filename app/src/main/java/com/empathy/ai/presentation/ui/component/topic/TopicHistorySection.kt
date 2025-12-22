package com.empathy.ai.presentation.ui.component.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConversationTopic

/**
 * 主题历史记录区域
 *
 * 显示联系人的历史主题列表，用户可以点击快速选择
 *
 * @param history 历史主题列表
 * @param onSelect 选择主题回调
 * @param modifier Modifier
 */
@Composable
fun TopicHistorySection(
    history: List<ConversationTopic>,
    onSelect: (ConversationTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = "历史主题",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history.take(5)) { topic ->
                TopicHistoryChip(
                    topic = topic,
                    onClick = { onSelect(topic) }
                )
            }
        }
    }
}

/**
 * 历史主题芯片
 */
@Composable
private fun TopicHistoryChip(
    topic: ConversationTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = topic.getPreview(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
    )
}
