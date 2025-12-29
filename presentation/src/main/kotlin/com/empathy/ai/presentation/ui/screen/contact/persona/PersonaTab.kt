package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.TagCategory
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSSystemGroupedBackground
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.persona.InferredTag
import com.empathy.ai.presentation.ui.component.persona.ModernFloatingSearchBar
import com.empathy.ai.presentation.ui.component.state.EmptyType
import com.empathy.ai.presentation.ui.component.state.EmptyView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 画像库标签页组件 (简化版 - iOS风格)
 *
 * 核心功能：
 * - 按Fact.key分类展示所有标签
 * - 支持搜索过滤
 * - 支持长按删除
 * - 支持重置本地状态（搜索、展开、滚动位置）
 * - 无"全部/已确认"分段控制器
 * - 无"雷区/策略"固定分类
 * 
 * BUG-00036 修复：
 * - 使用 rememberSaveable 持久化展开状态和搜索关键词
 * - 使用 LinkedHashMap 保持分类顺序稳定
 * - 添加滚动位置保存和恢复机制
 * - 添加重置功能（T3-05）- 在搜索栏右侧显示重置按钮
 *
 * @param facts 所有事实列表（直接使用Fact模型）
 * @param onFactClick 点击事实回调（用于编辑）
 * @param onFactLongClick 长按事实回调（用于删除）
 * @param showResetButton 是否显示重置按钮（默认true，当有搜索内容或折叠分类时显示）
 * @param modifier Modifier
 */
@Composable
fun PersonaTab(
    facts: List<Fact>,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    showResetButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    // 🆕 使用 rememberSaveable 持久化搜索关键词（配置变更时保持）
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // 🆕 使用 rememberSaveable 持久化展开状态
    var expandedCategories by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    
    // 是否已初始化展开状态
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    
    // 列表状态
    val listState = rememberLazyListState()
    
    // 🆕 保存滚动位置（用于数据变化后恢复）
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    
    // 🆕 监听滚动位置变化并保存
    LaunchedEffect(listState) {
        snapshotFlow { 
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset 
        }
        .distinctUntilChanged()
        .collect { (index, offset) ->
            savedScrollIndex = index
            savedScrollOffset = offset
        }
    }
    
    // 使用 derivedStateOf 优化重组，只有当 facts 或 searchQuery 真正变化时才重新计算
    val groupedFacts by remember(facts, searchQuery) {
        derivedStateOf {
            val filtered = if (searchQuery.isBlank()) {
                facts
            } else {
                facts.filter { 
                    it.key.contains(searchQuery, ignoreCase = true) ||
                    it.value.contains(searchQuery, ignoreCase = true)
                }
            }
            // 🆕 使用 LinkedHashMap 保持插入顺序，避免 toSortedMap() 导致的顺序变化
            // 按 key 分组，保持稳定的顺序
            filtered.groupBy { it.key }
                .entries
                .sortedBy { it.key }  // 按字母顺序排序，但结果是稳定的
                .associate { it.key to it.value }
        }
    }
    
    // 初始化展开状态（仅首次加载时全部展开）
    LaunchedEffect(groupedFacts.keys) {
        if (!isInitialized && groupedFacts.isNotEmpty()) {
            expandedCategories = groupedFacts.keys.toSet()
            isInitialized = true
        }
        // 🆕 新增分类时自动展开
        val newCategories = groupedFacts.keys - expandedCategories
        if (newCategories.isNotEmpty() && isInitialized) {
            expandedCategories = expandedCategories + newCategories
        }
    }
    
    // 🆕 数据变化后恢复滚动位置
    val factsSize = facts.size
    LaunchedEffect(factsSize) {
        if (savedScrollIndex > 0 && factsSize > 0) {
            // 延迟恢复，确保布局完成
            kotlinx.coroutines.delay(50)
            val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.scrollToItem(
                index = savedScrollIndex.coerceAtMost(maxIndex),
                scrollOffset = savedScrollOffset
            )
        }
    }
    
    // 🆕 BUG-00036 T3-05: 重置功能
    // 判断是否需要显示重置按钮（有搜索内容或有折叠的分类）
    val hasCollapsedCategories = groupedFacts.keys.any { it !in expandedCategories }
    val needsReset = searchQuery.isNotEmpty() || hasCollapsedCategories || savedScrollIndex > 0
    
    // 协程作用域用于重置滚动位置
    val coroutineScope = rememberCoroutineScope()
    
    // 重置所有本地状态的函数
    val resetAllState: () -> Unit = {
        searchQuery = ""
        expandedCategories = groupedFacts.keys.toSet()
        savedScrollIndex = 0
        savedScrollOffset = 0
        coroutineScope.launch {
            listState.animateScrollToItem(0)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSSystemGroupedBackground)
    ) {
        // 顶部搜索栏 + 重置按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernFloatingSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "搜索标签或分类",
                modifier = Modifier.weight(1f)
            )
            
            // BUG-00036 修复：显示重置按钮
            if (showResetButton && needsReset) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = resetAllState,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        tint = iOSBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        if (facts.isEmpty()) {
            // 空状态
            EmptyPersonaView(modifier = Modifier.weight(1f))
        } else if (groupedFacts.isEmpty()) {
            // 搜索无结果
            NoSearchResultView(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(dimensions.spacingMediumSmall)
            ) {
                // 按分类展示 - 使用稳定的 key 确保滚动位置不会因为数据变化而重置
                groupedFacts.forEach { (category, categoryFacts) ->
                    item(
                        key = "category_${category.hashCode()}", // 🆕 使用 hashCode 确保 key 稳定
                        contentType = "category_card"  // 🆕 指定内容类型，优化复用
                    ) {
                        SimpleCategoryCard(
                            categoryName = category,
                            facts = categoryFacts,
                            isExpanded = category in expandedCategories,
                            onToggle = {
                                expandedCategories = if (category in expandedCategories) {
                                    expandedCategories - category
                                } else {
                                    expandedCategories + category
                                }
                            },
                            onFactClick = onFactClick,
                            onFactLongClick = onFactLongClick
                        )
                    }
                }
                
                // 底部间距 - 使用固定 key
                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * 简化版分类卡片
 * 
 * iOS风格：白色圆角卡片 + 圆形彩色图标
 * BUG-00036 修复：使用响应式字体尺寸
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimpleCategoryCard(
    categoryName: String,
    facts: List<Fact>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onFactClick: (Fact) -> Unit,
    onFactLongClick: (Fact) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    // 根据分类名生成颜色
    val categoryColor = getCategoryColor(categoryName)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensions.cardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 头部：圆形图标 + 分类名 + 数量 + 展开/折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.spacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 圆形彩色图标
                Box(
                    modifier = Modifier
                        .size(dimensions.iosIconContainerSize)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(categoryName),
                        fontSize = dimensions.fontSizeSubtitle
                    )
                }
                
                Spacer(modifier = Modifier.width(dimensions.spacingMediumSmall))
                
                // 分类名 - 使用响应式字体
                Text(
                    text = categoryName,
                    fontSize = dimensions.fontSizeSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                
                // 数量 - 使用响应式字体
                Text(
                    text = "${facts.size}个",
                    fontSize = dimensions.fontSizeBody,
                    color = iOSTextSecondary
                )
                
                // 展开/折叠按钮
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "折叠" else "展开",
                        tint = iOSTextSecondary
                    )
                }
            }
            
            // 标签内容（展开时显示）
            if (isExpanded) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dimensions.spacingMedium, end = dimensions.spacingMedium, bottom = dimensions.spacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    facts.forEach { fact ->
                        SimpleTagChip(
                            text = fact.value,
                            color = categoryColor,
                            onClick = { onFactClick(fact) },
                            onLongClick = { onFactLongClick(fact) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简化版标签胶囊
 * 
 * 莫兰迪色系：浅色背景 + 深色文字
 * BUG-00036 修复：使用响应式字体尺寸
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleTagChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用响应式尺寸
    val dimensions = AdaptiveDimensions.current
    
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            fontSize = dimensions.fontSizeBody,
            color = color.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = dimensions.spacingMedium, vertical = dimensions.spacingSmall)
        )
    }
}

