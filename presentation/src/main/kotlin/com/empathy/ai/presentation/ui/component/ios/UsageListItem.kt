package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSSeparator
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * iOS风格用量列表项
 *
 * 显示单个服务商或模型的用量统计
 *
 * 设计规格（TD-00025）:
 * - iOS风格列表项样式
 * - 标题、副标题、请求数、Token数
 * - 分隔线
 *
 * @param title 标题（服务商名称或模型名称）
 * @param subtitle 副标题（可选，如服务商名称）
 * @param requestCount 请求数
 * @param tokenCount Token数（格式化后的字符串）
 * @param successRate 成功率（可选）
 * @param showDivider 是否显示分隔线
 * @param modifier Modifier
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
@Composable
fun UsageListItem(
    title: String,
    subtitle: String? = null,
    requestCount: Int,
    tokenCount: String,
    successRate: Float? = null,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val dividerColor = iOSSeparator

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle != null) (dimensions.iosListItemHeight * 1.5f) else dimensions.iosListItemHeight)
            .drawBehind {
                if (showDivider) {
                    val startX = dimensions.spacingMedium.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(startX, size.height - 0.5.dp.toPx()),
                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }
            .padding(horizontal = dimensions.spacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题和副标题
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = dimensions.fontSizeSubtitle,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = dimensions.fontSizeCaption,
                    color = iOSTextSecondary
                )
            }
        }

        // 右侧：统计数据
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 请求数
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = requestCount.toString(),
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSBlue
                )
                Text(
                    text = "请求",
                    fontSize = dimensions.fontSizeXSmall,
                    color = iOSTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMedium))

            // Token数
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = tokenCount,
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                Text(
                    text = "Token",
                    fontSize = dimensions.fontSizeXSmall,
                    color = iOSTextSecondary
                )
            }

            // 成功率（可选）
            if (successRate != null) {
                Spacer(modifier = Modifier.width(dimensions.spacingMedium))
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f%%", successRate),
                        fontSize = dimensions.fontSizeSubtitle,
                        fontWeight = FontWeight.SemiBold,
                        color = if (successRate >= 95f) {
                            com.empathy.ai.presentation.theme.iOSGreen
                        } else if (successRate >= 80f) {
                            com.empathy.ai.presentation.theme.iOSOrange
                        } else {
                            com.empathy.ai.presentation.theme.iOSRed
                        }
                    )
                    Text(
                        text = "成功率",
                        fontSize = dimensions.fontSizeXSmall,
                        color = iOSTextSecondary
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "用量列表项 - 服务商", showBackground = true)
@Composable
private fun UsageListItemProviderPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            UsageListItem(
                title = "OpenAI",
                requestCount = 456,
                tokenCount = "12.3K",
                successRate = 98.5f
            )
        }
    }
}

@Preview(name = "用量列表项 - 模型", showBackground = true)
@Composable
private fun UsageListItemModelPreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            UsageListItem(
                title = "GPT-4",
                subtitle = "OpenAI",
                requestCount = 123,
                tokenCount = "5.6K",
                successRate = 99.2f
            )
        }
    }
}

@Preview(name = "用量列表项 - 无成功率", showBackground = true)
@Composable
private fun UsageListItemNoRatePreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            UsageListItem(
                title = "DeepSeek Chat",
                requestCount = 789,
                tokenCount = "23.4K"
            )
        }
    }
}

@Preview(name = "用量列表项 - 低成功率", showBackground = true)
@Composable
private fun UsageListItemLowRatePreview() {
    EmpathyTheme {
        Surface(color = iOSCardBackground) {
            UsageListItem(
                title = "测试服务商",
                requestCount = 50,
                tokenCount = "1.2K",
                successRate = 72.0f
            )
        }
    }
}
