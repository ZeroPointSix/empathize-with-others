package com.empathy.ai.presentation.ui.screen.contact

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.list.ContactListItem
import com.empathy.ai.presentation.ui.component.state.EmptyView
import com.empathy.ai.presentation.ui.component.state.ErrorView
import com.empathy.ai.presentation.ui.component.state.LoadingIndicator
import com.empathy.ai.presentation.ui.component.state.LoadingIndicatorFullScreen
import com.empathy.ai.presentation.viewmodel.ContactListViewModel

/**
 * 联系人列表页面
 *
 * 功能：
 * - 显示所有联系人列表
 * - 支持搜索联系人
 * - 点击跳转到详情页
 * - 支持下拉刷新
 *
 * @param onNavigateToDetail 导航到详情页的回调，参数为联系人ID
 * @param viewModel 联系人列表ViewModel
 * @param modifier Modifier
 */
@Composable
fun ContactListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ContactListScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier
    )
}

/**
 * 联系人列表页面内容（无状态）
 *
 * 分离为无状态组件便于Preview和测试
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactListScreenContent(
    uiState: ContactListUiState,
    onEvent: (ContactListUiEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("联系人") },
                actions = {
                    IconButton(onClick = { onEvent(ContactListUiEvent.StartSearch) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDetail("") }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加联系人"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorFullScreen(
                        message = "加载联系人..."
                    )
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error,
                        onRetry = { onEvent(ContactListUiEvent.LoadContacts) }
                    )
                }
                uiState.isEmptyState -> {
                    EmptyView(
                        message = "还没有联系人",
                        actionText = "添加联系人",
                        onAction = { onNavigateToDetail("") }
                    )
                }
                else -> {
                    ContactList(
                        contacts = uiState.displayContacts,
                        onContactClick = onNavigateToDetail
                    )
                }
            }
        }
    }
}

/**
 * 联系人列表
 */
@Composable
private fun ContactList(
    contacts: List<ContactProfile>,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = contacts,
            key = { it.id }
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = { onContactClick(contact.id) }
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
                        facts = mapOf("职业" to "产品经理", "爱好" to "摄影")
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = mapOf("职业" to "设计师")
                    ),
                    ContactProfile(
                        id = "3",
                        name = "王五",
                        targetGoal = "保持联系",
                        contextDepth = 8,
                        facts = emptyMap()
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = mapOf("职业" to "产品经理", "爱好" to "摄影")
                    ),
                    ContactProfile(
                        id = "2",
                        name = "李四",
                        targetGoal = "成为好朋友",
                        contextDepth = 15,
                        facts = mapOf("职业" to "设计师")
                    ),
                    ContactProfile(
                        id = "3",
                        name = "王五",
                        targetGoal = "保持联系",
                        contextDepth = 8,
                        facts = emptyMap()
                    )
                )
            ),
            onEvent = {},
            onNavigateToDetail = {}
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
            onNavigateToDetail = {}
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
            onNavigateToDetail = {}
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
            onNavigateToDetail = {}
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
                        facts = mapOf("职业" to "产品经理")
                    )
                ),
                filteredContacts = listOf(
                    ContactProfile(
                        id = "1",
                        name = "张三",
                        targetGoal = "建立良好的合作关系",
                        contextDepth = 10,
                        facts = mapOf("职业" to "产品经理")
                    )
                )
            ),
            onEvent = {},
            onNavigateToDetail = {}
        )
    }
}