/**
 * 根据分类名获取颜色
 */
private fun getCategoryColor(categoryName: String): Color {
    return when {
        categoryName.contains("兴趣") || categoryName.contains("爱好") -> Color(0xFFF97316) // 橙色
        categoryName.contains("工作") || categoryName.contains("职业") -> Color(0xFF3B82F6) // 蓝色
        categoryName.contains("性格") || categoryName.contains("特点") -> Color(0xFF8B5CF6) // 紫色
        categoryName.contains("家庭") || categoryName.contains("亲人") -> Color(0xFFEC4899) // 粉色
        categoryName.contains("喜欢") || categoryName.contains("偏好") -> Color(0xFF10B981) // 绿色
        categoryName.contains("不喜欢") || categoryName.contains("禁忌") -> Color(0xFFEF4444) // 红色
        else -> {
            // 根据字符串hash生成稳定的颜色
            val colors = listOf(
                Color(0xFF3B82F6), // 蓝色
                Color(0xFF10B981), // 绿色
                Color(0xFFF97316), // 橙色
                Color(0xFF8B5CF6), // 紫色
                Color(0xFFEC4899), // 粉色
                Color(0xFF06B6D4), // 青色
                Color(0xFFF59E0B), // 琥珀色
                Color(0xFF6366F1)  // 靛蓝色
            )
            colors[Math.abs(categoryName.hashCode()) % colors.size]
        }
    }
}

/**
 * 根据分类名获取Emoji
 */
