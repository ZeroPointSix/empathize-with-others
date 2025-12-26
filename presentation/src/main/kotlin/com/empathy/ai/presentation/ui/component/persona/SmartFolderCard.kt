package com.empathy.ai.presentation.ui.component.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary

/**
 * 智能文件夹分类配置
 * 定义每个分类的图标、颜色等视觉属性
 */
object SmartFolderConfig {
    /** 兴趣爱好 - 淡橙色背景 + 游戏手柄图标 */
    val INTERESTS = FolderStyle(
        icon = Icons.Default.SportsEsports,
        iconBackgroundColor = Color(0xFFFFF3E0), // 淡橙色背景
        iconColor = Color(0xFFF97316),           // 橙色图标
        tagBackgroundColor = Color(0xFFFFF8F0), // 奶油橙背景
        tagTextColor = Color(0xFFEA580C)         // 深橙色文字
    )
    
    /** 性格特点/工作信息 - 淡青色背景 + 笑脸图标 */
    val PERSONALITY = FolderStyle(
        icon = Icons.Default.EmojiEmotions,
        iconBackgroundColor = Color(0xFFE0F7FA), // 淡青色背景
        iconColor = Color(0xFF0891B2),           // 青色图标
        tagBackgroundColor = Color(0xFFF0FDFA), // 薄荷绿背景
        tagTextColor = Color(0xFF0D9488)         // 深青色文字
    )
    
    /** 工作信息 - 淡蓝色背景 + 工作图标 */
    val WORK = FolderStyle(
        icon = Icons.Default.Work,
        iconBackgroundColor = Color(0xFFE3F2FD), // 淡蓝色背景
        iconColor = Color(0xFF3B82F6),           // 蓝色图标
        tagBackgroundColor = Color(0xFFF0F9FF), // 淡蓝背景
        tagTextColor = Color(0xFF1D4ED8)         // 深蓝色文字
    )
    
    /** 沟通策略 - 淡绿色背景 + 灯泡图标 */
    val STRATEGY = FolderStyle(
        icon = Icons.Outlined.Lightbulb,
        iconBackgroundColor = Color(0xFFE8F5E9), // 淡绿色背景
        iconColor = Color(0xFF10B981),           // 绿色图标
        tagBackgroundColor = Color(0xFFF0FDF4), // 薄荷绿背景
        tagTextColor = Color(0xFF059669)         // 深绿色文字
    )
    
    /** 雷区标签 - 淡红色背景 + 警告图标 */
    val RISK = FolderStyle(
        icon = Icons.Default.Warning,
        iconBackgroundColor = Color(0xFFFFEBEE), // 淡红色背景
        iconColor = Color(0xFFEF4444),           // 红色图标
        tagBackgroundColor = Color(0xFFFEF2F2), // 淡红背景
        tagTextColor = Color(0xFFDC2626)         // 深红色文字
    )
    
    fun getStyle(category: TagCategory): FolderStyle {
        return when (category) {
            TagCategory.INTERESTS -> INTERESTS
            TagCategory.WORK -> WORK
            TagCategory.STRATEGY -> STRATEGY
            TagCategory.RISK -> RISK
        }
    }
}

/**
 * 文件夹样式配置
 */
data class FolderStyle(
    val icon: ImageVector,
    val iconBackgroundColor: Color,
    val iconColor: Color,
    val tagBackgroundColor: Color,
    val tagTextColor: Color
)


/**
 * 智能文件夹卡片组件
 * 
 * iOS风格的分类卡片，采用"智能文件夹"概念设计
 * 
 * 技术要点:
 * - 白色大圆角卡片（16dp圆角）
 * - 左侧32x32pt圆形图标容器
 * - 右侧数量统计和折叠箭头
 * - 内部标签采用同色系配色
 * - 支持折叠/展开动画
 * 
 * @param category 分类类型
 * @param tags 标签列表
 * @param isExpanded 是否展开
 * @param onToggle 折叠/展开回调
 * @param onAddTag 添加标签回调
 * @param onTagClick 标签点击回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 画像库页UI优化
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartFolderCard(
    category: TagCategory,
    tags: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = SmartFolderConfig.getStyle(category)
    
    // 箭头旋转动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 头部：图标 + 标题 + 数量 + 折叠箭头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 圆形图标容器
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = style.iconBackgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 标题
                    Text(
                        text = category.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 数量统计
                    Text(
                        text = "${tags.size}个",
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 折叠箭头
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = iOSTextTertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
            
            // 内容：标签列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    if (tags.isEmpty()) {
                        // 空状态
                        EmptyTagsPlaceholder(
                            onAddClick = onAddTag,
                            style = style
                        )
                    } else {
                        // 标签流式布局
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                PastelTagChip(
                                    text = tag,
                                    backgroundColor = style.tagBackgroundColor,
                                    textColor = style.tagTextColor,
                                    onClick = { onTagClick(tag) }
                                )
                            }
                            
                            // 添加按钮
                            AddTagChip(onClick = onAddTag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 糖果色标签胶囊
 * 
 * @param text 标签文字
 * @param backgroundColor 背景色
 * @param textColor 文字色
 * @param onClick 点击回调
 */
@Composable
fun PastelTagChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/**
 * 虚线边框添加按钮
 */
@Composable
fun AddTagChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加",
            tint = iOSTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "添加",
            fontSize = 14.sp,
            color = iOSTextSecondary
        )
    }
}

/**
 * 空标签占位符
 */
@Composable
private fun EmptyTagsPlaceholder(
    onAddClick: () -> Unit,
    style: FolderStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAFAFA))
            .clickable(onClick = onAddClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = iOSTextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "暂无记录，点击添加",
            fontSize = 14.sp,
            color = iOSTextTertiary
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "兴趣爱好-展开", showBackground = true)
@Composable
private fun SmartFolderCardInterestsExpandedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SmartFolderCard(
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
private fun SmartFolderCardWorkCollapsedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SmartFolderCard(
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
private fun SmartFolderCardRiskExpandedPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SmartFolderCard(
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

@Preview(name = "空状态", showBackground = true)
@Composable
private fun SmartFolderCardEmptyPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SmartFolderCard(
                category = TagCategory.STRATEGY,
                tags = emptyList(),
                isExpanded = true,
                onToggle = {},
                onAddTag = {},
                onTagClick = {}
            )
        }
    }
}
