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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSSystemGroupedBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.theme.iOSTextTertiary
import com.empathy.ai.presentation.ui.component.ios.IOSSegmentedControl

/**
 * 现代化画像库标签页
 * 
 * iOS风格重新设计：
 * - 顶部：iOS分段控制器 + 悬浮搜索栏
 * - 卡片：白色圆角卡片 + 圆形图标（无色条）
 * - 标签：莫兰迪色系胶囊标签
 * - 背景：浅灰色系统背景
 * 
 * @see 画像库UI优化需求文档
 */
@Composable
fun ModernPersonaTab(
    categories: List<PersonaCategoryData>,
    modifier: Modifier = Modifier,
    onAddTag: ((TagCategory) -> Unit)? = null,
    onTagClick: ((String, TagCategory) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(0) } // 0=全部, 1=已确认
    var expandedCategories by remember { 
        mutableStateOf(categories.map { it.category }.toSet()) 
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSSystemGroupedBackground)
    ) {
        // 顶部区域：分段控制器 + 搜索栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // iOS分段控制器
            IOSSegmentedControl(
                tabs = listOf("全部标签", "已确认"),
                selectedIndex = viewMode,
                onTabSelected = { viewMode = it }
            )
            
            // 悬浮搜索栏
            ModernFloatingSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "搜索标签或分类"
            )
        }
        
        // 内容区域
        val filteredCategories = categories.map { category ->
            val filteredTags = if (searchQuery.isBlank()) {
                category.tags
            } else {
                category.tags.filter { it.contains(searchQuery, ignoreCase = true) }
            }
            category.copy(tags = filteredTags)
        }.filter { it.tags.isNotEmpty() || searchQuery.isBlank() }
        
        if (filteredCategories.isEmpty() || filteredCategories.all { it.tags.isEmpty() }) {
            EmptyPersonaPlaceholder(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredCategories.forEach { categoryData ->
                    item(key = categoryData.category.name) {
                        ModernFolderCard(
                            category = categoryData.category,
                            tags = categoryData.tags,
                            isExpanded = categoryData.category in expandedCategories,
                            onToggle = {
                                expandedCategories = if (categoryData.category in expandedCategories) {
                                    expandedCategories - categoryData.category
                                } else {
                                    expandedCategories + categoryData.category
                                }
                            },
                            onAddTag = { onAddTag?.invoke(categoryData.category) },
                            onTagClick = { tag -> onTagClick?.invoke(tag, categoryData.category) }
                        )
                    }
                }
                
                // 底部说明
                item {
                    PersonaFooterHint()
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * 画像分类数据
 */
data class PersonaCategoryData(
    val category: TagCategory,
    val tags: List<String>
)

/**
 * 现代化悬浮搜索栏
 * 
 * 设计规范：
 * - 纯白色背景（#FFFFFF）
 * - 圆角12px
 * - 极淡投影（悬浮效果）
 * - 左侧次级搜索图标（iOSTextSecondary）
 */
@Composable
fun ModernFloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "搜索",
            tint = iOSTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    color = iOSTextSecondary
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = iOSTextPrimary
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (query.isNotEmpty()) {
            // BUG-00036 修复：增大点击区域，确保清除按钮可点击
            Box(
                modifier = Modifier
                    .size(32.dp)  // 增大点击区域
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "清除",
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 现代化文件夹卡片
 * 
 * 设计规范：
 * - 白色圆角卡片（16px圆角）
 * - 左侧32x32pt圆形图标容器
 * - 无左侧色条
 * - 莫兰迪色系标签
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernFolderCard(
    category: TagCategory,
    tags: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddTag: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ModernFolderStyle.getStyle(category)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // 头部
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
                            .background(style.iconBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = category.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${tags.size}个",
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
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
            
            // 内容
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
                        EmptyTagsHint(onAddClick = onAddTag)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                MorandiTagChip(
                                    text = tag,
                                    category = category,
                                    onClick = { onTagClick(tag) }
                                )
                            }
                            
                            // 添加按钮
                            DashedAddButton(onClick = onAddTag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 莫兰迪色系标签胶囊
 * 
 * 配色公式：
 * - 背景色：分类主色的10%不透明度
 * - 文字色：分类主色的100%不透明度
 * - 形状：完全圆润胶囊形（Height: 28-32px）
 */
@Composable
fun MorandiTagChip(
    text: String,
    category: TagCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MorandiTagColors.getColors(category)
    
    Text(
        text = text,
        fontSize = 14.sp,
        color = colors.textColor,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/**
 * 莫兰迪标签配色
 */
object MorandiTagColors {
    data class TagColors(
        val backgroundColor: Color,
        val textColor: Color
    )
    
    // 兴趣爱好 - 淡橙色系
    private val INTERESTS = TagColors(
        backgroundColor = Color(0xFFFFF8F0), // 极淡橙
        textColor = Color(0xFFEA580C)        // 深橙
    )
    
    // 工作信息 - 淡蓝色系
    private val WORK = TagColors(
        backgroundColor = Color(0xFFF0F9FF), // 极淡蓝
        textColor = Color(0xFF1D4ED8)        // 深蓝
    )
    
    // 沟通策略 - 淡绿色系
    private val STRATEGY = TagColors(
        backgroundColor = Color(0xFFF0FDF4), // 极淡绿
        textColor = Color(0xFF059669)        // 深绿
    )
    
    // 雷区标签 - 淡红色系
    private val RISK = TagColors(
        backgroundColor = Color(0xFFFEF2F2), // 极淡红
        textColor = Color(0xFFDC2626)        // 深红
    )
    
    fun getColors(category: TagCategory): TagColors {
        return when (category) {
            TagCategory.INTERESTS -> INTERESTS
            TagCategory.WORK -> WORK
            TagCategory.STRATEGY -> STRATEGY
            TagCategory.RISK -> RISK
        }
    }
}

/**
 * 现代化文件夹样式配置
 */
object ModernFolderStyle {
    data class FolderStyle(
        val icon: ImageVector,
        val iconBackground: Color
    )
    
    // 兴趣爱好 - 橙色背景 + 游戏手柄
    private val INTERESTS = FolderStyle(
        icon = Icons.Default.SportsEsports,
        iconBackground = Color(0xFFF97316)
    )
    
    // 工作信息 - 蓝色背景 + 工作图标
    private val WORK = FolderStyle(
        icon = Icons.Default.Work,
        iconBackground = Color(0xFF3B82F6)
    )
    
    // 沟通策略 - 绿色背景 + 灯泡
    private val STRATEGY = FolderStyle(
        icon = Icons.Outlined.Lightbulb,
        iconBackground = Color(0xFF10B981)
    )
    
    // 雷区标签 - 红色背景 + 警告
    private val RISK = FolderStyle(
        icon = Icons.Default.Warning,
        iconBackground = Color(0xFFEF4444)
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
 * 虚线添加按钮
 */
@Composable
fun DashedAddButton(
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
 * 空标签提示
 */
@Composable
private fun EmptyTagsHint(
    onAddClick: () -> Unit,
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

/**
 * 空状态占位符
 */
@Composable
private fun EmptyPersonaPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEmotions,
                contentDescription = null,
                tint = iOSTextTertiary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无标签",
                fontSize = 17.sp,
                color = iOSTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "与联系人互动后，AI会自动推测标签",
                fontSize = 14.sp,
                color = iOSTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 底部提示
 */
@Composable
private fun PersonaFooterHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💡 点击标签可查看详情或编辑",
            fontSize = 13.sp,
            color = iOSTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "完整画像库", showBackground = true)
@Composable
private fun ModernPersonaTabFullPreview() {
    EmpathyTheme {
        ModernPersonaTab(
            categories = listOf(
                PersonaCategoryData(
                    category = TagCategory.INTERESTS,
                    tags = listOf("喜欢旅行", "爱看电影", "健身达人", "美食爱好者")
                ),
                PersonaCategoryData(
                    category = TagCategory.WORK,
                    tags = listOf("产品经理", "互联网行业")
                ),
                PersonaCategoryData(
                    category = TagCategory.STRATEGY,
                    tags = listOf("喜欢收到早安问候", "可能喜欢美食话题")
                ),
                PersonaCategoryData(
                    category = TagCategory.RISK,
                    tags = listOf("不喜欢被催促", "讨厌加班话题")
                )
            )
        )
    }
}

@Preview(name = "空状态", showBackground = true)
@Composable
private fun ModernPersonaTabEmptyPreview() {
    EmpathyTheme {
        ModernPersonaTab(categories = emptyList())
    }
}

@Preview(name = "单个卡片", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun ModernFolderCardPreview() {
    EmpathyTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ModernFolderCard(
                category = TagCategory.INTERESTS,
                tags = listOf("喜欢旅行", "爱看电影", "健身达人"),
                isExpanded = true,
                onToggle = {},
                onAddTag = {},
                onTagClick = {}
            )
        }
    }
}

@Preview(name = "莫兰迪标签", showBackground = true)
@Composable
private fun MorandiTagChipPreview() {
    EmpathyTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorandiTagChip(
                text = "喜欢旅行",
                category = TagCategory.INTERESTS,
                onClick = {}
            )
            MorandiTagChip(
                text = "产品经理",
                category = TagCategory.WORK,
                onClick = {}
            )
            MorandiTagChip(
                text = "不喜欢被催",
                category = TagCategory.RISK,
                onClick = {}
            )
        }
    }
}