private fun getCategoryEmoji(categoryName: String): String {
    return when {
        categoryName.contains("兴趣") || categoryName.contains("爱好") -> "🎯"
        categoryName.contains("工作") || categoryName.contains("职业") -> "💼"
        categoryName.contains("性格") || categoryName.contains("特点") -> "✨"
        categoryName.contains("家庭") || categoryName.contains("亲人") -> "👨‍👩‍👧"
        categoryName.contains("喜欢") || categoryName.contains("偏好") -> "❤️"
        categoryName.contains("不喜欢") || categoryName.contains("禁忌") -> "⚠️"
        categoryName.contains("生日") || categoryName.contains("纪念") -> "🎂"
        categoryName.contains("地址") || categoryName.contains("住址") -> "📍"
        categoryName.contains("联系") || categoryName.contains("电话") -> "📱"
        else -> "📋"
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyPersonaView(modifier: Modifier = Modifier) {
    EmptyView(
        message = "暂无标签\n添加事实后会自动显示在这里",
        actionText = null,
        onAction = null,
        modifier = modifier,
        emptyType = EmptyType.NoTags
    )
}

/**
 * 搜索无结果视图
 */
@Composable
private fun NoSearchResultView(modifier: Modifier = Modifier) {
    val dimensions = AdaptiveDimensions.current
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "没有找到匹配的标签",
            fontSize = dimensions.fontSizeBody,
            color = iOSTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ========== 兼容旧版API的重载函数 ==========

/**
 * 兼容旧版API的PersonaTab
 * 
 * 将BrainTag转换为Fact后调用新版PersonaTab
 */
@Composable
fun PersonaTab(
    riskTags: List<BrainTag>,
    strategyTags: List<BrainTag>,
    onConfirmTag: (BrainTag) -> Unit,
    onRejectTag: (BrainTag) -> Unit,
    modifier: Modifier = Modifier,
    interestTags: List<String> = emptyList(),
    workTags: List<String> = emptyList(),
    inferredTags: List<InferredTag> = emptyList(),
    onAcceptInferred: ((InferredTag) -> Unit)? = null,
    onRejectInferred: ((InferredTag) -> Unit)? = null,
    onAcceptAllInferred: (() -> Unit)? = null,
    onAddTag: ((String, TagCategory) -> Unit)? = null
) {
    // 将所有标签转换为Fact列表
    val facts = mutableListOf<Fact>()
    
    // 添加兴趣爱好
    interestTags.forEachIndexed { index, tag ->
        facts.add(Fact(
            id = "interest_$index",
            key = "兴趣爱好",
            value = tag,
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        ))
    }
    
    // 添加工作信息
    workTags.forEachIndexed { index, tag ->
        facts.add(Fact(
            id = "work_$index",
            key = "工作信息",
            value = tag,
            timestamp = System.currentTimeMillis(),
            source = FactSource.MANUAL
        ))
    }
    
    // 添加策略标签
    strategyTags.forEach { tag ->
        facts.add(Fact(
            id = "strategy_${tag.id}",
            key = "沟通策略",
            value = tag.content,
            timestamp = System.currentTimeMillis(),
            source = if (tag.isConfirmed) FactSource.MANUAL else FactSource.AI_INFERRED
        ))
    }
    
    // 添加雷区标签
    riskTags.forEach { tag ->
        facts.add(Fact(
            id = "risk_${tag.id}",
            key = "雷区标签",
            value = tag.content,
            timestamp = System.currentTimeMillis(),
            source = if (tag.isConfirmed) FactSource.MANUAL else FactSource.AI_INFERRED
        ))
    }
    
    PersonaTab(
        facts = facts,
        onFactClick = { fact ->
            // 查找对应的BrainTag并调用确认回调
            val brainTag = riskTags.find { "risk_${it.id}" == fact.id }
                ?: strategyTags.find { "strategy_${it.id}" == fact.id }
            brainTag?.let { onConfirmTag(it) }
        },
        onFactLongClick = { fact ->
            // 查找对应的BrainTag并调用驳回回调
            val brainTag = riskTags.find { "risk_${it.id}" == fact.id }
                ?: strategyTags.find { "strategy_${it.id}" == fact.id }
            brainTag?.let { onRejectTag(it) }
        },
        modifier = modifier
    )
}

// ========== 预览 ==========

@Preview(name = "画像库 - 有数据", showBackground = true)
@Composable
private fun PreviewPersonaTabWithData() {
    EmpathyTheme {
        PersonaTab(
            facts = listOf(
                Fact(id = "1", key = "兴趣爱好", value = "打羽毛球", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "2", key = "兴趣爱好", value = "喜欢爬山", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "3", key = "兴趣爱好", value = "猫奴", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "4", key = "工作信息", value = "大厂员工", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "5", key = "工作信息", value = "产品经理", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "6", key = "性格特点", value = "开朗活泼", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "7", key = "喜欢的食物", value = "川菜", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL),
                Fact(id = "8", key = "喜欢的食物", value = "火锅", timestamp = System.currentTimeMillis(), source = FactSource.MANUAL)
            ),
            onFactClick = {},
            onFactLongClick = {}
        )
    }
}

@Preview(name = "画像库 - 空状态", showBackground = true)
@Composable
private fun PreviewPersonaTabEmpty() {
    EmpathyTheme {
        PersonaTab(
            facts = emptyList(),
            onFactClick = {},
            onFactLongClick = {}
        )
    }
}
