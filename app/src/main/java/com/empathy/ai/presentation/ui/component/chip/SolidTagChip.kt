package com.empathy.ai.presentation.ui.component.chip

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.Dimensions
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 实心标签组件
 *
 * 用于展示用户确认的标签，采用高饱和度实心背景
 *
 * 职责：
 * - 展示标签文本
 * - 根据标签类型显示不同颜色
 * - 传递确定性和稳固感
 *
 * @param text 标签文本
 * @param backgroundColor 背景颜色
 * @param textColor 文本颜色
 * @param modifier Modifier
 */
@Composable
fun SolidTagChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = Dimensions.SpacingMedium,
                vertical = Dimensions.SpacingSmall
            ),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

/**
 * 实心标签颜色配置
 *
 * 根据标签类别返回对应的颜色
 * 注意：与TagChip.kt中的私有TagColors不同，这是公开的颜色配置
 */
object SolidTagColors {
    val Interest = Color(0xFF7FD8BE)      // 兴趣爱好 - 柔和绿色
    val Personality = Color(0xFFFFC371)   // 性格特征 - 柔和黄色
    val Value = Color(0xFF89B5E0)         // 价值观 - 中性蓝色
    val Taboo = Color(0xFFFF6B9D)         // 禁忌话题 - 粉色（警示）
    val Default = Color(0xFFB8C5D6)       // 默认 - 中性灰色
    
    /**
     * 根据标签类别获取颜色
     */
    fun getColorByCategory(category: String): Color {
        return when (category.lowercase()) {
            "兴趣爱好", "interest", "hobby" -> Interest
            "性格特征", "personality", "character" -> Personality
            "价值观", "value", "belief" -> Value
            "禁忌话题", "taboo", "sensitive" -> Taboo
            else -> Default
        }
    }
}

// ========== 预览 ==========

@Preview(name = "默认标签", showBackground = true)
@Composable
private fun PreviewSolidTagChipDefault() {
    EmpathyTheme {
        SolidTagChip(
            text = "喜欢吃辣",
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(name = "兴趣爱好标签", showBackground = true)
@Composable
private fun PreviewSolidTagChipInterest() {
    EmpathyTheme {
        SolidTagChip(
            text = "猫奴",
            backgroundColor = SolidTagColors.Interest,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(name = "性格特征标签", showBackground = true)
@Composable
private fun PreviewSolidTagChipPersonality() {
    EmpathyTheme {
        SolidTagChip(
            text = "外向开朗",
            backgroundColor = SolidTagColors.Personality,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(name = "禁忌话题标签", showBackground = true)
@Composable
private fun PreviewSolidTagChipTaboo() {
    EmpathyTheme {
        SolidTagChip(
            text = "不要提前任",
            backgroundColor = SolidTagColors.Taboo,
            modifier = Modifier.padding(8.dp)
        )
    }
}
