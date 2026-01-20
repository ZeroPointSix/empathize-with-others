package com.empathy.ai.presentation.ui.screen.contact

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.empathy.ai.presentation.theme.AdaptiveDimensions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.ContactSortOption
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.theme.iOSTextSecondary
import com.empathy.ai.presentation.ui.component.ios.IOSSearchBar
import com.empathy.ai.presentation.ui.component.list.ContactListItem
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation
import com.empathy.ai.presentation.ui.component.state.ContactListSkeleton
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.FriendlyErrorCard
import com.empathy.ai.presentation.util.UserFriendlyError
import com.empathy.ai.presentation.viewmodel.ContactListViewModel

/**
 * 联系人列表页面（iOS风格）
 *
 * ## 业务职责
 * 作为应用的核心入口页面之一，提供：
 * - 所有联系人的列表展示
 * - 实时搜索和过滤功能
 * - 点击跳转到联系人详情
 * - 下拉刷新数据
 * - 集成底部导航栏
 *
 * ## 关联文档
 * - PRD-00001: 联系人管理功能需求
 * - TDD-00001: 联系人列表技术设计
 *
 * ## 页面布局
 * ```
 * ┌─────────────────────────────────────┐
 * │                    [🔍]           │  ← 搜索按钮
 * │  联系人                       34sp │  ← iOS大标题
 * ├─────────────────────────────────────┤
 * │  ┌───────────────────────────────┐ │
 * │  │  👤 张三                    >  │ │  ← 白色卡片列表
 *  │  │  建立良好的合作关系          │ │
 *  │  ├───────────────────────────────┤ │
 *  │  │  👤 李四                    >  │ │
 *  │  │  成为好朋友                  │ │
 *  │  └───────────────────────────────┘ │
 * ├─────────────────────────────────────┤
 * │  [联系人]  [AI军师]  [设置]        │  ← 底部导航栏
 * └─────────────────────────────────────┘
 * ```
 *
 * ## 核心设计决策
 * 1. **iOS大标题**: 符合Apple HIG规范，大号标题增强可读性
 * 2. **卡片式列表**: 白色圆角卡片，白色背景，层次分明
 * 3. **骨架屏加载**: 数据加载时显示Skeleton，提升感知性能
 * 4. **状态分离**: Content组件无状态，便于Preview和测试
 *
 * ## 状态驱动的UI
 * - `isLoading`: 显示骨架屏
 * - `error != null`: 显示错误卡片
 * - `isEmptyState`: 显示空状态视图
 * - `else`: 显示联系人列表
 *
 * @param onNavigateToDetail 导航到详情页的回调，参数为联系人ID
 * @param onNavigateToSettings 导航到设置页的回调
 * @param onNavigate 底部导航栏导航回调
 * @param onAddClick 添加按钮点击回调
 * @param currentRoute 当前路由（用于底部导航栏高亮）
 * @param showBottomBar 是否显示底部导航栏
 * @param viewModel 联系人列表ViewModel
 * @param modifier Modifier
 * @see ContactListViewModel 管理页面状态和业务逻辑
 * @see ContactListItem 联系人列表项组件
 */
@Composable
fun ContactListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = { },
    currentRoute: String = NavRoutes.CONTACT_LIST,
    showBottomBar: Boolean = true,
    viewModel: ContactListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // ========== 最近访问刷新逻辑 ==========
    // 使用 LifecycleEventObserver 监听 ON_RESUME 事件
    // 当页面从后台返回前台时，自动刷新最近访问列表
    //
    // 设计决策:
    // - 使用 DisposableEffect 确保生命周期监听正确清理
    // - ON_RESUME 比 LaunchedEffect(onResume) 更可靠
    // - 从详情页返回联系人列表时触发刷新
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(ContactListUiEvent.RefreshRecentContacts)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ContactListScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSettings = onNavigateToSettings,
        onNavigate = onNavigate,
        onAddClick = onAddClick,  // 修复BUG-00031: 使用外部传入的回调，不要覆盖
        currentRoute = currentRoute,
        showBottomBar = showBottomBar,
        modifier = modifier
    )
}

/**
 * 联系人列表页面内容（无状态）
 *
 * 分离为无状态组件便于Preview和测试
 * 采用iOS风格设计：大标题 + 白色圆角卡片列表
 */
