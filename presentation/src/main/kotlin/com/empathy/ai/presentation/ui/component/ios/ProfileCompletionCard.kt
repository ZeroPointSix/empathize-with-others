package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 画像完整度卡片
 *
 * 设计规格:
 * - 圆角: 响应式
 * - 标题: 13sp, 灰色
 * - 百分比: 28sp, Bold, iOS蓝色
 * - 进度条: 响应式高度, iOS蓝色
 * - 标签数: 11sp, 灰色
 * - 进度动画: 使用animateFloatAsState
 *
 * @param completeness 完整度百分比（0-100）
 * @param tagCount 标签数量
 * @param modifier Modifier
 *
 * @see TDD-00021 3.7节 ProfileCompletionCard组件规格
 */
@Composable
fun ProfileCompletionCard(
    completeness: Int,
    tagCount: Int,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = completeness / 100f,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusMedium))
            .background(iOSCardBackground)
            .padding(dimensions.spacingMedium)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "画像完整度",
                fontSize = 13.sp,
                color = iOSTextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$tagCount 个标签",
                fontSize = 11.sp,
                color = iOSTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacingSmall))

        // 百分比
        Text(
            text = "$completeness%",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = iOSBlue
        )

        Spacer(modifier = Modifier.height(dimensions.spacingMediumSmall))

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.progressBarHeight)
                .clip(RoundedCornerShape(dimensions.progressBarHeight / 2))
                .background(iOSTextSecondary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(dimensions.progressBarHeight)
                    .clip(RoundedCornerShape(dimensions.progressBarHeight / 2))
                    .background(iOSBlue)
            )
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "画像完整度 - 低", showBackground = true)
@Composable
private fun ProfileCompletionCardLowPreview() {
    EmpathyTheme {
        ProfileCompletionCard(
            completeness = 25,
            tagCount = 5,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "画像完整度 - 中", showBackground = true)
@Composable
private fun ProfileCompletionCardMediumPreview() {
    EmpathyTheme {
        ProfileCompletionCard(
            completeness = 60,
            tagCount = 12,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "画像完整度 - 高", showBackground = true)
@Composable
private fun ProfileCompletionCardHighPreview() {
    EmpathyTheme {
        ProfileCompletionCard(
            completeness = 85,
            tagCount = 20,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "画像完整度 - 完整", showBackground = true)
@Composable
private fun ProfileCompletionCardFullPreview() {
    EmpathyTheme {
        ProfileCompletionCard(
            completeness = 100,
            tagCount = 25,
            modifier = Modifier.padding(16.dp)
        )
    }
}
