package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.CategoryBarColors
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.tag.AddTagButton
import com.empathy.ai.presentation.ui.component.tag.MacaronTagChip

/**
 * 分类卡片组件
 * 
 * 技术要点:
 * - 使用drawBehind绘制左侧4px色条
 * - 使用AnimatedVisibility实现折叠/展开动画
 * - 头部显示分类名+数量+折叠图标
 * - 内容区使用FlowRow显示标签列表
 * 
 * @param category 分类类型
 * @param tags 标签列表
 * @param isExpanded 是否展开
 * @param onToggle 折叠/展开回调
 * @param onAddTag 添加标签回调
 * @param onTagClick 标签点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 5.1 CategoryCard分类卡片
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryCard(
    category: TagCategory,
    tags: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = CategoryBarColors.getBarColor(category)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                // 左侧4px色条
                drawRect(
                    color = barColor,
                    topLeft = Offset.Zero,
                    size = Size(4.dp.toPx(), size.height)
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 头部：分类名 + 数量 + 折叠图标
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tags.size}个",
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = iOSTextSecondary
                )
            }
            
            // 内容：标签列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        MacaronTagChip(
                            text = tag,
                            onClick = { onTagClick(tag) }
                        )
                    }
                    
                    // 添加按钮
                    AddTagButton(onClick = onAddTag)
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "兴趣爱好-展开", showBackground = true)
@Composable
private fun CategoryCardInterestsExpandedPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                category = TagCategory.INTERESTS,
                tags = listOf("喜欢旅行", "爱看电影", "健身达人", "美食爱好者"),
                isExpanded = true,
                onToggle = {},
                onAddTag = {},
                onTagClick = {}
            )
        }
    }
}

@Preview(name = "工作信息-收起", showBackground = true)
@Composable
private fun CategoryCardWorkCollapsedPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                category = TagCategory.WORK,
                tags = listOf("产品经理", "互联网行业"),
                isExpanded = false,
                onToggle = {},
                onAddTag = {},
                onTagClick = {}
            )
        }
    }
}

@Preview(name = "雷区标签-展开", showBackground = true)
@Composable
private fun CategoryCardRiskExpandedPreview() {
    EmpathyTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            CategoryCard(
                category = TagCategory.RISK,
                tags = listOf("不喜欢被催", "讨厌迟到"),
                isExpanded = true,
                onToggle = {},
                onAddTag = {},
                onTagClick = {}
            )
        }
    }
}
