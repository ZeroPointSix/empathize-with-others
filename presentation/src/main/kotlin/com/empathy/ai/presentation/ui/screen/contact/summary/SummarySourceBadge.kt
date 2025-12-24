package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.GenerationSource

/**
 * 总结来源标识徽章
 *
 * 在时光轴中显示总结的生成来源：
 * - 自动生成：🤖 灰色背景
 * - 手动生成：👤 蓝色背景
 *
 * @param source 生成来源
 * @param modifier 修饰符
 */
@Composable
fun SummarySourceBadge(
    source: GenerationSource,
    modifier: Modifier = Modifier
) {
    val (icon, text, backgroundColor) = when (source) {
        GenerationSource.AUTO -> Triple(
            "🤖",
            "自动",
            MaterialTheme.colorScheme.surfaceVariant
        )
        GenerationSource.MANUAL -> Triple(
            "👤",
            "手动",
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    val textColor = when (source) {
        GenerationSource.AUTO -> MaterialTheme.colorScheme.onSurfaceVariant
        GenerationSource.MANUAL -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
