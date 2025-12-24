package com.empathy.ai.presentation.ui.component.relationship

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
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
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 关系趋势图标组件
 *
 * 显示关系变化趋势的图标和文字
 *
 * @param trend 关系趋势
 * @param showLabel 是否显示文字标签
 * @param modifier Modifier
 */
@Composable
fun TrendIcon(
    trend: RelationshipTrend,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = getTrendInfo(trend)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

/**
 * 获取趋势信息
 */
@Composable
private fun getTrendInfo(trend: RelationshipTrend): Triple<ImageVector, Color, String> {
    return when (trend) {
        RelationshipTrend.IMPROVING -> Triple(
            Icons.Default.TrendingUp,
            MaterialTheme.colorScheme.primary,
            "改善中"
        )
        RelationshipTrend.STABLE -> Triple(
            Icons.Default.TrendingFlat,
            MaterialTheme.colorScheme.tertiary,
            "稳定"
        )
        RelationshipTrend.DECLINING -> Triple(
            Icons.Default.TrendingDown,
            MaterialTheme.colorScheme.error,
            "下降中"
        )
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun TrendIconImprovingPreview() {
    EmpathyTheme {
        TrendIcon(trend = RelationshipTrend.IMPROVING)
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendIconStablePreview() {
    EmpathyTheme {
        TrendIcon(trend = RelationshipTrend.STABLE)
    }
}

@Preview(showBackground = true)
@Composable
private fun TrendIconDecliningPreview() {
    EmpathyTheme {
        TrendIcon(trend = RelationshipTrend.DECLINING)
    }
}
