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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.presentation.navigation.NavRoutes
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.iOSBackground
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.theme.iOSCardBackground
import com.empathy.ai.presentation.theme.iOSTextPrimary
import com.empathy.ai.presentation.ui.component.list.ContactListItem
import com.empathy.ai.presentation.ui.component.navigation.EmpathyBottomNavigation
import com.empathy.ai.presentation.ui.component.state.ContactListSkeleton
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.FriendlyErrorCard
import com.empathy.ai.presentation.util.UserFriendlyError
import com.empathy.ai.presentation.viewmodel.ContactListViewModel

/**
 * 联系人列表页面
 *
 * 功能：
 * - 显示所有联系人列表
 * - 支持搜索联系人
 * - 点击跳转到详情页
 * - 支持下拉刷新
 * - 集成底部导航栏
 *
 * @param onNavigateToDetail 导航到详情页的回调，参数为联系人ID
 * @param onNavigateToSettings 导航到设置页的回调
 * @param onNavigate 底部导航栏导航回调
 * @param onAddClick 添加按钮点击回调
 * @param currentRoute 当前路由（用于底部导航栏高亮）
 * @param viewModel 联系人列表ViewModel
 * @param modifier Modifier
 */
@Composable
fun ContactListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = { },
    currentRoute: String = NavRoutes.CONTACT_LIST,
    viewModel: ContactListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ContactListScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSettings = onNavigateToSettings,
        onNavigate = onNavigate,
        onAddClick = onAddClick,  // 修复BUG-00031: 使用外部传入的回调，不要覆盖
        currentRoute = currentRoute,
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
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = iOSBackground,
        bottomBar = {
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
                uiState.error != null -> {
                    FriendlyErrorCard(
                        error = UserFriendlyError(
                            title = "出错了",
                            message = uiState.error ?: "未知错误",
                            icon = Icons.Default.Warning
                        ),
                        onAction = { onEvent(ContactListUiEvent.LoadContacts) }
                    )
                }
                uiState.isEmptyState -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // iOS大标题导航栏
                        IOSLargeTitleHeader(
                            title = "联系人",
                            onSearchClick = { onEvent(ContactListUiEvent.StartSearch) }
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
                        onContactClick = onNavigateToDetail,
                        onSearchClick = { onEvent(ContactListUiEvent.StartSearch) }
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
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(iOSBackground)
            .padding(horizontal = 16.dp)
    ) {
        // 顶部工具栏（搜索按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = iOSBlue,
                modifier = Modifier
                    .size(24.dp)
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
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/**
 * 带标题的联系人列表
 */
@Composable
private fun ContactListWithHeader(
    contacts: List<ContactProfile>,
    onContactClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBackground)
    ) {
        // iOS大标题导航栏
        item {
            IOSLargeTitleHeader(
                title = "联系人",
                onSearchClick = onSearchClick
            )
        }

        // 白色圆角卡片容器
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
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
            Spacer(modifier = Modifier.height(24.dp))
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
