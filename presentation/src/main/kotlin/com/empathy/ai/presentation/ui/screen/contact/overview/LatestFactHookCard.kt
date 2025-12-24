package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.emotion.GlassmorphicCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 最新动态卡片组件
 *
 * 展示最近一次关键互动或AI的最新洞察，作为情感钩子
 *
 * 职责：
 * - 展示最新的事实记录
 * - 带有装饰性图标，强调动态性
 * - 显示相对时间
 *
 * @param latestFact 最新的事实记录
 * @param onViewMore 查看更多点击回调
 * @param modifier Modifier
 */
@Composable
fun LatestFactHookCard(
    latestFact: Fact?,
    modifier: Modifier = Modifier,
    onViewMore: (() -> Unit)? = null
) {
    if (latestFact == null) {
        // 空状态卡片
        EmptyLatestFactCard(modifier = modifier)
        return
    }
    
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.SpacingMedium),
        onClick = onViewMore
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 装饰性图标
            Icon(
                imageVector = getFactIcon(latestFact.key),
                contentDescription = null,
                tint = getFactIconColor(latestFact.key),
                modifier = Modifier.size(Dimensions.IconSizeLarge)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                // 标签
                Text(
                    text = "最新发现",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 内容
                Text(
                    text = latestFact.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 时间和来源
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRelativeTime(latestFact.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (latestFact.source == FactSource.AI_INFERRED) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI推测",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态卡片
 */
@Composable
private fun EmptyLatestFactCard(modifier: Modifier = Modifier) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.SpacingMedium)
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.IconSizeLarge)
            )
            
            Column {
                Text(
                    text = "等待发现",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "开始聊天，AI会帮你记录重要信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 根据标签类别获取图标
 */
private fun getFactIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "兴趣爱好", "interest", "hobby" -> Icons.Default.Favorite
        "性格特征", "personality", "character" -> Icons.Default.Star
        "禁忌话题", "taboo", "sensitive" -> Icons.Default.Lightbulb
        else -> Icons.Default.Lightbulb
    }
}

/**
 * 根据标签类别获取图标颜色
 */
private fun getFactIconColor(category: String): Color {
    return when (category.lowercase()) {
        "兴趣爱好", "interest", "hobby" -> Color(0xFFFF6B9D)
        "性格特征", "personality", "character" -> Color(0xFFFFC371)
        "禁忌话题", "taboo", "sensitive" -> Color(0xFF89B5E0)
        else -> Color(0xFFB8C5D6)
    }
}

/**
 * 格式化相对时间
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

// ========== 预览 ==========

@Preview(name = "有数据", showBackground = true)
@Composable
private fun PreviewLatestFactHookCardWithData() {
    EmpathyTheme {
        LatestFactHookCard(
            latestFact = Fact(
                key = "兴趣爱好",
                value = "喜欢周末去爬山，特别是秋天的时候",
                source = FactSource.AI_INFERRED,
                timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
            )
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun PreviewLatestFactHookCardEmpty() {
    EmpathyTheme {
        LatestFactHookCard(latestFact = null)
    }
}

@Preview(name = "禁忌话题", showBackground = true)
@Composable
private fun PreviewLatestFactHookCardTaboo() {
    EmpathyTheme {
        LatestFactHookCard(
            latestFact = Fact(
                key = "禁忌话题",
                value = "不要提起她的前任，这是敏感话题",
                source = FactSource.MANUAL,
                timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            )
        )
    }
}