@Composable
private fun ContactListScreenContent(
    uiState: ContactListUiState,
    onEvent: (ContactListUiEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    currentRoute: String = NavRoutes.CONTACT_LIST,
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 排序菜单状态提升到父组件，确保在多个地方调用时状态一致
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = iOSBackground,
        bottomBar = {
            if (showBottomBar) {
                EmpathyBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        when (route) {
                            NavRoutes.SETTINGS -> onNavigateToSettings()
                            else -> onNavigate(route)
                        }
                    },
                    onAddClick = onAddClick
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(iOSBackground)
        ) {
            when {
                uiState.isLoading -> {
                    ContactListSkeleton()
                }
                uiState.error != null && uiState.hasLoadedContacts -> {
                    FriendlyErrorCard(
                        error = UserFriendlyError(
                            title = "出错了",
                            message = uiState.error ?: "未知错误",
                            icon = Icons.Default.Warning
                        ),
                        onAction = { onEvent(ContactListUiEvent.LoadContacts) }
                    )
                }
                uiState.isSearching -> {
                    // BUG-00063修复：搜索模式UI
                    SearchModeContent(
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        searchHistory = uiState.searchHistory,
                        onQueryChange = { onEvent(ContactListUiEvent.UpdateSearchQuery(it)) },
                        onSearchClose = { onEvent(ContactListUiEvent.CancelSearch) },
                        onClearHistory = { onEvent(ContactListUiEvent.ClearSearchHistory) },
                        onContactClick = { contactId ->
                            onEvent(ContactListUiEvent.SaveSearchHistory)
                            onNavigateToDetail(contactId)
                        }
                    )
                }
                uiState.isEmptyState -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // iOS大标题导航栏
                        IOSLargeTitleHeader(
                            title = "联系人",
                            sortOption = uiState.sortOption,
                            isSortMenuExpanded = isSortMenuExpanded,
                            onSortMenuExpandedChange = { isSortMenuExpanded = it },
                            onSearchClick = { onEvent(ContactListUiEvent.StartSearch) },
                            onSortOptionSelected = { option ->
                                isSortMenuExpanded = false
                                onEvent(ContactListUiEvent.UpdateSortOption(option))
                            }
                        )
                        // 空状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyView(
                                message = "还没有联系人",
                                actionText = "添加联系人",
                                onAction = { onNavigateToDetail("") }
                            )
                        }
                    }
                }
                else -> {
                    ContactListWithHeader(
                        contacts = uiState.displayContacts,
                        recentContacts = uiState.recentContacts,
                        sortOption = uiState.sortOption,
                        isSortMenuExpanded = isSortMenuExpanded,
                        onSortMenuExpandedChange = { isSortMenuExpanded = it },
                        onContactClick = onNavigateToDetail,
                        onSearchClick = { onEvent(ContactListUiEvent.StartSearch) },
                        onSortOptionSelected = { option ->
                            isSortMenuExpanded = false
                            onEvent(ContactListUiEvent.UpdateSortOption(option))
                        },
                        onClearRecentContacts = { onEvent(ContactListUiEvent.ClearRecentContacts) }
                    )
                }
            }
        }
    }
}

/**
 * iOS大标题导航栏
 */
@Composable
private fun IOSLargeTitleHeader(
    title: String,
    sortOption: ContactSortOption,
    isSortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    // 缓存排序标签文本，避免每次重组都创建新字符串
    val sortLabel = remember(sortOption) { sortOption.toLabel() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBackground)
            .padding(horizontal = dimensions.spacingMedium)
    ) {
        // 顶部工具栏（搜索按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.iosNavigationBarHeight),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "排序",
                    tint = iOSBlue,
                    modifier = Modifier
                        .size(dimensions.iconSizeLarge)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSortMenuExpandedChange(true) }
                        )
                )
                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { onSortMenuExpandedChange(false) }
                ) {
                    // 排序选项列表
                    val sortOptions = listOf(
                        ContactSortOption.NAME to "姓名",
                        ContactSortOption.LAST_INTERACTION to "最近互动",
                        ContactSortOption.RELATIONSHIP_SCORE to "关系分数"
                    )

                    sortOptions.forEach { (option, label) ->
                        SortMenuItem(
                            text = label,
                            isSelected = sortOption == option,
                            onClick = {
                                onSortMenuExpandedChange(false)
                                onSortOptionSelected(option)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(dimensions.spacingSmall))
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = iOSBlue,
                modifier = Modifier
                    .size(dimensions.iconSizeLarge)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSearchClick
                    )
            )
        }
        // iOS大标题
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = iOSTextPrimary,
            modifier = Modifier.padding(bottom = dimensions.spacingXSmall)
        )
        Text(
            text = "排序：$sortLabel",
            fontSize = dimensions.fontSizeCaption,
            color = iOSTextSecondary,
            modifier = Modifier.padding(bottom = dimensions.spacingSmall)
        )
    }
}

@Composable
private fun SortMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        },
        onClick = onClick
    )
}

/**
 * 带标题的联系人列表
 */
