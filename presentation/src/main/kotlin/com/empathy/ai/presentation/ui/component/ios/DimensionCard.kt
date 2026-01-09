package com.empathy.ai.presentation.ui.component.ios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSGreen
import com.empathy.ai.presentation.theme.iOSOrange
import com.empathy.ai.presentation.theme.iOSPurple
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary

/**
 * 维度卡片（可展开）
 *
 * 设计规格:
 * - 圆角: 响应式
 * - 图标容器: 响应式尺寸
 * - 标题: 17sp, SemiBold
 * - 描述: 13sp, 灰色
 * - 展开图标: expand_less, 旋转动画
 * - 展开/收起动画: 200ms
 *
 * @param icon 图标
 * @param iconBackgroundColor 图标背景色
 * @param title 标题
 * @param description 描述
 * @param tags 已添加的标签列表
 * @param presetTags 预设标签列表
 * @param isExpanded 是否展开
 * @param onToggleExpand 展开/收起回调
 * @param onAddTag 添加标签回调
 * @param onEditTag 编辑标签回调
 * @param onSelectPresetTag 选择预设标签回调
 * @param modifier Modifier
 *
 * @see TDD-00021 3.8节 DimensionCard组件规格
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DimensionCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    description: String,
    tags: List<String>,
    presetTags: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddTag: () -> Unit,
    onEditTag: (String) -> Unit,
    onSelectPresetTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    // 图标容器尺寸
    val iconContainerSize = (36 * dimensions.fontScale).dp
    
    // 展开图标旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = tween(durationMillis = 200),
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.cornerRadiusMedium))
            .background(iOSCardBackground)
    ) {
        // 头部（可点击展开/收起）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 (响应式尺寸)
            Box(
                modifier = Modifier
                    .size(iconContainerSize)
                    .background(
                        color = iconBackgroundColor,
                        shape = RoundedCornerShape(dimensions.cornerRadiusSmall + 2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(dimensions.iconSizeSmall + 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))

            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                Text(
                    text = description,
                    fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                    color = iOSTextSecondary
                )
            }

            // 展开/收起图标
            Icon(
                imageVector = Icons.Default.ExpandLess,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = iOSTextSecondary,
                modifier = Modifier
                    .size(dimensions.iconSizeLarge - 8.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            )
        }

        // 展开内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingMedium)
                    .padding(bottom = dimensions.spacingMedium)
            ) {
                // 已添加标签
                if (tags.isNotEmpty()) {
                    Text(
                        text = "已添加",
                        fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                        color = iOSTextSecondary,
                        modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            EditableTag(
                                text = tag,
                                onClick = { onEditTag(tag) },
                                modifier = Modifier.padding(end = dimensions.spacingSmall, bottom = dimensions.spacingSmall)
                            )
                        }
                        AddTagButton(
                            onClick = onAddTag,
                            modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                        )
                    }
                } else {
                    // 无标签时只显示添加按钮
                    AddTagButton(
                        onClick = onAddTag,
                        modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                    )
                }

                // 快速选择
                if (presetTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                    Text(
                        text = "快速选择",
                        fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                        color = iOSTextSecondary,
                        modifier = Modifier.padding(bottom = dimensions.spacingSmall)
                    )
                    QuickSelectTags(
                        tags = presetTags,
                        onTagClick = onSelectPresetTag
                    )
                }
            }
        }
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "维度卡片 - 收起", showBackground = true)
@Composable
private fun DimensionCardCollapsedPreview() {
    EmpathyTheme {
        DimensionCard(
            icon = Icons.Default.Person,
            iconBackgroundColor = iOSBlue,
            title = "性格特点",
            description = "描述你的性格特征",
            tags = listOf("外向", "乐观"),
            presetTags = listOf("内向", "外向", "乐观", "谨慎"),
            isExpanded = false,
            onToggleExpand = {},
            onAddTag = {},
            onEditTag = {},
            onSelectPresetTag = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "维度卡片 - 展开", showBackground = true)
@Composable
private fun DimensionCardExpandedPreview() {
    EmpathyTheme {
        DimensionCard(
            icon = Icons.Default.Person,
            iconBackgroundColor = iOSPurple,
            title = "价值观",
            description = "你看重什么",
            tags = listOf("家庭", "事业", "健康"),
            presetTags = listOf("自由", "稳定", "成长", "创新"),
            isExpanded = true,
            onToggleExpand = {},
            onAddTag = {},
            onEditTag = {},
            onSelectPresetTag = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "维度卡片 - 无标签", showBackground = true)
@Composable
private fun DimensionCardEmptyPreview() {
    EmpathyTheme {
        DimensionCard(
            icon = Icons.Default.Person,
            iconBackgroundColor = iOSGreen,
            title = "兴趣爱好",
            description = "你喜欢做什么",
            tags = emptyList(),
            presetTags = listOf("阅读", "运动", "音乐", "旅行"),
            isExpanded = true,
            onToggleExpand = {},
            onAddTag = {},
            onEditTag = {},
            onSelectPresetTag = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "维度卡片列表", showBackground = true)
@Composable
private fun DimensionCardListPreview() {
    EmpathyTheme {
        var expanded1 by remember { mutableStateOf(true) }
        var expanded2 by remember { mutableStateOf(false) }
        
        Column(modifier = Modifier.padding(16.dp)) {
            DimensionCard(
                icon = Icons.Default.Person,
                iconBackgroundColor = iOSBlue,
                title = "性格特点",
                description = "描述你的性格特征",
                tags = listOf("外向", "乐观"),
                presetTags = listOf("内向", "外向", "乐观", "谨慎"),
                isExpanded = expanded1,
                onToggleExpand = { expanded1 = !expanded1 },
                onAddTag = {},
                onEditTag = {},
                onSelectPresetTag = {}
            )
            Spacer(modifier = Modifier.height(12.dp))
            DimensionCard(
                icon = Icons.Default.Person,
                iconBackgroundColor = iOSOrange,
                title = "沟通风格",
                description = "你的沟通方式",
                tags = listOf("直接"),
                presetTags = listOf("直接", "委婉", "幽默", "严肃"),
                isExpanded = expanded2,
                onToggleExpand = { expanded2 = !expanded2 },
                onAddTag = {},
                onEditTag = {},
                onSelectPresetTag = {}
            )
        }
    }
}
