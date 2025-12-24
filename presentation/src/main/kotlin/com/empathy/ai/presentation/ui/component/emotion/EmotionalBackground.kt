package com.empathy.ai.presentation.ui.component.emotion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.theme.AnimationSpec
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.RelationshipColors

/**
 * 情感化背景组件
 *
 * 根据关系分数动态生成背景渐变色，传递关系的情感温度
 *
 * 职责：
 * - 根据关系分数选择对应的颜色方案
 * - 提供平滑的颜色过渡动画
 * - 使用径向渐变营造聚焦效果
 *
 * 技术要点：
 * - 使用remember缓存颜色计算，避免每次重组时重新计算
 * - 使用animateColorAsState实现平滑过渡
 * - 径向渐变中心点设置在(0.5f, 0.3f)，营造从上方聚焦的效果
 *
 * @param relationshipScore 关系分数 (0-100)
 * @param modifier Modifier
 *
 * 使用示例：
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     EmotionalBackground(relationshipScore = 85)
 *     // 其他内容
 * }
 * ```
 */
@Composable
fun EmotionalBackground(
    relationshipScore: Int,
    modifier: Modifier = Modifier
) {
    // 根据分数选择颜色方案
    // 使用remember缓存，只在分数变化时重新计算
    val backgroundColor = remember(relationshipScore) {
        RelationshipColors.getColorsByScore(relationshipScore)
    }
    
    // 为每个颜色创建动画状态
    // 当分数变化时，颜色会平滑过渡
    val animatedColors = backgroundColor.map { color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = tween(durationMillis = AnimationSpec.DurationColorTransition),
            label = "BackgroundColorAnimation"
        ).value
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = animatedColors,
                    center = Offset(0.5f, 0.3f), // 从上方聚焦
                    radius = 1000f
                )
            )
    )
}

// ========== 预览 ==========

@Preview(name = "优秀关系 (85分)", showBackground = true)
@Composable
private fun PreviewEmotionalBackgroundExcellent() {
    EmpathyTheme {
        EmotionalBackground(relationshipScore = 85)
    }
}

@Preview(name = "良好关系 (70分)", showBackground = true)
@Composable
private fun PreviewEmotionalBackgroundGood() {
    EmpathyTheme {
        EmotionalBackground(relationshipScore = 70)
    }
}

@Preview(name = "一般关系 (45分)", showBackground = true)
@Composable
private fun PreviewEmotionalBackgroundNormal() {
    EmpathyTheme {
        EmotionalBackground(relationshipScore = 45)
    }
}

@Preview(name = "冷淡关系 (20分)", showBackground = true)
@Composable
private fun PreviewEmotionalBackgroundPoor() {
    EmpathyTheme {
        EmotionalBackground(relationshipScore = 20)
    }
}
