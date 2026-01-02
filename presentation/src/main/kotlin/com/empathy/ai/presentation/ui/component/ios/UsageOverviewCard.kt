package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格用量概览卡片
 *
 * 显示API用量的概览统计信息
 *
 * 设计规格（TD-00025）:
 * - 圆角卡片（12dp）
 * - 三列布局（请求数、Token数、成功率）
 * - 数字样式：28sp, Bold, iOS蓝色
 * - 标签样式：13sp, 灰色
 *
 * @param totalRequests 总请求数
 * @param totalTokens 总Token数（格式化后的字符串）
 * @param successRate 成功率（格式化后的字符串）
 * @param modifier Modifier
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
@Composable
fun UsageOverviewCard(
    totalRequests: Int,
    totalTokens: String,
    successRate: String,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = iOSCardBackground,
        shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.iosCardPadding),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UsageStatColumn(
                value = totalRequests.toString(),
                label = "请求数"
            )
            UsageStatColumn(
                value = totalTokens,
                label = "Token数"
            )
            UsageStatColumn(
                value = successRate,
                label = "成功率"
            )
        }
    }
}

/**
 * 用量统计列
 */
@Composable
private fun UsageStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = iOSBlue
        )
        Text(
            text = label,
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "用量概览卡片 - 有数据", showBackground = true)
@Composable
private fun UsageOverviewCardWithDataPreview() {
    EmpathyTheme {
        UsageOverviewCard(
            totalRequests = 1234,
            totalTokens = "45.6K",
            successRate = "98.5%",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "用量概览卡片 - 无数据", showBackground = true)
@Composable
private fun UsageOverviewCardEmptyPreview() {
    EmpathyTheme {
        UsageOverviewCard(
            totalRequests = 0,
            totalTokens = "0",
            successRate = "0%",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "用量概览卡片 - 大数据", showBackground = true)
@Composable
private fun UsageOverviewCardLargeDataPreview() {
    EmpathyTheme {
        UsageOverviewCard(
            totalRequests = 99999,
            totalTokens = "1.2M",
            successRate = "99.9%",
            modifier = Modifier.padding(16.dp)
        )
    }
}
