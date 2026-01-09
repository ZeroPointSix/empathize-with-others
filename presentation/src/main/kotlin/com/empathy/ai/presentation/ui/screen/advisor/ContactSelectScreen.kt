package com.empathy.ai.presentation.ui.screen.advisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.viewmodel.ContactSelectViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 联系人选择页面
 *
 * 🔴 必须参考原型: 文档/开发文档/UI-原型/PRD29/ai-advisor-home-ios.html
 *
 * ## 业务职责
 * 显示所有联系人列表，支持：
 * - 查看联系人列表
 * - 搜索联系人（P2优先级）
 * - 选择联系人进入对话界面
 *
 * ## 关联文档
 * - PRD-00029: AI军师UI架构优化需求
 * - TDD-00029: AI军师UI架构优化技术设计
 * - FD-00029: AI军师UI架构优化功能设计
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │ [<]      选择联系人                 │  ← iOS导航栏
 * ├─────────────────────────────────────┤
 * │ [🔍 搜索联系人                    ] │  ← 搜索框
 * ├─────────────────────────────────────┤
 * │ 联系人                              │  ← 分组标题
 * ├─────────────────────────────────────┤
 * │ [头像] 张三    亲密      昨天     > │  ← 联系人列表项
 * │        最后一条消息预览...          │
 * ├─────────────────────────────────────┤
 * │ [头像] 李四    普通      3天前   > │
 * │        最后一条消息预览...          │
 * └─────────────────────────────────────┘
 * ```
 *
 * @param onNavigateBack 返回按钮点击回调
 * @param onSelectContact 选择联系人后的导航回调
 * @param viewModel 注入的ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectScreen(
    onNavigateBack: () -> Unit,
    onSelectContact: (contactId: String) -> Unit,
    viewModel: ContactSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimensions = AdaptiveDimensions.current

    // 监听选择状态，触发导航
    LaunchedEffect(uiState.selectedContactId) {
        uiState.selectedContactId?.let { contactId ->
            onSelectContact(contactId)
            viewModel.resetSelection()
        }
    }

    Scaffold(
        containerColor = iOSBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "选择联系人",
                        fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = iOSBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = iOSCardBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchContacts(it) },
                placeholder = "搜索联系人"
            )

            // 分组标题
            Text(
                text = "联系人",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary,
                fontWeight = FontWeight.Normal
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iOSBlue)
                    }
                }
                uiState.isEmpty -> {
                    EmptyContactsView()
                }
                uiState.filteredContacts.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    NoSearchResultsView(query = uiState.searchQuery)
                }
                else -> {
                    ContactList(
                        contacts = uiState.filteredContacts,
                        onContactClick = { viewModel.selectContact(it.id) }
                    )
                }
            }
        }
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val searchBackground = Color(0x1F8E8E93) // rgba(142, 142, 147, 0.12)
    val dimensions = AdaptiveDimensions.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(searchBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(
                fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                color = iOSTextPrimary
            ),
            cursorBrush = SolidColor(iOSBlue),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = iOSTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = iOSTextSecondary,
                                fontSize = dimensions.fontSizeSubtitle  // BUG-00055: 使用响应式字体
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

/**
 * 联系人列表
 */
@Composable
private fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (ContactProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact) }
            )
        }
    }
}

/**
 * 联系人列表项
 *
 * 🔴 必须参考原型样式：
 * - 头像: 44dp方形圆角，彩色背景+姓氏首字
 * - 姓名: 15sp, 黑色
 * - 关系标签: 11sp, 灰色
 * - 时间: 11sp, 灰色
 * - 消息预览: 13sp, 灰色, 单行截断
 */
@Composable
private fun ContactListItem(
    contact: ContactProfile,
    onClick: () -> Unit
) {
    val dimensions = AdaptiveDimensions.current
    
    // 头像颜色方案
    val avatarColors = listOf(
        Color(0xFFE8EAF6) to Color(0xFF5C6BC0), // indigo
        Color(0xFFE3F2FD) to Color(0xFF42A5F5), // blue
        Color(0xFFFCE4EC) to Color(0xFFEC407A), // rose
        Color(0xFFE8F5E9) to Color(0xFF66BB6A), // emerald
        Color(0xFFE0F7FA) to Color(0xFF26C6DA)  // cyan
    )

    // 根据联系人ID选择颜色
    val colorIndex = contact.id.hashCode().let { kotlin.math.abs(it) % avatarColors.size }
    val (bgColor, textColor) = avatarColors[colorIndex]

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(iOSCardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 信息区域
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                            fontWeight = FontWeight.Medium,
                            color = iOSTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contact.getRelationshipLevel().displayName,
                            fontSize = dimensions.fontSizeXSmall,  // BUG-00055: 使用响应式字体
                            color = iOSTextSecondary
                        )
                    }
                    Text(
                        text = contact.lastInteractionDate ?: "未知",
                        fontSize = dimensions.fontSizeXSmall,  // BUG-00055: 使用响应式字体
                        color = iOSTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 最后消息预览
                Text(
                    text = contact.targetGoal.ifEmpty { "暂无消息" },
                    fontSize = dimensions.fontSizeCaption,  // BUG-00055: 使用响应式字体
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }

        // 分隔线
        HorizontalDivider(
            modifier = Modifier.padding(start = 72.dp),
            color = Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyContactsView() {
    val dimensions = AdaptiveDimensions.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 空状态图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iOSBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "暂无联系人",
                fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请先添加联系人",
                fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 搜索无结果视图
 */
@Composable
private fun NoSearchResultsView(query: String) {
    val dimensions = AdaptiveDimensions.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = iOSTextSecondary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "未找到 \"$query\"",
                fontSize = dimensions.fontSizeTitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请尝试其他关键词",
                fontSize = dimensions.fontSizeSubtitle,  // BUG-00055: 使用响应式字体
                color = iOSTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 格式化相对时间
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> {
            val calendar = Calendar.getInstance()
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (timestamp >= todayStart) {
                "今天"
            } else {
                "昨天"
            }
        }
        diff < 2 * 24 * 60 * 60 * 1000 -> "昨天"
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            val days = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        }
        else -> {
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