@Composable
private fun ContactListWithHeader(
    contacts: List<ContactProfile>,
    recentContacts: List<ContactProfile>,
    sortOption: ContactSortOption,
    isSortMenuExpanded: Boolean,
    onSortMenuExpandedChange: (Boolean) -> Unit,
    onContactClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    onClearRecentContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS大标题导航栏
        item {
            IOSLargeTitleHeader(
                title = "联系人",
                sortOption = sortOption,
                isSortMenuExpanded = isSortMenuExpanded,
                onSortMenuExpandedChange = onSortMenuExpandedChange,
                onSearchClick = onSearchClick,
                onSortOptionSelected = onSortOptionSelected
            )
        }

        if (recentContacts.isNotEmpty()) {
            item {
                RecentContactsSection(
                    contacts = recentContacts,
                    onContactClick = onContactClick,
                    onClearRecentContacts = onClearRecentContacts
                )
            }
            item {
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
            }
        }

        // 白色圆角卡片容器
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingMedium),
                shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                color = iOSCardBackground,
                shadowElevation = 1.dp
            ) {
                Column {
                    contacts.forEachIndexed { index, contact ->
                        ContactListItem(
                            contact = contact,
                            onClick = { onContactClick(contact.id) },
                            showDivider = index < contacts.size - 1
                        )
                    }
                }
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        }
    }
}

/**
 * 联系人列表（旧版，保留兼容）
 */
@Composable
private fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        items(
            count = contacts.size,
            key = { contacts[it].id }
        ) { index ->
            val contact = contacts[index]
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact.id) },
                showDivider = index < contacts.size - 1
            )
        }
    }
}

/**
 * 最近访问联系人区块
 *
 * ## 功能说明
 * 展示用户最近访问的联系人列表，提供快速回访入口。
 * 显示在联系人列表顶部，独立于主列表之外。
 *
 * ## 设计决策
 * - **顶部独立区块**: 与主列表分离，提高可见性
 * - **支持清空**: 提供"清空"按钮重置历史
 * - **点击跳转**: 点击联系人直接进入详情页
 *
 * ## 数据来源
 * 从 [ContactListUiState.recentContacts] 获取数据
 * 数据由 [com.empathy.ai.domain.usecase.RecordContactVisitUseCase] 记录
 *
 * ## 关联文档
 * - FREE-20260119: 最近访问联系人快捷入口
 * - TE-00077: 最近访问联系人测试用例
 *
 * @param contacts 最近访问的联系人列表
 * @param onContactClick 联系人点击事件
 * @param onClearRecentContacts 清空历史事件
 * @param modifier Modifier
 */
@Composable
private fun RecentContactsSection(
    contacts: List<ContactProfile>,
    onContactClick: (String) -> Unit,
    onClearRecentContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近访问",
                fontSize = dimensions.fontSizeBody,
                color = iOSTextSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClearRecentContacts) {
                Text(
                    text = "清空",
                    fontSize = dimensions.fontSizeBody,
                    color = iOSBlue
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingMedium),
            shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
            color = iOSCardBackground,
            shadowElevation = 1.dp
        ) {
            Column {
                contacts.forEachIndexed { index, contact ->
                    ContactListItem(
                        contact = contact,
                        onClick = { onContactClick(contact.id) },
                        showDivider = index < contacts.size - 1
                    )
                }
            }
        }
    }
}

private fun ContactSortOption.toLabel(): String {
    return when (this) {
        ContactSortOption.NAME -> "姓名"
        ContactSortOption.LAST_INTERACTION -> "最近互动"
        ContactSortOption.RELATIONSHIP_SCORE -> "关系分数"
    }
}

// ==================== 搜索模式组件 (BUG-00063) ====================

/**
 * 搜索模式内容
 * 
 * BUG-00063修复：实现联系人搜索功能
 * 
 * 包含：
 * 1. iOS风格搜索栏
 * 2. 搜索结果列表
 * 3. 空结果提示
 * 
 * @param searchQuery 当前搜索关键词
 * @param searchResults 搜索结果列表
 * @param onQueryChange 搜索词变化回调
 * @param onSearchClose 关闭搜索回调
 * @param onContactClick 联系人点击回调
 * @param modifier Modifier
 */
