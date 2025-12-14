package com.empathy.ai.presentation.ui.component.relationship

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.RelationshipLevel
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.util.MemoryConstants
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 关系分数展示组件
 *
 * 显示联系人的关系分数、等级和趋势
 *
 * @param score 关系分数 (0-100)
 * @param level 关系等级
 * @param trend 关系趋势
 * @param lastInteractionDate 最后互动日期
 * @param modifier Modifier
 */
@Composable
fun RelationshipScoreSection(
    score: Int,
    level: RelationshipLevel,
    trend: RelationshipTrend,
    lastInteractionDate: String?,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "关系进展",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TrendIcon(trend = trend)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 分数和等级
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineLarge,
                    color = getScoreColor(score)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "/ 100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = level.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = getScoreColor(score)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 进度条
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = getScoreColor(score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // 最后互动日期
            if (!lastInteractionDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最后互动: $lastInteractionDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 关系分数颜色配置
 *
 * 使用MaterialTheme颜色系统，自动适配深色/浅色模式
 */
object RelationshipScoreColors {
    /**
     * 获取分数对应的颜色
     * - 陌生/冷淡 (0-30): error颜色
     * - 普通 (31-60): tertiary颜色
     * - 熟悉 (61-80): secondary颜色
     * - 亲密 (81-100): primary颜色
     */
    @Composable
    fun getColor(score: Int): Color {
        return when {
            score <= MemoryConstants.STRANGER_THRESHOLD -> MaterialTheme.colorScheme.error
            score <= MemoryConstants.ACQUAINTANCE_THRESHOLD -> MaterialTheme.colorScheme.tertiary
            score <= MemoryConstants.FAMILIAR_THRESHOLD -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        }
    }
}

/**
 * 根据分数获取颜色
 */
@Composable
private fun getScoreColor(score: Int): Color = RelationshipScoreColors.getColor(score)

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun RelationshipScoreSectionPreview() {
    EmpathyTheme {
        RelationshipScoreSection(
            score = 75,
            level = RelationshipLevel.FAMILIAR,
            trend = RelationshipTrend.IMPROVING,
            lastInteractionDate = "2025-12-14"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RelationshipScoreSectionLowPreview() {
    EmpathyTheme {
        RelationshipScoreSection(
            score = 25,
            level = RelationshipLevel.STRANGER,
            trend = RelationshipTrend.DECLINING,
            lastInteractionDate = "2025-12-10"
        )
    }
}
