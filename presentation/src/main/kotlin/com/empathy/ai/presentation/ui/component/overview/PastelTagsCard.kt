package com.empathy.ai.presentation.ui.component.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 糖果色标签卡片
 *
 * 设计原则：
 * 1. 白色卡片底座，左上角标明小标题"核心画像"
 * 2. 莫兰迪色系的浅色背景（淡紫、淡青、淡粉）
 * 3. 同色系的深色文字
 * 4. 圆润的胶囊形状
 * 5. 引入Emoji增加趣味性
 *
 * @param tags 标签列表
 * @param onTagClick 标签点击回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastelTagsCard(
    tags: List<Fact>,
    modifier: Modifier = Modifier,
    onTagClick: ((Fact) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "核心画像",
                fontSize = 13.sp,
                color = iOSTextSecondary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (tags.isEmpty()) {
                // 空状态
                Text(
                    text = "暂无标签，AI正在学习中...",
                    fontSize = 14.sp,
                    color = iOSTextSecondary
                )
            } else {
                // 标签流式布局
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(6).forEachIndexed { index, tag ->
                        PastelTagChip(
                            text = tag.value,
                            category = tag.key,
                            colorIndex = index
                        )
                    }
                }
            }
        }
    }
}

/**
 * 糖果色标签胶囊
 */
@Composable
private fun PastelTagChip(
    text: String,
    category: String,
    colorIndex: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = getPastelColorPair(colorIndex)
    val emoji = getCategoryEmoji(category)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = "$emoji $text",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * 获取莫兰迪色系配色
 */
private fun getPastelColorPair(index: Int): Pair<Color, Color> {
    val colorPairs = listOf(
        // 淡紫色系
        Color(0xFFF3E5F5) to Color(0xFF7B1FA2),
        // 淡青色系
        Color(0xFFE0F7FA) to Color(0xFF00838F),
        // 淡粉色系
        Color(0xFFFCE4EC) to Color(0xFFC2185B),
        // 淡黄色系
        Color(0xFFFFF8E1) to Color(0xFFFF8F00),
        // 淡绿色系
        Color(0xFFE8F5E9) to Color(0xFF388E3C),
        // 淡蓝色系
        Color(0xFFE3F2FD) to Color(0xFF1976D2)
    )
    return colorPairs[index % colorPairs.size]
}

/**
 * 根据类别获取Emoji
 */
private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "兴趣爱好", "interest", "hobby" -> "🎯"
        "性格特点", "性格特征", "personality", "character" -> "😊"
        "工作信息", "work", "job" -> "💼"
        "家庭情况", "family" -> "🏠"
        "重要日期", "date", "birthday" -> "📅"
        "禁忌话题", "taboo", "sensitive" -> "⚠️"
        "沟通策略", "strategy" -> "💡"
        "饮食偏好", "food" -> "🍽️"
        "运动健身", "sport", "fitness" -> "🏃"
        "旅行", "travel" -> "✈️"
        "音乐", "music" -> "🎵"
        "电影", "movie" -> "🎬"
        "阅读", "reading", "book" -> "📚"
        "游戏", "game" -> "🎮"
        else -> "📝"
    }
}

// ==================== Previews ====================

@Preview(name = "有标签", showBackground = true)
@Composable
private fun PastelTagsCardWithTagsPreview() {
    EmpathyTheme {
        PastelTagsCard(
            tags = listOf(
                Fact(
                    key = "兴趣爱好",
                    value = "喜欢吃辣",
                    source = FactSource.MANUAL,
                    timestamp = 1L
                ),
                Fact(
                    key = "性格特点",
                    value = "开朗外向",
                    source = FactSource.MANUAL,
                    timestamp = 2L
                ),
                Fact(
                    key = "工作信息",
                    value = "程序员",
                    source = FactSource.AI_INFERRED,
                    timestamp = 3L
                ),
                Fact(
                    key = "禁忌话题",
                    value = "不要提前任",
                    source = FactSource.MANUAL,
                    timestamp = 4L
                ),
                Fact(
                    key = "运动健身",
                    value = "周末爬山",
                    source = FactSource.MANUAL,
                    timestamp = 5L
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun PastelTagsCardEmptyPreview() {
    EmpathyTheme {
        PastelTagsCard(
            tags = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