@Composable
private fun SearchModeContent(
    searchQuery: String,
    searchResults: List<ContactProfile>,
    searchHistory: List<String>,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onClearHistory: () -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    val focusRequester = remember { FocusRequester() }
    
    // 自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // 搜索栏
        SearchHeader(
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            onSearchClose = onSearchClose,
            focusRequester = focusRequester
        )
        
        // 搜索结果/搜索历史
        when {
            searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                // 无结果提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyView(
                        message = "未找到匹配的联系人",
                        actionText = null,
                        onAction = {}
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                // 搜索结果列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensions.spacingMedium),
                            shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                            color = iOSCardBackground,
                            shadowElevation = 1.dp
                        ) {
                            Column {
                                searchResults.forEachIndexed { index, contact ->
                                    ContactListItem(
                                        contact = contact,
                                        onClick = { onContactClick(contact.id) },
                                        showDivider = index < searchResults.size - 1,
                                        highlightQuery = searchQuery
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(dimensions.spacingLarge))
                    }
                }
            }
            else -> {
                if (searchHistory.isEmpty()) {
                    // 搜索词为空且无历史
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "输入关键词搜索联系人",
                            color = iOSTextSecondary,
                            fontSize = dimensions.fontSizeBody
                        )
                    }
                } else {
                    SearchHistorySection(
                        history = searchHistory,
                        onHistoryClick = onQueryChange,
                        onClearHistory = onClearHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近搜索",
                    fontSize = dimensions.fontSizeBody,
                    color = iOSTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearHistory) {
                    Text(
                        text = "清空",
                        fontSize = dimensions.fontSizeBody,
                        color = iOSBlue
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingMedium),
                shape = RoundedCornerShape(dimensions.cornerRadiusMedium),
                color = iOSCardBackground,
                shadowElevation = 1.dp
            ) {
                Column {
                    history.forEachIndexed { index, query ->
                        SearchHistoryItem(
                            query = query,
                            showDivider = index < history.size - 1,
                            onClick = { onHistoryClick(query) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(dimensions.spacingLarge))
        }
    }
}

@Composable
private fun SearchHistoryItem(
    query: String,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions.spacingMedium,
                    vertical = dimensions.spacingSmall
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = iOSTextSecondary,
                modifier = Modifier.size(dimensions.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(dimensions.spacingSmall))
            Text(
                text = query,
                fontSize = dimensions.fontSizeBody,
                color = iOSTextPrimary
            )
        }
        if (showDivider) {
            HorizontalDivider(color = iOSBackground)
        }
    }
}

/**
 * 搜索头部
 * 
 * 包含搜索输入框和取消按钮
 * 
 * @param searchQuery 当前搜索关键词
 * @param onQueryChange 搜索词变化回调
 * @param onSearchClose 关闭搜索回调
 * @param focusRequester 焦点请求器
 * @param modifier Modifier
 */
@Composable
private fun SearchHeader(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val dimensions = AdaptiveDimensions.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBackground)
            .padding(
                horizontal = dimensions.spacingMedium,
                vertical = dimensions.spacingSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索输入框
        IOSSearchBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
            placeholder = "搜索联系人",
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
        )
        
        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
        
        // 取消按钮
        TextButton(onClick = onSearchClose) {
            Text(
                text = "取消",
                color = iOSBlue,
                fontSize = dimensions.fontSizeBody
            )
        }
    }
}

// ==================== Previews ====================

@Preview(name = "联系人列表 - 默认", showBackground = true)
@Composable
private fun ContactListScreenPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(
                contacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                            Fact(key = "爱好", value = "摄影", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = listOf(
                            Fact(key = "职业", value = "设计师", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "3",
                        name = "王五",
                        targetGoal = "保持联系",
                        contextDepth = 8,
                        facts = emptyList()
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL),
                            Fact(key = "爱好", value = "摄影", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = listOf(
                            Fact(key = "职业", value = "设计师", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "3",
                        name = "王五",
                        targetGoal = "保持联系",
                        contextDepth = 8,
                        facts = emptyList()
                    )
                )
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "联系人列表 - 加载中", showBackground = true)
@Composable
private fun ContactListScreenLoadingPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(isLoading = true),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "联系人列表 - 空状态", showBackground = true)
@Composable
private fun ContactListScreenEmptyPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "联系人列表 - 错误", showBackground = true)
@Composable
private fun ContactListScreenErrorPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(error = "网络连接失败，请检查网络设置"),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(
    name = "联系人列表 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContactListScreenDarkPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(
                contacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    )
                )
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "联系人列表 - 搜索模式", showBackground = true)
@Composable
private fun ContactListScreenSearchPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(
                contacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = emptyList()
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = emptyList()
                    )
                ),
                isSearching = true,
                searchQuery = "张",
                searchResults = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = listOf(
                            Fact(key = "职业", value = "产品经理", timestamp = System.currentTimeMillis(), source = com.empathy.ai.domain.model.FactSource.MANUAL)
                        )
                    )
                )
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}

@Preview(name = "联系人列表 - 搜索无结果", showBackground = true)
@Composable
private fun ContactListScreenSearchEmptyPreview() {
    EmpathyTheme {
        ContactListScreenContent(
            uiState = ContactListUiState(
                contacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = emptyList()
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = emptyList()
                    )
                ),
                isSearching = true,
                searchQuery = "不存在的联系人",
                searchResults = emptyList()
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigate = {},
            onAddClick = {}
        )
    }
}
